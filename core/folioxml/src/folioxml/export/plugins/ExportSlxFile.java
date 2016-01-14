package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.LogStreamProvider;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;


public class ExportSlxFile implements InfobaseSetPlugin {

    protected BufferedWriter out;

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException {


        this.out = Files.newBufferedWriter(export.getLocalPath("export.slx", AssetType.Slx, FolderCreation.CreateParents), Charset.forName("UTF-8"));

    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) {

    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        out.append(clean_slx.toSlxMarkup(false));
    }

    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        return null;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {

    }


    @Override
    public void endInfobase(InfobaseConfig infobase) {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        if (out != null) out.close();
    }
}
