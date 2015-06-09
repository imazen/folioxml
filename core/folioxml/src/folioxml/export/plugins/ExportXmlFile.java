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


public class ExportXmlFile implements InfobaseSetPlugin {

    public boolean indentXml = true;

    protected OutputStreamWriter out;

    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException {
        out  = new OutputStreamWriter(new FileOutputStream(exportBaseName + ".xml"), "UTF8");

        out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        out.append("<infobases>\n");
    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        out.append("<infobase name=\"" + infobase.getId() + "\">\n");
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
        out.write(r.toXmlString(indentXml));
    }

    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException {
        out.append("</infobase>\n");
    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        out.write("\n</infobases>");
        out.close();
    }
}

