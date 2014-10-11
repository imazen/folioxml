package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;

public  class NodeFilter implements IFilter{
	
	public NodeFilter(){}
	
	
	public NodeFilter(String tagName){
		tagNameRegex = tagName;
	}
	
	public NodeFilter(String attrName, String attrValRegex){
		this.attrName = attrName;
		this.attrValRegex = attrValRegex;
	}
	
	public NodeFilter(String tagName, String attrName, String attrValRegex){
		tagNameRegex = tagName;
		this.attrName = attrName;
		this.attrValRegex = attrValRegex;
	}
	
	String tagNameRegex = null;
	String attrName = null;
	String attrValRegex = null;
	

	public boolean matches(Node n) throws InvalidMarkupException{
		if (tagNameRegex != null){
			if (!TokenUtils.fastMatches(tagNameRegex, n.getTagNameSilent())) return false;
		}
		if (attrName != null){
			String val = n.get(attrName);
			if (val == null) return false;
			if (attrValRegex != null){
				if (!TokenUtils.fastMatches(attrValRegex,val)) return false;
			}
		}
		return true;
	}

}
