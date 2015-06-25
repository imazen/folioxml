package folioxml.export;

import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public interface InfobaseSetPlugin {
    void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider log) throws IOException, InvalidMarkupException;

    void beginInfobase(InfobaseConfig infobase) throws IOException;

    ISlxTokenReader wrapSlxReader(ISlxTokenReader reader);

    void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException;

    void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException;

    FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx)  throws InvalidMarkupException, IOException;

    void onRecordComplete(XmlRecord xr, FileNode file)  throws InvalidMarkupException, IOException;


    void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException;

    void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException;
}
