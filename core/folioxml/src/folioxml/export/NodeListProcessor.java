package folioxml.export;

import folioxml.core.InvalidMarkupException;
import folioxml.xml.NodeList;

import java.io.IOException;

public interface NodeListProcessor {
	
	public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException;
	

}
