package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.css.StylesheetBuilder;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class ExportCssFile implements InfobaseSetPlugin {

    List<Pair<InfobaseConfig,SlxRecord>> allInfobases = new ArrayList<Pair<InfobaseConfig, SlxRecord>>();

    String cssFile;
    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException, InvalidMarkupException {
        cssFile = exportBaseName + ".css";
    }

    InfobaseConfig current;
    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        current = infobase;
    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        if (clean_slx.isRootRecord()){
            allInfobases.add(new Pair<InfobaseConfig, SlxRecord>(current,clean_slx));
        }
    }

    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        String recordClass = xr.get("class");
        String addClass = "infobase-" + current.getId();
        xr.set("class" , recordClass == null ? addClass : addClass + " " + recordClass);

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

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException {
        OutputStreamWriter out  = new OutputStreamWriter(new FileOutputStream(cssFile), "UTF8");
        try{
            for(Pair<InfobaseConfig, SlxRecord> p : allInfobases){
               out.write(new StylesheetBuilder(p.getSecond()).getCss(".infobase-" + p.getFirst().getId()));
            }

        }finally {
            out.close();
        }
    }
}
