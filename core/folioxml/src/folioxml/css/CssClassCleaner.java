package folioxml.css;

import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.core.TokenBase;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Don't use this at the same time as SlxTranslator, since this will process tokens before SlxTranslator has finished adding the class names... (They're subsequent tags)
 * @author nathanael
 *
 */
public class CssClassCleaner {
	
	public CssClassCleaner(){
	}
	
	
	/**
	 * Map of namespace -> Map of 'originalName'.lowerCaseCultureEnglish -> (newName, originalName)
	 */
	public Map<String,Map<String,Pair<String,String>>> dict = new HashMap<String,Map<String,Pair<String,String>>>();
	/**
	 * Map of namespace -> Map of 'newName'.lowerCaseCultureEnglish -> (originalName)
	 */
	public Map<String,Map<String,String>> reverseDict = new HashMap<String,Map<String,String>>();
	
	
	/**
	 * Map of namespace -> Collection of newName.lowerCaseCultureEnglish for conflict checking. 
	 */
	public Map<String,HashSet<String>> valueDict = new HashMap<String,HashSet<String>>();
	
	/**
	 * Valid CSS names (and now, XML IDs) (A subset of the specification, since some browsers don't support all the spec)
	 */
	protected static Pattern pName = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9-]*$"); 
	//Removed leading dash. It wasn't allowed in XML IDs, and we need this multi-purpose. Was ^(-)?[_a-zA-Z][_a-zA-Z0-9-]*$
	
	/* XML 1.0 rev 5 spec for IDs
	 * [4]   	NameStartChar	   ::=   	":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
	  [4a]   	NameChar	   ::=   	NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
	 */
	
	/**
	 * Provides a CSS-compliant version of the specified identifier. 
	 * It is suggested that you process the infobase header <style-def/> tags first, since the 
	 * additional information they contain can assist in making more intelligent naming
	 * choices in the case of conflicts. (Such as Normal-highlighter).
	 * Appending a random 4-digit hex value such as "-f92a" is a last resort.
	 * 
	 * It is important to use the same CssClassCleaner instance across the entire infobase to maintain consistency (or save and restore the name table).
	 * The name table should be preserved so translation back to FFF can be performed.
	 * Storage in the root infobase record is a good idea...
	 * 
	 * @param name The class name to sanitize
	 * @param parent The token this class name originated from. Helps choose names in the case of conflicts.
	 * @return
	 * @throws InvalidMarkupException 
	 */
	public String cleanId(String name, String namespace, boolean throwExceptionIfDuplicate) throws InvalidMarkupException{
		//Namespaces are separated
		Map<String,Pair<String,String>> mappings = dict.get(namespace);
		if (mappings == null) {
			mappings = new HashMap<String,Pair<String,String>>();
			dict.put(namespace, mappings);
		}
		
		Map<String,String> reverseMappings = reverseDict.get(namespace);
		if (reverseMappings == null) {
			reverseMappings = new HashMap<String,String>();
			reverseDict.put(namespace, reverseMappings);
		}
		
		HashSet<String> values = valueDict.get(namespace);
		if (values == null) {
			values = new HashSet<String>();
			valueDict.put(namespace, values);
		}
		
		/*CSS identifiers
		-?[_a-z]|{nonascii}|{escape}([_a-z0-9-]|{nonascii}|{escape})*
		
		Simplified:
		-?[_a-zA-z][_a-zA-Z0-9-]*
		
		
		Folio names are case-insensitive, but preserve case.
		
		Do browsers limit length? Test this! We have names that are probably longer than 255.
	 	*/
		String lowerName = name.toLowerCase(Locale.ENGLISH);
		
		
		//TODO: What if there is both a character style named "Normal" and a link style named "Normal"? This needs the intelligence to know which Normal variant to use when the <span> or <link> tag is reached. Style-def isn't enough
        //What if character-style, field, and highlighter have overlapping names?
		//The map doesn't add new conflicts, but what if they already exist between style types?
		
		//First check if 'name' exists in the mappings.
		Pair<String,String> result = mappings.get(lowerName);
		
		//If so, return precomputed result.
		if (result != null) {
			if (throwExceptionIfDuplicate) throw new InvalidMarkupException("Duplicate mapping for (" + lowerName + ") encountered: " + result.getFirst() + " -> " + result.getSecond() + ". Please rename character styles, highlighters, and fields to use unique names; they cannot overlap in CSS.");
			return result.getFirst();
		}
		
		//If 'name' is valid anyways, cache to the Map
		/* Optmization causes problem when run twice on the same css class
		 * if (pName.matcher(name).matches()) {
		 
			
			
			mappings.put(lowerName, new Pair<String,String>(name,name));
			values.add(lowerName);
			return name; //Nothing to do - already a valid name.
		}else{*/
			String sanitizedName = sanitizeString(name);
			//Sanitize
			String newName = sanitizedName;
			String lowerNewName = newName.toLowerCase(Locale.ENGLISH);
			
			//Check for conflicts. Attempt style-def naming
			/*if (values.contains(lowerNewName) && parent != null && parent.matches("style-def")){
				//Append -type if present on parent and valid.
				String type = parent.get("type");
				if (type != null && pName.matcher(type).matches()){
					newName = sanitizedName + "-" + type;
					lowerNewName = newName.toLowerCase(Locale.ENGLISH);
				}
				
			}*/
			boolean foundConflict = false;
			//Check for conflicts. Generate 4-digit hex suffix.
			while (values.contains(lowerNewName)){
				newName = sanitizedName + "-" + Integer.toHexString(new Random().nextInt(256 * (256 - 16)) + (256 * 16));
				lowerNewName = newName.toLowerCase(Locale.ENGLISH);
				foundConflict = true;
			}
			if (foundConflict){
				for (Pair<String,String> pair: mappings.values()){
					if (pair.getFirst().equalsIgnoreCase(sanitizedName)){
						System.out.append("Found conflict in CSS name... " + name + " conflicted with existing entry " + pair.getSecond());
						break;
					}
				}
			}
			
			//Add mapping
			mappings.put(lowerName, new Pair<String,String>(newName,name));
			values.add(lowerNewName);
			reverseMappings.put(lowerNewName, name);
			
			//Return new name
			return newName;
		//}
	}
	
	private String sanitizeString(String name){
		//Remove all characters that don't match the regex.
		//When removing spaces, and the next character is lowercase, uppercase it.
		StringBuilder sb = new StringBuilder(name.length());

		boolean lastCharDelimiter = false;
		
		for (int i = 0; i < name.length(); i++){
			char c = name.charAt(i);

            //Allow [_a-zA-Z] as the first character
            // Aug 12. Removed hyphen allowance for XML ID compat
            //And the remainder allow [_a-zA-Z0-9-]
			boolean valid = isCharValid(c,i > 0,true,true,i > 0);

			if (valid){
				//Uppercase lowercase letters following a space.
				if (lastCharDelimiter && sb.length() > 0 && sb.charAt(sb.length() - 1) != '_'){
                    sb.append('_');
				}
				//Keep the character
				sb.append(c);
			}
            lastCharDelimiter = (c == ' ' || c == '.' || c == ',' || c == '+' || c == '/' || c == '\\' || c == '#' || c == '%' || c == '(' || c == ':');
		}
		return sb.toString();
	}
	
	private boolean isCharValid(char c, boolean allowHyphen, boolean allowUnderline, boolean allowAZ, boolean allowNumbers){
		if (c == '-' && allowHyphen) return true;
		if (c == '_' && allowUnderline) return true;
		if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && allowAZ) return true;
		if (allowNumbers && (c >= '0' && c <= '9')) return true;
		return false;
	}
	
	public void process(SlxToken t) throws InvalidMarkupException{
		process(t,getNamespace(t), getPrefix(t),false);
	}
		
	public void process(TokenBase t, String namespace, String prefix, boolean throwExceptionIfDuplicate) throws InvalidMarkupException{
		if (!t.isTag()) return; //Only tags have attributes
		
		//Requires SLX valid. 

		//For paragraphs, spans, links... 
		String s = t.get("class");
		if (s != null){
			String ns = cleanId(prefix + s,namespace,throwExceptionIfDuplicate);
			if (!s.equals(ns)) t.set("class", ns);
		}
		
		//For <bookmarks name="" and <link jumpdestination=""
		//For objects and bookmarks, use hash instead - don't CSS clean.
	}
	/**
	 * Must be called before .process().  TODO: Resolve replaceDefiniton=true first
	 * @param r
	 * @throws InvalidMarkupException
	 */
	public void processRootRecord(SlxRecord r) throws InvalidMarkupException{
		//Index style-def tags only. Then repeat and get the rest.
		for (TokenBase t:r.getTokens()){
			if (t.isTag() && t.matches("style-def")){
				process(t,getNamespace(t), getPrefix(t),true);
				//disabled at one point because of Neil's style class name changed quick fix was to disable
			}
		}
		
	}

	/**
	 objects, bookmarks, and popups are not processed. They are hashed.
	 * @param t
	 * @param attrName
	 * @return
	 * @throws InvalidMarkupException
	 */
	private String getNamespace(TokenBase t) throws InvalidMarkupException{
		/* Namespaces
		 * Character styles
			Link Styles
			Paragraph Styles
			Level Styles
			Highlighter Styles
			Field Styles
		 */
		String type = t.get("type");
		
		
		
		
		if (t.matches("p|paragraph-attribute") || (t.matches("style-def") && "paragraph".equalsIgnoreCase(type))) return "paragraph";
		if (t.matches("record|record-attribute") || (t.matches("style-def") && "level".equalsIgnoreCase(type))) return "level";
		if (t.matches("link|popupLink|a") || (t.matches("style-def") && "link".equalsIgnoreCase(type))) return "link"; //Correct for the 'class' attribute, but not for 'objectName'.
		
		//if (t.matches("span|style-def") && "character-style".equalsIgnoreCase(type)) return "character-style";
		//if (t.matches("span|style-def") && "highlighter".equalsIgnoreCase(type)) return "highlighter";
		
		//All other span tags are fields.
		//if (t.matches("span") || (t.matches("style-def") && TokenUtils.fastMatches("text|date|time|integer|decimal", type))) return "field";
		
		
		return "span";
	}
    private String getPrefix(TokenBase t) throws InvalidMarkupException{
		/* Namespaces
		 * Character styles
			Link Styles
			Paragraph Styles
			Level Styles
			Highlighter Styles
			Field Styles
		 */
        String type = t.get("type");
        if (t.matches("span|style-def") && "character-style".equalsIgnoreCase(type)) return "cs_";
        if (t.matches("span|style-def") && "highlighter".equalsIgnoreCase(type)) return "hl_";


        //All other span tags are fields.
        //if (t.matches("span") || (t.matches("style-def") && TokenUtils.fastMatches("text|date|time|integer|decimal", type))) return "";

        return "";
    }

	/**
	 * Returns the original name for the specified token based on the class attribute and tag name
	 * @param t
	 * @param cssClass
	 * @return
	 * @throws InvalidMarkupException
	 */
	public String findOriginalName(SlxToken t) throws InvalidMarkupException{
		return findOriginalName(getNamespace(t),t.get("class"));
	}
	
	/**
	 * Returns the original name for the specified cssClass use the token specified to determine the namespace.
	 * @param t
	 * @param cssClass
	 * @return
	 * @throws InvalidMarkupException
	 */
	public String findOriginalName(SlxToken t, String cssClass) throws InvalidMarkupException{
		return findOriginalName(getNamespace(t),cssClass);
	}
	
	/**
	 * Returns the original name for the specified cssClass
	 * @param t
	 * @param cssClass
	 * @return
	 * @throws InvalidMarkupException
	 */
	public String findOriginalName(String namespace, String cssClass) throws InvalidMarkupException{
		if (cssClass == null) return null;
		Map<String,String> mappings = reverseDict.get(namespace);
		if (mappings != null) {
			String s = mappings.get(cssClass.toLowerCase(Locale.ENGLISH));
			//if (pair == null)  return cssClass;
				//WARNING: TERRIBLE THING TO DO.... WILL BReAK EVERYTHING
			if (s == null) throw new InvalidMarkupException("Cannot find original css name for " + cssClass + " (in " + namespace + " namespace)");
			return s;//return pair.getSecond();
		}
		return null;
	}

	
	
	/**
	 * NOT IMPLEMENTED Saves the mappings to the specified record. Verify that root.level = "root". 
	 * Builds a map of String->SlxToken (<style-def> tags). 
	 * If a <style-def /> doesn't exist for the mapping, insert it after the last style-def. 
	 * originalName = "" attribute is where the original names are stored.
	 * 
	 * @param root
	 * @throws InvalidMarkupException 
	 */
	public void saveTo(SlxRecord root) throws InvalidMarkupException {
		
		// TODO: We need this for later - For converting back, and possible for reference
		for (String namespace: dict.keySet()){
			for (String key:dict.get(namespace).keySet()){
				Pair<String,String> pair = dict.get(namespace).get(key);
				SlxToken t = new SlxToken("<mapping from=\"" + pair.getSecond() + "\" to=\"" + pair.getFirst() + "\" namespace=\"" + namespace + "\" />");
				root.write(t);
				//System.out.println(t.toTokenString());
			}
		}
		
		
	}
	public void loadFrom(NodeList nodes) throws InvalidMarkupException{
		//Index style-def tags only. Then repeat and get the rest.
		///System.out.print(nodes.toXmlString(true));
		nodes = nodes.searchOuter(new NodeFilter("mapping"));
		for (TokenBase t: nodes.list()){
			String namespace = t.get("namespace");
			//Namespaces are separated
			Map<String,Pair<String,String>> mappings = dict.get(namespace);
			if (mappings == null) {
				mappings = new HashMap<String,Pair<String,String>>();
				dict.put(namespace, mappings);
			}
			Map<String,String> reverseMappings = reverseDict.get(namespace);
			if (reverseMappings == null) {
				reverseMappings = new HashMap<String,String>();
				reverseDict.put(namespace, reverseMappings);
			}
			
			HashSet<String> values = valueDict.get(namespace);
			if (values == null) {
				values = new HashSet<String>();
				valueDict.put(namespace, values);
			}
			String lowerName = t.get("from").toLowerCase(Locale.ENGLISH);
			
			mappings.put(lowerName, new Pair<String,String>(t.get("from"),t.get("to")));
			reverseMappings.put(t.get("to").toLowerCase(Locale.ENGLISH), t.get("from"));
			values.add(lowerName);
			
		}
		
		assert(nodes.count() > 1);
	}

	
}
