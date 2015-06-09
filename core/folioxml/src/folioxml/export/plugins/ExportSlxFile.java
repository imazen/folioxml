package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.css.StylesheetBuilder;
import folioxml.export.InfobaseSetPlugin;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.*;


public class ExportSlxFile implements InfobaseSetPlugin {

    protected OutputStreamWriter out;

    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws FileNotFoundException, UnsupportedEncodingException {
        this.out  = new OutputStreamWriter(new FileOutputStream(exportBaseName + ".slx"), "UTF8");
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
    public void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) {

    }

    @Override
    public void endInfobase(InfobaseConfig infobase) {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        if (out != null) out.close();
    }
}
