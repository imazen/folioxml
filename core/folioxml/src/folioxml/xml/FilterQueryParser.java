package folioxml.xml;

public class FilterQueryParser {
	
	public FilterQueryParser(){
		
	}
	public IFilter parse(String query){
		//TODO: make a real parser...
		assert(query.indexOf(" ") < 0); //No whitespace allowed.
		
		//Parse basic element.class, .class, or element
		String elementName = query;
		String className = "";
		if (query.indexOf(".") > -1){
			elementName = query.substring(query.indexOf("."));
			className = query.substring(query.indexOf(".") + 1);
		}
		
		if (className.length() > 0 && elementName.length() > 0) return new NodeFilter(elementName,"class",className);
		if (className.length() > 0)return new NodeFilter("class",className);
		if (elementName.length() > 0 )return new NodeFilter(elementName);
		assert(false);
		return null;
	}

}
