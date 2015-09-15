package folioxml.export;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.xml.XmlRecord;


public interface NodeInfoProvider {

    boolean separateInfobases(InfobaseConfig ic, InfobaseSet set);

    boolean startNewFile(XmlRecord r) throws InvalidMarkupException;

    void PopulateNodeInfo(XmlRecord r, FileNode fn) throws InvalidMarkupException;

    String getRelativePathFor(FileNode fn);
}
