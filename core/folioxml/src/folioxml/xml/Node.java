package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase;
import folioxml.slx.SlxToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node extends TokenBase<Node> {

	/*
	 * Should parse a well-formed set of tokens. Any tags than open must be closed, and any tags that close must be opened.
	 * 
	 * Can use XmlTokenReader
	 */

    public Node() {

    }

    public Node(SlxToken t, boolean deepCopyAttrs) {
        if (t != null) {
            t.copyTo(this, deepCopyAttrs);
            if (this.isClosing()) this.deleteAttributes();
        }
    }

    public Node(String xml) throws IOException, InvalidMarkupException {
        XmlTokenReader xtr = new XmlTokenReader(new StringReader(xml)); //Get the token reader

        XmlToken t = xtr.read();

        t.copyTo(this, false);
        if (this.isTag() && this.isOpening()) {
            children = new NodeList();
            children.setParent(this);
            children.addUntil(xtr, this.getTagName());
        }

        //There should be no remaining tokens.
        while (xtr.canRead()) {
            XmlToken temp = xtr.read();
            if (temp != null && temp.toString().length() > 0)
                throw new InvalidMarkupException("Unexpected token: \"" + temp.toString() + "\".", temp);
        }

    }

    public Node(XmlToken t, IXmlTokenReader reader, boolean deepCopyAttrs) throws IOException, InvalidMarkupException {
        t.copyTo(this, deepCopyAttrs);
        if (this.isTag() && this.isOpening()) {
            children = new NodeList();
            children.setParent(this);
            children.addUntil(reader, this.getTagName());
        }
    }

    public Node parent = null;

    public NodeList children = null;

    private boolean _deleted = false;

    public void markDeleted() {
        _deleted = true;
    }

    public Node remove() {
        return remove(true);
    }

    /**
     * Removes from the parent. Cannot be reused if 'markDeleted=true'
     *
     * @param markDeleted
     * @return
     */
    public Node remove(boolean markDeleted) {
        assert (!_deleted) : "Node already deleted";
        assert (this.parent != null);
        this.parent.children.remove(this);
        this.parent = null;
        if (markDeleted) markDeleted();
        return this;
    }

    /**
     * Pulls the element, replacing it with its children.
     *
     * @return
     */
    public Node pull() {
        if (children != null) {
            if (parent != null) {
                int thisIndex = parent.children.list().indexOf(this);
                parent.addChildren(children.list(), thisIndex); //Put children in parent
            }
            children.list().clear(); //Remove children.
        }
        //Remove this
        remove();
        return this;
    }

    public XmlToStringWrapper getStringWrapper() throws InvalidMarkupException {
        return new XmlToStringWrapper(this);
    }

    public XmlToStringWrapper getStringWrapper(boolean entityDecode) throws InvalidMarkupException {
        return new XmlToStringWrapper(this, entityDecode);
    }
	/*
	public Node mergeTextAndEntities(boolean recursive){
		//TODO - complete
		return this;
	}
	public Node splitEntities(boolean recursive){
		//TODO - complete
		return this;
	}
	*/

    public Node addChild(Node n) {
        return addChild(n, -1);
    }

    public Node addChild(Node n, int atIndex) {
        List<Node> l = new ArrayList<Node>();
        l.add(n);
        addChildren(l, atIndex);
        return this;
    }

    public Node insertBeforeThis(Node n) {
        this.parent.addChild(n, parent.children.list().indexOf(this));
        return this;
    }

    public Node insertAfterThis(Node n) {
        this.parent.addChild(n, parent.children.list().indexOf(this) + 1);
        return this;
    }

    public Node addChildren(Collection<Node> items) {
        return addChildren(items, -1);
    }

    public Node addChildren(NodeList items) {
        return addChildren(items.list(), -1);
    }

    public Node addChildren(Collection<Node> items, int atIndex) {
        if (children == null) {
            children = new NodeList(items.size()); //Set up the collection
            children.setParent(this);
            this.tagType = TagType.Opening;
        }
        if (atIndex > -1) {
            children.list().addAll(atIndex, items); //Add them
        } else {
            children.list().addAll(items);
        }
        for (Node n : children.list()) n.parent = this; //Change child references
        return this;
    }

    /**
     * Moves, (not copies) children from this node to the specified node.
     *
     * @param newParent
     * @return
     */
    public Node moveChildrenTo(Node newParent) {
        if (children == null) return this;
        newParent.addChildren(children.list()); //Add children to new parent and change references
        children.list().clear(); //Remove children from this parent
        return this;
    }

    /**
     * Returns a closing tag for this node.
     * Throws an exception if this node is not a tag.
     *
     * @return
     * @throws InvalidMarkupException
     * @throws InvalidMarkupException
     * @throws InvalidMarkupException
     */
    public XmlToken getClosingTag() {
        assert (this.isTag()) : "Can only be called on tags.";
        String tagName = this.getTagNameSilent();
        assert (tagName != null) : "getClosingTag() can only be called on XML nodes with non-null tag names.";

        XmlToken t = new XmlToken();
        t.markup = "</" + tagName + ">";
        t.type = TokenType.Tag;
        t.setTagName(tagName, false);
        t.tagType = TagType.Closing;
        return t;
    }

    /**
     * Returns the SLX token for the opening tag or text token.
     *
     * @return
     */
    public SlxToken getSlxToken() {
        return new SlxToken(this);
    }


    public StringBuilder writeXmlTo(StringBuilder sb) {
        //Grow or create StringBuilder
        if (sb != null) {
            sb.ensureCapacity(sb.length() + (int) Math.round(estimateTextSize() * 1.2));
        } else {
            sb = new StringBuilder((int) Math.round(estimateTextSize() * 1.2));
        }

        //Write to StringBuilder
        super.writeTokenTo(sb);
        //Make sure it is an opening tag if it has children.
        if (children != null) assert this.isOpening() && this.isTag();
        if (this.isTag() && this.isOpening()) {
            //Write children
            if (children != null) children.writeTo(sb);
            //Write closing
            sb.append("</");
            sb.append(getTagNameSilent());
            sb.append(">");
        }
        return sb;
    }

    /**
     * Recursively estimates required markup size.
     *
     * @return
     */
    private int estimateTextSize() {
        int size = 0;
        //For the closing tag.
        if (isTag() && isOpening()) size += this.getTagNameSilent().length() + 3; //Forces a parse...
        //For the opening token/text token.
        size += markup != null ? markup.length() : 10; //Main token
        //Add children sizes recursively
        if (children != null) for (Node n : children.list()) size += n.estimateTextSize();
        return size;
    }


    public String toXmlString(boolean autoIndent) throws InvalidMarkupException {
        if (autoIndent) {
            return new XmlFormatter(0).format(this);
        } else {
            return writeXmlTo(null).toString();
        }
    }

    /**
     * Returns a list of ancestors, ordered by closest to farthest.
     *
     * @return
     */
    public NodeList ancestors() {
        NodeList nl = new NodeList();
        Node p = this.parent;
        while (p != null) {
            nl.list().add(p);
            p = p.parent;
        }
        return nl;
    }


    public String getClosingTagString() {
        return "</" + getTagNameSilent() + ">";
    }

    public Node deepCopy() {
        Node n = new Node();
        this.copyTo(n, true);
        n.parent = this.parent;
        if (this.children != null) {
            n.children = this.children.deepCopy();
            n.children.setParent(n);
            for (Node c : n.children.list()) {
                c.parent = n;
            }
        }
        return n;
    }

    /**
     * Needs unit tests
     *
     * @param className
     * @throws InvalidMarkupException
     */
    public void addClass(String className) throws InvalidMarkupException {
        String cls = (this.get("class"));
        if (cls == null) cls = className;
        else {
            cls = cls.trim() + " " + className.trim();
        }
        //TODO: add class name(s) validation.
        this.set("class", cls);


    }

    /**
     * Needs unit tests
     *
     * @param className
     * @return
     * @throws InvalidMarkupException
     */
    public boolean hasClass(String className) throws InvalidMarkupException {
        String cls = (this.get("class"));
        if (cls == null) return false;
        String[] classes = cls.split("\\s+");
        for (String s : classes) {
            if (s.equals(className)) return true;
        }
        return false;
    }


    public Node addTo(Node n) {
        n.addChild(this);
        return this;
    }

    public Node addTo(Node n, int addAt) {
        n.addChild(this, addAt);
        return this;
    }

    public boolean isLastNode() {
        return (this.parent.children.list().lastIndexOf(this) == this.parent.children.list().size() - 1);
    }

    /**
     * Returns the root parent (whose parent is null)
     *
     * @return
     */
    public Node rootNode() {
        Node p = this;
        while (p.parent != null) p = p.parent;
        return p;
    }


}
