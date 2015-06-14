package folioxml.export.deprecated;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

/**
 * Fixes jump links and bookmarks, turning them into 'a' tags and using the hashes of their IDs for HTML/XML validity.
 * Strips cross-infobase jump links.
 * @author nathanael
 *
 */
public class BookmarksAndJumpLinks implements NodeListProcessor {

    public static String hashPath(String path)  {

        //djl 09-22-2010 added in underscore since they can't start with #s or spaces
        return "z" + Integer.toHexString(path.hashCode());
    }

	public NodeList process(NodeList nodes) throws InvalidMarkupException {
		//Fix jump destinations and bookmarks - use hash instead of original name
		//TODO: Jump destinations often exist inside link tags. HTML doesn't handle this well (I don't think). Move them outside.
		
		for (Node n:nodes.search(new NodeFilter("bookmark")).list())
			n.setTagName("a").set("id", hashPath(n.get("name")));
			
		
		
		//Convert local jump links, delete cross-infobase links
		NodeList jumpLinks = nodes.search(new NodeFilter("link","jumpDestination",null));
		for (Node n:jumpLinks.list()){
			if (n.get("infobase") != null){
				//Whoa... cross infobase links not supported.
				n.pull();
			}else{
				n.setTagName("a");
				n.set("href", "#" + hashPath(n.get("jumpDestination")));
			}
			
		}
		return nodes;
	}

}
