package folioxml.export.plugins;

import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.html.RenameImages;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;

import java.io.IOException;

/**
 * Created by nathanael on 6/9/15.
 */
public class RenameFiles implements InfobaseSetPlugin {

    RenameImages renamer;
    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export) throws IOException {
        renamer = new RenameImages(set, export);

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

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {
        if  (!xr.isRootRecord()){
            renamer.process(new NodeList(xr), file);
        }
    }



    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        System.out.println("Copying/converting referenced files...");
        renamer.CopyConvertFiles();
    }
}
