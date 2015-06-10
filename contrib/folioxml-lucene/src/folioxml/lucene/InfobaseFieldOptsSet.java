package folioxml.lucene;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.lucene.analysis.AnalyzerPicker;
import folioxml.lucene.analysis.ListAnalyzer;
import folioxml.lucene.analysis.LowercaseKeywordAnalyzer;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import folioxml.xml.Node;
import folioxml.xml.XmlRecord;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import java.util.TreeMap;


public class InfobaseFieldOptsSet implements IndexFieldOptsProvider, AnalyzerPicker {


    public InfobaseFieldOptsSet(XmlRecord root) throws InvalidMarkupException {
        fields.put("groups", new IndexFieldOpts(new ListAnalyzer()));
        fields.put("bookmarks", new IndexFieldOpts(new ListAnalyzer()));
        fields.put("folioSectionHeading", new IndexFieldOpts(new LowercaseKeywordAnalyzer()));

        //<style-def type="field" fieldType="text|date|time|integer|decimal" format="[format string]" class="field name" indexOptions="TF,PF,TE,NO,PR,DT,FP,SW" />
        for (Node t : root.children.filterByTagName("style-def",true).list()) {
            if (TokenUtils.fastMatches("field", t.get("type"))){
                IndexFieldOpts indexOpts = getOptions(t);
                String fieldName = t.get("styleName");
                if (indexOpts != null && !fields.containsKey(fieldName)){
                    fields.put(fieldName, indexOpts);
                }
            }
        }
    }

    private IndexFieldOpts getOptions(Node t) throws InvalidMarkupException {
        if (!TokenUtils.fastMatches("text", t.get("fieldType"))) return null; //We only parse text indexing fields right now. Others aren't implemented yet.
        //Split the options
        String sOpts = t.get("indexOptions");
        return  new IndexFieldOpts((sOpts == null) ? new String[]{} : sOpts.split(","));
    }

    IndexFieldOpts defaultOpts = new IndexFieldOpts(new StandardAnalyzer(Version.LUCENE_33));

    Analyzer textContentsAnalyzer =  new StandardAnalyzer(Version.LUCENE_33);

    public TreeMap<String,IndexFieldOpts> fields = new TreeMap<String,IndexFieldOpts>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public IndexFieldOpts getFieldOptions(String fieldName) {
        if (fields.containsKey(fieldName)){
            IndexFieldOpts a = fields.get(fieldName);
            if (a != null) return a;
        }
        return defaultOpts;
    }

    @Override
    public String getDefaultField() {
        return "contents";
    }

    public static String getStaticDefaultField() {
        return "contents";
    }

    public Analyzer getAnalyzer(String fieldName) {
        if (fieldName.equals(getDefaultField())) return textContentsAnalyzer;
        if (fields.containsKey(fieldName)){
            Analyzer a = fields.get(fieldName).fieldAnalyzer;
            if (a != null) return a;
        }
        return defaultOpts.fieldAnalyzer;
    }
}
