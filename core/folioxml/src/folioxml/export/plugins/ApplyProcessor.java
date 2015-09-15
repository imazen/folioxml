package folioxml.export.plugins;

import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.*;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;

import java.io.IOException;


public class ApplyProcessor implements InfobaseSetPlugin {

    NodeListProcessor processor;

    public ApplyProcessor(NodeListProcessor p) {
        this.processor = p;
    }


    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        if (processor instanceof ExportingNodeListProcessor) {
            ExportingNodeListProcessor enlp = (ExportingNodeListProcessor) processor;
            enlp.setExportLocations(export);
            enlp.setLogProvider(logs);
        }
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
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        return null;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {
        if (!xr.isRootRecord()) {
            if (processor instanceof ExportingNodeListProcessor) {
                ExportingNodeListProcessor enlp = (ExportingNodeListProcessor) processor;
                enlp.setFileNode(file);
            }
            processor.process(new NodeList(xr));
        }
    }

    @Override
    public void onRecordTransformed(XmlRecord r, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
