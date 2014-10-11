package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeList;

import java.io.IOException;

public class MultiRunner implements NodeListProcessor {

	public NodeListProcessor[] filters = null;
	public MultiRunner(NodeListProcessor ... filters){
		this.filters = filters;
	}
	
	
	public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
		for (NodeListProcessor p:filters)
			nodes = p.process(nodes);
		
		return nodes;
	}
	
	public static NodeList process(NodeList nodes,NodeListProcessor ... filters) throws InvalidMarkupException, IOException {
		for (NodeListProcessor p:filters)
			nodes = p.process(nodes);
		
		return nodes;
	}
	
}
