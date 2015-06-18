package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NodeList {

	private ArrayList<Node> a = null;
	private Node parent = null;


	public NodeList() {
		a = new ArrayList<Node>();
	}

	public NodeList(int startingCapacity) {
		a = new ArrayList<Node>(startingCapacity);
	}

	/**
	 * Wraps the specified node list...
	 *
	 * @param data
	 */
	public NodeList(ArrayList<Node> data, boolean useListDirectly) {
		if (useListDirectly) {
			a = data;
		} else {
			a = new ArrayList<Node>(data.size());
			a.addAll(data);
		}
	}

	public NodeList(List<Node> data) {
		a = new ArrayList<Node>(data.size());
		a.addAll(data);
	}

	public NodeList(Node aChild) {
		a = new ArrayList<Node>();
		a.add(aChild);
	}

	public NodeList(String xml) throws IOException, InvalidMarkupException {
		a = new ArrayList<Node>(); //There may only be one root node - in fact, this is likely. No need for guesses.
		addAll(new XmlTokenReader(new StringReader(xml)));
	}

	/**
	 * Reads tokens from the reader into this list until the reader is empty. Constructs the XML tree properly.
	 * Remember this will build a DOM - only use for small bits that can fit into memory.
	 *
	 * @param reader
	 * @throws IOException
	 * @throws InvalidMarkupException
	 */
	public NodeList(IXmlTokenReader reader) throws IOException, InvalidMarkupException {
		this();
		addAll(reader);
	}


	/**
	 * Always call this when NodeList is a child of a Node.
	 * Doesn't modify the .parent propery of children.
	 *
	 * @param parent
	 * @return
	 */
	public NodeList setParent(Node parent) {
		this.parent = parent;
		return this;
	}


	public NodeList addAll(IXmlTokenReader reader) throws IOException, InvalidMarkupException {
		addUntil(reader, null);
		return this;
	}

	/**
	 * Dangerous - keeps contents in memory.
	 *
	 * @param reader
	 * @param closingTag
	 * @return
	 * @throws IOException
	 * @throws InvalidMarkupException
	 */
	public NodeList addUntil(IXmlTokenReader reader, String closingTag) throws IOException, InvalidMarkupException {
		while (reader.canRead()) {
			XmlToken r = reader.read();
			if (r == null) break;
			//Closing tags should not be present at this level. Node.ctor should be handling them, if they are paired properly.
			if (r.isTag() && r.isClosing()) {
				if (closingTag == null)
					throw new InvalidMarkupException("Unexpected closing tag encountered :" + r.toString(), r);

				if (closingTag.equalsIgnoreCase(r.getTagName()))
					return this; //Exit this level
				else
					throw new InvalidMarkupException("Unexpected closing tag encountered :" + r.toString() + ". Expected </" + closingTag + ">.", r); //We hit a unpaired closing tag.
			}
			Node n = new Node(r, reader, false); //If this is an opening tag, the constructor will read the contents and matching closing tag before returning.
			a.add(n); //This doesn't give us great debugging info.. A stack would be a better way ..
			n.parent = this.parent;
		}
		return this;
	}


	public Collection<Node> getCollection() {
		return a;
	}

	public List<Node> list() {
		return a;
	}

	/**
	 * Removes all these items from the tree. The NodeList returned will contain a copy of the deleted items.
	 *
	 * @return
	 */
	public NodeList remove() {
		return this.remove(true);
	}

	/**
	 * Removes these items and their children from the tree. The NodeList returned may or may not be the same instance, but will contain the deleted items.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public NodeList remove(boolean markDeleted) {
		if (parent != null && parent.children == this) {
			NodeList ret = new NodeList((ArrayList<Node>) a.clone());
			for (Node n : a) {
				assert (n.parent == parent);
				n.parent = null;
				if (markDeleted) n.markDeleted();
			}
			a.clear();
			return ret;
		} else {
			for (Node n : a) n.remove(markDeleted);
		}
		return this;
	}


	/**
	 * Removes the specified item from the collection.
	 *
	 * @param n
	 * @return
	 */
	public NodeList remove(Node n) {
		a.remove(n);
		return this;
	}

	/**
	 * Pulls the opening-closing pairs, but leaves the children. Note: If performed on a &lt;td>, &lt;th>, &lt;tr>, &lt;table>, &lt;record>, or &lt;infobase-meta>, invalid markup will result.
	 *
	 * @return
	 */
	public NodeList pull() {
		if (parent != null && parent.children == this) {
			Node[] items = a.toArray(new Node[]{});
			a.clear(); //We clear children. We have a copy
			for (Node n : items) {
				assert (n.parent == parent);
				//Copy grandchildren.
				if (n.children != null) {
					Collection<Node> grandchildren = n.children.getCollection();
					parent.addChildren(grandchildren);
				}
				//Delete backreferences
				n.parent = null;
				n.markDeleted();

			}
		} else {
			for (Node n : a) n.pull(); //One-by-one
		}
		return this;
	}

	/**
	 * Sets the specified attribute value on all tags in the collection.
	 *
	 * @param attrName
	 * @param value
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList set(String attrName, String value) throws InvalidMarkupException {
		for (Node n : a) if (n.isTag()) n.set(attrName, value);
		return this;
	}

	/**
	 * Removes the specified attribute on all tags in the collection.
	 *
	 * @param attrName
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList removeAttr(String attrName) throws InvalidMarkupException {
		for (Node n : a) if (n.isTag()) n.removeAttr(attrName);
		return this;
	}

	/**
	 * Sets the tag name for all tags in the collection
	 *
	 * @param newName
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList setTagName(String newName) throws InvalidMarkupException {
		for (Node n : a) if (n.isTag()) n.setTagName(newName, true);
		return this;
	}


	/**
	 * Returns a new list of all the children of the items in this collection.
	 *
	 * @return
	 */
	public NodeList allChildren() {
		//calculate the list size
		int sizeNeeded = 0;
		for (Node n : a) if (n.children != null) sizeNeeded += n.children.count();

		NodeList nl = new NodeList(sizeNeeded);
		//Build the list
		for (Node n : a) if (n.children != null) nl.list().addAll(n.children.list());

		return nl;
	}


	public NodeList flattenRecursive() {
		//calculate the list size
		int sizeNeeded = a.size();
		for (Node n : a) if (n.children != null) sizeNeeded += n.children.count();


		NodeList nl = new NodeList(sizeNeeded);
		//Build the list
		for (Node n : a) {
			nl.list().add(n);
			if (n.children != null) nl.list().addAll(n.children.flattenRecursive().list());
		}

		return nl;
	}


	//@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		writeTo(sb);
		return sb.toString();
	}

	public void writeTo(StringBuilder sb) {
		for (Node n : a) n.writeXmlTo(sb);
	}

	public String toXmlString(boolean autoIndent) {
		if (autoIndent)
			return new XmlFormatter(0).format(this);
		else {
			return toString();
		}
	}

	/**
	 * Replaces each child with a copy of 'n'
	 *
	 * @param n
	 * @return
	 */
	public NodeList replaceEach(Node newNode) {
		for (Node n : a) {
			Node c = newNode.deepCopy();
			c.parent = n.parent;
			n.parent.children.replace(n, c);
			n.markDeleted();
			n.parent = null;
		}
		return this;
	}

	/**
	 * Replaces instances of node A in the collection with node B. Dosen't modify parent properties
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public NodeList replace(Node A, Node B) {
		for (int i = 0; i < count(); i++) {
			if (a.get(i) == A) a.set(i, B);
		}
		return this;
	}

	public Node first() {
		return count() > 0 ? a.get(0) : null;
	}

	/**
	 * Returns a NodeList containing the first node in this list. Never returns null.
	 *
	 * @return
	 */
	public NodeList firstList() {
		return count() > 0 ? new NodeList(a.get(0)) : new NodeList();
	}

	public Node first(boolean assertCountEquals1) {
		assert (!assertCountEquals1 || count() == 1) : "Only one element is expected here. " + count() + " were found.";
		return first();
	}

	/**
	 * Returns the plaintext contents of the node and its descendants.
	 * TODO: Entity decoding?
	 *
	 * @return
	 */
	public String getTextContents() {
		return writeTextContentsTo(null).toString();
	}

	/**
	 * Writes the text contents of the array to the specified stringbuilder
	 *
	 * @param sb
	 * @return
	 */
	public StringBuilder writeTextContentsTo(StringBuilder sb) {
		if (sb == null) sb = new StringBuilder(estimateTextLength());

		for (Node n : a) {
			if (n.isTextOrEntity()) n.writeTokenTo(sb);
			else if (n.children != null) n.children.writeTextContentsTo(sb);
		}
		return sb;
	}

	/**
	 * Not exact. Performs no entity decoding.
	 *
	 * @return
	 */
	protected int estimateTextLength() {
		int size = 0;
		for (Node n : a) {
			if (n.isTextOrEntity()) size += n.markup.length();
			else if (n.children != null) size += n.children.estimateTextLength();
		}
		return size;
	}

	/**
	 * Returns the text contents between the two nodes (they can be descendants). A and B are not included, neither are their children.
	 * You can pass null to A to search for all nodes prior to B, or pass null to B to search all nodes after A.
	 * <p/>
	 * TODO: Entity decoding?
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public String getTextContentsBetween(Node a, Node b) {
		StringBuilder sb = new StringBuilder();
		writeTextContentsBetweenCore(sb, a, b);
		return sb.toString();
	}

	/**
	 * 1 means B was found. We are done.
	 * 0 means A was found, but B wasn't.
	 * -1 means neither was found.
	 * <p/>
	 * You must pass a non-null value to B to get a result of 1... otherwise you will get 0.
	 *
	 * @param sb
	 * @param a
	 * @param b
	 * @return
	 */
	private int writeTextContentsBetweenCore(StringBuilder sb, Node a, Node b) {
		boolean afound = (a == null); //If it is null, A has already been found
		boolean bfound = false; //If b is null, we are searching to the end.
		for (Node n : this.a) {
			if (n == b && b != null) {
				bfound = true; //We hit the termination node
				return 1; //We are done. Stop.
			}
			if (afound && !bfound) {
				if (n.isTextOrEntity()) n.writeTokenTo(sb); //We are between A and B. Write text
			}

			//Process children.
			if (n.children != null) {
				int result = n.children.writeTextContentsBetweenCore(sb, afound ? null : a, b);
				if (result == 1) return 1; //We're done. We found B.
				if (result == 0) afound = true; //We found A... we can start writing now.
				//Nothing to do for -1
			}
			//Look for A last, since we don't want to write the contents of A...
			if (a != null && a == n) afound = true;
		}
		if (afound)
			return 0; //We found A, and may have written content.
		else
			return -1; //A hasn't been found yet.
	}


	/**
	 * Returns true if all elements in the collection are tags, and the tag name for every one matches 'regex'. Returns true if the collection is empty.
	 *
	 * @param regex
	 * @return
	 * @throws InvalidMarkupException
	 */
	public boolean matches(String regex) throws InvalidMarkupException {
		for (Node n : a) if (!n.isTag() || !n.matches(regex)) return false;
		return true;
	}

	/**
	 * The number of items in the collection
	 *
	 * @return
	 */
	public int count() {
		return a.size();
	}

	/**
	 * Returns a new NodeList containing tags matching the specified tagname. Use recursive to get results deeper than 'children'
	 *
	 * @param regex
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList filterByTagName(String regex, boolean recursive) throws InvalidMarkupException {
		NodeList nl = new NodeList(count());
		filterByTagName(regex, recursive, nl);
		return nl;
	}

	private void filterByTagName(String regex, boolean recursive, NodeList addTo) throws InvalidMarkupException {
		for (Node n : list()) {
			if (n.matches(regex)) addTo.list().add(n);
			if (recursive && n.children != null) {
				n.children.filterByTagName(regex, recursive, addTo);
			}
		}
	}

	public NodeList sublist(Node from, Node endBefore) {
		int fromIx = from == null ? 0 : list().indexOf(from);
		int toIx = endBefore == null ? list().size() : list().indexOf(endBefore);
		//if (fromIx == -1 || toIx == -1) throw new IndexOutOfBoundsException();
		return sublist(fromIx, toIx);
	}

	public NodeList sublist(int fromIndex, int toIndex) {
		return new NodeList(list().subList(fromIndex, toIndex));
	}

	/**
	 * Retuns all nodes (recursively) that match the given set of filters. If no filters are specified, all nodes are returned.
	 * Some results may be children of other results
	 *
	 * @param filters
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList search(IFilter... filters) throws InvalidMarkupException {
		NodeList nl = new NodeList(count());
		searchNodes(nl, new And(filters), false, true);
		return nl;
	}

	/**
	 * Retuns all nodes (recursively) that match the given set of filters. If no filters are specified, all nodes are returned.
	 * If a node matches, child nodes are not searched
	 *
	 * @param filters
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList searchOuter(IFilter... filters) throws InvalidMarkupException {
		NodeList nl = new NodeList(count());
		searchNodes(nl, new And(filters), true, true);
		return nl;
	}

	public NodeList search(String query) throws InvalidMarkupException {
		NodeList nl = new NodeList(count());

		searchNodes(nl, new FilterQueryParser().parse(query), false, true);
		return nl;
	}

	public XmlToStringWrapper getStringWrapper() throws InvalidMarkupException {
		return new XmlToStringWrapper(this);
	}

	public XmlToStringWrapper getStringWrapper(boolean entityDecode) throws InvalidMarkupException {
		return new XmlToStringWrapper(this, entityDecode);
	}

	private void searchNodes(NodeList addTo, IFilter filter, boolean outermostOnly, boolean recursive) throws InvalidMarkupException {
		for (Node n : a) {
			boolean matches = (filter.matches(n));
			if (matches) {
				addTo.list().add(n);
				if (outermostOnly) continue; //Don't process children in outermostOnly mode
			}
			if (n.children != null && recursive) {
				n.children.searchNodes(addTo, filter, outermostOnly, recursive);
			}
		}
	}

	public NodeList searchBetween(Node after, Node before, IFilter... filters) throws InvalidMarkupException {
		NodeList nl = new NodeList(count());
		searchNodesBetween(after, before, nl, new And(filters), null, false, true);
		return nl;
	}


	public NodeList textEntityNodesBetween(Node after, Node before, IFilter exclude) throws InvalidMarkupException {
		NodeList nl = new NodeList(countMatchingNodes(new TextEntityFilter(1)));
		searchNodesBetween(after, before, nl, new TextEntityFilter(1),exclude, false, true);
		return nl;
	}

	///Recursive
	public int countMatchingNodes(IFilter filter) throws InvalidMarkupException {
		int len = 0;
		for (Node n : this.a) {
			//Local length
			if (filter.matches(n)){
				len++;
			} else if (n.children != null) {
				len += n.children.countMatchingNodes(filter);
			}

		}
		return len;
	}



	/**
	 * 1 means B was found. We are done.
	 * 0 means A was found, but B wasn't.
	 * -1 means neither was found.
	 *
	 * You must pass a non-null value to B to get a result of 1... otherwise you will get 0.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	private int searchNodesBetween(Node a, Node b, NodeList addTo, IFilter filter, IFilter exclude, boolean outermostOnly, boolean recursive) throws InvalidMarkupException{
		boolean afound = (a == null); //If it is null, A has already been found
		boolean bfound = false; //If b is null, we are searching to the end.
		for (Node n:this.a){
			if (n == b && b != null) {
				bfound = true; //We hit the termination node
				return 1; //We are done. Stop.
			}
			boolean matches = false;
			boolean excluded = exclude != null && exclude.matches(n);
			boolean searchChildren = recursive && n.children != null && !excluded;

			if (afound && !bfound && !excluded){
				matches =  (filter.matches(n));
			}

			if (matches){
				addTo.list().add(n);
				if (outermostOnly) searchChildren = false; //Don't process children in outermostOnly mode
			}

			//Process children.
			if (searchChildren) {
				int result = n.children.searchNodesBetween(afound ? null : a, b,addTo,filter,exclude, outermostOnly,recursive);
				if (result == 1) return 1; //We're done. We found B.
				if (result == 0) afound = true; //We found A... we can start writing now.
				//Nothing to do for -1
			}
			//Look for A last, since we don't want to write the contents of A...
			if (a != null && a == n) afound = true;
		}
		if (afound)
			return 0; //We found A, and may have written content.
		else
			return -1; //A hasn't been found yet.
	}




	/**
	 * Returns only text nodes.
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList textNodes() throws InvalidMarkupException {
		return search(new NodeFilter(){
			public boolean matches(Node n){
				return n.isTextOrEntity();
			}
		});

	}


	/**
	 * Filters the list to matching items. Not recursive
	 * @param filters
	 * @return
	 * @throws InvalidMarkupException
	 */
	public NodeList filter(IFilter... filters) throws InvalidMarkupException{
		NodeList nl = new NodeList(count());
		searchNodes(nl,new And(filters),false,false);
		return nl;
	}
	/**
	 * Fails assertion if regex doesn't match the contents of each translator link
	 * @param regex
	 */
	public NodeList assertEachContentMatches(String regex) {
		for (Node n:a){
			if (!TokenUtils.fastMatchesNonCached(regex, new NodeList(n).getTextContents())){
				//assert false: "Content of node doesn't match expectations: " + regex + " != " + new NodeList(n).getTextContents();
                throw new RuntimeException( "Content of node doesn't match expectations: " + regex + " != " + new NodeList(n).getTextContents());
			}
		}
		return this;
	}

	public boolean eachContentMatches(String regex) {
		for (Node n:a){
			if (!TokenUtils.fastMatchesNonCached(regex, new NodeList(n).getTextContents())){
				return false;
			}
		}
		return true;
	}

	/**
	 * If there are any elements, mustBeTrue must be true or an exception is thrown
	 * @param mustBeTrue
	 * @return
	 */
	public NodeList assertTrue(boolean mustBeTrue){
		if (count() > 0) assert(mustBeTrue);
		return this;
	}
	public Node last() {
		if (count() > 0) return a.get(a.size() -1);
		else return null;
	}
	/**
	 * Creates a recursive, deep copy of the list of nodes.
	 * @return
	 */
	public NodeList deepCopy() {
		NodeList nl = new NodeList(count());
		nl.setParent(this.parent);
		for (Node c:a){
			nl.list().add(c.deepCopy());
		}
		return nl;
	}
	public NodeList assertCombinedContentMatches(String regex) {
		if (!TokenUtils.fastMatchesNonCached(regex, this.getTextContents())){
            throw new RuntimeException( "Content of node doesn't match expectations: " + regex + " != " + this.getTextContents());
			//assert false: "Content of node doesn't match expectations: " + regex + " != " + this.getTextContents();
		}
		return this;
	}
	/**
	 * Sets the parent property of every child to null. Use carefully - you must make sure the parent object also deletes its reference, and/or is deleted itself.
	 * @return
	 */
	public NodeList nullParentRefs() {
		for (Node n:a) n.parent = null;
		return this;
	}
	/**
	 * Searches recursively for the specified Node instance. Returns false if not found.
	 * @param firstNumber
	 * @return
	 */
	public boolean has(Node firstNumber) {
		//Search shallow
		if (a.contains(firstNumber)) return true;

		for (Node n: a){
			if(n.children != null){// added null check djl 08-26-2010
				if (n.children.has(firstNumber)) return true;
			}
		}

		return false;
	}
	///False if the list contains any nodes (recursively) that are not valid phrasing content per HTML5
	public boolean phrasingContentOnly() {
		//https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Content_categories#Phrasing_content
		for (Node every : flattenRecursive().list()) {
			if (every.isTag()) {
				if (!every.matches("abbr|audio|b|bdo|br|button|canvas|cite|code|command|datalist|dfn|em|embded|i|iframe|img|input|kbd|keygen|label|mark|math|meter|noscript|object|output|progress|q|ruby|samp|script|select|small|span|strong|sub|sup|svg|textarea|time|var|video|wbr|")
						&&
						!every.matches("a|area|del|ins|map|link|meta|bookmark")) {
					return false;
				}
			}
		}
		return true;
	}

}
/*
 class NodeListXmlTokenReader implements IXmlTokenReader{
	private NodeList nl = null;
	public NodeListXmlTokenReader(NodeList nl){
		this.nl = nl;
		current = nl.first();
		indexes.push(0);
		childrenFinished.push(false);
	}
	private Node current = null;
	private Stack<Integer> indexes = new Stack<Integer>();
	private Stack<Boolean> childrenFinished = new Stack<Boolean>();
	
	public XmlToken read() throws IOException, InvalidMarkupException {
		//current can point to a node that has been processed but hasn't had the closing tag done yet.
		
		SlxToken t = current.toToken();
		
		
		if (current.children != null && current.count)
		
	}8
	
	
	public boolean canRead() {
		return current != null;
	}
	public void close()  {}
	

	
}*/
