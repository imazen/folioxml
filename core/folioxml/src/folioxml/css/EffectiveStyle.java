package folioxml.css;

import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//tracks the effective style for a single css effect, like 'display'
public class EffectiveStyle {


    private String key;
    private Map<String, Map<String, String>> stylesheet;

    public EffectiveStyle(String cssKey) {
        key = cssKey;
        stylesheet = new HashMap<>();
    }


    public void addStylesheet(SlxRecord root) throws InvalidMarkupException {

        for (SlxToken t : root.getTokens()) {
            if (t.matches("style-def")) {
                String cls = t.get("class");
                String style = t.get("style");
                String type = t.get("type");
                if (cls != null && style != null) {

                    if (!stylesheet.containsKey(cls)) {
                        stylesheet.put(cls, new HashMap<>());
                    }

                    Map<String, String> byClass = stylesheet.get(cls);

                    Map<String, String> css = CssUtils.parseCss(style, true);
                    if (css.containsKey(key)) {
                        byClass.put(simplifyType(type), css.get(key));
                    }
                }
            }
        }
    }

    private String simplifyType(String type) {
        if (type.equalsIgnoreCase("level")) return "div";
        if (type.equalsIgnoreCase("paragraph")) return "p";
        if (type.equalsIgnoreCase("link")) return "a";
        return "span";
    }

    public String stylesheetValueFor(SlxToken t) throws InvalidMarkupException {
        if (!t.isTag()) return null;
        String cls = t.get("class");
        if (cls == null) return null;
        Map<String, String> forClass = stylesheet.get(cls);
        if (forClass == null) return null;
        String type = "span";
        if (t.matches("record|div")) type = "div";
        if (t.matches("p")) type = "p";
        if (t.matches("link|a")) type = "a";

        return forClass.get(type);
    }

    private String extractValue(String css) {
        if (css == null) return null;
        Map<String, String> cssMap = CssUtils.parseCss(css, true);
        return cssMap.get(key);
    }

    private String extractValue(SlxToken t) throws InvalidMarkupException {
        return t.isTag() ? extractValue(t.get("style")) : null;
    }

    private String extractOrLookup(SlxToken t) throws InvalidMarkupException {
        String v = extractValue(t);
        if (v == null) v = stylesheetValueFor(t);
        return v;
    }

    public String getEffectiveValue(SlxToken t, List<SlxToken> parents) throws InvalidMarkupException {
        String result = extractOrLookup(t);
        for (SlxToken p : parents) {
            if (result != null) return result;
            result = extractOrLookup(p);
        }
        return result;
    }


}
