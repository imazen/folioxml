package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.UUID;

public class Notes implements NodeListProcessor {

	
	public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
		NodeList objects = nodes.filterByTagName("note", true);
		for (Node t:objects.list()){
			String id = UUID.randomUUID().toString();
			String heading = t.get("title").replace("'","\\'"); //TODO: AND html encoding is needed too...
			Node link = new Node("<a class=\"highslide noteIcon\" href=\"#\" onclick=\"return hs.htmlExpand(this, { maincontentId: '" + id + "', headingText: '" + heading + "'})\"><img src=\"images/note.png\" alt=\"Notes\" /></a>");
			t.insertBeforeThis(link);
			t.setTagName("div",true);
			t.set("class","highslide-maincontent");
			t.set("id", id);
			t.removeAttr("width");
			t.removeAttr("height");
			//And move outside the parent paragraph.... but don't move outside a context node.
			Node insertAfter = t.ancestors().sublist(null,t.ancestors().filter(new NodeFilter("infobase-meta|record|note|popup|namedPopup")).first(false)).filter(new NodeFilter("p")).first();
			if (insertAfter != null) insertAfter.insertBeforeThis(t.remove(false));
		}
		return nodes;
	}

}
