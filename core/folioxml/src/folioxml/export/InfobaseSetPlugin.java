package folioxml.export;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by nathanael on 6/9/15.
 */
public interface InfobaseSetPlugin {
    void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException, InvalidMarkupException;

    void beginInfobase(InfobaseConfig infobase) throws IOException;

    ISlxTokenReader wrapSlxReader(ISlxTokenReader reader);

    void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException;

    void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) throws InvalidMarkupException, IOException;

    void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException;

    void endInfobaseSet(InfobaseSet set) throws IOException;
}
