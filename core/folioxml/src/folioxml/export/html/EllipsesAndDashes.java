package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeList;
import folioxml.xml.XmlToStringWrapper;

/**
 * Converts "..." to a real ellipsis and "--" to an mdash.
 * @author nathanael
 *
 */
public class EllipsesAndDashes implements NodeListProcessor {

	
	public NodeList process(NodeList nodes) throws InvalidMarkupException {
		//Text search and replace.
		XmlToStringWrapper text = nodes.getStringWrapper(false); //No entity decoding/encoding. We're dealing with it here
		text.replaceAll("--", "&mdash;", false);
		//BUG: When this is used at the start of a sentence, and the previous sentence has no trailing whitespace, and ends with a period...
		//The wrong 3 ellipses will be converted.
		
		//TEMP FIX: Only convert the last three in a series of dots.
		text.replaceAll("\\.\\.\\.(?!\\.)", "&hellip;", false);
		
		return nodes;
	}

}
