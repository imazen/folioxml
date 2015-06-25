package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class ExportHtmlFiles implements InfobaseSetPlugin {

    protected OutputStreamWriter out;
    private ExportLocations export;
    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.export = export;
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
            if (lastFile != null) closeFile();
            openFile(file, xr);
            lastFile = file;
        }
        out.write(xr.toXmlString(false));
    }

    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {
        closeFile();
    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException {

    }

    private void openFile(FileNode fn, XmlRecord xr) throws IOException {
        XmlRecord root = xr.getRoot();

        if (out != null) throw new IOException(); //Invalid state

        List<String> cssUris = new ArrayList<String>();
        List<String> jsUris = new ArrayList<String>();


        String filename = fn.getRelativePath();
        if (filename.length() == 0 && xr == root) filename = "_xmlroot_info";

        Path htmlPath = export.getLocalPath(filename , AssetType.Html, FolderCreation.CreateParents);


        cssUris.add(export.getUri("foliostyle.css", AssetType.Css, htmlPath));

        //Add highslide javascript/css
        String highslideFolder = export.getUri("highslide/", AssetType.Javascript, htmlPath) + "/";

        jsUris.add(URI.create(highslideFolder).resolve("highslide-with-html.js").toString());
        cssUris.add(URI.create(highslideFolder).resolve("highslide.css").toString());



        out  = new OutputStreamWriter(new FileOutputStream(htmlPath.toFile()), "UTF8");

        out.append("<!DOCTYPE html>\n");
        openElement("html");
        openElement("head");
        writeIndent();
        out.append("<meta charset=\"utf-8\" />\n");
        openElement("title");
        out.write(TokenUtils.lightEntityEncode(fn.getAttributes().get("heading")));
        closeElement("title");
        writeIndent();
        for (String uri: cssUris)
            out.append("<link rel='stylesheet' type='text/css' href='" + uri + "' />\n");
        for (String uri: jsUris)
            out.append("<script type='text/javascript' src='" + uri+ "'></script>\n");

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

}
