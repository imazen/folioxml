package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase;

import java.util.UUID;

public class SlxToken extends TokenBase<SlxToken> {

	protected SlxToken(){}
    public SlxToken(String text) throws InvalidMarkupException{
        this.markup = text;
        this.reparse();
    }
    public SlxToken(TokenType type, String text) throws InvalidMarkupException{
        this.type = type;
        this.markup = text;
        if (this.type == TokenType.Tag) parseTag();
    }
    public SlxToken(TokenBase tb){
    	tb.copyTo(this, true);
    }
    protected SlxToken(SlxToken base, boolean deepCopyAttrs){
    	this.isGhost = base.isGhost;
    	this.startsNewContext = base.startsNewContext;
    	base.copyTo(this, deepCopyAttrs);
    }
    
    public  boolean inXmlTokenMode(){
    	return false;
    }
    
    /**
     * Contexts are boundaries which ghost elements (span, link) cannot cross.
     */
    public boolean startsNewContext = false;
    
    /**
     * Ghost elements aren't hierarchical - they're start and stop points, and may overlap and nest.
     * They can break all the rules - except they can't cross contexts.
     * Ghost tags don't exist in xml, so make sure this is false (the default) if this is an XmlNode. 
     */
    public boolean isGhost = false;
    
    /**
     * Allows SlxContextStack to mark ghost pairs for easier processing later. Not cloned.
     */
    public UUID ghostPair;
   
    
    /**
     * Creates a deep copy of the token, minus the ghostPair value.
     */
    public SlxToken clone(){
    	return new SlxToken(this,true);
    }
    public SlxToken clone(boolean deepCopyAttrs){
    	return new SlxToken(this,deepCopyAttrs);
    }
    
    
    
    /**
     * Returns an XML-compliant closing tag (no isGhost, sourceToken, startsNewContext, or attrs.
     * Throws an exception if this token is not an opening tag.
     * @return
     * @throws InvalidMarkupException 
     */
    public SlxToken getClosingTag() throws InvalidMarkupException{
    	
    	if (!(this.isOpening() && this.isTag()) || this.getTagName() == null)
    		throw new InvalidMarkupException("getClosingTag() can only be called on opening tag tokens with non-null tag names.",this);
    	
    	SlxToken t = new SlxToken();
    	t.markup = "</" + this.getTagName() + ">";
    	t.type = TokenType.Tag;
    	t.setTagName(this.getTagName(),false);
    	t.tagType = TagType.Closing;
    	
    	return t;
    }
    /**
     * Returns a clone, but with .isGhost=false. If the tag is a closing tag, the attribute collection is cleared.
     * @param deepCopyAttrs
     * @return
     */
    public SlxToken toNonGhostVersion(boolean deepCopyAttrs) {
    	//TODO: should return a XmlToken
    	SlxToken t = new SlxToken(this,deepCopyAttrs);
    	t.isGhost = false;
    	if (t.isClosing()) t.deleteAttributes();
    	return t;
    }
    
    
}