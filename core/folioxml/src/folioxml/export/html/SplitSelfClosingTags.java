package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase.TagType;
import folioxml.export.NodeListProcessor;
import folioxml.xml.IFilter;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

/**
 * 
 * @author nathanael
 *
 */
public class SplitSelfClosingTags implements NodeListProcessor {

	
	public NodeList process(NodeList nodes) throws InvalidMarkupException {
		NodeList tofix = nodes.search(new SelfClosingFilter(),new NodeFilter("div|object|a|p|bookmark|style-def")); //Don't do img, br, hr,..
		
		for (Node n:tofix.list()){
			n.tagType = TagType.Opening; //In an XML hierarchy, there are no closing tags. We don't have to worry about that.
		}

		return nodes;
	}
	
	private class SelfClosingFilter implements IFilter{
		public SelfClosingFilter(){}
		
		public boolean matches(Node n) throws InvalidMarkupException{
			return n.isSelfClosing();
		} 
	}

}
