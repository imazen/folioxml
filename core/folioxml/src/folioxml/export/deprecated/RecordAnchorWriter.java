package folioxml.export.deprecated;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.io.IOException;

/**
 * Inserts an anchor at the beginning of each record with id="rid[folioId]"
 *
 * @author nathanael
 */
public class RecordAnchorWriter implements NodeListProcessor {


    public NodeList process(NodeList nodes) throws InvalidMarkupException {


        //Rename the record
        NodeList results = nodes.searchOuter(new NodeFilter("record"));
        for (Node n : results.list()) {
            String rid = n.get("folioId").toLowerCase();
            if (rid != null) {
                n.set("id", "rid" + rid);
                Node c;
                try {
                    c = new Node("<a id=\"rid" + n.get("folioId") + "\" ></a>");
                    n.addChild(c, 0);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }


        return nodes;
    }

}
