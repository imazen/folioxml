package folioxml.lucene;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.slx.SlxContextStack;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

import java.util.List;
import java.util.TreeMap;

public class FieldCollector {

    public FieldCollector(Document d, IndexFieldOptsProvider c) {
        this.d = d;
        this.c = c;
    }

    public Document d;

    public IndexFieldOptsProvider c;


    public TreeMap<String, StringBuilder> fields = new TreeMap<String, StringBuilder>(String.CASE_INSENSITIVE_ORDER);
    public TreeMap<String, Boolean> unflushed = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);


    protected void send(String field, String text) {
        if (!fields.containsKey(field)) fields.put(field, new StringBuilder());
        StringBuilder sb = fields.get(field);
        sb.append(text);
    }

    protected void flush(String field, boolean removeFromUnflushed) {
        //Adds the field to the document if it has more than 0 chars.
        if (removeFromUnflushed && unflushed.containsKey(field)) unflushed.remove(field);
        if (fields.containsKey(field)) {
            StringBuilder sb = fields.get(field);
            if (sb != null && sb.length() > 0) {
                d.add(new TextField(field, sb.toString(), Field.Store.YES));
                sb.setLength(0);
            }
        }
    }

    /*
     * Call this after all the tokens have been processed.
     */
    public void flush() {

        //add all ghostTags to lucene
        for (String key : fields.keySet()) {
            flush(key, true);
        }
    }


    /*
     * Returns true if the specified token has been 'eaten' by a field and should not be added to the main text.
     * Call this method after stack.process().
     */
    public boolean collect(SlxToken t, SlxContextStack stack, SlxRecord r) throws InvalidMarkupException {

        // For an opening field, if the field is in unflushed, and [mergefields], remove it from unflushed.
        // For a closing field, and [mergefields], add to the unflushed set. If [!mergefields], then flush the field value.
        if (t.matches("span")) {
            String fname = t.get("type");
            if (fname != null) {
                IndexFieldOpts fopts = c.getFieldOptions(fname);
                if (t.isOpening()) {
                    if (fopts.mergeTouchingApplications && unflushed.containsKey(fname))
                        unflushed.remove(fname);
                } else if (t.isClosing()) {
                    if (fopts.mergeTouchingApplications)
                        unflushed.put(fname, true);
                    else
                        flush(fname, true);
                }
            }
        }

        //Certain types of tokens break all fields, and should cause all fields to flush.
        //A closing paragraph token breaks all fields
        //A br token indexes as whitespace.
        //TODO: Test what td, th, and note do to phrase field terms..


        //The rest of this function deals only with text
        if (!t.isTextOrEntity()) return false;

        //The topmost item in the stack rules. It gets to determine whether the text is hidden or not.

        //1 Search for the topmost span that is flagging the node as hidden.
        String fieldHidingStuff = null;

        List<SlxToken> tags = stack.getOpenTags("span", false, false); //Must not be returning all the span tags... maybe a different query needed
        for (SlxToken g : tags) {
            String fname = g.get("type");
            if (fname == null) continue;
            IndexFieldOpts opts = c.getFieldOptions(fname);
            if (!opts.allowOthersToIndex) {
                fieldHidingStuff = fname;
                break;
            }
        }
        //Decode entities.
        String text = t.markup;
        if (t.isEntity()) text = TokenUtils.entityDecodeString(text);

        if (fieldHidingStuff != null) {
            send(fieldHidingStuff, text);
            return true;
        }
        //Send to each field on the stack
        for (SlxToken g : tags) {
            String fname = g.get("type");
            if (fname == null) continue;
            send(fname, text);
            //Make sure nothing on the unflushed stack is in the slx stack... that way we can flush the unflushed afterwards
            if (unflushed.containsKey(fname))
                unflushed.remove(fname);
        }
        //We have a publicly visible token, and everything in 'unflushed' is NOT on the stack.
        for (String unfl : unflushed.keySet()) {
            flush(unfl, false);
        }
        unflushed.clear();

        return false;
    }

}
