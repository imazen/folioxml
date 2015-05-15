package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.css.CssClassCleaner;
import folioxml.folio.FolioTokenReader;
import folioxml.translation.SlxTranslatingReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Reads and transforms a stream of compatibilty slx into valid slx.
 * Auto-creates a root record to contain all incoming tokens.
 * 
 * Call .saveCssTable() after completion so that transformation back to FFF is possible.
 * @author nathanael
 */
public class SlxRecordReader {


    public SlxRecord getRootRecord(){
    	return root;
    }

    public SlxRecordReader(ISlxTokenReader stream, ISlxTokenWriter postTransformFilter, boolean require_record_ids) throws InvalidMarkupException {
        this.stream = stream;
        this.defaultFilter = postTransformFilter;
        root = nextRecordTag = new SlxRecord("<record level=\"root\">");
        cssCleaner = new CssClassCleaner();
        objectResolver = new ObjectResolver(root);
        this.requireRecordIds = require_record_ids;
    }

    public SlxRecordReader(ISlxTokenReader stream) throws InvalidMarkupException {
        this(stream, null, true);
    }
    
    /**
     * Reads a folio flat file into an SlxRecord stream
     * @param f
     * @throws InvalidMarkupException
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public SlxRecordReader(File f) throws InvalidMarkupException, UnsupportedEncodingException, FileNotFoundException, IOException {
        this(new SlxTranslatingReader(new FolioTokenReader(f)), null, true);
    }

    /**
     * Reads a folio flat file into an SlxRecord stream
     * @param f
     * @throws InvalidMarkupException
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public SlxRecordReader(File f, boolean require_record_ids) throws InvalidMarkupException, UnsupportedEncodingException, FileNotFoundException, IOException {
        this(new SlxTranslatingReader(new FolioTokenReader(f)), null, require_record_ids);
    }
    
    private boolean requireRecordIds = true;

    private int recordIdsPresent = 0;
    private int recordIdsMissing = 0;

    private ISlxTokenReader stream = null;
    private ISlxTokenWriter defaultFilter = null;
    
    private SlxRecord root = null;
    private CssClassCleaner cssCleaner = null;
    private ObjectResolver objectResolver = null;
    
    /**
     * Points to the last parsed record that has a level indicator. This can be used to determine the next record's parent.
     */
    private SlxRecord lastLevelRecord = null;
    /**
     * Points to the record token that was encountered on the last read(), and terminated it.
     */
    private SlxRecord nextRecordTag = null;
    
    private Set<String> processedIds = new HashSet<String>();
	public boolean silent;

    /**
     * Returns a reference to the CssClassCleaner in use. 
     * @return
     */
    public CssClassCleaner getCssCleaner(){
    	return cssCleaner;
    }
    public ObjectResolver getObjectResolver(){
    	return objectResolver;
    }
    /**
     * Saves the CSS mapping to the root.
     * @throws InvalidMarkupException 
     */
    //public void saveCssTable() throws InvalidMarkupException{
   // 	cssCleaner.saveTo(root);
    //}
    
    /**
     * Returns the next record. The first call to read() will return the root record, which contains the infobase-level data and is parent to all records.
     * @return
     * @throws java.io.IOException
     */
    public SlxRecord read() throws IOException, InvalidMarkupException {
        return read(null);
    }
    
    public void close() throws IOException{
        //Complain about missing record IDs.
        if (recordIdsMissing > 0 ){
            System.out.println(Integer.toString(recordIdsMissing) + " of " +Integer.toString (recordIdsMissing + recordIdsPresent) + " records were missing a record ID. Please re-export the infobase with record IDs enabled.");
        }
    	stream.close();
    	stream = null;
    }


    /**
     * Returns the next record. The first call to read() will return the root record, which contains the infobase-level data and is parent to all records.
     * @return
     * @throws java.io.IOException
     */
    public SlxRecord read(ISlxTokenWriter postTransformFilter) throws IOException, InvalidMarkupException {
        SlxRecord r = nextRecordTag;
        nextRecordTag = null;

        if (r == null) return null; //No record tag was found last read(), or no root node was defined in the constructor

        
        
        //Transforms the 'translated' Slx token stream into a valid Slx token stream. There isn't a 1-1 correlation, so it may write any number of tokens into
        //the specified record when .write(SlxToken) is called. Or it may write none, and simply modify previous tokens.
        if (postTransformFilter == null) {
            if (defaultFilter != null) {
                postTransformFilter = defaultFilter;
            } else {
                postTransformFilter = r;
            }
        }
        postTransformFilter.setUnderlyingWriter(r);
        
        //Transformer
        SlxTransformer transformer = new SlxTransformer(postTransformFilter, r);
        transformer.silent = this.silent;

        //Read folio tokens one-by-one
        while (stream.canRead()) {
            SlxToken ft = stream.read();
            //Stop when we hit the next record
            if (ft != null && ft.matches("record")) {
                //Store for next read() call
                this.nextRecordTag = new SlxRecord(ft);
                //The record ID
                String recordId = nextRecordTag.get("recordId");
                
                if (recordId != null){
	                //Verify the recordId is unique right here.
                	//Tested with 100,000 entries - less than 1% difference.
                	//TODO: I think this takes ~10MB of extra memory, but that is probably because the strings are staying in memory.
	                if (processedIds.contains(recordId)){
	                	throw new InvalidMarkupException("Duplicate record ID detected",this.nextRecordTag);
	                }else{
	                	processedIds.add(recordId);
	                }
                    recordIdsPresent++;
	                /*
	                Record ID is optional. If used, it indicates a persistent ID for the record. This persistent ID allows you to re-create infobases without invalidating shadow files associated with the infobase. 
	                Record IDs can be generated when an infobase is exported or specified in the flat file for import (should you choose to maintain the IDs yourself). 
	                Record IDs are hexadecimal values ranging from 1 to FFFFFFFF (do not use spaces in the hex number).
	                */
                }else{
                    recordIdsMissing ++;

                    if (requireRecordIds){
                        throw new InvalidMarkupException("Record found without a recordID. Please re-export with record IDs enabled.",this.nextRecordTag);
                    }
                	nextRecordTag.set("recordID", UUID.randomUUID().toString());
                }
                
                break;
            }
            if (ft != null)
            	transformer.write(ft);
        }
        
        
        //Fix identifiers such as class, jumpdestination, and name. Note! This cannot be done during transformation, since <p> tags only receive their class attribute after they are already written.
        //Css cleaning layer.
        //Resolve object tags
        
        if (r == root){
        	cssCleaner.processRootRecord(r);
        	
        }else{
        	//Fixed indexing bug Feb. 2. Running cssCleaner twice on the def was corrupting the tables.
	        cssCleaner.process(r);
	        for (SlxToken t: r.getTokens()){
	        	cssCleaner.process(t); //Fix CSS classes
	        	objectResolver.fixToken(t); //Fix object tags - resolve using infobase definition.
	        }
        }
        
        
        //Set default 'class'
        if (r.get("class") == null) r.set("class","NormalLevel");
        
        
        //LV (level) tags can appear anywhere in the record, so calculateParent() must be called after the record has been parsed through the FolioSlxTransformer
        r.calculateParent(lastLevelRecord,true);
        r.ghostPairsGenerated  = true;
        
        r.set("fullPath", r.fullPath());

        //Store last level record for heirarchy calculations
        if (r.isLevelRecord()) lastLevelRecord = r;
        
        //HACK - doesn't handled undefined styles... Re-asses this.
        if ("root".equalsIgnoreCase(r.get("level"))) {
        	
        	cssCleaner.saveTo(r);
        }
        //Must be called after cssCleaner.saveTo(), since the mapping tags cannot be written outside the record node.
        transformer.endRecord(true);
        transformer.verifyDone();

        return r;
    }


}
