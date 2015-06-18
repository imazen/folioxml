package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.css.CssUtils;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * This class provides functions that are capable of translating folio tags and tag options into CSS statements.
 * All functions receive an array of options, a start index, and a StringWriter reference. They return the first index they were unable to parse and translate.
 * @author nathanael
 */
public class FolioCssUtils{

    /**
     * Translates paragraph attributes AP|BP|JU|BR|KT|KN|LH|LS|LW|SD|TS|IN and character atrributes
     * SB (Subscript), SP (Superscript), FC (foreground), BC (background), PT (font size), FT (font family), and text attributes:
     * BD+ BD- IT+ IT- HD+ HD- SO+ SO- UN+ UN- UN2+ CD+ CD- OU+ OU- SH+ SH-.
     * Loops until an unknown tag is encountered. calls tryParseParagraphAttribute() and tryParseCharacterAttribute()
     * @param startIndex
     * @param opts
     * @param css
     * @return
     * @throws InvalidMarkupException 
     */
    public static int tryParseAll(int startIndex, List<String> opts, StringWriter css) throws InvalidMarkupException{
       int i = startIndex;
       int t;
       //Contine parsing until we hit an unrecognized tag.
       while(true){
           if (i >= opts.size()) return i; //we hit the end
           //Try all of the parsing methods against it, the first that succeed will cause the execution to {index = nextIndex;continue;}
           t = tryParseParagraphAttribute(i,opts,css);
           if (t == i) t = tryParseCharacterAttribute(i,opts,css);
           if (t == i) return i;
           else i = t;

       }
    }

    
    

        /**
     * Translates paragraph attributes AP|BP|JU|BR|KT|KN|LH|LS|LW|SD|TS|IN|KN|KT
     * Stops when an unknown tag is encountered - parses as many as it understands.
     * @param startIndex
     * @param opts
     * @param css
     * @return
         * @throws InvalidMarkupException 
     */
    public static int tryParseParagraphAttrs(int startIndex,List<String> opts,StringWriter css) throws InvalidMarkupException{
       int i = startIndex;
       int t;
       //Contine parsing until we hit an unrecognized tag.
       while(true){
           if (i >= opts.size()) return i; //we hit the end
           //Try all of the parsing methods against it, the first that succeed will cause the execution to {index = nextIndex;continue;}
           t = tryParseParagraphAttribute(i,opts,css);
           if (i == t) return i;
           else i = t;

       }
    }
    

  
    

    /** Important!! Default codes do not have to be supported in style definitions - they're not allowed there anyway. */
    /* Single codes shouldn't reach here - the preliminary parsing code should recognize the closing tag and know that no CSS output is needed */

    /* DC behavior (default color).
     * SD:NO - > transparent
     * FC:DC - > WindowText
     * BC:DC - > transparent
     * Border FC:DC -> no color, current foreground.
    */
    /**
     * This method calls tryParseFontFace, tryParseFontSize, tryParseSubOrSuperscript, tryParseBackgroundForegroundColors,tryParseTextAttributes.
     * Therefore understands SB (Subscript), SP (Superscript), FC (foreground), BC (background), PT (font size), FT (font family), and text attributes:
     * BD+ BD- IT+ IT- HD+ HD- SO+ SO- UN+ UN- UN2+ CD+ CD- OU+ OU- SH+ SH-
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseCharacterAttribute(int startIndex,List<String> opts, StringWriter css){
        //Return the first call that succeeds
        int i = startIndex;
        int t = i;
        if (t == i) t = FolioCssUtils.tryParseFontFace(i, opts, css);
        if (t == i) t = FolioCssUtils.tryParseFontSize(i, opts, css);
        if (t == i) t = FolioCssUtils.tryParseSubOrSuperscript(i, opts, css);
        if (t == i) t = FolioCssUtils.tryParseBackgroundForegroundColors(i, opts, css);
        if (t == i) t = FolioCssUtils.tryParseTextAttributes(i, opts, css);
        return t;
    }
    /**
     * 5 of the 9 cell <CO> attrs are parsed here. The others don't go to CSS.
     * 
     * KN, KT, SD (bgcolor), BR (border), VA are parsed.
     * MD (rowspan), MR (colspan), HI (height), and HC (columnHeader) are not parsed
     * @param startIndex
     * @param opts
     * @param css
     * @return
     * @throws InvalidMarkupException 
     */
    public static int tryParseCellStyleAttribute(int startIndex,List<String> opts, StringWriter css) throws InvalidMarkupException{
        //Return the first call that succeeds
        int i = startIndex;
        int t = i;
        //MD, MR, HC go into other attrs. MD -> rowspan, MR -> colspan, HI -> height, HC -> 
        if (t == i) t = FolioCssUtils.tryParseKeepAttr(i, opts, css); //KN, KT
        if (t == i) t = FolioCssUtils.tryParseParagraphBackgroundColor(i, opts, css); //SD
        if (t == i) t = FolioCssUtils.tryParseBorder(i, opts, css,false); //BR
        if (t == i) t = FolioCssUtils.tryParseVerticalAlign(i, opts, css); //VA
        if (t == i) t = tryParseHeight(i,opts,css);
        return t;
    }
    
    
    /**
     * Attempts to parse one of the following:
     * IN, BR, AP, BP, SD, JU (align),
     *  HZ, VT, VG, and HG go into other attrs
     * 
     * @param startIndex
     * @param opts
     * @param css
     * @return
     * @throws InvalidMarkupException 
     */
    public static int tryParseTableStyleAttribute(int startIndex,List<String> opts, StringWriter css) throws InvalidMarkupException{
    	//Tables CSS http://www.w3.org/TR/CSS2/tables.html
    	
    	
    	/*
    	 * Table attributes affect the entire table. Some options may be overwritten in individual cells. Table attributes may be listed in any order and may include:
			JU:Justification � Sets the justification (horizontal alignment) for the table. Justification may be one of the following:
			LF � Left. This is the default setting. If no justification is specified, the table is left justified.
			CN � Center.
			RT � Right.
			IN:Left,Right,First � Sets the left and right indent for the table (first line indents are ignored). The entire table indented; indents for paragraphs within individual cells are specified by using the Indent code within the cell. Uses the standard Indent code.
			VG:Value � Sets the vertical gap for text within the table. This option must be set to use borders for the table (left and right borders can be no wider than the vertical gap). Value is a decimal number in inches (XX.xxx). This roughly corresponds to the Cell Margins in Folio Views.
			HG:Value � Sets the horizontal gap for text within the table. This option must be set to use borders for the table (top and bottom borders can be no wider than the horizontal gap). Value is a decimal number in inches (XX.xxx). This roughly corresponds to the Cell Margins in Folio Views.
			BR:Border Options � Sets the border for the table. Uses the standard Border code options for the outside borders for the table and adds the following codes for the default cell borders:
			HZ:Width,Inside Space,Color � Sets the default horizontal border for cells within the table.
			VT:Width,Inside Space,Color � Sets the default vertical border for cells within the table.
			SD:Shade � Sets the shading for the table. Shade is an RGB color combination. Uses the standard Shade code.
			AP:Value � Sets the space after the table. Value is a decimal number in inches (XX.xxx). Uses the standard After Paragraph code.
			BP:Value � Sets the space before the table. Value is a decimal number in inches (XX.xxx). Uses the standard Before Paragraph code.
    	 */
    	/*
table	Common, border (Pixels), cellpadding (Length), cellspacing (Length), frame ("void" | "above" | "below" | "hsides" | "lhs" | "rhs" | "vsides" | "box" | "border"), rules ("none" | "groups" | "rows" | "cols" | "all"), summary (Text), width (Length)	caption?, ( col* | colgroup* ), (( thead?, tfoot?, tbody+ ) | ( tr+ ))
td	Common, abbr (Text), align ("left" | "center" | "right" | "justify" | "char"), axis (CDATA), char (Character), charoff (Length), colspan (Number), headers (IDREFS), rowspan (Number), scope ("row" | "col" | "rowgroup" | "colgroup"), valign ("top" | "middle" | "bottom" | "baseline")	(PCDATA | Flow)*
th	Common, abbr (Text), align ("left" | "center" | "right" | "justify" | "char"), axis (CDATA), char (Character), charoff (Length), colspan (Number), headers (IDREFS), rowspan (Number), scope ("row" | "col" | "rowgroup" | "colgroup"), valign ("top" | "middle" | "bottom" | "baseline")	(PCDATA | Flow)*
tr	Common, align ("left" | "center" | "right" | "justify" | "char"), char (Character), charoff (Length), valign ("top" | "middle" | "bottom" | "baseline")	(td | th)+

*/
    	// HZ, VT, VG, HG go into other attrs
    	// JU -> align tag
    	//HZ, VT - > store for expansion among cells.
    	//VG, HG - > store for expansion among cells
    	
    	/*
    	 * VG:Value � Sets the vertical gap for text within the table. This option must be set to use borders for the table (left and right borders can be no wider than the vertical gap). 
    	 * Value is a decimal number in inches (XX.xxx). This roughly corresponds to the Cell Margins in Folio Views.
    	HG:Value � Sets the horizontal gap for text within the table. This option must be set to use borders for the table (top and bottom borders can be no wider than the horizontal gap).
    	 Value is a decimal number in inches (XX.xxx). This roughly corresponds to the Cell Margins in Folio Views.
    	BR:Border Options � Sets the border for the table. Uses the standard Border code options for the outside borders for the table and adds the following codes for the default cell borders:
    	HZ:Width,Inside Space,Color � Sets the default horizontal border for cells within the table.
    	VT:Width,Inside Space,Color � Sets the default vertical border for cells within the table.
    	
    	Inside Space = padding. This NEEDS to be translated!
    	
    	
    	Subtract border from gap when calculating padding. Add to InsideSpace
    	
    	 */
    	
    	//IN, BR, AP, BP, SD, 
    	
    	//border-collapse: collapse; needs to be default... Static CSS?
    	
    	//
    	
        //Return the first call that succeeds
        int i = startIndex;
        int t = i;
        if (t == i) t = FolioCssUtils.tryParseMarginTopBottomAttr(i, opts, css); //AP, BP
        if (t == i) t = FolioCssUtils.tryParseParagraphBackgroundColor(i, opts, css); //SD
        if (t == i) t = FolioCssUtils.tryParseBorder(i, opts, css,true); //BR
        if (t == i) t = FolioCssUtils.tryParseIndents(i, opts, css); //IN 
        if (t == i) t = FolioCssUtils.tryParseTableAlign(i, opts, css); //JU
        if (t == i) t = FolioCssUtils.tryParseTableGaps(i, opts, css); //zvg, HG
        

    	 
        return t;
    }
    //HG|VG + requred folio unit. -> -folio-horizontal|vertical-gap:[cssunits]
    public static int tryParseTableGaps(int startIndex,List<String> opts,StringWriter css){
    	String name = opts.get(startIndex);
    	if (!"HG".equalsIgnoreCase(name) &&
    			!"VG".equalsIgnoreCase(name)) return startIndex; 
    	
    	assert(opts.size() > startIndex + 1); //1 argument required for this option
        String value = opts.get(startIndex+1);
      
       	assert(FolioCssUtils.isFolioUnit(value));
       	
        if (name.equalsIgnoreCase("HG"))
        	css.append("-folio-horizontal-gap:" +  FolioCssUtils.fixUnits(value) + ";");
         else if (name.equalsIgnoreCase("VG"))
        	 css.append("-folio-vertical-gap:" +  FolioCssUtils.fixUnits(value) + ";");
        
        return startIndex + 2;   
        
    	
    }
    
    public static int tryParseHeight(int startIndex,List<String> opts,StringWriter css){
    	if (!"HI".equalsIgnoreCase(opts.get(startIndex))) return startIndex; //HI is the only tag we parse here.
    	//HI:Height � Sets the cell height. Height is a decimal number (XX.xxx). Set HI:0 for auto height.
        assert(opts.size() > startIndex + 1); //1 argument required for this option
        String value = opts.get(startIndex+1);
      
       	 assert(FolioCssUtils.isFolioUnit(value));
       	//HI decimal. HI:0 = height:auto;
 
       	 if (value.equals("0"))
       		 css.append("height:auto;");
       	 else 
       		 css.append("height:" +  FolioCssUtils.fixUnits(value) + ";");
            
       	 return startIndex + 2;
    }
    

    public static int tryParseTableAlign(int startIndex,List<String> opts,StringWriter css){
        String name = opts.get(startIndex);
		if (name.equalsIgnoreCase("JU")){
			//TODO: switch from assert() calls to throwing exception
			assert(opts.size() > startIndex + 1); //1 argument required for this option
	        String value = opts.get(startIndex+1);
	        
	        //One solution... Start tables with margin-left:0px; margin-right:0px... 
	        //Even with this solution, behavior differs from Folio.
	        //Folio allows indents to affect the centering of tables...
	        //Ours only affects left or right alignment. 
	        //We need to wrap the table in  a dif to achieve the same effect.
	        if (value.equalsIgnoreCase("LF")) css.append("margin-right:auto !important; -folio-align:left;"); //Conflicts with indents -- need to have smart CSS resolve this...
	        else if (value.equalsIgnoreCase("CN")) css.append("margin-left:auto !important; margin-right:auto !important; -folio-align:center;");
	        else if (value.equalsIgnoreCase("RT")) css.append("margin-left:auto !important; -folio-align:right;");
	        else{
	            assert(false):"Invalid alignment argument for JU: " + value;
	        }
	        return startIndex + 2;
		}
		return startIndex;
    }
    /**
     * Understands SB (Subscript) and SP (Superscript)
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseSubOrSuperscript(int startIndex,List<String> opts,StringWriter css){
        String name = opts.get(startIndex);
        if (name.equalsIgnoreCase("SB") || name.equalsIgnoreCase("SP")){
            assert(opts.size() > startIndex + 1);
            String value = fixUnits(opts.get(startIndex + 1));
            //We use top: to specify.
            if (name.equalsIgnoreCase("SB"))
                css.append("vertical-align:baseline;position:relative;top:" + value + ";" );
            else
                css.append("vertical-align:baseline;position:relative;top:" + multiply(value,-1) + ";" );

            return startIndex + 2;//move to next token

        }
        return startIndex;
    }
    /**
     * Understands FC and BG colors... (DC) option is translated to WindowText and transparent respectively - not sure any true CSS equivalent exists.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseBackgroundForegroundColors(int startIndex,List<String> opts,StringWriter css){
        String name = opts.get(startIndex);
        if (name.equalsIgnoreCase("FC") || name.equalsIgnoreCase("BC")){

        	//TODO: <BC> can appear by itself as a reset code. 
            int index = startIndex + 1;
            assert(opts.size() > index); //at least one argument required
            String color = "";
            if (opts.get(index).equalsIgnoreCase("DC")){
                if (name.equalsIgnoreCase("FC")) color = "WindowText"; 
                else if (name.equalsIgnoreCase("BC")) color = "transparent";
                index++;//move to next token
            }else{
                assert(opts.size() > index + 2); //three color arguments required if not DC
                color = "#" + getHexColor(opts.get(index),opts.get(index+1),opts.get(index+2));
                index+=3;//Move to after colors
            }

            if (name.equalsIgnoreCase("FC")) css.append("color:" + color + ";");
            else if (name.equalsIgnoreCase("BC")) css.append("background-color:" + color + ";");

            return index;

        }
        return startIndex;
    }
    /**
     * Understands PT (font-size)  tag.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseFontSize(int startIndex,List<String> opts,StringWriter css){
        String name = opts.get(startIndex);
        if (name.equalsIgnoreCase("PT")){
            assert(opts.size() > startIndex + 1); //at least one argument required
            double pts = Double.parseDouble(opts.get(startIndex + 1));
            css.append("font-size:" + Double.toString(pts) + "pt;");
            return startIndex + 2 ;

        }
        return startIndex;
    }
    /**
     * Understands <FT:Fontname,FamilyFallback,Character set>
     * Exports -folio-charset:pc-ibm|symbol|ansi; for charset attribute.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseFontFace(int startIndex,List<String> opts,StringWriter css){
        if (opts.get(startIndex).equalsIgnoreCase("FT")){
            int index = startIndex+1;
            assert(index < opts.size()); //one argument required
            //Parse font name
            String fontName = opts.get(index);
            if (!fontName.matches("^[a-zA-z]+$")) fontName = "\"" + fontName + "\"";
            index++;
            //Optional: Parse font type fallback
            String fallback = "";
            if (index < opts.size()){
                String family = opts.get(index);
                if (family.equalsIgnoreCase("SR")) fallback = ", serif";
                if (family.equalsIgnoreCase("SN")) fallback = ", sans-serif";
                if (family.equalsIgnoreCase("FX")) fallback = ", monospace";
                if (family.equalsIgnoreCase("SC")) fallback = ", cursive";
                if (family.equalsIgnoreCase("DV")) fallback = ", fantasy";

                if (fallback.length() > 0){
                    index++;
                }
            }

            //write font-family tag
            css.append("font-family:" + fontName + fallback + ";");



            //Optional: Parse character set
            if (index < opts.size()){

                if (opts.get(index).equalsIgnoreCase("PC")) css.append("-folio-charset:pc-ibm;");
                else if (opts.get(index).equalsIgnoreCase("SY")) css.append("-folio-charset:symbol;");
                else if (opts.get(index).equalsIgnoreCase("ANSI")) css.append("-folio-charset:ansi;");
                else index--; //a little clumsy - just to cancel out the increment if none matched.
                index++;
            }


            return index;

        }
        return startIndex;
    }

    /**
     * Understands BD+ BD- IT+ IT- HD+ HD- SO+ SO- UN+ UN- UN2+ CD+ CD- OU+ OU- SH+ SH-
     * Maps HD- to display:inline;, UN- and SO- map to text-decoration:normal;-folio-underline:off; and text-decoration:normal;-folio-strikeout:off; respectively.
     * Double-underline is achieved with text-decoration:underline; border-bottom:1px solid;
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseTextAttributes(int startIndex,List<String> opts,StringWriter css){
        boolean usedOption = false;
        String code = opts.get(startIndex);
        if (opts.size() > startIndex + 1){
            if (code.equalsIgnoreCase("UN") && opts.get(startIndex + 1).equalsIgnoreCase("2+")){
                code += "2+";
                usedOption = true;
            }
        }
        String p = getTextAttributeCss(code);
        if (p != null){
            css.append(p);

            //Increment the position
            if (usedOption) return startIndex += 2;
            else return startIndex + 1;
        }
        return startIndex;
    }
    /**
     * Understands BD+ BD- IT+ IT- HD+ HD- SO+ SO- UN+ UN- UN2+ CD+ CD- OU+ OU- SH+ SH-
     * Maps HD- to display:inline;
     * @param codeName
     * @return
     */
    private static String getTextAttributeCss(String c){
        if (c.equalsIgnoreCase("BD+")) return "font-weight:bold;";
        if (c.equalsIgnoreCase("BD-")) return "font-weight:normal;";
        if (c.equalsIgnoreCase("IT+")) return "font-style:italic;";
        if (c.equalsIgnoreCase("IT-")) return "font-style:normal;";
        if (c.equalsIgnoreCase("HD+")) return "display:none;";
        if (c.equalsIgnoreCase("HD-")) return "display:inline;";
        if (c.equalsIgnoreCase("SO+")) return "text-decoration:line-through;";
        if (c.equalsIgnoreCase("SO-")) return "text-decoration:normal;-folio-strikeout:off;";
        if (c.equalsIgnoreCase("UN+")) return "text-decoration:underline;";
        if (c.equalsIgnoreCase("UN2+")) return "text-decoration:underline; border-bottom:1px solid;";
        if (c.equalsIgnoreCase("UN-")) return "text-decoration:normal;-folio-underline:off;";
        if (c.equalsIgnoreCase("CD+")) return "-folio-condensed:on;";
        if (c.equalsIgnoreCase("CD-")) return "-folio-condensed:off;";
        if (c.equalsIgnoreCase("OU+")) return "-folio-outline:on;";
        if (c.equalsIgnoreCase("OU-")) return "-folio-outline:off;";
        if (c.equalsIgnoreCase("SH+")) return "-folio-shadow:on;";
        if (c.equalsIgnoreCase("SH-")) return "-folio-shadow:off;";
        return null;
    }


    /**
     * Understands attributes AP|BP|JU|BR|KT|KN|LH|LS|LW|SD|TS|IN|KN|KT
     * Delegates to tryParseTabSet, tryParseBorder, tryParseParagraphBackgroundColor, tryParseIndents, tryParseSimpleParagraphAttr
     * @param startIndex
     * @param opts
     * @param css
     * @return
     * @throws InvalidMarkupException 
     */
    public static int tryParseParagraphAttribute(int startIndex,List<String> opts,StringWriter css) throws InvalidMarkupException{
        int i = startIndex;
        int t = i;
        if (t == i) t = tryParseSimpleParagraphAttr(i,opts,css);
        if (t == i) t = tryParseTabSet(i,opts,css);
        if (t == i) t = tryParseBorder(i,opts,css,false);
        if (t == i) t = tryParseParagraphBackgroundColor(i,opts,css);
        if (t == i) t = tryParseIndents(i,opts,css);
        if (t == i) t = tryParseKeepAttr(i,opts,css);
        return t;

    }

    /**
     * Translates a keep next and keep together
     * KN,KT
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseKeepAttr(int startIndex,List<String> opts, StringWriter css){

        String name = opts.get(startIndex);
        //No arguments
        //KN: Keep next
        if (name.equalsIgnoreCase("KN") || name.equalsIgnoreCase("KN+")){
            css.append("-folio-keep-next:true;");
            return startIndex+1;
        }
        if (name.equalsIgnoreCase("KN-")){
            css.append("-folio-keep-next:false;");
            return startIndex+1;
        }
        //KT: Keep together
        if (name.equalsIgnoreCase("KT") || name.equalsIgnoreCase("KT+")){
            css.append("-folio-keep-together:true;");
            return startIndex+1;
        }else if (name.equalsIgnoreCase("KT-")){
            css.append("-folio-keep-together:false;");
            return startIndex+1;
        }
        return startIndex;
    }
    /**
     * Only tries to parse AP and BP
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseMarginTopBottomAttr(int startIndex,List<String> opts, StringWriter css){

        String name = opts.get(startIndex);
        
        if (name.equalsIgnoreCase("AP") || name.equalsIgnoreCase("BP")){
        	return tryParseSimpleParagraphAttr(startIndex,opts,css);
        }
        return startIndex;
    }
    /**
     * Attempts to parse VA:Alignment alignment = (CN|TN|BO). Into vertical-align:top|middle|bottom;
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseVerticalAlign(int startIndex,List<String> opts, StringWriter css){
    	String name = opts.get(startIndex);
    	if (name.equalsIgnoreCase("VA")){
    		//TODO: switch from assert() calls to throwing exception
    		assert(opts.size() > startIndex + 1); //1 argument required for this option
	        String value = opts.get(startIndex+1);
	        /*vertical-align values.
	         *	baseline: The baseline of the cell is put at the same height as the baseline of the first of the rows it spans (see below for the definition of baselines of cells and rows).
				top: The top of the cell box is aligned with the top of the first row it spans.
				bottom: The bottom of the cell box is aligned with the bottom of the last row it spans.
				middle: The center of the cell is aligned with the center of the rows it spans.
				sub, super, text-top, text-bottom, <length>, <percentage>: These values do not apply to cells; the cell is aligned at the baseline instead.
				*/
					        
	        if (value.equalsIgnoreCase("CN")) css.append("vertical-align:middle;");
            else if (value.equalsIgnoreCase("TO")) css.append("vertical-align:top;");
            else if (value.equalsIgnoreCase("BO")) css.append("vertical-align:bottom;");
            else{
                assert(false):"Invalid alignment argument for VA: " + value;
            }
	        return startIndex + 2;
    	}
	        
	    return startIndex;
    }
    /**
     * Translates a range of the simpler paragraph attributes:
     * KN,KT,LH,LW,AP,BP,JU,LS
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseSimpleParagraphAttr(int startIndex,List<String> opts, StringWriter css){

        String name = opts.get(startIndex);
        
        int temp = tryParseKeepAttr(startIndex,opts,css);
        if (temp != startIndex) return temp;
        

        //These are for when there isn't another agrument, such as a lone <LH> tag
        //LH: line-height default
        if (name.equalsIgnoreCase("LH") && opts.size() == startIndex + 1){
            css.append("line-height:auto;");
            return startIndex+1;
        }
        //LW: paragraph width default
        if (name.equalsIgnoreCase("LW") && opts.size() == startIndex + 1){
            css.append("width:auto;");
            return startIndex+1;
        }
        //These require a single argument
        if (TokenUtils.fastMatches("AP|BP|JU|LH|LS|LW", name)){
            assert(opts.size() > startIndex + 1); //1 argument required for this option
            String value = opts.get(startIndex+1);

            //AP: Margin-bottom padding
            if (name.equalsIgnoreCase("AP")){
                css.append("margin-bottom:" + fixUnits(value) + ";");
            //BP: Margin-top padding
            }else if (name.equalsIgnoreCase("BP")){
                css.append("margin-top:" + fixUnits(value) + ";");
            //JU: Text justification
            }else if (name.equalsIgnoreCase("JU")){
                if (value.equalsIgnoreCase("LF")) css.append("text-align:left;");
                else if (value.equalsIgnoreCase("CN")) css.append("text-align:center;");
                else if (value.equalsIgnoreCase("RT")) css.append("text-align:right;");
                else if (value.equalsIgnoreCase("FL")) css.append("text-align:justify;");
                else{
                    assert(false):"Invalid justification argument for JU: " + value;
                }
            //LH: Line height
            }else if (name.equalsIgnoreCase("LH")){
                if (value.matches("^0+$")) css.append("line-height:auto;");
                else{
                    css.append("line-height:" + fixUnits(value) + ";");
                }
            //LS: Line spacing
            //NOTE! LS and LH can't work together. Only one or the other will work in HTML
            //TODO: To fix this, we can multiple LS*LH to get the real line height.
            }else if (name.equalsIgnoreCase("LS")){
                double d = Double.parseDouble(value);
                assert( d >= 0);//Line spacing must be greater than 1
                css.append("line-height:" + Double.toString(d) + "em;");
            //LW: paragraph width
            }else if (name.equalsIgnoreCase("LW")){
                if (value.matches("^0+$")) css.append("width:auto;");
                else{
                    css.append("width:" + fixUnits(value) + ";");
                }
            }else{
                assert(false):"Impossible bug";
            }
            return startIndex + 2;
        }
        return startIndex;

    }

    /**
     * Understands TS (tab sets). Outputs -folio-tab-set css values for each tab set. Handles multiple values, stops when it hits something that isn't a tab set value.
     * returns startIndex if not a TS.
     * TS tags are required to have at least one tab set, which requires 3 arguments.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseTabSet(int startIndex,List<String> opts, StringWriter css) throws InvalidMarkupException {
        if (opts.get(startIndex).equalsIgnoreCase("TS")){
            int index = startIndex;
            index++;//move to first argument

            if (startIndex + 1 >= opts.size()){
                //No arguments for tab stop?
                System.out.println("Empty tab stop!");
                return index;
            }
            //TS has a minimum of 3 arguments required
            if(startIndex + 2 >= opts.size()){
               throw new InvalidMarkupException("Invalid Tab Stob (TS) token: " + opts.subList(startIndex, opts.size() - 1).toString());
            }
            while(true){
                if (index >= opts.size()) return index; //we hit the end
                if (index + 2 >= opts.size()) return index; //We don't have enough arguments for another tag set, quit

                String location = opts.get(index);
                String justification = opts.get(index + 1);
                String leader = opts.get(index + 2);

                //Validate that it looks like a tab set. Could be another tag.
                if (TokenUtils.fastMatchesNonCached("^(?:" + rDec + ")|Center|Right$",location)){
                    //This must be a tab set if the location matches.
                    css.append("-folio-tab-set:" + location + " " + justification + " " + leader + ";");
                    index +=3;//Go to next tag
                    continue;
                }else{
                    return index;
                }
            }

        }
        return startIndex;
    }
    /**
     * Understands the IN (indents) tag and options. Corresponds to margin-left (LF), margin-right (RT), and text-indent (FI). Returns startIndex if not a indent tag.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseIndents(int startIndex,List<String> opts, StringWriter css){
        if (opts.get(startIndex).equalsIgnoreCase("IN")){
            int index = startIndex;
            index++;//move to first argument
            while(true){
                if (index >= opts.size()) return index; //we hit the end
                String type = opts.get(index);
                if (!TokenUtils.fastMatches("LF|RT|FI", type)){
                    return index; //we hit an unknown tag
                }else{
                    assert(index + 1 < opts.size()); //A distance unit is required here!
                    //get the value
                    String value = fixUnits(opts.get(index+1));
                    if (type.equalsIgnoreCase("LF")) css.append("margin-left:" + value + ";");
                    else if (type.equalsIgnoreCase("RT")) css.append("margin-right:" + value + ";");
                    else if (type.equalsIgnoreCase("FI")) css.append("text-indent:" + value + ";");
                    else{
                        assert(false);//can't happen
                    }
                    index += 2;//Go to the next tag
                }
            }

        }
        return startIndex;
    }
    /**
     * Understands SD (shade, background color). outputs background-color css style. Returns startIndex if not a match.
     * @param startIndex
     * @param opts
     * @param css
     * @return
     * @throws InvalidMarkupException 
     */
    public static int tryParseParagraphBackgroundColor(int startIndex,List<String> opts, StringWriter css) throws InvalidMarkupException{
        if (opts.get(startIndex).equalsIgnoreCase("SD")){
            assert(opts.size() > startIndex + 1); //One parameter required
            String arg1 = opts.get(startIndex +1);
            if (arg1.equalsIgnoreCase("NO")){
                css.append("background-color:transparent;");
                return startIndex + 2;
            }
            else if (arg1.equalsIgnoreCase("DC")){
                css.append("background-color:transparent;");
                return startIndex + 2;
            }
            else{
            	if (opts.size() <= startIndex +3)
            		////Three parameter requireds
            		throw new InvalidMarkupException("SD:NO or SD:r,g,b are the only valid formats. 3 args are required if the second argument is not NO. ");
               
                css.append("background-color:#" + getHexColor(opts.get(startIndex +1),opts.get(startIndex + 2),opts.get(startIndex + 3)) + ";");
                return startIndex + 4;
            }
        }
        return startIndex;
    }


    /**
     * Understands BR and the sub-options. Returns startIndex if it can't parse the tag. (i.e., if it's not a BR tag).
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseBorder(int startIndex, List<String> opts, StringWriter css, boolean enableTableScopes){
        int index = startIndex;
        if (!opts.get(index).equalsIgnoreCase("BR")){
            return startIndex; //not a match
        }else
        {
            index++;//increment to first argument
            assert(index < opts.size());//At least one argument required for BR option
            while(true){
                if (index >= opts.size()) return index; //We hit the end of the options
                int newIndex = tryParseBorderOption(index,opts,css, enableTableScopes);
                if (newIndex == index) {
                    if (index == startIndex + 1) {
                        assert(false);//This BR tag had no arguments!!!!
                    }
                    return index; //Can't parse no more - we hit an unrecognized tag.
                }else{
                    index = newIndex;
                }
            }
        }
    }
    /**
     * Understands border options RT, LF, TP, BT, AL (right, left, top, bottom, all). returns startIndex if it doesn't recognize the first tag.
     * Parses one side specification and returns the index of the subsequent tag. Border options also include the padding information
     * @param startIndex
     * @param opts
     * @param css
     * @return
     */
    public static int tryParseBorderOption(int startIndex, List<String> opts, StringWriter css, boolean enableTableScopes){
        int index = startIndex;
        String scope = null;
        if (opts.get(index).equalsIgnoreCase("RT")) scope = "-right";
        if (opts.get(index).equalsIgnoreCase("LF")) scope = "-left";
        if (opts.get(index).equalsIgnoreCase("TP")) scope = "-top";
        if (opts.get(index).equalsIgnoreCase("BT")) scope = "-bottom";
        if (enableTableScopes){
	        if (opts.get(index).equalsIgnoreCase("HZ")) scope = "-horizontal";
	        if (opts.get(index).equalsIgnoreCase("VT")) scope = "-vertical";
        }
        if (opts.get(index).equalsIgnoreCase("AL")) scope = "";
        if (scope != null) {
            return tryParseBorderData(startIndex + 1,opts,css,scope); //parse the data
        }else{
            return startIndex; //Nothing this function can parse here.
        }
    }
    
    public static int tryParseBorderData(int startIndex, List<String> opts, StringWriter css, String scope){
        int index = startIndex;
        assert(index + 1 < opts.size());//two subsequent numbers required!
        //parse padding
        css.append("padding" + scope + ":" + fixUnits(opts.get(index + 1)) + ";");//inside spacing == padding
        //parse width
        String width = fixUnits(opts.get(index));
        //is width non-0?
        boolean widthZero = opts.get(index).matches("^0+$");
        //parse color
        String color = "";
        //rebase for ease of tracking. Start after padding option
        index+= 2;
        if (index < opts.size()){
            if (opts.get(index).equalsIgnoreCase("FC")){
                index++;//rebase to first arg
                assert(index < opts.size());//FC requires at least one argument
                if (opts.get(index).equalsIgnoreCase("DC")){
                    //default color, do nothing
                    index++;
                }else{
                    assert(index + 2 < opts.size());//three arguments required if not DC
                    color = " #" + getHexColor(opts.get(index),opts.get(index+1),opts.get(index+2));
                    index+=3;//Move to after colors
                }
            }
        }
        //write css even if border 0
        css.append("border" + scope + ":" + width + " solid" + color + ";");
        return index; //The next option after the parsed ones 
        
    }
    

    /**
     * Converts three integer strings into a single 6-character hexadecimal equivalent.
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static String getHexColor(String red, String green, String blue){
        assert(red != null && green != null && blue != null);
        int iRed = Integer.parseInt(red);
        int iGreen = Integer.parseInt(green);
        int iBlue = Integer.parseInt(blue);
        assert(iRed >= 0 && iGreen >= 0 && iBlue >= 0 && iRed < 256 && iGreen < 256 && iBlue < 256);
        return toHexByte(iRed) + toHexByte(iGreen) + toHexByte(iBlue);
    }
    /**
     * Converts an integer to a 2-digit hexadecimal string. i.e. 0 -> 00, 11 -> 0b, 255 -> ff
     * i must fall inside 0 and 255 inclusive.
     * @param i
     * @return
     */
    public static String toHexByte(int i){
        assert(i >= 0 && i < 256);
        String s = Integer.toString(i,16);
        if (s.length() < 2) return "0" + s;
        return s;
    }
        /**
     * Numeric with optional decimal 
     */
    public static String rDec = "-?[0-9]++(?:\\.[0-9]*)?"; //Added optional negative sign - Jul 30, 09

        /**
     * Translates a unit from folio's notation (5c, 5p, 5t, 3.44) to css: (5cm, 5pt , 100in, 3.44in)
     * @param u
     * @return
     */
    public static String fixUnits(String u){
        if (u.matches("^" + rDec + "c$")) return u + "m"; //TODO: Isn't this case-sensitive?!!!
        if (u.matches("^" + rDec + "p$")) return u + "t";
        String twips = getFirstMatch(u,"^(" + rDec + ")t$");
        if (twips != null) {

            return (Double.toString(Double.parseDouble(twips) * 20)) + "pt"; //convert to points - no twips in css
        }
        //assert it is just a plain number
       assert( u.matches("^" + rDec + "$")) : "Failed to parse " + u;
        
        // if (!u.matches("^" + rDec + "$")) throw new InvalidMarkupException("Failed to parse value - unrecognized units: \"" + u + "\".");

        return u + "in"; //TODO: Do we know inches is the default? Could this be an infobase-level setting?
    }
    
    /**
     * Translates a unit to folio's notation (5c, 5p, 5t, 3.44) from css: (5cm, 5pt , 100in, 3.44in)
     * @param u
     * @return
     */
    public static String unfixUnits(String u){
        if (u.matches("^" + rDec + "cm$")) return u.substring(0, u.length() -1);
        if (u.matches("^" + rDec + "pt$")) return u.substring(0, u.length() -1);
        if (u.matches("^" + rDec + "in$")) return u.substring(0, u.length() -2);
       assert(false);
       return null;
    }
    
    
    /**
     * Doesn't support pixels.
     * @param cssUnit
     * @return
     */
    public static double toInches(String cssUnit){
    	//0.0138888889 inches in a point.
    	//1 centimeter = 0.393700787 inches
    	
    	assert(isCssUnit(cssUnit)):cssUnit;
    	
    	String unit = "in";
    	if (cssUnit.endsWith("pt")) unit = "pt";
    	if (cssUnit.endsWith("cm")) unit = "cm";
    	assert(!cssUnit.endsWith("px"));
    	
    	//Drop the units when parsing.
    	double val = Double.parseDouble(cssUnit.endsWith(unit) ? cssUnit.substring(0, cssUnit.length() - unit.length()) : cssUnit);
    	
    	if (unit.equals("pt")) return val * 0.0138888889;
    	if (unit.equals("cm")) return val * 0.393700787;
    	return val;
    }
    
    public static Double toInches(String cssUnit, Double ifAuto){
        //0.0138888889 inches in a point.
        //1 centimeter = 0.393700787 inches

        if ("auto".equalsIgnoreCase(cssUnit)) return ifAuto;
        return toInches(cssUnit);
    }
    
    /**
     * Returns true if the specified string could be a unit specification (i.e, decimal followed by p|c|t
     * @param u
     * @return
     */
    public static boolean isFolioUnit(String u){
        return TokenUtils.fastMatchesNonCached("^" + rDec + "(?:p|c|t)?$",u);
    }
    
    /**
     * Returns true if the specified string could be a unit specification (i.e, decimal followed by p|c|t
     * @param u
     * @return
     */
    public static boolean isCssUnit(String u){
        return TokenUtils.fastMatchesNonCached("^" + rDec + "(?:pt|cm|px|in)?$",u);
    }
    

	public static boolean isNumber(String s) {
		 return TokenUtils.fastMatchesNonCached("^" + rDec + "$",s);
	}

    /**
     * Multiplies the decimal portion of the specified units string by the specified factor and returns the string result.
     * @param units
     * @param factor
     * @return
     */
    public static String multiply(String units, double factor){
        Matcher m = TokenUtils.getPatternCachedCI("^(" + rDec + ")(t|c|p|pt|cm|in|)$").matcher(units);
        if (!m.find()) assert(false); //Invalid unit units

        return Double.toString(Double.parseDouble(m.group(1)) * factor) + m.group(2);
    }

    /**
     * Case-insensitive
     * @param s
     * @param regex
     * @return
     */
    public static String getFirstMatch(String s, String regex){
        Matcher m = TokenUtils.getPatternCachedCI(regex).matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }



    /**
     * Some Folio->CSS conversion can only happen after all CSS is added.
     * @param css
     * @return
     */
	public static String fixCss(String css, boolean silent) {
		if (css.indexOf("margin-left") > -1 && css.indexOf("text-indent") > -1){
			Map<String, String> map = CssUtils.parseCss(css, true);
			
			double indent = toInches(map.get("text-indent"));
			double margin = toInches(map.get("margin-left"));
			double padding = map.get("padding-left") == null ? 0 : toInches(map.get("padding-left"));
			if (indent < 0){
				margin += indent; //Move indent distance from 'margin' to 'padding
				padding -= indent;
				//Leave indent along
				map.put("margin-left", Double.toString(margin) + "in");
				map.put("padding-left", Double.toString(padding) + "in");
				//if (!silent) System.out.print("Fixing css (" + indent + "): " + css + "\n");
			}
			
			CssUtils.coalesce(map);
			return CssUtils.writeCss(map);
		}
		return css;
	}




}