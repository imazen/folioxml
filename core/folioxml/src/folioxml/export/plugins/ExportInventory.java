package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
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
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException {
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
    public void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) throws InvalidMarkupException, IOException {
        if  (!dirty_slx.isRootRecord()){
            inventory.process(new NodeList(r));
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
