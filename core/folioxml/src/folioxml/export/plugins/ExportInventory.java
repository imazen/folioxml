package folioxml.export.plugins;

import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.InventoryNodes;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;

import java.io.IOException;


public class ExportInventory implements InfobaseSetPlugin {

    public ExportInventory(){}

    InventoryNodes inventory = null;


    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export) throws IOException, InvalidMarkupException {
        inventory = new InventoryNodes();
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
            inventory.process(new NodeList(xr));
        }
    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        inventory.PrintStats();
        inventory.PrintExternalInfobases(set);
        inventory.PrintUniques();
    }
}
