package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.UUID;

public class Notes implements NodeListProcessor {

    private String imageDataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAABHVBMVEVMaXH8+tH37pj69bpuZiP375vz53WmmymsoCqxpSpsZCOimCn48qn38aJ0ayRxaST375/48qz06X/y5Wx5cCX59bz27I38+dG0qCv59Lf164r38aX06oH06Hf48af59LT5867UuFOckSmijSKflCj7+Mr69r/698H253z27pb17It1biWDeSZ/dSWIfiaTgiHy5WqTgiv69r706oLSxmLo2WiUhjLNslLEsjqWhCX7+Mz587H7+Mf8+c6pnir8+tP798To2F9oXRrNsEb14FuMgC2ymSDy5m2fjDjCtEj69Lb698KWiyjNslSNgieSiCdnXR7Pw1eZjih8cybk1lqGfzKxpUWAdRt9cyHMwV+HfSaroUXx42Ty42VsYyNrt5qTAAAAAXRSTlMAQObYZgAAAMVJREFUeF6NyFOCA0EABcB0j23Htrm2bd//GHmzJ0h9VmYDYShJorgnCIpSLZcR0v4/EgSEkBuESILd1FYN7hFCbQcOUteNO4TSqNfHY89jGCaK+ojqVVQobEOxaFldxJOlqmo+nySappk9xCs1TY5yHKW01XpGfLw1myfL2Pfb7Th2Ef33QeWsMp1ks7Zt64ivz8Ft6Xg0y+V4njcQXf774aK0mB+eO47DInrD4aNxNFqdXr78/MoI1+10dN2QWZb9k+XMGu/iHKaE4S6MAAAAAElFTkSuQmCC";


    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
        NodeList objects = nodes.filterByTagName("note", true);
        for (Node t : objects.list()) {
            String id = "note_" + UUID.randomUUID().toString();
            String heading = t.get("title").replace("'", "\\'"); //TODO: AND html encoding is needed too...
            Node link = new Node("<a class=\"highslide noteIcon\" href=\"#\" onclick=\"return hs.htmlExpand(this, { maincontentId: '" + id + "', headingText: '" + heading + "'})\"><img src=\"" + imageDataUri + "\" alt=\"Notes\" /></a>");
            t.insertBeforeThis(link);
            t.setTagName("div", true);
            t.set("class", "highslide-maincontent");
            t.set("id", id);
            t.removeAttr("width");
            t.removeAttr("height");
            //And move outside the parent paragraph.... but don't move outside a context node.
            Node insertAfter = t.ancestors().sublist(null, t.ancestors().filter(new NodeFilter("infobase-meta|record|note|popup|namedPopup")).first(false)).filter(new NodeFilter("p")).first();
            if (insertAfter != null) insertAfter.insertBeforeThis(t.remove(false));
        }
        return nodes;
    }

}
