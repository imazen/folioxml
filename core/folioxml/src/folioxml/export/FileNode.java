package folioxml.export;

import java.util.Map;


public interface FileNode {

    //The parent, or null.
    FileNode getParent();

    //Includes level, extra IDs, heading, isOnlyHeading
    Map<String, String> getAttributes();

    //Used to store working variables, like slugs, counters,
    Map<String, Object> getBag();

    //Can be hashed to provide an ID if none are present in attributes
    //Must be unique across the infobaseSet
    String getRelativePath();

}
