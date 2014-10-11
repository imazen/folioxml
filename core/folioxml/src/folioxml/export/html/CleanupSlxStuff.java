package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.Not;

/**
 * Pulls pagebraks, se, pp, program links, recordHeading spans
 * Removes groups attr from record.
 * Removes the type attribute from all elements (except object-def).
 * Renames record -> div.
 * 
 * @author nathanael
 *
 */
public class CleanupSlxStuff implements NodeListProcessor {

	
	
	public NodeList process(NodeList nodes) throws InvalidMarkupException {
		
		//Remove pagebreaks, proximity markers
		nodes.search(new NodeFilter("pagebreak|se|pp")).pull(); 
		
		//Remove program links
		nodes.search(new NodeFilter("link|a","program",null)).pull(); 
		
		//Remove headings
		nodes.search(new NodeFilter("span","type","recordHeading")).pull();
		
		//Remove groups
		nodes.search(new NodeFilter("record")).removeAttr("groups");
		
		//Removes the type attribute from everything... 
		//nov-30-09 NDJ: Except object-defs!!! Huge hard bug... stopped object refs from resolving. ?What about style-defs? Other stuff that relies on the 
		//type attribute after a record is procesed.
		nodes.search(new NodeFilter("type",null), new Not(new NodeFilter("object-def"))).removeAttr("type");
		
		//Rename the record
		nodes.searchOuter(new NodeFilter("record")).setTagName("div"); //convert records to divs.
		
		
		
		return nodes;
	}
	
}
