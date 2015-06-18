package folioxml.css;

import folioxml.core.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class CssUtils {
	
    /**
     * Jan 20, 09. Can't use posessive quantifiers here - sorry.
     */
    public static String CommentRegex = "\\/\\*(.*?)\\*\\/";
        /**
     * Matches a comment tag and any intermediate comments. Lazy, of course.
     */
    private static Pattern rComment = Pattern.compile( CommentRegex,Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	
	public static Map<String,String> parseCss(String css, boolean expandBorderPaddingToParts){
		HashMap<String,String> map = new HashMap<String,String>();
		
		if (css == null) return map; //Empty css
		
		//Strip all comments
		css = rComment.matcher(css).replaceAll(" ");
		
		//Split into pairs (watch for escapes!)
		String[] pairs = css.split("(?!\\\\);");
		
		
		for(String s:pairs){
			s = s.trim();
			if (s.length() == 0) continue;
			
			String[] parts = s.split("(?!\\\\):");
			assert(parts.length == 2): s; //Or we have invalid syntax.
			
			String key = parts[0].trim().toLowerCase(Locale.ENGLISH);
			String value = parts[1].trim();
			if (expandBorderPaddingToParts && (key.equalsIgnoreCase("border") || key.equalsIgnoreCase("padding"))){
				map.put(key + "-top", value);
				map.put(key + "-left", value);
				map.put(key + "-right", value);
				map.put(key + "-bottom", value);
			}else{
				map.put(key, value);
			}
		}		
		return map;
	}

	public static List<Pair<String,String>> parseCssAsList(String css, boolean expandBorderPaddingToParts){
		List<Pair<String,String>> pairs = new ArrayList<Pair<String,String>>();

		if (css == null) return pairs; //Empty css

		//Strip all comments
		css = rComment.matcher(css).replaceAll(" ");

		//Split into pairs (watch for escapes!)
		String[] spairs = css.split("(?!\\\\);");


		for(String s:spairs){
			s = s.trim();
			if (s.length() == 0) continue;

			String[] parts = s.split("(?!\\\\):");
			assert(parts.length == 2): s; //Or we have invalid syntax.

			String key = parts[0].trim().toLowerCase(Locale.ENGLISH);
			String value = parts[1].trim();
			if (expandBorderPaddingToParts && (key.equalsIgnoreCase("border") || key.equalsIgnoreCase("padding"))){
				pairs.add(new Pair<String, String>(key + "-top", value));
				pairs.add(new Pair<String, String>(key + "-left", value));
				pairs.add(new Pair<String, String>(key + "-right", value));
				pairs.add(new Pair<String, String>(key + "-bottom", value));
			}else{
				pairs.add(new Pair<String, String>(key, value));
			}
		}
		return pairs;
	}
	/**
	 * Combines matching padding-left,-right,-top, -bottom elements into a single pair. (Also handles border and margin).
	 * Leaves them expaned if they 
	 * @param map
	 */
	public static void coalesce(Map<String,String> map){
		String[] names = new String[]{"border", "margin","padding"};
		String[] sides = new String[]{"-left","-top","-bottom","-right"};
		
		for (String name:names){
			
			String shared = null;
			for (String side:sides){
				String val = map.get(name + side);
				if (val == null) {shared = null; break; }//All sides must be non-null
				if (shared == null) shared = val; //First item must init the checker
				
				if (!val.equalsIgnoreCase(shared)){
					shared = null;
					break; //They don't all match - quit for this name.
				}
			}
			if (shared != null){
				if (map.containsKey(name) && !map.get(name).equalsIgnoreCase(shared)){
					//The generic one already exists. skip name
					continue;
				}
				//Clean out the indiviudal mappings
				for (String side:sides){
					map.remove(name + side); 
				}
				//Add the generic mapping
				map.put(name, shared);
			}
			
		}
	}
	
	public static String writeCss(Map<String,String> map){
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> pair:map.entrySet()){
			sb.append(pair.getKey() + ":" + pair.getValue() + ";");
		}
		return sb.toString();
	}
	
}
