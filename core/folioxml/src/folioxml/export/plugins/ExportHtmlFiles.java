package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase;
import folioxml.core.TokenUtils;
import folioxml.export.ExportingNodeListProcessor;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.LogStreamProvider;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.utils.HtmlEntities;
import folioxml.xml.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class ExportHtmlFiles implements InfobaseSetPlugin {

    public ExportHtmlFiles(){}

    public ExportHtmlFiles(boolean addNavLinks, boolean useHighslide){
        this.addNavLinks = addNavLinks;
        this.useHighslide = useHighslide;
    }

    Boolean useHighslide = null;
    Boolean addNavLinks = null;

    protected OutputStreamWriter out;
    private ExportLocations export;
    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.export = export;

        if (useHighslide == null) useHighslide = set.getBool("use_highslide");
        if (useHighslide == null) useHighslide = true;

        if (addNavLinks == null) addNavLinks = set.getBool("add_nav_links");
        if (addNavLinks == null) addNavLinks = true;

    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {

    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        return null;
    }

    FileNode lastFile = null;
    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {
        if (lastFile != file){
            if (lastFile != null && out != null) {
                //New URI
                String newUri = export.getUri(file.getRelativePath(), AssetType.Html, export.getLocalPath(lastFile.getRelativePath(), AssetType.Html, FolderCreation.None));
                writeNextLink(newUri, file);
                closeFile();
            }
            openFile(file, xr);
            if (lastFile != null){
                String previousUrl = export.getUri(lastFile.getRelativePath(), AssetType.Html,export.getLocalPath(file.getRelativePath(),AssetType.Html, FolderCreation.None));
                writePrevLink(previousUrl, lastFile);
            }
            lastFile = file;
        }
        //Let's not write the root record, mkay?
        if (!xr.isRootRecord()) out.write(xr.toXmlString(false));
    }

    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException {
        closeFile();
    }

    private void openFile(FileNode fn, XmlRecord xr) throws IOException {
        XmlRecord root = xr.getRoot();

        if (out != null) throw new IOException(); //Invalid state

        List<String> cssUris = new ArrayList<String>();
        List<String> jsUris = new ArrayList<String>();


        String filename = fn.getRelativePath();
        Path htmlPath = export.getLocalPath(filename, AssetType.Html, FolderCreation.CreateParents);
        if (htmlPath.toFile().exists()) throw new FileAlreadyExistsException(htmlPath.toString());


        cssUris.add(export.getUri("foliostyle.css", AssetType.Css, htmlPath));

        //Add highslide javascript/css
        String highslideFolder = export.getUri("highslide/", AssetType.Javascript, htmlPath) + "/";

        if (useHighslide) {
            jsUris.add(URI.create(highslideFolder).resolve("highslide-with-html.js").toString());
            cssUris.add(URI.create(highslideFolder).resolve("highslide.css").toString());
        }


        out  = new OutputStreamWriter(new FileOutputStream(htmlPath.toFile()), "UTF8");

        out.append("<!DOCTYPE html>\n");
        openElement("html");
        openElement("head");
        writeIndent();
        out.append("<meta charset=\"utf-8\" />\n");
        openElement("title");
        out.write(TokenUtils.lightEntityEncode(fn.getAttributes().get("heading")));
        closeElement("title");
        if (fn.getBag().get("folio-id") != null){
            writeIndent();
            out.append("<meta data-first-folio-id=\"" + fn.getBag().get("folio-id").toString() + "\" />\n");
        }
        if (fn.getBag().get("folio-level") != null){
            writeIndent();
            out.append("<meta data-folio-level=\"" + fn.getBag().get("folio-level").toString() + "\" />\n");
        }

        writeIndent();
        for (String uri: cssUris)
            out.append("<link rel='stylesheet' type='text/css' href='" + uri + "' />\n");
        for (String uri: jsUris)
            out.append("<script type='text/javascript' src='" + uri+ "'></script>\n");

        if (useHighslide)
            out.append("<script type=\"text/javascript\">hs.graphicsDir = '" + URI.create(highslideFolder).resolve("graphics") + "/';</script>\n");

        closeElement("head");
        openElement("body");
    }
    private void closeFile() throws IOException {
        closeElement("body");
        closeElement("html");
        out.close();
        out = null;
    }

    private boolean indentXml = true;
    private int indentLevel = 0;
    private String indentString = "  ";
    private void writeIndent() throws IOException {
        for (int i = 0; i < indentLevel; i++){
            out.append(indentString);
        }
    }
    private void openElement(String elementName) throws IOException {
        writeIndent();
        out.append("<");
        out.append(elementName);
        out.append(">\n");
        indentLevel++;
    }
    private void openElement(Node element) throws IOException {
        StringBuilder sb = new StringBuilder();
        writeIndent();
        element.writeTokenTo(sb);
        out.append(sb);
        out.append("\n");
        indentLevel++;
    }
    private void closeElement(String elementName) throws IOException {
        indentLevel--;
        writeIndent();
        out.append("</");
        out.append(elementName);
        out.append(">\n");
    }

    private void writeLink(String uri, String text) throws IOException, InvalidMarkupException {
        Node a = new Node("<a>" + TokenUtils.lightEntityEncode(text) + "</a>");
        a.setTagName("a");
        a.addClass("folio_pagination_link");
        a.set("href", uri);


        out.write(a.toXmlString(false));
    }


    private void writeNextLink(String uri, FileNode target) throws IOException, InvalidMarkupException {
        writeLink(uri, "Next: " + target.getAttributes().get("heading"));
    }

    private void writePrevLink(String uri, FileNode target) throws IOException, InvalidMarkupException {

        writeLink(uri, "Prev: " + target.getAttributes().get("heading"));

    }

}
