package folioxml.text;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase.TokenType;
import folioxml.xml.IFilter;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.util.ArrayList;

public class TextTokenList extends ArrayList<ITextToken>{
	
	
	public TextTokenList(Node n, IFilter excludeRecursive) throws InvalidMarkupException{
		this.ensureCapacity(countTextNodes(n)); //Minimize reallocs.
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
		for (Node c:n.list()) size += countTextNodes(c);
		this.ensureCapacity(size); //Minimize reallocs.
		
		for (Node c:n.list()) addNodeRecursive(c, excludeRecursive);
		
	}

	/**
	 * Counts the number of non-empty text nodes so the arrays can be created appropriately.
	 * @param n
	 * @return
	 */
	private int countTextNodes(Node n){
		//Local length
		int len = (n.isTextOrEntity() && n.markup.length() > 0)  ? 1 : 0;
		if (n.children == null) return len;
		//Recursive.
		for (Node c:n.children.list())
			len += countTextNodes(c);	
		return len;
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
	

	
	 class NodeTextTokenWrapper implements ITextToken{
		private Node n;
		public NodeTextTokenWrapper(Node n){
			this.n = n;
		}
		public String getText(){
			return n.markup;
		}
		public void setText(String s){
			n.markup = s;
			n.type = TokenType.Text;
			if (s.length() == 0){
				//Delete
				n.remove(true); //Delete token from parent.
			}
		}
	}
}
