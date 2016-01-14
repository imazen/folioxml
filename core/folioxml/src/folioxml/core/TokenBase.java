package folioxml.core;


import folioxml.folio.FolioToken;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a base class for all XML-style tokens (SLX, XML). Override inXmlTokenMode() to determine whether attributes are allowed on closing tags.
 *
 * @param <T>
 * @author nathanael
 */
public class TokenBase<T extends TokenBase> {

    protected TokenBase() {
    }

    public TokenBase(String text) throws InvalidMarkupException {
        this.markup = text;
        this.reparse();
    }

    public TokenBase(TokenType type, String text) throws InvalidMarkupException {
        this.type = type;
        this.markup = text;
        if (this.type == TokenType.Tag) parseTag();
    }

    /**
     * Copies markup, sourceToken, type, tagName, tagType, and attributes
     *
     * @param target
     * @param deepCopyAttrs
     */
    public void copyTo(TokenBase target, boolean deepCopyAttrs) {
        target.markup = this.markup;
        target.sourceToken = this.sourceToken;
        target.type = this.type;
        target.tagName = this.tagName;
        target.tagType = this.tagType;
        if (this.attrs != null) {
            if (deepCopyAttrs)
                target.attrs = (TreeMap<String, String>) this.attrs.clone();
            else
                target.attrs = this.attrs;

        }
    }

    public boolean inXmlTokenMode() {
        return true;
    }


    /**
     * The Folio token this originated from (assuming this token was translated from a FolioToken). Not always present.
     */
    public FolioToken sourceToken = null;

    /**
     * The original text the token was created with. Use updateMarkup() to update this to match the attributes and tag name.
     */
    public String markup = null;


    private String tagName = null;
    private TreeMap<String, String> attrs = null;

    public enum TokenType {
        None, Text, Entity, Comment, Tag
    }

    /**
     * The type of token - Text, Entity, Comment, or Tag
     */
    public TokenType type = TokenType.None;


    public enum TagType {
        None, Opening, Closing, SelfClosing
    }

    /**
     * The tag type - can be opening, closing, or selfClosing.
     * Closing is not a valid value for an XmlNode.
     */
    public TagType tagType = TagType.None;


    public void setTagName(String tagName, boolean updateMarkup) {
        this.tagName = tagName;
        if (updateMarkup) updateMarkup();
    }

    /**
     * Changes the tag name of the token, and updates the .markup property accordingly.
     *
     * @param tagName
     */
    public T setTagName(String tagName) {
        setTagName(tagName, true);
        return (T) this;
    }

    /**
     * this.markup = this.toString();
     * Rebuilds markup variable from memory structure.
     */
    public void updateMarkup() {
        this.markup = toTokenString();
    }


    /**
     * True if this token is a comment. False if it is text, entity, or tag
     *
     * @return
     */
    public boolean isComment() {
        return this.type == TokenType.Comment;
    }

    /**
     * True if a text or entity token
     *
     * @return
     */
    public boolean isTextOrEntity() {
        return (this.type == TokenType.Text || this.type == TokenType.Entity);
    }

    /**
     * Returns null if this in not a TokenType.Comment. Throws an InvalidMarkupException if the markup is not a token.
     * Returns the text between <!-- and -->
     *
     * @return
     * @throws InvalidMarkupException
     */
    public String getCommentContents() throws InvalidMarkupException {
        if (!isComment()) return null;
        if (!markup.startsWith("<!--") || !markup.endsWith("-->"))
            throw new InvalidMarkupException("Failed to parse comment", this);

        return markup.substring(4, markup.length() - 3);
    }

    /**
     * Returns true if the token is not whitespace, and is either text or an entity.
     *
     * @return
     */
    public boolean isContent() {
        return (isTextOrEntity() && !TokenUtils.isWhitespace(this.markup));
    }

    /**
     * Returns true if the token is an SLX tag, not a comment, entity, or text token.
     *
     * @return
     */
    public boolean isTag() {
        return this.type == TokenType.Tag;
    }

    public boolean isEntity() {
        return this.type == TokenType.Entity;
    }

    /**
     * Only defined for isTag() == true
     *
     * @return
     */
    public boolean isOpening() {
        return this.tagType == TagType.Opening;
    }

    /**
     * Only defined for isTag() == true
     *
     * @return
     */
    public boolean isClosing() {
        return this.tagType == TagType.Closing;
    }

    /**
     * Only defined for isTag() == true
     *
     * @return
     */
    public boolean isSelfClosing() {
        return this.tagType == TagType.SelfClosing;
    }


    protected static String RegexEntity = "&[^;&< ]++;"; //Aug 21. Added space as banned character. Should help perf. Possessive quantifiers are good here - mutually exclusive groups
    /**
     * Needs DOTALL
     */
    protected static String RegexComment = "<!--(.*?)-->"; //Lazy quantifier is what we want for proper comment parsing

    protected static String RegexText = "((?:[^<&]++|<\\s|&\\s)++)"; //Aug 21. Fixed so it doesn't match empty strings any more... was "((?:[^<&]++|<\\s|&\\s)*+)"


    protected static String RegexTag = "<(/)?+([\\w\\-\\.:]++)(\\s++[^>]*?)??(/)??>";

    /**
     * Aug 21. Was: <(/)?+([\\w\\-\\.:]++)(\\s++[^>]*?)?(/)?+>
     * <p>
     * This regex was flawed, because groups 3 and 4 overlapped with character '/' (found in some tags)...
     * Since the first was lazy, and the second possessive, it exposed a java bug...
     * <p>
     * Added lazy quantifiers after both groups.. Should be correct parsing now.
     * <p>
     * Text discovered:
     * <record class="NormalLevel" fullPath="/" level="root" levelDefOrder="Year,Tape,Chapter,Section,Normal Level"
     * levels="Year,Tape,Chapter,Section">
     */


    protected static Pattern pEntity = Pattern.compile("^" + RegexEntity + "$", Pattern.DOTALL);
    protected static Pattern pComment = Pattern.compile("^" + RegexComment + "$", Pattern.DOTALL);
    /**
     * No opening angle brackets or ampersands, unless they are followed by whitespace.
     */
    protected static Pattern pText = Pattern.compile("^" + RegexText + "$");
    /**
     * group(1) closing slash
     * group(2) tag name
     * group(3) tag attributes
     * group(4) self closing slash
     **/
    protected static Pattern pTag = Pattern.compile("^" + RegexTag + "$");
    /**
     * group(1) name
     * group(2,3,4,5) values
     */
    protected static Pattern attributePair = Pattern.compile("\\G\\s++(\\w[\\w-:]*+)(?:\\s*+=\\s*+\"([^\"]*+)\"|\\s*+=\\s*+'([^']*+)'|\\s*+=\\s*+([^\\s=/>]*+)|(\\s*?))");


    /**
     * Returns true if the tag name (case-insensitive) matches the regex. Returns false unless the token is a tag. Returns false if there is a parse exception
     *
     * @param regex
     * @return
     */
    public boolean matches(String regex) {
        if (!isTag()) return false;
        return TokenUtils.fastMatches(regex, this.getTagNameSilent());
    }

    /**
     * Reparses all cached data from the .markup attribute. Also re-determines token type.
     *
     * @throws folioxml.core.InvalidMarkupException
     */
    public void reparse() throws InvalidMarkupException {
        if (pEntity.matcher(markup).find()) {
            this.type = TokenType.Entity;
        } else if (pTag.matcher(markup).find()) {
            this.type = TokenType.Tag;
            parseTag(true);
        } else if (pComment.matcher(markup).find()) {
            this.type = TokenType.Comment;
        } else if (pText.matcher(markup).find()) {
            this.type = TokenType.Text;
            //TODO: parse whitespace=true/false here and cache for later use. It's always needed.
        } else {
            throw new InvalidMarkupException("Invalid use of < or &:" + markup);
        }
        //TODO: check for -- in XML comments.
        //Check for invalid text and entities also.
    }

    /**
     * Parses the 'markup' attribute if needed
     *
     * @throws folioxml.core.InvalidMarkupException
     */
    protected void parseTag() throws InvalidMarkupException {
        parseTag(false);
    }

    protected void parseTag(boolean reparse) throws InvalidMarkupException {
        if (this.type == TokenType.None) {
            reparse();
            return;
        }
        if (!isTag()) return; //Only parse tags
        if (tagName != null && !reparse) return; //Don't parse if it's already done

        Matcher m = pTag.matcher(markup);
        if (!m.find()) throw new InvalidMarkupException("Tag syntax is wrong: \"" + markup + "\".", this);

        parseTagFromMatcher(m);
    }

    protected void parseTagFromMatcher(Matcher m) throws InvalidMarkupException {
        //Tag type
        boolean closing = (m.group(1) != null && m.group(1).length() > 0);
        boolean selfClosing = (m.group(4) != null && m.group(4).length() > 0);
        if (closing) this.tagType = TagType.Closing;
        else if (selfClosing) this.tagType = TagType.SelfClosing;
        else this.tagType = TagType.Opening;

        //Tag name
        this.tagName = m.group(2);

        if (attrs != null) attrs.clear(); //Empty if we are doing a reparse
        //Parse attributes
        String attrText = m.group(3);
        if (attrText == null) attrText = "";
        Matcher ma = attributePair.matcher(attrText);
        int index = 0;
        while (ma.find(index)) {
            if (attrs == null)
                attrs = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);//Default Collator case-insensitive (TETERARY) //FIixed bug #80 on Feb 2.
            String name = ma.group(1);
            String value = ma.group(2);
            if (value == null) value = ma.group(3); //Aug 21... fixed typo m->ma
            if (value == null) value = ma.group(4);
            if (value == null) value = ma.group(5);

            assert (name != null && value != null);
            //Jan 21. 2009. Fixed attribute parsing
            attrs.put(name, TokenUtils.attributeDecode(value));
            index = ma.end();
        }
        //check remainder
        if (index < attrText.length()) {
            String remainder = attrText.substring(index);
            if (!remainder.matches("^\\s*$")) {
                //Any remaining text after attribute parsing should be whitespace. Invalid syntax.
                throw new InvalidMarkupException("Failed to parse tag attributes: " + remainder, this);

            }
        }


        if (this.isClosing() && inXmlTokenMode() && attrs != null && !attrs.isEmpty())
            throw new InvalidMarkupException("Closing xml tags cannot have attributes!", this);
    }

    /**
     * Returns the markup representation of the token, whether it is is an entity, comment, text, or tag
     *
     * @throws InvalidMarkupException
     */
    public String toString() {
        return toTokenString();
    }

    /**
     * Returns the markup representation of the token, whether it is is an entity, comment, text, or tag
     *
     * @throws InvalidMarkupException
     */
    public String toTokenString() {
        if (this.isTag()) return writeTokenTo(null).toString();
        else {
            return this.markup; //TODO: We need some way to prevent -- in comments (other than the start and end...) Added fix to SlxTranslator so comments are encoded properly when arriving from Folio
        }
    }

    public StringBuilder writeTokenTo(StringBuilder sb) {
        return writeTokenTo(sb, false);
    }

    public StringBuilder writeTokenTo(StringBuilder sb, boolean decodeEntitiesInText) {
        //Calculate size
        int initialCapacity = 20;
        if (markup != null) initialCapacity = markup.length();
        //Grow or create
        if (sb != null) sb.ensureCapacity(sb.length() + initialCapacity);
        else sb = new StringBuilder(initialCapacity);


        if (tagName == null || !isTag()) {
            sb.append(decodeEntitiesInText ? TokenUtils.entityDecodeString(markup) : markup);
        } else {
            if (tagType == TagType.Closing) sb.append("</");
            else sb.append("<");
            //name
            sb.append(tagName);

            if (attrs != null) {
                Set<Entry<String, String>> pairs = attrs.entrySet();
                for (Entry<String, String> entry : pairs) {
                    sb.append(' '); //TODO add wrapping code here
                    sb.append(entry.getKey());
                    sb.append("=\"");
                    //Jan 21, 2009 - fixed attribute encoding bug.
                    if (entry.getValue() != null) sb.append(TokenUtils.attributeEncode(entry.getValue()));
                    sb.append('"');
                }
            }

            if (tagType == TagType.SelfClosing) sb.append(" />");
            else sb.append('>');
        }
        return sb;
    }

    /**
     * Returns the tag name
     *
     * @return
     * @throws InvalidMarkupException
     */
    public String getTagName() throws InvalidMarkupException {
        parseTag();
        return tagName;
    }

    /**
     * Returns the tag name
     *
     * @return
     * @throws InvalidMarkupException
     */
    public String getTagNameSilent() {
        try {
            parseTag();
        } catch (InvalidMarkupException e) {
        }
        return tagName;
    }

    /**
     * Returns the value of the specified attribute.
     *
     * @param attributeName
     * @return
     * @throws InvalidMarkupException
     */
    public String get(String attributeName) throws InvalidMarkupException {
        parseTag();
        if (attrs == null) return null;
        return attrs.get(attributeName);
    }

    /**
     * Call before manipulating .attrs
     * Makes sure the tag has been parsed, and initializes the attribute collection if it is null.
     *
     * @throws folioxml.core.InvalidMarkupException
     */
    protected void prepareAttrs() throws InvalidMarkupException {
        parseTag();

        if (inXmlTokenMode() && !(this.isOpening() || this.isSelfClosing()))
            throw new InvalidMarkupException("You can only set attributes on opening and self-closing XML tokens", this);

        if (attrs == null) attrs = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Returns a reference to the Map of the attributes.  If the map is null, it is initialized.
     * Calling on a closing token in xmlMode will cause an exception.
     *
     * @return
     * @throws InvalidMarkupException
     */
    public Map<String, String> getAttributes() throws InvalidMarkupException {
        prepareAttrs();
        return attrs;
    }

    /**
     * Deletes the attribute map from this token
     *
     * @return
     */
    public T deleteAttributes() {
        attrs = null;
        return (T) this;
    }

    //public boolean stopsNewContext;

    /**
     * Sets the value of the specified attribute.
     * Returns this; for chaining.
     *
     * @param attributeName
     * @param value
     * @return
     */
    public T set(String attributeName, String value) throws InvalidMarkupException {
        prepareAttrs();
        if (attributeName == null || value == null) throw new NullPointerException();
        attrs.put(attributeName, value);
        return (T) this;
    }

    /**
     * Removes the specified attribute by name.
     * Returns this; for chaining.
     *
     * @param attributeName
     * @param value
     * @return
     */
    public T removeAttr(String attributeName) throws InvalidMarkupException {
        prepareAttrs();
        if (attributeName == null) throw new NullPointerException();
        attrs.remove(attributeName);
        return (T) this;
    }

    /**
     * Appends the specified value to the current value of the attribute. Creates the attribute if it is missing. Returns this; for chaining.
     *
     * @param attributeName
     * @param value
     */
    public T appendToAttribute(String attributeName, String value) throws InvalidMarkupException {
        prepareAttrs();
        if (attributeName == null || value == null) throw new NullPointerException();
        if (attrs.containsKey(attributeName))
            attrs.put(attributeName, get(attributeName) + value);
        else attrs.put(attributeName, value);
        return (T) this;
    }

    /**
     * Appends the specified value to the current value of the attribute. Creates the attribute if it is missing. Returns this; for chaining.
     * If there is already data in the attribute, it will add a semicolon or comma. If attributename=="style", a semicolon is used. Otherwise a comma is used. For comma-delimted data, commas are html-encoded.
     *
     * @param attributeName
     * @param value
     */
    public T appendToAttributeSmart(String attributeName, String value) throws InvalidMarkupException {
        prepareAttrs();
        if (attributeName == null || value == null) throw new NullPointerException();
        if (attrs.containsKey(attributeName)) {
            String originalValue = get(attributeName);
            String delimiter = attributeName.equalsIgnoreCase("style") ? ";" : ",";

            //add appropriate delimiter
            if (originalValue.length() > 0) {
                if (!originalValue.endsWith(delimiter)) {
                    //TODO: This is a bug. We can't know if the first commas inserted are delimiters or just commas.
                    //We have to have a way to mark that an attribute's value is a list... a trailing delimiter?
                    //TODO: Analyze use cases and build tests. This is breaking groups at the moment.
                    //if (delimiter.equals(",")) originalValue = originalValue.replace(",", "&#44;");
                    originalValue += delimiter; //Encode delimiters if it doesn't end with one

                }
            }
            //Remove trailing commas/semicolons prior to encoding.

            String suffix = "";
            while (value.endsWith(delimiter)) {
                suffix += delimiter;
                value = value.substring(0, value.length() - 1);
            }

            //Enode commas *only*. semicolons are a bad idea - sometimes we want to add multiple css pairs at a time. Maybe an overload later?
            if (delimiter.equals(",")) value = value.replace(",", "&#44;");

            attrs.put(attributeName, originalValue + value + suffix);
        } else attrs.put(attributeName, value); //Don't encode until the second item is added.
        return (T) this;
    }

    public T addAttributesTo(T target) throws InvalidMarkupException {
        prepareAttrs();

        for (Entry<String, String> e : attrs.entrySet()) {
            target.appendToAttributeSmart(e.getKey(), e.getValue());
        }
        return (T) this;
    }

}