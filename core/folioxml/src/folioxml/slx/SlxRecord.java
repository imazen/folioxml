package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.xml.XmlRecord;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;


public class SlxRecord extends SlxToken implements ISlxTokenWriter{
    private static Pattern rdPattern = Pattern.compile("^<RD(?:[,:;]ID:([A-Z0-9a-z]++))?+(:\"?+([A-Za-z 0-9]++)\"?+)?(,CH)?+>$");
    
    
 
    public SlxRecord(SlxToken st) throws InvalidMarkupException{
        this.markup = st.toString();
        this.parseTag();
    }
    public SlxRecord() throws InvalidMarkupException{
        this.markup = "<record>";
        this.reparse();
    }
    public SlxRecord(String text) throws InvalidMarkupException{
        super(text);
        assert(this.tagType == TagType.Opening);
    }
    
    private SlxRecord(SlxRecord base, boolean shallowCloneTokens){
    	super(base,true);
    	this.parent = base.parent;
    	this.tokens = (ArrayList<SlxToken>) (shallowCloneTokens ? base.tokens.clone() : new ArrayList<SlxToken>());
    	this.ghostPairsGenerated = base.ghostPairsGenerated;
    	//this.xmlMode = base.xmlMode;
    }
    /**
     * Returns a copy of this SlxRecord token, but doesn't copy the token collection.
     * @return
     */
    public SlxRecord cloneToken(){
    	return new SlxRecord(this,false);
    }
    /**
     * Deep cop
     * @return
     */
    public SlxRecord deepClone(){
    	SlxRecord r= new SlxRecord(this,true);
    	for (int i = 0; i < r.tokens.size(); i++)
    		r.tokens.set(i, r.tokens.get(i).clone());
    	return r;
    }
    /**
     * Used when converting an SlxRecord hierarchy into XmlRecord instances.
     */
    public XmlRecord slxXmlRecordTag = null;
    /**
     * If true, then the .ghostPair property of child tokens has been set. Used for the last step to XML.
     */
    public boolean ghostPairsGenerated = false;
    /**
     * If true, the contents of this record are XML compliant.
     */
    //public boolean xmlMode = false;
    
    private ArrayList<SlxToken> tokens = new ArrayList<SlxToken>(64);
    
    public List<SlxToken> getTokens(){
    	return tokens;
    }
    
    public ISlxTokenReader getTokenReaderForRecord(){
    	return new SlxRecordTokenReader(this);
    }
    
   
    /**
     * Writes the specified token to this record's accumulation buffer
     * @param token
     */
    public void write(SlxToken token){
        tokens.add(token);
    }
    
    /**
     * For interface compatibility. The only supported value for base is 'this'
     * @param base
     */
    public void setUnderlyingWriter(ISlxTokenWriter base) {
        if (base != this) throw new UnsupportedOperationException("The only supported underlying receiver for an SlxRecord is itself. It must be the end point in the filter chain.");
    }
    
    public SlxRecord parent = null;
    /**
     * Returns true if there is a value for the 'level' attribute
     * @return
     */
    public boolean isLevelRecord() throws InvalidMarkupException{
        return getLevelType() != null;
    }
    /**
     * returns this.get("level"). Returns null if the string is empty.
     * @return
     */
    public String getLevelType() throws InvalidMarkupException{
        String s= this.get("level"); if (s == null || s.length() == 0) return null;
        return s;
    }
    /**
     * Call this after FolioSlxTransformer has transformed the data stream. Otherwise the LV tag may not be applied to this element.
     * if (lastLevelRecord == null), then this method will do nothing. if strict, then that is only allowed on root nodes.
     * A root node *must* be in the heirarchy if strict == true. 
     * For level records, if (lastLevelRecord != null && requireValidation), then the order of levels will be validated against the definition file information in the root record's levels="" or levelDefOrder=" attribute.
     * @param lastLevelRecord
     */
    public void calculateParent(SlxRecord lastLevelRecord, boolean strict) throws InvalidMarkupException{
        String levelType = this.getLevelType();
        
        //We can't do anything without lastLevelRecord. Should only be null on root nodes
        if (lastLevelRecord == null) {
            if (strict && !levelType.equalsIgnoreCase("root")) throw new InvalidMarkupException("The first record must have level=\"root\".",this);
            return;
        }
        //Find root node
        SlxRecord root = getRoot(lastLevelRecord);
        if (strict && (root == null || !"root".equalsIgnoreCase(root.get("level")))) throw new InvalidMarkupException("Root node not found. Root node must have level=\"root\" and be the top of the heirarchy.",lastLevelRecord);
        
        

        
        //If this is not a level record, then "lastLevelRecord" is the parent.
        if (levelType == null){
            this.parent = lastLevelRecord;
        }else{
            String[] levels = null;
            String list = root.get("levels");
            if (list == null) list = root.get("levelDefOrder");
            if (list == null) 
                throw new InvalidMarkupException("Infobase levels not found. They are either determined by the LN tag (level list), or by the order of the level style definitions. Your DEF file may be missing.",root);
            levels = list.split("\\s*,\\s*");
        
            
            //If this IS a level record, then "lastLevelRecord" and 'levels' can be used to discover the parent.
            
            //Records can skip levels, and appear in any order...

        	//So, build a set of all levels that are a parent to this one... And make sure this levelType is valid.
            boolean isValidLevelType = false;
        	Set<String> parentLevels = new HashSet<String>();
        	parentLevels.add("root");
        	for (String s:levels){
        		if (s.equalsIgnoreCase(levelType)) {
        			isValidLevelType = true;
        			break; //Stop when we hit this level..
        		}
        		parentLevels.add(s.toLowerCase(Locale.ENGLISH));
        	}
        	if (!isValidLevelType) 
        		throw new InvalidMarkupException("Encountered level not defined in definition: \"" + levelType + "\". You can define a level with an LN tag (level list), or by adding a level style definition for it.",this);
            
        	
        	//Traverse the previous sibling ancestors until a record with a level matching one if parentLevels is found. 
        	
            //Old algorithm was designed with the misunderstanding that levels couldn't be skipped.
            SlxRecord chainIndex = lastLevelRecord;
            while (chainIndex != null){
            	if (parentLevels.contains(chainIndex.getLevelType().toLowerCase(Locale.ENGLISH))){
            		this.parent = chainIndex; break;
            	}
                chainIndex = chainIndex.parent; //Deeper
            }
            if (this.parent == null) {
            	//If no equivalent is found, throw an error.
            	throw new InvalidMarkupException("This is impossible... Since root always exists, and root exists in parentLevels.");
            }
        }
    }
    
    public boolean isRootRecord() throws InvalidMarkupException{
    	return (this.getLevelType() != null && this.getLevelType().equalsIgnoreCase("root"));
    }
    
    private void calculateHeading() throws InvalidMarkupException{
		if (this.get("heading") != null) return; //Only calculate once
		if ("root".equalsIgnoreCase(this.get("level"))) {
			this.set("heading", "root"); //TODO: select infobase title.
			return;
		}
		StringWriter title = new StringWriter();
		StringWriter all = new StringWriter();
		SlxContextStack stack = new SlxContextStack(false,false);
		stack.process(this);
		boolean firstParagraph = true;
		for (SlxToken t : this.tokens) {
			stack.process(t);// call this on each token.
			if (!stack.has("note|popup|table") ){
				if (t.isTextOrEntity()){
                    String s = t.markup;
                    if (t.isEntity()) s = TokenUtils.entityDecodeString(s);
					if (stack.find("span", "recordHeading", false) != null) {
					    title.append(s);
					}
					if (firstParagraph) all.append(s); //The failover is the entire first paragraph (that contains text or whitespace)
				}

                if (t.matches("p|br|td|th|note") && !t.isOpening()) {
                    if (stack.find("span", "recordHeading", false) != null) {
                        title.append(" ");
                    }
                    if (firstParagraph) all.append(" ");
                }

				if (t.matches("p") && t.isClosing() && all.toString().trim().length() > 0) firstParagraph = false;
			}
			
		}
		if (stack.size() > 0) throw new InvalidMarkupException("Stack is not empty after processing record");
		
		
		this.set("heading", title.toString());
		if (this.getLevelType() != null && this.get("heading").length() < 1) set("heading", all.toString()); //Only fall back on level records

    }

    public String getHeading() throws InvalidMarkupException {
        if (isRootRecord()) return ""; //for root, return nothing.
        calculateHeading();
        String heading = get("heading");
        if (heading == null) throw new InvalidMarkupException("calculateParent must be called first");
        return heading;
    }
    
    public String getFullHeading(String delimiter, boolean mostSpecificFirst, int maxNames) throws InvalidMarkupException{
    	calculateHeading();
    	
    	if (maxNames == 0) return ""; //No names are wanted. Negative values mean there are no limits
    	
    	if (isRootRecord()) return ""; //for root, return nothing.
    	
    	String heading = get("heading");
    	if (heading == null) throw new InvalidMarkupException("calculateParent must be called first");
    	
    	heading = heading.trim();

    	if (heading.length() == 0) {
    		if (parent == null) return ""; //Nothing left
    		return parent.getFullHeading(delimiter,mostSpecificFirst,maxNames);
    	}
    	
    	String parentHeading = (parent != null) ? parent.getFullHeading(delimiter,mostSpecificFirst, maxNames - 1) : null;
    	
    	if (parentHeading == null || parentHeading.length() == 0) return heading;
    	
    	if (mostSpecificFirst)
    		return heading + delimiter + parentHeading ;
    	else 
    		return parentHeading+ delimiter + heading;
    }
    

    /**
     * Returns a list of the level names from *after* the root node up the the current node. Expects all ancestors to have a level value.
     * @param current
     * @return
     * @throws folioxml.core.InvalidMarkupException
     */
    private List<String> getCurrentLevels(SlxRecord current) throws InvalidMarkupException{
        //We'll make a list, then reverse it
        ArrayList<String> levels = new ArrayList<String>(10); //Default capacity
        
        SlxRecord level= current;
        //Drop the top level off if it isn't a level
        if (level.getLevelType() == null) level = level.parent;
        //Add them child-to-ancestor
        while (level != null){
            String l = level.getLevelType();
            if (l == null) throw new InvalidMarkupException("Non-level record cannot be the parent of another record", level);
            else if (l.equalsIgnoreCase("root")){ //we don't want to add root
                assert(level.parent == null); //root element cannot have parent
                break;
            }
            levels.add(l);
            level = level.parent;
        }
        //Reverse the list to be ancestor>child
        ArrayList<String> reversed = new ArrayList<String>(levels.size());
        for (int i = levels.size() -1; i > -1; i--){
            reversed.add(levels.get(i));
        }
        return reversed;
    }
    /**
     * Returns the deepest parent value. Doesn't check the level="" attribute
     * @param record
     * @return
     */
    private SlxRecord getRoot(SlxRecord record){
        if (record == null) return null;
        if (record.parent == null) return record;
        else return getRoot(record.parent);
    }
    /**
     * Pretty formatting of the SLX or XML markup
     * @return
     * @throws InvalidMarkupException
     */
    public String toSlxMarkup(boolean addWhitespace) throws InvalidMarkupException{
    	StringBuilder sb = new StringBuilder(this.tokens.size() * 20 + 100);
    	if (addWhitespace){
	    	SlxContextStack scs = new SlxContextStack(false,false);
	    	writeToken(this,sb,scs);
	    	for (SlxToken st: this.tokens){
	    		writeToken(st,sb,scs);
	    	}
    	}else{
    		sb.append(this.toString());
    		for (SlxToken st:this.tokens) sb.append(st.toString());
    	}
    	return sb.toString();
    	
    }
    private void writeToken(SlxToken t, StringBuilder sb, SlxContextStack scs) throws InvalidMarkupException{
    	int initialDepth = scs.nonGhostCount();
    	scs.process(t);
    	int endingDepth = scs.nonGhostCount(); //The depth *after* popping or pushing
    	boolean onNewLine = sb.length() > 0 ? (sb.charAt(sb.length() -1) == '\n') : true;
    	//We want 
    	// * non-ghost tags to appear by themselves on their own lines (and their closing tags)
    	// * ghost tags, text, entities, and comments are all the same, except text and comments are wrapped.
    	
		if (t.isTag() && !t.isGhost && !t.matches("span|link")){
			if (!onNewLine) sb.append('\n');
			if (t.isClosing()){
				//Opening and self-closing tags - use original indentation
				for (int i = 0; i < initialDepth; i++) sb.append('\t');
			}else{
				//Closing tags - use post-pop indentation
				for (int i = 0; i < endingDepth; i++) sb.append('\t');
			}
			sb.append(t.toString());
			//write a newline after
			sb.append('\n');
		}else {
			if (onNewLine){
				for (int i = 0; i < initialDepth; i++) sb.append('\t');
			}
			if (t.isTag()){
				//Can't just wrap tags like text
				sb.append(t.toString());
			}else if (t.isEntity()) {
				sb.append(t.toString());//Write inline - append to the previous line.
			}else{
				//Build indent string
				StringBuilder indentStr = new StringBuilder(initialDepth + 1);
				for (int i = 0; i < initialDepth; i++) indentStr.append('\t');
				//Write text wrapped
				writeText(t.toString(),sb,80,indentStr.toString());
			}
		}

    }
    private void writeText(String s, StringBuilder sb, int wrapChars, String indentString){
    	//Keeps track of how many characters are on the current line.
    	int currentChars = sb.length();
    	//Find the last newline if it exists.
    	int lastNl = sb.lastIndexOf("\n");
    	if (lastNl > -1) currentChars = sb.length() - lastNl -1;
    	
    	for (int i = 0; i < s.length(); i++){
    		char c = s.charAt(i);
    		sb.append(c);
    		currentChars++;
    		//Break lines after whitespace
    		if (currentChars > wrapChars){
    			if (c == '\t' || c == ' '){
    				sb.append('\n');
    				sb.append(indentString);
    				currentChars = indentString.length();
    			}
    		}
    	}
    }
    
    
}
/*
 * Provides a reader that returns all tokens within an SlxRecord (including the record token itself)
 */
class SlxRecordTokenReader implements ISlxTokenReader{

	public SlxRecordTokenReader(SlxRecord r){
		this.r = r;
	}
	
	private SlxRecord r;
	private int index = -1;
	
	public boolean canRead() {
		return (index < r.getTokens().size());
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public SlxToken read() throws  InvalidMarkupException {
		if (index == -1) {
			index ++;
			return r;
		}
		if (index < r.getTokens().size()) {
			index ++;
			return r.getTokens().get(index -1);
		}
		
		
		return null;
	}
}

