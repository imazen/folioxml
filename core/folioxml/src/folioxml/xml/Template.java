package folioxml.xml;

import java.util.ArrayList;
import java.util.List;

public class Template {
	
	private String text = null;
	
	List<Expression> expressions = new ArrayList<Expression>();
	
	public Template(String code){
		this.text = code;
		
		//parse();
	}
	
	
	
	public String execute(Node context, String ... params){
		//{query} (xml results)
		//[query] (text results)
		// $1, $2, etc... params
		return null;
		
	}

}
 class Expression{
	
	private String text = null;
	private IFilter filter = null;
	private boolean plainTextOutput = false;
	
	
}