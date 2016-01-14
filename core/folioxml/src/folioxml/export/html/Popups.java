package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.util.UUID;

/**
 * CleanupSlxStuff must be run *after* this!
 *
 * @author nathanael
 */
public class Popups implements NodeListProcessor {

	/*
    <a href="index.htm" onclick="return hs.htmlExpand(this, { headingText: 'Lorem ipsum' })">
	Open HTML-content
</a>
<div class="highslide-maincontent">Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aliquam dapibus leo quis nisl. In lectus. Vivamus consectetuer pede in nisl. Mauris cursus pretium mauris. Suspendisse condimentum mi ac tellus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Donec sed enim. Ut vel ipsum. Cras consequat velit et justo. Donec mollis, mi at tincidunt vehicula, nisl mi luctus risus, quis scelerisque arcu nibh ac nisi. Sed risus. Curabitur urna. Aliquam vitae nisl. Quisque imperdiet semper justo. Pellentesque nonummy pretium tellus.
</div>
*/

    //Needs to handle nesting properly.. and DIVs need to be placed outside paragraphs
    //<note height="3in" title="Tape Note" width="5in">
    //TODO: Doesn't support named popups

    public NodeList process(NodeList nodes) throws InvalidMarkupException {
        NodeList objects = nodes.search(new NodeFilter("a|link", "type", "popup"));
        for (Node t : objects.list()) {
            Node popup = objects.search(new NodeFilter("popup")).first();
            if (popup == null) continue; //TODO: Log and warn about this...

            String id = "popup_" + UUID.randomUUID().toString();
            String heading = popup.get("title") != null ? popup.get("title").replace("'", "\\'") : ""; //TODO: AND html encoding is needed too...
            t.addClass("highslide popupLink");
            t.set("href", "#");
            t.set("onclick", "return hs.htmlExpand(this, { maincontentId: '" + id + "', headingText: '" + heading + "'})");
            t.setTagName("a"); //Rename to html link

            //Now... change the popup top a div.
            popup.setTagName("div", true);
            popup.set("class", "highslide-maincontent");
            popup.set("id", id);
            popup.removeAttr("width");
            popup.removeAttr("height");
            //And move outside the parent paragraph.... but don't move outside a context node.
            Node insertAfter = t.ancestors().sublist(null, t.ancestors().filter(new NodeFilter("infobase-meta|record|note|popup|namedPopup")).first(false)).filter(new NodeFilter("p")).first();
            if (insertAfter == null) insertAfter = t;
            insertAfter.insertAfterThis(popup.remove(false));
        }
        return nodes;
    }

}
