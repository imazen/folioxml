package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase;

public class XmlToken extends TokenBase<XmlToken> {
    protected XmlToken() {
    }

    public XmlToken(String text) throws InvalidMarkupException {
        this.markup = text;
        this.reparse();
    }

    public XmlToken(TokenType type, String text) throws InvalidMarkupException {
        this.type = type;
        this.markup = text;
        if (this.type == TokenType.Tag) parseTag();
    }

    protected XmlToken(XmlToken base, boolean deepCopyAttrs) {
        base.copyTo(this, deepCopyAttrs);
    }

    public boolean inXmlTokenMode() {
        return true;
    }


    /**
     * Creates a deep copy of the token, minus the ghostPair value.
     */
    public XmlToken clone() {
        return new XmlToken(this, true);
    }

    public XmlToken clone(boolean deepCopyAttrs) {
        return new XmlToken(this, deepCopyAttrs);
    }


    /**
     * Returns an XML-compliant closing tag (no isGhost, sourceToken, startsNewContext, or attrs.
     * Throws an exception if this token is not an opening tag.
     *
     * @return
     * @throws InvalidMarkupException
     */
    public XmlToken getClosingTag() throws InvalidMarkupException {

        if (!(this.isOpening() && this.isTag()) || this.getTagName() == null)
            throw new InvalidMarkupException("getClosingTag() can only be called on opening tag tokens with non-null tag names.", this);

        XmlToken t = new XmlToken();
        t.markup = "</" + this.getTagName() + ">";
        t.type = TokenType.Tag;
        t.setTagName(this.getTagName(), false);
        t.tagType = TagType.Closing;

        return t;
    }

}

