package folioxml.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TokenUtils {

    /**
     * Returns true if the specified string contains only [A-Za-z0-9-]
     *
     * @param s
     * @return
     */
    public static boolean isPlaintext(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i); //Return false if c isn't one of the allowed values.
            if (!(c == '-' ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= 'a' && c <= 'z') ||
                    (c >= '0' && c <= '9')
            )) return false;
        }
        return true;
    }

    /**
     * Returns true if the string is composed of whitespace [  \t\n\x0B\f\r] or is empty.
     *
     * @param s
     * @return
     */
    public static boolean isWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i); //Return false if c isn't one of the allowed values.
            if (!(c == ' ' ||
                    c == '\t' ||
                    c == '\n' ||
                    c == '\u000b' ||
                    c == '\f' ||
                    c == '\r')
                    ) return false;
        }
        return true;
    }

    /**
     * Returns true if the specified string contains only [A-Za-z0-9-] and |
     *
     * @param s
     * @return
     */
    protected static boolean isAlternation(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i); //Return false if c isn't one of the allowed values.
            if (!(c == '-' || c == '|' ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= 'a' && c <= 'z') ||
                    (c >= '0' && c <= '9')
            )) return false;
        }
        return true;
    }

    /**
     * Case-insensitive. If s.length() == 0, false is always returned.
     *
     * @param alt
     * @param s
     * @return
     */
    protected static boolean matchesAlternation(String alt, String s) {
        if (s.length() == 0) return false;
        //A = index of current starting |
        //B = index of current ending |
        int a = -1; //Imaginary | before beginning of alt
        int b = -1;
        while (b < alt.length() - 1) {
            a = b;
            b = alt.indexOf('|', a + 1); //Find the next alternation |
            if (b < 0) b = alt.length(); //Imaginary | after the ending of alt.
            if (a + s.length() + 1 == b) { //s.length() must exactly match the distance between a and b for a valid match. Can't do an inequality without opening substring loophole.
                if (alt.regionMatches(true, a + 1, s, 0, s.length())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static Pattern pPlaintext = Pattern.compile("^[A-Za-z0-9\\-]*+$");
    protected static Pattern pSimpleAlternation = Pattern.compile("^[A-Za-z0-9\\-\\|]*+$");

    /**
     * Returns true if the tag name (case-insensitive) matches the regex. Tries an simple case-insenstive compare first, then an alternation-sensitive compare, then a full case-insensitive regex.
     * Both successful and failed matches are cached in a HashSet by hashcode.
     *
     * @param regex
     * @return
     */
    public static boolean fastMatches(String regex, String name) {
        if (name == null || regex == null) return false;
        
        /* This slows things down... 155 seconds vs...195
         * All I can say is... HashSet must be awful fast to beat char comparisons.
        if (isPlaintext(regex)){
        	return regex.equalsIgnoreCase(name); //Optimization - quick answer for 60% of cases
        }else if (isAlternation(regex)){
        	return matchesAlternation(regex,name); //For the other 30%
        }
        */

        //HASHING - DANGEROUS.
        Integer pair = Integer.valueOf(name.hashCode() ^ ((regex.hashCode() ^ 0xf0f0f0f) >>> 32));


        //TODO: run asserts here to check validity of hashing
        //Double-check cached to non-cached results

        if (cached_matches == null) cached_matches = new HashSet<Integer>(8000); //Not too many valid combinations
        if (cached_failures == null)
            cached_failures = new HashSet<Integer>(40000); //n^2 invalid combinations. Guessing at 120^2

        //failures are 60x more common
        if (cached_failures.contains(pair))
            return false;
        if (cached_matches.contains(pair))
            return true;

        //Never encountered before??
        boolean result = fastMatchesNonCached(regex, name);
        if (result) cached_matches.add(pair);
        else cached_failures.add(pair);

        return result;
    }

    private static Set<Integer> cached_matches = null;
    private static Set<Integer> cached_failures = null;


    /**
     * Returns true if the tag name (case-insensitive) matches the regex. Tries an simple case-insenstive compare first, then an alternation-sensitive compare, then a full case-insensitive regex.
     *
     * @param regex
     * @return
     */
    public static boolean fastMatchesNonCached(String regex, String name) {
        if (name == null || regex == null) return false;

        if (isPlaintext(regex)) {
            return regex.equalsIgnoreCase(name); //Optimization - quick answer for 60% of cases
        } else if (isAlternation(regex)) {
            return matchesAlternation(regex, name); //For the other 30%
        }
        /*
        //Alternation compaare
        if (regex.indexOf('|') >= 0 && pSimpleAlternation.matcher(regex).matches()){

           String[] options = regex.split("\\|");
           for (int i =0; i < options.length; i++){
               if (name.equalsIgnoreCase(options[i])) return true;
           }
           return false; //It's a simple alternation. Compiling a regex won't make a difference
        }
*/

        return matchesCI(regex, name);
    }

    /**
     * Case-insensitive version of matches()
     *
     * @param regex
     * @param s
     * @return
     */
    public static boolean matchesCI(String regex, String s) {
        Pattern p = getPatternCachedCI(regex);//Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(s);
        return m.matches();
    }

    private static HashMap<String, Pattern> cachedPatterns;

    public static Pattern getPatternCachedCI(String regex) {
        if (cachedPatterns == null) cachedPatterns = new HashMap<String, Pattern>(2000);
        Pattern p = cachedPatterns.get(regex);
        if (p == null) {
            p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            cachedPatterns.put(regex, p);
        }
        return p;

    }
    
    /*  XML spec
     * 4.1 Character and Entity References

[Definition: A character reference refers to a specific character in the ISO/IEC 10646 character set, for example one not directly accessible from available input devices.]

Character Reference

[66]   	CharRef	   ::=   	'&#' [0-9]+ ';'
| '&#x' [0-9a-fA-F]+ ';'	[WFC: Legal Character]
Well-formedness constraint: Legal Character

Characters referred to using character references must match the production for Char.

If the character reference begins with " &#x ", the digits and letters up to the terminating ; provide a hexadecimal representation of the character's code point in ISO/IEC 10646. If it begins just with " &# ", the digits up to the terminating ; provide a decimal representation of the character's code point.

[Definition: An entity reference refers to the content of a named entity.] [Definition: References to parsed general entities use ampersand (&) and semicolon (;) as delimiters.] [Definition: Parameter-entity references use percent-sign (%) and semicolon (;) as delimiters.]
    
    Named entities: 
     *[4]   	NameStartChar	   ::=   	":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
[4a]   	NameChar	   ::=   	NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
     *
     */

    /**
     * Decodes entity references  (XML character refs and named, doesn't yet support the HTML list)
     */
    public static String attributeDecode(String s) {
        return entityDecodeString(s);
    }

    /**
     * Decodes the name of an entity to its value. Ex, "quot" -> "
     * Currently only supports XML 1.0 basic entities (char references and the 5 named).
     * XHTML entities on todo list.
     *
     * @param s
     * @return
     */
    private static String decodeEntityValue(String s) {
        if (s == null || s.length() == 0) return null;
        //Most common first (these are all the XML 1.0 entities)
        if (s.equalsIgnoreCase("apos")) return "'";
        if (s.equalsIgnoreCase("quot")) return "\"";
        if (s.equalsIgnoreCase("amp")) return "&";
        if (s.equalsIgnoreCase("lt")) return "<";
        if (s.equalsIgnoreCase("gt")) return ">";

        //Character references
        if (s.charAt(0) == '#' && s.length() > 1) {
            try {
                if (s.charAt(1) == 'x') {
                    return new String(Character.toChars(Integer.parseInt(s.substring(2), 16)));
                }
                return new String(Character.toChars(Integer.parseInt(s.substring(1))));
            } catch (NumberFormatException nfe) {
                //Invalid entity.
                return null;
            }
        }
        //Named references
        //TODO: add support (separate class, hashtree lookup) for all XHTML entities in http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references

        return null;
    }

    /**
     * Decodes all entities found in the specified string. Unrecognized entities are ignored.
     *
     * @param s
     * @return
     */
    public static String entityDecodeString(String s) {
        StringBuilder sb = new StringBuilder();
        boolean insideEntity = false;
        int entityStart = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (insideEntity) {

                if (c == ' ' || c == '&') {
                    sb.append(s.substring(entityStart, i)); //Flush that false entity out. No decoding
                    //TODO: Add validation warning for invalid characters in SLX attributes.
                    insideEntity = false;
                } else if (c == ';') {
                    insideEntity = false;
                    String result = decodeEntityValue(s.substring(entityStart + 1, i));
                    if (result == null) {
                        //Invalid entity.
                        sb.append(s.substring(entityStart, i)); //Flush that false entity out. No decoding
                        //TODO: Add validation warning for invalid entities in SLX attributes.
                    } else {
                        sb.append(result);
                        continue; //We don't need to process the trailing semicolon.
                    }

                } else {
                    //We skip characters when (insideEntity == true)
                    continue;
                }
            }

            if (c == '&') {
                insideEntity = true;
                entityStart = i;
                continue;
            } else {
                sb.append(c);
            }
        }
        //Flush last bit out if needed. It's impossible for an valid entity to cause this - the semicolon would finish it.
        if (insideEntity) sb.append(s.substring(entityStart));
        return sb.toString();
    }

    /**
     * Encodes the 5 special XML characters > < " ' and &
     *
     * @param s
     * @return
     */
    public static String attributeEncode(String s) {
        return entityEncode(s);
    }

    /**
     * Encodes the 5 special XML characters > < " ' and &. use lightEntityEncode for text bodies.
     * TODO: Does this handle << properly? Create a unit test to make sure things are decoded properly
     *
     * @param s
     * @return
     */
    public static String entityEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"') sb.append("&quot;");
            else if (c == '\'') sb.append("&apos;");
            else if (c == '<') sb.append("&lt;");
            else if (c == '>') sb.append("&gt;");
            else if (c == '&') sb.append("&amp;");
            else sb.append(c);
        }
        return sb.toString();
    }


    /**
     * Encodes the 2 special XML characters for body text: &lt; and &amp;
     * TODO: Does this handle << properly? Create a unit test to make sure things are decoded properly
     *
     * @param s
     * @return
     */
    public static String lightEntityEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') sb.append("&lt;");
            else if (c == '&') sb.append("&amp;");
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String lightEntityEncodeAndConvertFolioBrackets(String s) {
        StringBuilder sb = new StringBuilder();
        boolean lastWasBracket = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                if (!lastWasBracket)
                    sb.append("&lt;");
                lastWasBracket = true;
            } else if (c == '&') {
                sb.append("&amp;");
                lastWasBracket = false;
            } else {
                sb.append(c);
                lastWasBracket = false;
            }

        }
        return sb.toString();
    }


}