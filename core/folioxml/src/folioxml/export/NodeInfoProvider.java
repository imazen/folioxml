package folioxml.export;

import folioxml.core.InvalidMarkupException;
import folioxml.xml.XmlRecord;



public interface NodeInfoProvider {
    boolean startNewFile(XmlRecord r) throws InvalidMarkupException;

    void PopulateNodeInfo(XmlRecord r, FileNode fn) throws InvalidMarkupException;

    String getRelativePathFor(FileNode fn);
}
