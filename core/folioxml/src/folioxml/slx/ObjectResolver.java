package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Works with both XML and SLX valid. Parses object definitions from the root record into a table. Expands &lt;OB> object references to include the object definition data.
 * Unless you plan on doing dynamic reference expansion using javascript, you probably want your objects inlined. Object definitions don't include much more than the path and type of the data.
 *
 * @author nathanael
 */
public class ObjectResolver {

    SlxRecord root = null;

    public ObjectResolver(SlxRecord rootRecord) {
        root = rootRecord;
    }


    Map<String, Map<String, String>> defs = null;


    private void parseDefs() throws IOException, InvalidMarkupException {
        if (defs != null) return; //Don't parse twice
        defs = new HashMap<String, Map<String, String>>();

        ISlxTokenReader reader = root.getTokenReaderForRecord();

        while (reader.canRead()) {
            SlxToken t = reader.read();
            if (t == null) break;

            if (t.matches("object-def")) {
                String key = t.get("name").toLowerCase(Locale.ENGLISH);
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.putAll(t.getAttributes());
                attrs.remove("name"); //Remove the name attribute. No longer wanted... it's the key.
                if ("true".equalsIgnoreCase(attrs.get("replaceDefinition")) || !defs.containsKey(key)) {
                    defs.put(key, attrs); //Store if no existing key exists, or if replaceDefiniton is specified.
                    attrs.remove("replaceDefinition"); //No need for it anymore - it's been used.
                }
            }
        }
        //Done.
    }

    /**
     * Fixes all the tokens within the record.
     *
     * @param r
     * @throws InvalidMarkupException
     * @throws IOException
     */
    public void fixRecord(SlxRecord r) throws IOException, InvalidMarkupException {
        ISlxTokenReader reader = r.getTokenReaderForRecord();

        while (reader.canRead()) {
            SlxToken t = reader.read();
            if (t == null) break;
            fixToken(t);
        }
    }

    /**
     * If the specified token is an &lt;object> tag, it is resolved, and modified to include definition data. An exception is thrown if the reference doesn't exist in the definition.
     *
     * @param t
     * @throws InvalidMarkupException
     * @throws IOException
     */
    public void fixToken(SlxToken t) throws InvalidMarkupException, IOException {
        if (!t.isTag()) return;
        if (!t.matches("object|link")) return;


        String key = null;

        if (t.matches("link")) {
            key = t.get("objectName"); //OL (Object link)
            if (key == null) key = t.get("dataLink"); //DL (Data link)

            if (key == null || t.get("infobase") != null)
                return; //Not the right type of link. Only local infobase links are supported.
        } else {
            key = t.get("name");
        }
        key = key.toLowerCase(Locale.ENGLISH);

        parseDefs();//Make sure defs are parsed.

        Map<String, String> newAttrs = defs.get(key);
        if (newAttrs == null)
            throw new InvalidMarkupException("Object named \"" + t.get("name") + "\" was not found in the definiton.", t);

        //TODO: Isn't type a required attribute?

        if (t.matches("object") && newAttrs.get("type") != null && newAttrs.get("type").equalsIgnoreCase("data-link"))
            throw new InvalidMarkupException("Data-link objects cannot be embedded, only linked to.");

        //Ok, replace with the new attrs. (is this what we want)?
        t.getAttributes().putAll(newAttrs);

        //Link tags need special help.
        if (t.matches("link")) {

            //Type may be any of 'folio', 'data-link', 'ole', or 'class-object', as the attributes from the object definition have been merged in
            if (TokenUtils.fastMatches("folio", t.get("type"))) {

                //<link class="Object" handler="Bitmap" objectName="Faircom Logo - Used in Folio Validator" src="FolioHlp\FFF6.OB" type="folio">
                //Rename 'src' to 'href'.
                if (t.get("src") != null) {
                    t.set("href", t.get("src"));
                    t.removeAttr("src");
                }
            } else if (TokenUtils.fastMatches("data-link", t.get("type"))) {
                //Rename 'src' to 'href'.
                if (t.get("src") != null) {
                    t.set("href", t.get("src"));
                    t.removeAttr("src");
                }
            } else
                throw new InvalidMarkupException("OL (Object Links) cannot point to OLE or class objects. Type=" + t.get("type"));

        }

        t.updateMarkup(); //We're done! Object resolved - definition arguments have been copied. The 'type' value will be replaced, but that's good.

    }
}
