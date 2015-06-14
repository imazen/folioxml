package folioxml.export.plugins;

import folioxml.config.FolderCreation;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.config.YamlInfobaseConfig;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.utils.HtmlEntities;
import folioxml.xml.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;

/**
 * Created by nathanael on 6/13/15.
 */
public class ExportHtmlFiles implements InfobaseSetPlugin {

    protected OutputStreamWriter out;
    private String exportBaseName;
    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException, InvalidMarkupException {
        this.exportBaseName = exportBaseName;
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


        Path htmlPath = Paths.get(exportBaseName + fn.getRelativePath() + ".html");
        Path cssPath = Paths.get(exportBaseName + ".css");
        Path jsPath = Paths.get(exportBaseName + ".js");

        YamlInfobaseConfig.createFoldersInPath(htmlPath.toString(), FolderCreation.CreateParents);
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
        out.append("<link rel='stylesheet' type='text/css' href='" + htmlPath.getParent().relativize(cssPath).toString() + "' />");
        out.append("<script type='text/javascript' src='" + htmlPath.getParent().relativize(jsPath).toString() + "'></script>");

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
