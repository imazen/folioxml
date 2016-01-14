package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.LogStreamProvider;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.IOException;


public class ExportMappingsFiles implements InfobaseSetPlugin {

    public ExportMappingsFiles() {
    }

    ExportLocations export;

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.export = export;
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
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {
        if (tool != null)
            tool.outputDataFiles(export.getLocalPath(infobase.getId() + ".mappings.txt", AssetType.Text, FolderCreation.CreateParents).toString());
        tool = null;

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
