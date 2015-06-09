package folioxml.export;

import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

/**
 * Created by nathanael on 6/9/15.
 */
public interface StatefulStructureProvider {


    void beginInfobaseSet();

    void beginInfobase(String infobase_identifier);


    void onSlxRecordParsed(SlxRecord clean_slx);

    void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r);


    void endInfobase();

    void endInfobaseSet();




    //infobasechanged
    //nextRecord


    //apply structure to Xml node

    //I suppose we might need these for structural queries. Should they reflect Folio hierarchy or an arbitrary structure.

    //localPath
    //parentPath
    //parentPaths
    //fullPath

}
