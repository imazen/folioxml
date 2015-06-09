package folioxml.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import folioxml.lucene.analysis.AnalyzerPicker;
import folioxml.lucene.analysis.DynamicAnalyzer;
import folioxml.lucene.analysis.ListAnalyzer;
import folioxml.lucene.analysis.LowercaseKeywordAnalyzer;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;

import java.util.EnumSet;
import java.util.TreeMap;

public class SlxIndexingConfig implements AnalyzerPicker, IndexFieldOptsProvider {
	
	public SlxIndexingConfig(){
		fields.put("groups", new IndexFieldOpts(new ListAnalyzer()));
		fields.put("fullpath", new IndexFieldOpts(new LowercaseKeywordAnalyzer()));
		fields.put("parentpath", new IndexFieldOpts(new LowercaseKeywordAnalyzer()));
		fields.put("localpath", new IndexFieldOpts(new LowercaseKeywordAnalyzer()));

	}

    public static SlxIndexingConfig FolioQueryCompatible() {
		SlxIndexingConfig c = new SlxIndexingConfig();
		c.Indexing = EnumSet.of(IndexingOptions.IndexText, IndexingOptions.IndexNotesAndPopups, IndexingOptions.IndexGroups);
		return c;
	}
	
	
	
	public enum IndexingOptions{
		StoreXml, 
		StoreGeneratedStylesheet, 
		StoreSlx, 
		StoreText, 
		StoreRecordIdPaths,
		StoreXhtml, 		
		IndexText, 
		IndexNotesAndPopups, 
		IndexGroups,
		TermVectorWithPositionOffsetsText,
        /*
            Adds an incrementing integer field to each record as 'indexRecordOrderNumber'
         */
		IndexRecordOrderNumber,
		AutoIncrementIndexVersionNumber,
	}
	public EnumSet<IndexingOptions> Indexing = EnumSet.allOf(IndexingOptions.class);
	
	public enum DebugOptions{
		SilenceMarkupWarnings, SilenceQueryLinkErrors, SilenceProgressInfo
	}
	public EnumSet<DebugOptions> Debug = EnumSet.of(DebugOptions.SilenceMarkupWarnings);
	
	
	public enum FolioIndexingFlags{
		IgnoreFolioFlags, UseFolioFlags, FallbackToFolioFlags
	}
	
	public FolioIndexingFlags FolioFieldIndexing = FolioIndexingFlags.FallbackToFolioFlags;
	
	//keep track of indexRecordOrderNumber required for IndexRecordOrderNumber field 
	private int indexRecordOrderNumber;

	
	public int incrementIndexRecordOrderNumber(){
		return indexRecordOrderNumber = indexRecordOrderNumber + 10;
	}
	
	//keep track of indexVersion
	private int indexVersion = 0;
	
	public void setIndexVersion(int version){
		indexVersion = version;
	}
	public int incrementIndexVersion(){
		return indexVersion++;
	}
	
	/**
	 * The default field; where to index the infobase contents
	 */
	public String textField = "contents";
	
	public Analyzer textAnalyzer = new StandardAnalyzer(Version.LUCENE_33);
	
	public NodeListProcessor xhtmlFilter = null;

	public NodeListProcessor xmlFilter = null;
	
	IndexFieldOpts defaultOpts = new IndexFieldOpts(new StandardAnalyzer(Version.LUCENE_33));
	
	public TreeMap<String,IndexFieldOpts> fields = new TreeMap<String,IndexFieldOpts>(String.CASE_INSENSITIVE_ORDER);
	
	/*
	 * This method is called after the root node has been processed. This allows the folio field indexing options to be applied, if FolioFieldIndexing != IgnoreFolioFlags
	 */
	public void UpdateFieldIndexingInfo(SlxRecord root) throws InvalidMarkupException{
		if (FolioFieldIndexing == FolioIndexingFlags.IgnoreFolioFlags) return;
		
		//<style-def type="field" fieldType="text|date|time|integer|decimal" format="[format string]" class="field name" indexOptions="TF,PF,TE,NO,PR,DT,FP,SW" />
		for (SlxToken t : root.getTokens()) {
			if (!t.isTag()) continue;
			if (t.matches("style-def") && TokenUtils.fastMatches("field", t.get("type"))){
				IndexFieldOpts iopts = getOptions(t);
				String fieldName = t.get("styleName");
				if (iopts != null){
					if (FolioFieldIndexing == FolioIndexingFlags.UseFolioFlags || !fields.containsKey(fieldName))
						fields.put(fieldName, iopts);
				}
			}
		}
	}
	protected IndexFieldOpts getOptions(SlxToken t) throws InvalidMarkupException{
		if (!TokenUtils.fastMatches("text", t.get("fieldType"))) return null; //We only parse text fields right now. Others aren't implemented yet.
		//Split the options
		String sOpts = t.get("indexOptions");
		if (sOpts == null) return new IndexFieldOpts(new String[]{});
		String[] opts = sOpts.split(",");
		IndexFieldOpts fo = new IndexFieldOpts(opts);
		return fo;
	}

	public IndexFieldOpts getFieldOptions(String fieldName) {
		if (fields.containsKey(fieldName)){
			IndexFieldOpts a = fields.get(fieldName);
			if (a != null) return a;
		}
		return defaultOpts;
	}
	
	
	public Analyzer getAnalyzer(String fieldName) {
		if (fieldName.equals(this.textField)) return textAnalyzer;
		if (fields.containsKey(fieldName)){
			Analyzer a = fields.get(fieldName).fieldAnalyzer;
			if (a != null) return a;
		}
		return defaultOpts.fieldAnalyzer;
	}
	
	
	
	public Analyzer getRecordAnalyzer(){
		return new DynamicAnalyzer(this);
    }
}
