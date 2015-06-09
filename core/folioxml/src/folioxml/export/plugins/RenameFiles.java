package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
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
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException {
        renamer = new RenameImages(set);
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
    public void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) throws InvalidMarkupException, IOException {
        if  (!dirty_slx.isRootRecord()){
            renamer.process(new NodeList(r));
        }
    }

    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        System.out.println("Copying referenced files...");
        renamer.CopyFiles();
        System.out.println("Converting bitmaps to png...");
        renamer.CompressFiles();
    }
}
