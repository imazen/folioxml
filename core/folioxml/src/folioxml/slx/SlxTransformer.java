package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.css.CssUtils;
import folioxml.folio.FolioToken;
import folioxml.translation.FolioCssUtils;
import folioxml.translation.FolioSlxTranslator;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlxTransformer implements ISlxTokenWriter{

    private SlxContextStack stack = new SlxContextStack(false,true);
    private ISlxTokenWriter reciever = null;
    private SlxValidator validator = new SlxValidator(stack);

    /**
     * Creats a new Slx transfomer with the specified record as the root context. You must call .endRecord() at the end, since no closing record tag will be arriving.
     * @param record
     * @throws InvalidMarkupException 
     */
    public SlxTransformer(SlxRecord record) throws InvalidMarkupException{
        this(record,record);
    }
    /**
     * Allows you to specify an alterate token receiever instead of the record. Can be used to add a post-proccessing filter.
     * @param r
     * @param record
     * @throws InvalidMarkupException 
     */
    public SlxTransformer(ISlxTokenWriter r, SlxRecord record) throws InvalidMarkupException{
        this.reciever = r;
        record.startsNewContext = true;
        stack.add(record);
    }

    /**
     * Creats a new SlxTransformer with the specfied reciever. If you don't pass in a root record, opening and closing record tokens will be expected.
     * @param r
     * @param record
     */
    public SlxTransformer(ISlxTokenWriter r){
        this.reciever = r;
    }

	/**
	 * In practice this shouldn't need to be called. Usually a SlxTransfomer is initialized pointing to the correct underlying instance, and it doesn't have to change.
	 * Remember that a token stack is being maintained.
	 */
	public void setUnderlyingWriter(ISlxTokenWriter underlyingReceiver) {
		this.reciever = underlyingReceiver;
	}


    /**
     * If record tags are being filtered out before the transformer, you can call this to cause opening tags to flush.
     */
    public void endRecord(boolean writeClosingTag) throws InvalidMarkupException{
       disableOutput = !writeClosingTag;
        write(newToken("</record>"));
        
        disableOutput = false;
    }
    /*
     * Call this to make sure that the stack is empty once you have finished using SlxTransformer.
     */
    public void verifyDone() throws InvalidMarkupException{
    	//Throw an exception if we have leftovers.
        if (stack.topItem() != null) throw new InvalidMarkupException("Token stream is not complete - there are orphaned tags",stack.topItem());
    }
    protected boolean disableOutput = false;

    protected void out(SlxToken t) throws InvalidMarkupException{
        if (!disableOutput) reciever.write(t);
    }
    
    public boolean silent = false;
    


    /* Slx compatibility tag set
     infobase-meta, style-def/>, record, record-attribute/>, span, link, popupLink, end-popup-contents/>, note, namedPopup, parabreak />,  object/>, table, tr, td
     * paragraph-attribute/>, pagebreak />, br/>, bookmark/>, pp/>, se/>
    */

    /* Transformed tag set
     * new: <p>, <popup>, <link type="popup">
     * infobase-meta, style-def/>, record>, span, link, popup, note, namedPopup,   object/>, table, tr, td
     *  br/>, bookmark/>
    */

    /* removed by transform: record-attribute, paragraph-attribute, pp, se, pagebreak, popupLink, end-popup-contents, parabreak*/

    /*
     * context tags:  record, infobase-meta, popupLink, note, namedPopup, popup
     * standard: p, table, tr, td, object/>, br/>, bookmark/>, style-def/>, record-attribute/>, paragraph-attribute/>
     * ghost: span, link
     *
     * auto-repairs: insert <p> tags, close p tags
     * auto-close tr, td, p
     * auto-close record tags.
     * auto-close ghost tags (span,link) before context end.
     */

    /**
     * The token receieved by the write() command.
     */
    protected SlxToken input = null;

    private static Pattern pEntity = Pattern.compile("&[^;&<]++;",Pattern.CASE_INSENSITIVE);

    /**
     * Splits text tokens that contain entities apart into alternating text/entity tokens
     * @param t
     * @throws folioxml.core.InvalidMarkupException
     */
    public void writeText(SlxToken t)throws InvalidMarkupException{
        Matcher m = pEntity.matcher(t.markup);
        int lastEnd = 0;
        while(m.find(lastEnd)){
            //Text
            if (m.start() > lastEnd){
                outValidate(newToken(t.markup.substring(lastEnd,m.start())));
            }
            //Entity
            outValidate(newToken(m.group()));
            //Increment
            lastEnd = m.end();
        }
        if (lastEnd > 0){
            //Last bit
            if (t.markup.length() > lastEnd){
                outValidate(newToken(t.markup.substring(lastEnd,t.markup.length())));
            }
        }else{
            //No entities, I guess.
            outValidate(t);
        }
    }
    public void write(SlxToken t) throws InvalidMarkupException{
        input = t;

        //Folio compatibility: auto-open paragraph before the text or entities. Needed inside table cells...
        
        boolean isContent = t.isContent();
        
        if (!(stack.has("p")) && isContent)
            writeTag(newToken("<p>"));
        
 
        //Pass tags to writeTag(), pass others on to the receiver.
        if (t.isTag()){
            writeTag(t);
        //Pass text to writeText() for entity splitting
        } else if (t.type == SlxToken.TokenType.Text){
            writeText(t);
        //Pass comments and entities through
        }else{
            outValidate(t);
        }
        
        if (isContent){
        	//Mark containing paragraph that it has content...
        	//Value can be inverted on closing p tag...
        	SlxToken p = stack.get("p");
        	if (p != null && p.get("hasContent") == null){
        		p.set("hasContent","true");
        	}
        }

    }
    public void outValidate(SlxToken t) throws InvalidMarkupException{
        validator.preValidate(t);
        validator.validate(t);
        out(t);
    }

   

    public void writeTag(SlxToken t) throws InvalidMarkupException{
        if (t.tagType == SlxToken.TagType.None) throw new InvalidMarkupException("Tags must be opening, closing, or self closing; TagType.None is not a valid value.",t);

        /** Classify tags **/

        //Mark context tags
        if (t.matches("infobase-meta|record|note|popup|namedPopup")) t.startsNewContext = true; //Not |td|tr|table?

        //Mark ghost tags - they aren't hierarchical. They get put in the stack, but top() and pop() ignore them, and they aren't checked against the hierarchy
        if (t.matches("span|link")) t.isGhost = true;




        /************************************
        /** (Additive only) Folio compatibility - these all call writeTag() recursively, so we can ignore the order. These only go one level deep though.. Nested paragraphs would cause a problem **/

        //Use !t.isClosing instead of t.isOpening - otherwise a self-closing tag won't cause the previous tag to be auto-closed

        //Auto close paragraphs before opening a new paragraph or table
        if (t.matches("p|table") && !t.isClosing() && stack.has("p"))
            writeTag(makeClosingTag(stack.get("p")));
        
        

        //Auto close paragraphs before the end of a table cell
        if (t.matches("td|th") && t.isClosing() && stack.has("p"))
            writeTag(makeClosingTag(stack.get("p")));
        
        //There should never be a open paragraph tag
        if (t.matches("td|th") && t.isClosing() && stack.has("p"))
        	throw new InvalidMarkupException("Nested paragraphs!");
        
        //Auto close paragraphs before closing a context scope
        if (t.matches("infobase-meta|record|note|popup|namedPopup") && t.isClosing() && stack.has("p"))
            writeTag(makeClosingTag(stack.get("p")));

        //Auto close ghosts before closing a context scope
        if (t.startsNewContext && t.isClosing())
            closeGhosts();

        //Auto close cells before opening a new cell, or opening or closing a row. (T
        if (stack.has("td|th") && ((t.matches("td|th") && !t.isClosing()) || t.matches("tr")) )
            writeTag(makeClosingTag(stack.get("td|th")));

        //Auto close table rows before the end of the table or the start of a new row.
        if (stack.has("tr") && ((t.matches("table") && t.isClosing()) || (t.matches("tr") && !t.isClosing())))
            writeTag(makeClosingTag(stack.get("tr")));

        //Auto-close open records before opening another. Records can't overlap
        if (t.matches("record") && !t.isClosing() && stack.has("record",true))
            writeTag(newToken("</record>"));

        //Start a new paragraph before any of these tags - if it's not already open.
        //Paragraph attributes are specified inside <td> tags also, so we MUST start <p> inside <td> quickly.
        if (!t.isClosing() && !(stack.has("p")) && t.matches("span|link|object|note|paragraph-attribute|pagebreak|br"))
            writeTag(newToken("<p>"));

        //Certain types of folio tags don't have closing tags - such as character attributes. These need to be auto-closed when another of the same type is encountered.
        //TODO - in XML mode, this isn't wanted. But in folio mode, this is wanted for all types - to prevent overlapping
        //Added Jul 27-09
        if (t.matches("span") && t.isOpening()){
            String type = t.get("type");
            if (type != null){
                if (TokenUtils.fastMatches("bold|italic|hidden|strikeout|underline|condensed|outline|shadow|font-family|font-size|background-color|foreground-color|subsuperscript", type)){
                    //it's one of the character attributes.

                    SlxToken opener = stack.find(t.getTagName(),type,false);
                    if (opener != null){
                    	//Ok, there's already an open ghost tag of this type in the context. Close it, since we're overriding that now.
                    	writeTag(makeClosingTag(opener));
                    }

                }
            }
        }
        
        /**
         * Table support. 
         * 
         * Many attributes must be copied from the table style="" tag to each cell.
         * 
         * -folio-horizontal-gap:unit;-folio-horizontal-gap:unit;
         * padding-horizontal:unit;padding-vertical:unit;
         * border-horizontal, border-vertical;
         * 
         * Cells may already have padding and border, since it can be individually specified. In that case, cell border wins. 
         * 
         * Padding = max(0,gap - borderResult) +   (padding-local + padding-horizontal/vertical)
         * 
         * Also...
         * 
         * cellWidths attr must be divided among the table cells.. aggregate widths for cells using colSpan.  Folio also uses model where padding subtracts from width.
         */
        if (t.matches("tr|table") && t.isClosing()){
        	SlxToken ta = stack.get("table");
        	assert(ta != null);
        	ta.removeAttr("currentColumn");
        }

        /* table, row, and cell tags take 3x as long to process... Could be optimized to take 50% of the time... But would require
         * in-memory collections attached to SlxTokens...
         */
        if (t.matches("td") && t.isOpening()){
        	String sCols = t.get("colspan") != null ? t.get("colspan") : "1"; //1-based (1 is default)
        	int cols = Integer.parseInt(sCols); //May throw an exception, but only if FolioSlxTranslator didn't do the translation.
        	
        	SlxToken ta = stack.get("table"); //Parent table
        	assert(ta != null);
        	int columnIndex = ta.get("currentColumn") == null ? 0 : Integer.parseInt(ta.get("currentColumn")); //We must store the currentColumn index on the table, in case we ever wish to support nested tables.
        	
        	
        	SlxToken tr = stack.get("tr"); //Parent row
        	assert(tr != null);
        	boolean isTh =  ("true".equalsIgnoreCase(tr.get("rowIsHeader")));
        	
        	//Add this column index to the list of header columns if columnIsHeader=true
        	if ("true".equalsIgnoreCase(t.get("columnIsHeader"))){
        		ta.appendToAttributeSmart("headerCols", Integer.toString(columnIndex));
        	}
        	//Is this in a header column?
        	String[] headerCols = ta.get("headerCols") == null ? new String[]{} :ta.get("headerCols").split(",");
        	for (String h:headerCols){
        		if (h.equals(Integer.toString(columnIndex))) isTh = true;
        	}
        	if (isTh) t.set("th", "true");
        	
        	if (ta.get("colWidths") != null){
	        	//Handle column widths
	        	String[] widths = ta.get("colWidths").split(",");
	        	//TEMP: TODOD!!! Changed Jul1 for helptaulojistas
	        	//OLD: if (columnIndex >= widths.length) throw new InvalidMarkupException("More columns in table  than specified in the column widths collection (" + (columnIndex + 1) + ").",ta);
	        	if (columnIndex < widths.length) 
	        		t.appendToAttributeSmart("style", "width:" + widths[columnIndex] + ";");
        	}else{
        		//t.set("nowidth", "true");
        	}
        	
        	//Parsing all this css every time is very slow... adding ~15% execution time to infobases that are 100% tables.
        	Map<String,String> cellCss = CssUtils.parseCss(t.get("style"), true); 
        	Map<String,String> tableCss = CssUtils.parseCss(ta.get("style"), true); 
        	
        	
        	//Now, time to calculate padding and copy border settings
        	//Padding = max(0,gap - borderResult) +   (padding-local + padding-horizontal/vertical)
        	for (String side:new String[]{"left","top","right","bottom"}){
        		//Docs are wrong!!! They are clear, but wrong. vertical maps to bottom and top, not right and left.
        		
        		String orientation = (side.equalsIgnoreCase("left") || side.equalsIgnoreCase("right")) ? "horizontal" : "vertical"; 
        		//Copy the table border settings to the cell if they do not already exist.
        		if (tableCss.containsKey("border-" + orientation) && !cellCss.containsKey("border-" + side)){
        			cellCss.put("border-" + side, tableCss.get("border-" + orientation));
        		}
        		
        		
        		String globalPadding = tableCss.get("padding-" + orientation);
        		String globalGap = tableCss.get("-folio-" + orientation + "-gap");
        		
        		String localPadding = cellCss.get("padding-" + side);
        		String localBorder = cellCss.get("border-" + side); //The first element is usually the units (if FolioCssUtils generated). 
        		if (localBorder != null){
        			for (String token:localBorder.split("\\s+")){
        				if (FolioCssUtils.isCssUnit(token)){ //The first unit token.
        					localBorder = token;
        					break;
        				}
        			}
        		}
        		
        		
        		//We need to convert them all to the same unit...
        		if (globalPadding == null) globalPadding = "0in";
        		if (localPadding == null) localPadding = "0in";
        		if (localBorder == null) localBorder = "0in";
        		if (globalGap == null) globalGap = "0in";
        		//And calculate.
        		//ToInches calls add 8% overhead to entire conversion proccess.
        		double padding = Math.max(0, FolioCssUtils.toInches(globalGap) - FolioCssUtils.toInches(localBorder)) + FolioCssUtils.toInches(localPadding) + FolioCssUtils.toInches(globalPadding);
        		
        		//And store
        		cellCss.put("padding-" + side, padding + "in");
        	}
        	
        	CssUtils.coalesce(cellCss); //Re-simplify
        	//Simplify and write back to style attr.
        	t.set("style", CssUtils.writeCss(cellCss));
        	
        	columnIndex += cols;
        	ta.set("currentColumn", Integer.toString(columnIndex));
        }

     

        /** (Destructive) Folio compatibility */
        
        if (t.matches("p") && t.isClosing()){
        	SlxToken opener = stack.get("p");
        	if (opener.get("hasContent") == null)
        	{
        		//Append class '_empty'
        		//opener.set("class", (opener.get("class") != null ? opener.get("class") + " ": "") + "_empty");
        		opener.appendToAttributeSmart("style", "padding-top:1em;"); //Better than changing the class. Multiple CSS names make things harder to parse.
        	}else
        		opener.removeAttr("hasContent");
        }
        
           /* transform <td tr="true"></td> pairs to <th> </th>. Must be after any additive code.
         */
        if (t.matches("td") && t.isClosing() ){
        	SlxToken opener = stack.get("td");
        	assert(opener != null);
        	if ("true".equalsIgnoreCase(opener.get("th"))){
        		t.setTagName("th");
        		opener.setTagName("th");
        		opener.removeAttr("th");
        		//opener.set("th", "found");
        	}
        }
        
        

        
 
        
        

        //Transform <popupLink> into <link><popup>, <end-popup-contents/> into </popup>, and </popupLink> into </link>
        if (t.matches("popupLink")){
            if (t.isOpening()){
                SlxToken extraTag = null;
                //link tag
                this.writeTag(newToken("<link type=\"popup\">"));
                //Handle <PW:Popup,5.47917,1.22917,"Various Pictures",FD:"non indexed field">
                //Put in extraTags attribute: FD,"non indexed field"
                if (t.get("extraTags") != null){
                    extraTag = FolioSlxTranslator.translate(new FolioToken("<" + t.get("extraTags") + ">"));
                    t.removeAttr("extraTags");
                }
                SlxToken popup = newToken("<popup>");
                t.addAttributesTo(popup);
                this.writeTag(popup);
                //Write extra tag.
                if (extraTag != null) {
                    this.warn("Siamese tag encountered in PW tag. Placing the following token inside <popup>: " + extraTag,t);
                    this.writeTag(extraTag);
                }
                return;

            } else if (t.isClosing()){
                this.writeTag(newToken("</link type=\"popup\">"));
                return;
            } else {return;} //just delete self-closing <popupLinks/>

        }else if (t.matches("end-popup-contents")){
                assert(t.isSelfClosing());
                this.writeTag(newToken("</popup>"));
                return;
        }

        //Start a new paragraph when we hit a <parabreak/> or <parabreak> tag. Do nothing if it is a closing </parabreak> tag. Always discards the parabreak tag.
        if (t.matches("parabreak")){
            if (!t.isClosing()) writeTag(newToken("<p>"));
            return;
        }

        //Delete these - for now
        if (t.matches("pp|se|pagebreak")) return; //TODO: document

        //Pre validate before we perform any modifications to ancestors (where the source tag is deleted)
        validator.preValidate(t);

        /** Record the order of the infobase level definitions on the record */
        if (t.matches("style-def") && "level".equalsIgnoreCase(t.get("type")))
            stack.get("record").appendToAttributeSmart("levelDefOrder", t.get("class"));

        /** Copy the <LN:> list to the containing record (should be the root)*/
        if (t.matches("infobase-meta") && "levels".equalsIgnoreCase(t.get("type")))
            stack.get("record").set("levels", t.get("content"));

        //Put these attributes on the parent paragraph, and eat the tags
        if (t.matches("paragraph-attribute")){
            if (!t.isClosing()) t.addAttributesTo(stack.get("p"));
            return; //What about the 'few paragraph attributes that apply to the entire table?'
        }
        //Put these attributes on the parent record, and eat the tags
        if (t.matches("record-attribute")){
            SlxToken rec = stack.get("record", true);
            if (rec == null)
                throw new InvalidMarkupException("record-attribute can only exist inside record",t);
            if (!t.isClosing()) t.addAttributesTo(rec); //bypass context boundaries for this one
            return;
        }

        //Character attributes can have default tags (closing tags) without having opening tags. We don't need these - they're pointless, but allowed in folio. Remove them

        if (t.matches("span") && t.isClosing()){
            String type = t.get("type");
            if (type != null){
                if (TokenUtils.fastMatches("bold|italic|hidden|strikeout|underline|condensed|outline|shadow|font-family|font-size|background-color|foreground-color|subsuperscript", type)){
                    //it's one of the character attributes.

                    SlxToken opener = stack.find(t.getTagName(),type,false);
                    if (opener == null){
                        warn("No opening tag found for this character attribute tag. Removing.",t);
                        return;
                    }

                }
            }
        }
        if (t.isClosing() && t.isGhost){
        	//The other, less normal orphaned closing ghost tags, like link and non-char attrib uses of span //jul 27 09
        	if (!stack.matchingOpeningTagExists(t)){
        		//Should throw an InvalidMarkupException, but for now we can just drop these.
        		//TODO 
        		warn("Dropping orphaned closing ghost tag",t);
        		return;
        	}
        }
        
        //Check for CSS combinations. Must be after any additive code.
        if (!t.isOpening() ){
        	SlxToken opener = t.isClosing() ? stack.getOpeningTag(t) : t; //t is its own opener if it is self closing.
        	//No additives should remain. Check css for bad combos and fix
        	String css = opener.get("style");
        	
            if (css != null){
            	String newCss = FolioCssUtils.fixCss(css,silent);
            	if (newCss != css) opener.set("style", newCss);
            } 
            
        }
        

        //Validate tag before modifying the top of the stack
        validator.validate(t);


        /************************
         * Adding to/subtracting from the stack
         * All tags that aren't eaten (like pp, se, pagebreak, paragraph-attribute, record-attribute) go through here eventually.
         */

        //Should throw an exception if there are any orphaned or mismatched tag pairs.
        stack.process(t); //Strict and tag pairs
        

        //Write tag
        out(t);
    }
    /**
     * Creats a new token from the specified string, and attaches the original parsing token.
     * @param s
     * @return
     * @throws folioxml.folio.InvalidMarkupException
     */
    public SlxToken newToken(String s) throws InvalidMarkupException{
        SlxToken t =  new SlxToken(s); //add reference to current
        if (input != null) t.sourceToken = input.sourceToken;
        return t;
    }
    /**
     * Creates a matching closing tag for the specified opening tag. Attches the original parsing token.
     * @param t
     * @return
     */
    public SlxToken makeClosingTag(SlxToken t) throws InvalidMarkupException{
        SlxToken s = new SlxToken();
        s.setTagName(t.getTagName());
        s.type = t.type;
        s.tagType = SlxToken.TagType.Closing;
        s.startsNewContext = t.startsNewContext;
        s.isGhost = t.isGhost;
        s.sourceToken = input.sourceToken;
        String type= t.get("type");
        if (type != null)s.set("type", type);
        return s;
    }
    /**
     * Closes any ghost tags floating at the top of the stack. Uses writeTag()
     * @throws folioxml.folio.InvalidMarkupException
     */
    public void closeGhosts() throws InvalidMarkupException{
        SlxToken g;
        //Close ghost tags
        while ((g = stack.topGhost()) != null){
        	boolean isCurrentRecord = input != null && input.matches("record") && input.isClosing();
            if (!compatMode || (!isCurrentRecord && !"characterstyle".equalsIgnoreCase(g.get("type")))) warn("Closing tag not found. Inserting closing tag automatically",g);
            writeTag(makeClosingTag(g)); //Writing the closing tag will remove it from the stack
        }
    }
    public boolean compatMode = true;
    public void warn(String message){
        warn(message,input);
    }
    public void warn(String message, SlxToken t){
    	if (silent) return;
        System.out.println(message);
        printToken(t);
        if (t != input) {
        	System.out.print("Triggered by: ");
          printToken(input);

        }

    }

    public void printToken(SlxToken t){
        System.out.print("{ " + t + "  :  ");
        if (t.sourceToken != null && t.sourceToken.info != null){
            if (t.sourceToken.info.text != null) System.out.print(t.sourceToken.info.text);
            System.out.println();
            System.out.print("  " + t.sourceToken.info.toString());
        }
        System.out.println(" }");
    }

}

