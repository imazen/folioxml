package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.css.StylesheetBuilder;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.LogStreamProvider;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ExportCssFile implements InfobaseSetPlugin {

    List<Pair<InfobaseConfig,SlxRecord>> allInfobases = new ArrayList<Pair<InfobaseConfig, SlxRecord>>();

    public static String CSS_FILE_NAME = "foliostyle.css";

    String cssFile;
    InfobaseSet set;
    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        cssFile = export.getLocalPath(CSS_FILE_NAME, AssetType.Css, FolderCreation.CreateParents).toString();
        this.set = set;

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
            if (set.getInfobases().size() == 1){
                flushCss(); //We can write CSS immediately when there is only one infobase.
            }
        }
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
        String recordClass = xr.get("class");
        String addClass = "infobase-" + current.getId();
        xr.set("class" , recordClass == null ? addClass : addClass + " " + recordClass);

    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    private boolean css_flushed = false;

    private void flushCss() throws IOException, InvalidMarkupException {
        if (css_flushed) return;
        OutputStreamWriter out  = new OutputStreamWriter(new FileOutputStream(cssFile), "UTF8");
        try{
            for(Pair<InfobaseConfig, SlxRecord> p : allInfobases){
                out.write(new StylesheetBuilder(p.getSecond()).getCss(".infobase-" + p.getFirst().getId(),true));
            }

        }finally {
            out.close();
        }
        css_flushed = true;
    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException {
        flushCss();
    }
}
