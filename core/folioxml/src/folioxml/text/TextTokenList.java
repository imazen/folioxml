package folioxml.text;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase.TokenType;
import folioxml.xml.IFilter;
import folioxml.xml.Node;
import folioxml.xml.NodeList;
import folioxml.xml.TextEntityFilter;

import java.util.ArrayList;

public class TextTokenList extends ArrayList<ITextToken>{
	
	
	public TextTokenList(Node n, IFilter excludeRecursive) throws InvalidMarkupException{
		this.ensureCapacity(new NodeList(n).countMatchingNodes(new TextEntityFilter(1))); //Minimize reallocs.
		addNodeRecursive(n, excludeRecursive);
	}
	/**
	 * Warning! Text may shift across records if you have more than 1 record in the nodelist.
	 * @param n
	 * @param excludeRecursive
	 * @throws InvalidMarkupException
	 */
	public TextTokenList(NodeList n, IFilter excludeRecursive) throws InvalidMarkupException{
		int size = 0;
		size += n.countMatchingNodes(new TextEntityFilter(1));
		this.ensureCapacity(size); //Minimize reallocs.
		
		for (Node c:n.list()) addNodeRecursive(c, excludeRecursive);
		
	}


	/**
	 * Adds all the text nodes within n (or n itself) recursively to the arrays.
	 * @param n
	 * @throws InvalidMarkupException 
	 */
	public void addNodeRecursive(Node n, IFilter excludeRecursive) throws InvalidMarkupException{
		if (excludeRecursive.matches(n)) return; //Skip matches
		
		if (n.isTextOrEntity() && n.markup.length() > 0){
			this.add(new NodeTextTokenWrapper(n));
		}
		if (n.children != null){
			for (Node c:n.children.list()){
				addNodeRecursive(c,excludeRecursive);
			}
		}
	}
	

}
