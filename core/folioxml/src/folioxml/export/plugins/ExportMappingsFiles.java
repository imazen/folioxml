package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.export.InfobaseSetPlugin;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.IOException;

/**
 * Created by nathanael on 6/9/15.
 */
public class ExportMappingsFiles implements InfobaseSetPlugin {

    public ExportMappingsFiles(){}
    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException {

    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {

    }

    FolioToSlxDiagnosticTool tool = null;
    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        tool = new FolioToSlxDiagnosticTool(reader);
        return tool;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) throws InvalidMarkupException, IOException {

    }

    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {
        if (tool != null) tool.outputDataFiles(infobase.generateExportBaseFile() + ".mappings.txt");
        tool = null;

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
