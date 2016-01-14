package folioxml.folio;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenInfo;
import folioxml.core.TokenUtils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opening angle brackets must be paired. Closing angle brackets cannot be present at all! (&lt;/example>)
 *
 * @author nathanael
 */
public class FolioToken {
    //style attributes
    //arbitrary attributes

    public FolioToken(TokenType type) {
        this.type = type;
    }

    public FolioToken(String text) throws InvalidMarkupException {
        this.text = text;
        if (pTag.matcher(text).find()) {
            this.type = TokenType.Tag;
            parseTag();
        } else if (pComment.matcher(text).find()) {
            this.type = TokenType.Comment;
        } else if (pText.matcher(text).find()) {
            this.type = TokenType.Text;
        } else {
            throw new InvalidMarkupException("Invalid token; neither tag, comment, or plain text:" + text);
        }
    }

    public enum TokenType {
        None, Comment, Text, Tag
    }

    /**
     * Comment, text, or tag
     */
    public TokenType type = TokenType.None;

    public String text = null;
    public String tagName = null;
    public String tagOptions = null;

    public TokenInfo info = null;

    protected boolean _isClosing = false;

    /**
     * Returns true if it is a closing tag (if it has a forwardslash after the angle braket)
     *
     * @return
     */
    public boolean isClosing() {
        return _isClosing;
    }

    /**
     * Sets whether or not the tag has a forwardslash after the angle bracket.
     *
     * @param value
     * @return
     */
    public void isClosing(boolean value) {
        _isClosing = value;
    }

    /**
     * Matches a comment tag and any intermediate comments. Lazy, of course.
     */
    private static Pattern pComment = Pattern.compile("^" + FolioTokenReader.CommentRegex + "$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    /**
     * Matches text that doesn't contain any open brackets that are directly followed by a letter or a closing slash.
     */
    private static Pattern pText = Pattern.compile("^" + FolioTokenReader.TextRegex + "$"); //non <, expect doubles
    /**
     * Matches any two-letter tag (and +/-), and captures (optional) options. group 1 and 2, respectively. Adjacent delimiters handled for TA:; bug in export.
     */
    private static Pattern pTag = Pattern.compile("^" + FolioTokenReader.TagRegex + "$");

    protected void parseTag() {
        assert (tagOptions == null && tagName == null);

        Matcher m = pTag.matcher(text);
        if (!m.find()) {
            assert (false); //Tag has invalid syntax.
        }
        parseTagFromMatcher(m);
        // this.stackID = this.tagName;
    }

    protected void parseTagFromMatcher(Matcher m) {
        //Tag type
        isClosing((m.group(1) != null));

        //Tag name
        this.tagName = m.group(2);

        //Parse attributes
        this.tagOptions = m.group(3);

    }

    /**
     * Returns true if the tag name (case-insensitive) matches the regex.
     *
     * @param regex
     * @return
     */
    public boolean matches(String regex) {
        return TokenUtils.fastMatches(regex, tagName);
    }

    /**
     * Throws an exception if this tag doesn't have the specified number of options. Always returns true
     *
     * @param count
     * @return
     */
    public boolean assertCount(int count) throws InvalidMarkupException {
        if (count() != count)
            throw new InvalidMarkupException("This tag " + text + " is required to have " + count + " options.");
        return true;
    }

    /**
     * Alphanumeric or quoted string. Quoted string can contain paired quotes.  Adjacent delimiters handled for TA:; bug in export.
     */
    private static Pattern pString = Pattern.compile("^\\s*([^;,:\"]+|\"(?:[^\"]|(?:\"\"))*\")(?:\\s*[:;,]+\\s*|$)");
    /**
     * Numeric with optional decimal. Allowed units: p t c
     */
    private static Pattern pUnit = Pattern.compile("^\\s*(-?[0-9]+(?:\\.[0-9]*)?[ptc]?)(?:\\s*[:;,]\\s*|$)");

    private String _cachedOptionsText = null;
    private List<String> _cachedOptionsList = null;
    private List<Boolean> _cachedOptionsQuoted = null;

    /**
     * Returns an array of the options specified on this tag. unquotes text options automatically and inserts entities for " and &lt;&lt;
     * Cached - don't modify the List<> or you'll mess everybody up.
     *
     * @return
     */
    public List<String> getOptionsArray() throws InvalidMarkupException {
        //Jan 21, 2009 - profiled this method - Was taking 20% of overall execution time.
        //75% of method time in ReplaceAll
        if (tagOptions != null) {
            if (_cachedOptionsText != null) if (_cachedOptionsText.equals(tagOptions)) return _cachedOptionsList;


            ArrayList<String> attrs = new ArrayList<String>(6);
            ArrayList<Boolean> attrsQuoted = new ArrayList<Boolean>(6);
            Matcher mStr = pString.matcher(tagOptions);
            Matcher mUnit = pUnit.matcher(tagOptions);
            int index = 0;
            while (index < tagOptions.length()) {
                Boolean quoted = false;
                //Set the region to parse
                mUnit.region(index, tagOptions.length());
                mStr.region(index, tagOptions.length());


                if (mUnit.find()) {
                    attrs.add(mUnit.group(1));
                    index = mUnit.end();
                } else if (mStr.find()) {
                    //This section was taking 75% in replaceAll calls.
                   /* Old code: took 13% of entire library CPU
            	    * String fixed = mStr.group(1).replaceAll("\"\"", "&quot;").replaceAll("^\"|\"$", "").replaceAll("<<","&lt;").replaceAll("<","&lt;").replaceAll(">","&gt;");
                   if (mStr.group(1).replaceAll("\"\"","").startsWith("\"")) quoted = true;
            	    */
                    String val = mStr.group(1);

                    //If the first char is a quote, but not the second, and more than 2 chars
                    if (val.length() > 2 && val.charAt(0) == '"' && val.charAt(1) != '"') quoted = true;
                    //or if both chars are quotes, then value is quoted.
                    if (val.length() == 2 && val.contentEquals("\"\"")) quoted = true;

                    attrs.add(convertString(val, quoted));


                    index = mStr.end();
                } else {
                    throw new InvalidMarkupException("Invalid syntax for a tag option:" + tagOptions.substring(index));

                    //assert false : "Invalid token : " + tagOptions.substring(index);
                }
                attrsQuoted.add(quoted);
            }
            _cachedOptionsQuoted = attrsQuoted;
            _cachedOptionsText = tagOptions;
            _cachedOptionsList = attrs;
            return attrs;
        }
        return new ArrayList<String>();
    }

    /**
     * Converts a folio-style attribute (with "", <<,<,> escaping methods) to XML style entities.
     *
     * @param s
     * @param removeQuotes
     * @return
     */
    private String convertString(String s, boolean removeQuotes) {
        //This section was taking 75% in replaceAll calls.
 	   /* Old code: took 13% of entire library CPU
 	    * String fixed = mStr.group(1).replaceAll("\"\"", "&quot;").replaceAll("^\"|\"$", "").replaceAll("<<","&lt;").replaceAll("<","&lt;").replaceAll(">","&gt;");
        if (mStr.group(1).replaceAll("\"\"","").startsWith("\"")) quoted = true;
 	    */
        //New version takes 8% instead of 75% of getOptionsArray();
        StringBuilder sb = new StringBuilder(s.length());

        //Turn remaining quote pairs and angle bracket pairs in &quot; and &lt; respectively.
        //Convert single angle braket pairs int &lt; and &gt; for compatibility.
        //Convert single quote pairs into &quot;
        boolean lastCharQuote = false;
        boolean lastCharLtBracket = false;
        for (int i = 0; i < s.length(); i++) {
            //If removeQuotes, remove first and last character if they are quotes.
            if (removeQuotes && (i == 0 || i == s.length() - 1)) continue;

            char c = s.charAt(i);
            //Quotes, doubled and single
            if (lastCharQuote) {
                lastCharQuote = false;
                //Flush the last char regardless.
                sb.append("&quot;");
                //Skip this char also if a quote.
                if (c == '\"') continue;
            } else if (c == '\"') {
                lastCharQuote = true;
                continue;
            }
            //Less than, doubled and single
            if (lastCharLtBracket) {
                lastCharLtBracket = false;
                //Flush the last char regardless.
                sb.append("&lt;");
                //Skip this char also if a quote.
                if (c == '<') continue;
            } else if (c == '<') {
                lastCharLtBracket = true;
                continue;
            }
            //> greater than
            if (c == '>') {
                sb.append("&gt;");
                continue;
            }
            sb.append(c);
        }


        //if (fixed.indexOf("\"") > -1) throw new InvalidMarkupException("")
        //assert(fixed.indexOf("\"") < 0);//No single quote marks should be present within the string.
        //assert(fixed.indexOf("<") < 0); //No single opening brackets should be present

        return sb.toString();
    }

    public List<String> getOptionsArrayWithTagName() throws InvalidMarkupException {
        ArrayList<String> opts = new ArrayList<String>();
        opts.add(tagName);
        opts.addAll(getOptionsArray());
        return opts;

    }

    public boolean wasQuoted(int index) throws InvalidMarkupException {
        getOptionsArray();
        if (_cachedOptionsQuoted != null) return _cachedOptionsQuoted.get(index);
        return false;
    }

    /**
     * Returns the option at the specfied index. Returns null if there is no option at that index
     *
     * @param optionIndex
     * @return
     */
    public String get(int optionIndex) throws InvalidMarkupException {
        List<String> opts = getOptionsArray();
        if (opts.size() > optionIndex) return opts.get(optionIndex);
        else return null;
    }

    public FolioToken remove(int optionIndex) throws InvalidMarkupException {
        List<String> opts = getOptionsArray();
        opts.remove(optionIndex);

        StringWriter sw = new StringWriter();
        for (int i = 0; i < opts.size(); i++) {
            String s = opts.get(i);
            if (i > 0) sw.append(",");
            int oldIndex = i;
            if (i >= optionIndex) oldIndex++;

            if (!wasQuoted(oldIndex))//if (FolioCssUtils.matchesCaseInsensitive("^[A-Za-z0-9\\.]+$", s))
                sw.append(s);
            else
                sw.append("\"" + s + "\"");
        }

        this.tagOptions = sw.toString();
        if (tagOptions.length() == 0) tagOptions = null;//round down
        return this;
    }


    /**
     * Returns the number of options specified in the tag
     *
     * @return
     */
    public int count() throws InvalidMarkupException {
        return getOptionsArray().size();
    }

    /**
     * Returns the option directly following the specified option. Case-insensitive comparison. Returns null if not found.
     *
     * @param precedingOption
     * @return
     */
    public String getOptionAfter(String precedingOption) throws InvalidMarkupException {
        List<String> opts = getOptionsArray();

        for (int i = 1; i < opts.size(); i++) {
            if (opts.get((i - 1)).equalsIgnoreCase(precedingOption)) {
                return opts.get(i);
            }
        }
        return null;
    }

    public boolean hasOption(String option) throws InvalidMarkupException {
        List<String> opts = getOptionsArray();
        for (int i = 0; i < opts.size(); i++) {
            if (opts.get(i).equalsIgnoreCase(option)) {
                return true;
            }
        }
        return false;
    }
}