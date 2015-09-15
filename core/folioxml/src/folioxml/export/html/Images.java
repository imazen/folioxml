package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

public class Images implements NodeListProcessor {

    public NodeList process(NodeList nodes) throws InvalidMarkupException {
        NodeList objects = nodes.filterByTagName("object", true);
        for (Node t : objects.list()) {
            String handler = t.get("handler");
            if (TokenUtils.fastMatches("bitmap|metafile|picture", handler)) { //Convert these three types to "img" tags immediately.
                t.setTagName("img");
                t.removeAttr("type");
                t.removeAttr("handler");
                t.set("alt", t.get("name")); //The alt tag can use the name
                t.removeAttr("name");

            }
        }
        return nodes;
    }

}
