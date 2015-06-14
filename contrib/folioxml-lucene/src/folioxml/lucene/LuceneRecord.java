package folioxml.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.css.StylesheetBuilder;
import folioxml.lucene.SlxIndexingConfig.IndexingOptions;
import folioxml.slx.SlxContextStack;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import folioxml.xml.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class LuceneRecord { //Renamed to DocumentBuilder
    private SlxRecord r;
    private Document doc;
    private StringBuilder contentSb = null;
    private SlxContextStack stack = null;
    private SlxIndexingConfig conf;
    private FieldCollector coll;

    public LuceneRecord(SlxRecord r, SlxContextStack stack, SlxIndexingConfig conf) {
		super();
		this.r = r;
		this.stack = stack;
		this.contentSb = new StringBuilder();
		this.doc = new Document();
		this.coll = new FieldCollector(doc,conf);
		this.conf =  conf;
    }
    
    
    public Document process() throws InvalidMarkupException, IOException {
    	//We need the root record for the stylesheet 
		boolean isRoot = "root".equals(r.getLevelType());
	    
		Map<String, String> attr = r.getAttributes();
	
		if(conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.IndexRecordOrderNumber)){
			doc.add(addNonAnalyzedField("indexRecordOrderNumber", Integer.toString(conf.incrementIndexRecordOrderNumber())));
		}
		
		
		//Index CSS stylesheet on root record.
		if (isRoot){
			if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreGeneratedStylesheet))
				doc.add(new Field("stylesheetTag",new StylesheetBuilder(r).getCssAndStyleTags(),Field.Store.YES, Field.Index.NO)); //Don't analyze!
			if(conf.Indexing.contains(IndexingOptions.AutoIncrementIndexVersionNumber)){
				doc.add(new Field("indexVersion",String.valueOf(conf.incrementIndexVersion()).toString(),Field.Store.YES, Field.Index.NO));
			}
			//Store a list of levels (in the correct order) in the root record. Do not index it
			String levels = r.get("levels") != null ? r.get("levels") : r.get("levelDefOrder");
			doc.add(new Field("levels",levels,Field.Store.YES, Field.Index.NO));
			
			//Configure field indexing based on the .DEF file. Only if IgnoreFolioFlags is not specified.
			conf.UpdateFieldIndexingInfo(r);
		}
		
		
		// handles record level field
		if (r.getLevelType() == null) {
		    doc.add(addNonAnalyzedField("level", "Normal"));
		} else {
		    doc.add(addNonAnalyzedField("level", r.getLevelType()));
		}
		
		if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.IndexGroups))
			doc.add(addAnalyzedField("groups", attr.get("groups"))); //NDJ Sep 13, 2001: previous behavior was to only index groups on Normal level records.

        //NJ Jun 9 2015, dropping path indexing
		//doc.add(addNonAnalyzedField("fullPath",r.fullPath()));
		
		if (!isRoot){
			//doc.add(addNonAnalyzedField("folioId", attr.get("folioId")));
			//doc.add(addNonAnalyzedField("localPath",r.localPath()));
			//doc.add(addNonAnalyzedField("parentPath",r.parentPath()));
			
			SlxRecord temp = r;
			while (temp.parent != null){
				//doc.add(addNonAnalyzedField("parentPaths",temp.parentPath())); //Hey... different! paths, not path
				temp = temp.parent;
			}
			
			
			
		
			StringBuilder slx = new StringBuilder();
			
			boolean createText = conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.IndexText) ||
									conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreText);
			
			if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreSlx))
				slx.append(r.toSlxMarkup(false));
			
			for (SlxToken t : r.getTokens()) {
				stack.process(t);// call this on each token.

				boolean hidden = coll.collect(t, stack,r);
				
				if (createText){
					if (!hidden && t.isTextOrEntity()) { //Changed dec 17 to include whitespace... was causing indexing errors.. fields separated by whitespace were being joined.
					    String s = t.markup; 
					    if (t.isEntity()) s = TokenUtils.entityDecodeString(s);
					    contentSb.append(s);
					}
					if (t.matches("p|br|td|th|note") && !t.isOpening()) contentSb.append(" " + TokenUtils.entityDecodeString("&#x00A0;") + " ");
				}
	
				
				//TODO: add support for heading queries, etc.
				/*if (stack.find("span", "recordHeading", false) != null && t.isTextOrEntity()) {
				    doc.add(addKeywordField("recordHeading", t.markup));
				}*/
				
			}
			if (createText)
				doc.add(new Field(conf.textField,contentSb.toString(),
						conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreText) ? Field.Store.YES : Field.Store.NO,
						// original: 	conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.IndexText) ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED));
						conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.IndexText) ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED,
				        conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.TermVectorWithPositionOffsetsText) ? Field.TermVector.WITH_POSITIONS_OFFSETS : Field.TermVector.NO
				));
								
			
			doc.add(new Field("title",r.getFullHeading(" - ",true,2),Field.Store.YES, Field.Index.NO));
			doc.add(new Field("heading",r.get("heading"),Field.Store.YES, Field.Index.NO));
			
			if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreSlx))
				doc.add(new Field("slx",slx.toString(),Field.Store.YES, Field.Index.NO));
			
			
			coll.flush();
		}
		
		XmlRecord rx = new SlxToXmlTransformer().convert(r);
		
		
		if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreXml)){
			NodeList nl = new NodeList(rx);
			if (conf.xmlFilter != null) {
				nl = conf.xmlFilter.process(nl.deepCopy());
			}
			doc.add(new Field("xml",nl.toXmlString(false),Field.Store.YES, Field.Index.NO));
		}

		if (conf.Indexing.contains(SlxIndexingConfig.IndexingOptions.StoreXhtml)){
			NodeList nl = new NodeList(rx);
			if (conf.xhtmlFilter != null) nl = conf.xhtmlFilter.process(nl.deepCopy());
			doc.add(new Field("contentXML",nl.toXmlString(false),Field.Store.YES, Field.Index.NO)); //NDJ Sept 12 2011, was:  Field.Index.ANALYZED));
		}
		
		return doc;
    }

    private Field addNonAnalyzedField(String name, String value) {
	return new Field(name, value.toLowerCase(Locale.ENGLISH).trim(), Field.Store.YES,
		Field.Index.NOT_ANALYZED_NO_NORMS);
    }
    private Field addNonAnalyzedNoStoreField(String name, String value) {
		return new Field(name, value.toLowerCase(Locale.ENGLISH).trim(),
				Field.Store.NO, Field.Index.NOT_ANALYZED);
	}
    

    private Field addAnalyzedField(String name, String value) {
    	if  (value == null) value = ""; //Some records have no groups... causing null
    	return new Field(name, value, Field.Store.YES, Field.Index.ANALYZED);
    }
    @Override
    public String toString(){
		StringBuilder sb = new StringBuilder();
		if(doc != null){
		    List fields = doc.getFields();
	        
		    for (int i = 0; i < fields.size(); i++){
			Fieldable f = ((Fieldable)fields.get(i));
			sb.append(f.name() + " : " + f.stringValue() + "\n");
		    }
		}else {
		    sb.append("null");
		}
        return sb.toString();
    }
}
