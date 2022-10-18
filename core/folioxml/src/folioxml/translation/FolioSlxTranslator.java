package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.folio.FolioToken;
import folioxml.slx.SlxToken;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Doesn't translate into true SLX - that requires actual transformation. Use FolioSlxTransformer.
 * This just performs syntax checking and direct translations into TransitionalSlx
 *
 * @author nathanael
 */
public class FolioSlxTranslator {

    public FolioSlxTranslator() {
    }

    /**
     * Translates the specified FolioToken into a SLX transitional token.
     *
     * @param ft
     * @return
     * @throws InvalidMarkupException
     */
    public static SlxToken translate(FolioToken ft) throws InvalidMarkupException {
        SlxToken t = null;

        if (ft.type == FolioToken.TokenType.Text) {
            //Entity encode text tokens. Entities are split apart into separate tokens during Transformation
            t = new SlxToken(SlxToken.TokenType.Text, TokenUtils.lightEntityEncodeAndConvertFolioBrackets(ft.text));
        } else if (ft.type == FolioToken.TokenType.Comment) {
            //Translate comments. SlxToken takes the 'text' paramater as the entire markup of the token, including special tags.
            t = new SlxToken(SlxToken.TokenType.Comment, "<!--" + ft.text.replaceAll("--", "-&#x002D") + "-->");  //Added -- encoding.
        } else if (ft.type == FolioToken.TokenType.Tag) {
            //Tag conversion is the difficult part. This goes in a separate function
            t = convertTag(ft);

        } else {
            throw new InvalidMarkupException("TokenType.None is not allowed");
        }
        //Attach the source token reference. If the token throws an error, we can trace backwards and find the line/char count.
        t.sourceToken = ft;


        return t;
    }


    /**
     * Returned tags include  infobase-meta, style-def/>, record, record-attribute/>, span, link, popupLink, end-popup-contents/>, note, namedPopup, parabreak />,  object/>, table, tr, td
     * paragraph-attribute/>, pagebreak />, br/>, bookmark/>, pp/>, se/>
     *
     * @param ft
     * @return
     * @throws InvalidMarkupException
     */
    public static SlxToken convertTag(FolioToken ft) throws InvalidMarkupException {
        SlxToken t = null;

        //TODO: Ordering these such that the most common tags are checked first would be a great improvement.


        try {

            //All checks follow the pattern if (ft.matches("TAG")), and are mutually exclusive.


            //Translate record tag. Only opening <RD> tags exist.
            if (ft.matches("RD")) {
                t = new SlxToken("<record>");

                //Get the ID
                String recordId = ft.getOptionAfter("ID");
                if (recordId != null) t.set("folioId", recordId);

                //Get the custom heading flag
                if (ft.hasOption("CH")) t.set("customHeading", "true");

                //Remove ID and custom heading - the level type should be left.
                List<String> opts = new ArrayList<String>();
                opts.addAll(ft.getOptionsArray());
                int ixCH = getIndexOfCaseInsensitive(opts, "CH");
                if (ixCH > -1) opts.remove(ixCH);
                int ixID = getIndexOfCaseInsensitive(opts, "ID");
                if (ixID > -1) {
                    opts.remove(ixID); //remove "id"
                    if (ixID < opts.size()) {
                        opts.remove(ixID);
                    } //Remove the ID's value (if present)
                }
                //Level
                if (opts.size() > 0) {
                    t.set("level", opts.get(0));
                    t.set("class", opts.get(0));
                }
                //Shouldn't be any other tags
                assert (opts.size() <= 1);
                return t;
            }

            if (ft.matches("GR") && ft.assertCount(1))
                return new SlxToken("<record-attribute/>").set("groups", ft.get(0));
            if (ft.matches("LV") && ft.assertCount(1))
                return new SlxToken("<record-attribute/>").set("level", ft.get(0)).set("class", ft.get(0)); //Aug 13 - think I fixed <LV> handling bug where class didn't get added.
            //What if the LV tag exists AND the record specifies a level? Unit test this.


            if (ft.matches("BK") && ft.assertCount(1)) return new SlxToken("<bookmark name=\"" + ft.get(0) + "\" />");
            if (ft.matches("JD") && ft.assertCount(1)) return new SlxToken("<bookmark name=\"_" + ft.get(0) + "\" />");

            //Links
            t = FolioLinkUtils.translate(ft);
            if (t != null) return t;
            //Objects
            t = FolioObjectUtils.translateObject(ft);
            if (t != null) return t;


            //Field application
            if (ft.matches("FD")) {
                ft.assertCount(1); //Fields are only supposed to have one parameter

                String style = ft.get(0);
                if (ft.isClosing()) {
                    return new SlxToken("</span>").set("type", style);
                } else {
                    return new SlxToken("<span>").set("class", style).set("type", style);
                }
            }
            //Character styles and highlighter application. These cannot be nested or overlapped.
            if (ft.matches("CS") || ft.matches("PN")) {

                if (ft.isClosing()) {
                    if (ft.matches("CS")) return new SlxToken("</span type=\"characterstyle\">");
                    if (ft.matches("PN")) return new SlxToken("</span type=\"highlighter\">");
                } else {
                    ft.assertCount(1);
                    String style = ft.get(0);
                    if (ft.matches("CS")) return new SlxToken("<span type=\"characterstyle\">").set("class", style);
                    if (ft.matches("PN")) return new SlxToken("<span type=\"highlighter\">").set("class", style);
                }
            }


            if (ft.matches("SE") && ft.assertCount(0)) return new SlxToken("<se />");

            if (ft.matches("HR") && ft.assertCount(0)) return new SlxToken("<parabreak />");
            if (ft.matches("PP") && ft.assertCount(0)) return new SlxToken("<pp />");

            //Paragraph style application
            if (ft.matches("PS")) return new SlxToken("<paragraph-attribute/>").set("class", ft.get(0));


            if (ft.matches("CR") && ft.assertCount(0)) return new SlxToken("<br />");
            if (ft.matches("TB") && ft.assertCount(0)) return new SlxToken("&#09;");
            if (ft.matches("HS") && ft.assertCount(0)) return new SlxToken("&nbsp;");


            //Paragraph attributes AP|BP|JU|BR|KT|KN|LH|LS|LW|SD|TS|IN|KN //Aug 12 - Added KN/KT+/- - a paragraph attr.
            if (ft.matches("^(AP|BP|JU|BR|KT|KN|LH|LS|LW|SD|TS|IN)[\\+\\-]?$")) {
                //Parse the folio arguments
                StringWriter css = new StringWriter(50);
                int stoppedBefore = FolioCssUtils.tryParseParagraphAttrs(0, ft.getOptionsArrayWithTagName(), css);
                assert (stoppedBefore == ft.count() + 1); //should end after last tag

                return new SlxToken("<paragraph-attribute/>").set("style", css.toString());
            }


            //Character attributes BD, IT, HD, SO, UN, CD, OU, SH, FT, PT, BC, FC, SB, SP, SS
            String type = getCharacterAttrType(ft.tagName);
            if (type != null) {
                t = new SlxToken("<span>").set("type", type); //type="bold|italic|hidden|strikeout|underline|condensed|outline|shadow|font-family|font-size|background-color|foreground-color|subsuperscript

                if ((ft.tagName.length() > 2 || ft.count() > 0) && !ft.isClosing()) {
                    //Opening tag of ON or OFF setting
                    t.tagType = SlxToken.TagType.Opening;
                    //Parse into css
                    StringWriter css = new StringWriter(50);
                    List<String> opts = ft.getOptionsArrayWithTagName();
                    int stoppedBefore = FolioCssUtils.tryParseCharacterAttribute(0, opts, css);
                    if (stoppedBefore < ft.count() + 1)
                        throw new InvalidMarkupException("Failed to parse all of character style tag " + ft.text + " : " + join(opts.subList(stoppedBefore, opts.size()))); //Should parse all available tags
                    t.set("style", css.toString());

                } else {
                    //default, closing tag
                    t.tagType = SlxToken.TagType.Closing;
                    assert (ft.count() == 0 && ft.tagName.length() == 2);
                }
                t.updateMarkup(); //Update textual representation from member variables. We've changed the tag type since we created it, and the markup needs to be updated.
                return t;
            }

            if (ft.matches("PB") && ft.assertCount(0)) return new SlxToken("<pagebreak />");
        
        /*<CH:ANSI Value (Decimal)>
        <CH:$ANSI Value (Hex)>
        Parameters
        ANSI Value is taken from the Windows ANSI character set. Characters may be translated to another character depending on the font you are using. If the infobase is to be used on both the Windows and Macintosh platforms, ensure that that character can be displayed on both (add the character to an infobase on Windows and see if it displays on the Macintosh).
        Description
        Character is a special character formatting code used to add ANSI characters to flat files. This code is supported only on import; the code is not exported from the infobase (instead of this code, the actual character is exported, although the character will not display correctly in most DOS text editors).
        Use either the decimal value for the ANSI character (32 - 255) or the Hex value ($20 - $FF). Hex values must be preceded by a dollar sign ($).
        Examples
        To insert an em dash, use either <CH:151> or <CH:$97>.
        To insert a section symbol, use either <CH:167> or <CH:$A7>.
   */
            //Handles CH (character ascii), PB (pagebreak), CR (carriage return), TB (tab), BK (Bookmark), JD (Jump destination)
            if (ft.matches("CH")) {
                //Validate
                assert (ft.tagOptions.matches("\\$?[0-9a-fA-F]+"));

                if (ft.tagOptions.startsWith("$")) {
                    return new SlxToken("&#x" + ft.tagOptions.replace("$", "") + ";");
                } else {
                    return new SlxToken("&#" + ft.tagOptions + ";");
                }
            }

            //Popup Link <PW, Style Name, Width, Height,"Title"> <LT> </PW>

            //Notes <NT:Width,Height,"Title"> </NT>

            //<DP:Width,Height,"Title"> ... </DP> Named popup link definition
            if (ft.matches("PW|NT|DP")) {
                if (ft.matches("PW")) t = new SlxToken("<popupLink>");
                else if (ft.matches("NT")) t = new SlxToken("<note>");
                else if (ft.matches("DP")) t = new SlxToken("<namedPopup>");

                if (ft.isClosing()) t.tagType = SlxToken.TagType.Closing;
                else {
                    int index = 0;
                    List<String> opts = ft.getOptionsArray();

                    //Popup links have a (required) style name at index 0;
                    if (ft.matches("PW")) {
                        assert (opts.size() > index);
                        t.set("class", opts.get(0));
                        index++;
                    }

                    while (index < opts.size()) {
                        if ((t.get("title") == null) && !FolioCssUtils.isFolioUnit(ft.get(index))) {
                            if (!ft.wasQuoted(index)) {
                            } //warning!!! Quotes are required on the title!!

                            t.set("title", ft.get(index));
                        } else if (t.get("width") == null) {
                            t.set("width", FolioCssUtils.fixUnits(ft.get(index)));
                        } else if (t.get("height") == null) {
                            t.set("height", FolioCssUtils.fixUnits(ft.get(index)));
                        } else {
                            break; //extra paramter
                        }
                        index++;
                    }
                    if (ft.matches("DP") && (t.get("title") == null))
                        throw new InvalidMarkupException("The title is required on named popups - they must be named. ");
                    if (index < opts.size()) {
                        //This is for a corner case issue in the export filter of folio: ticket #2
                        //<PW:Popup,5.3,1.4,"Various Pictures",FD:"info">
                        if (ft.matches("PW")) {
                            t.set("extraTags", join(opts.subList(index, opts.size()))); //store them in an attribute
                        } else
                            //This is for a corner case issue in the export filter of folio:. ticket #2
                            //<NT:6.3,3.6,"Anomaly",FD:Popup>
                            if (ft.matches("NT")) {
                                t.set("extraTags", join(opts.subList(index, opts.size()))); //store them in an attribute
                            } else
                                throw new InvalidMarkupException("Failed to parse all of note tag " + ft.text + " : " + join(opts.subList(index, opts.size())));
                    }
                }
                return t;
            }
            if (ft.matches("LT")) return new SlxToken("<end-popup-contents/>");

            //Try Tables (TA, RO, CE)
            t = tryTables(ft);
            if (t != null) return t;

            //Record heading fields
            if (ft.matches("BH") && ft.assertCount(0)) return new SlxToken("<span type=\"recordHeading\">");
            if (ft.matches("EH") && ft.assertCount(0)) return new SlxToken("</span type=\"recordHeading\">");

            //Infobase level tags, including all that use "replace definition"
            t = tryDefinitionTags(ft);
            if (t != null) return t;

            //comment out User Extension data
            t = tryCommentOut(ft, "UX");
            if (t != null) return t;


            throw new InvalidMarkupException("Unknown tag: " + ft.text);
        } catch (InvalidMarkupException e) {
            if (e.getToken() == null) e.setToken(ft); //Works great!
            throw e;
        }
    }

    /**
     * BD, IT, HD, SO, UN, CD, OU, SH, FT, PT, BC, FC, SB, SP, SS
     *
     * @param c
     * @return
     */
    public static String getCharacterAttrType(String c) {
        c = c.toLowerCase();
        if (c.length() >= 2) c = c.substring(0, 2);
        if (c.equalsIgnoreCase("BD")) return "bold";
        if (c.equalsIgnoreCase("IT")) return "italic";
        if (c.equalsIgnoreCase("HD")) return "hidden";
        if (c.equalsIgnoreCase("SO")) return "strikeout";
        if (c.equalsIgnoreCase("UN")) return "underline";
        if (c.equalsIgnoreCase("CD")) return "condensed";
        if (c.equalsIgnoreCase("OU")) return "outline";
        if (c.equalsIgnoreCase("SH")) return "shadow";
        if (c.equalsIgnoreCase("FT")) return "font-family";
        if (c.equalsIgnoreCase("PT")) return "font-size";
        if (c.equalsIgnoreCase("BC")) return "background-color";
        if (c.equalsIgnoreCase("FC")) return "foreground-color";
        if (c.equalsIgnoreCase("SB")) return "subsuperscript";
        if (c.equalsIgnoreCase("SP")) return "subsuperscript";
        if (c.equalsIgnoreCase("SS")) return "subsuperscript";
        return null;
    }

    public static SlxToken tryDefinitionTags(FolioToken ft) throws InvalidMarkupException {
        //Detect and remove replace definition
        String lastOption = null;
        String replaceMode = null;
        if (ft.count() > 0) {
            lastOption = (ft.get(ft.count() - 1));
            //Jan 21, 2009. This (String.matches) is taking 7% of library time. Changed to use TokenUtils.matchesCI (cached patterns)
            //Generic parsing of Replace Definition option for all definition tags.
            if (TokenUtils.matchesCI("^RP\\+?$", lastOption)) {
                ft.remove(ft.count() - 1);
                replaceMode = "true";
            } else if (TokenUtils.matchesCI("^RP\\-$", lastOption)) {
                ft.remove(ft.count() - 1);
                replaceMode = "false";
            }
        }
        SlxToken t = tryDefinitionTagsCore(ft);
        if (t != null) {
            if (replaceMode != null)
                t.set("replaceDefinition", replaceMode); //TODO: is this handled properly in HTML/CSS conversion?
            return t;
        }
        if (replaceMode != null)
            throw new InvalidMarkupException("RP (Replace Definition) option found on a non-definition tag.");
        return null;
    }

    public static SlxToken tryDefinitionTagsCore(FolioToken ft) throws InvalidMarkupException {

        SlxToken t;

        //Infobase-level remarks RM (one per infobase) abstract AS, Author AU, footer FO, header HE
        /*  Abstract	<AS>Abstract text</AS>	No limit
            Author	<AU>Author text</AU>	No limit
            Remark	<RM>Remark text</RM>	No limit
            Revision Date	<RE:Date text>	45
            Subject	<SU>Subject text</SU>	No limit
            Title	<TT:"Title text">	127
         *  Header <HE>
         *  Footer <FO>
         *  Version <VI>
        */
        //HE FO AU AS RM SU RE TT VI
        t = tryRename(ft, "HE", "<infobase-meta type=\"header\">");
        if (t != null) return t;
        t = tryRename(ft, "FO", "<infobase-meta type=\"footer\">");
        if (t != null) return t;


        t = tryRename(ft, "AU", "<infobase-meta type=\"author\">");
        if (t != null) return t;
        t = tryRename(ft, "AS", "<infobase-meta type=\"abstract\">");
        if (t != null) return t;
        t = tryRename(ft, "RM", "<infobase-meta type=\"remark\">");
        if (t != null) return t;
        t = tryRename(ft, "SU", "<infobase-meta type=\"subject\">");
        if (t != null) return t;
        if (ft.matches("RE") && ft.assertCount(1))
            return new SlxToken("<infobase-meta type=\"revision-date\" />").set("content", ft.get(0));

        if (ft.matches("TT") && ft.assertCount(1))
            return new SlxToken("<infobase-meta type=\"title\" />").set("content", ft.get(0));

        if (ft.matches("VI") && ft.assertCount(3))
            return new SlxToken("<infobase-meta type=\"version\" />").set("data", ft.get(0)).set("filetype", ft.get(1)).set("version", ft.get(2));

        t = tryRename(ft, "DQ", "<infobase-meta type=\"default-query\">");
        if (t != null) return t;

        //Generate codes for footer/header
        t = tryCommentOut(ft, "GP|GT|GD|GM|GI|GA|GF|GQ");
        if (t != null) return t;
        //t = tryRename(ft,"GP","<obj type=\"generate\"  generate=\"page-number\"/>"); if (t != null) return t;


        if (ft.matches("FE|ST|LE|PD|PA")) {
            //FE field definition
            //PD ighlighter pen definition
            //ST Text Styles
            //LE level style defintion
            //PA Paragraph style definition

            t = new SlxToken("<style-def />");

            //Style name
            assert (ft.count() > 0);
            t.set("class", ft.get(0));
            t.set("styleName", ft.get(0));

            if (ft.matches("LE")) t.set("type", "level");
            if (ft.matches("PD")) t.set("type", "highlighter");
            if (ft.matches("PA")) t.set("type", "paragraph");

            int start = 1;

            if (ft.matches("ST")) {
                if (ft.get(1).equalsIgnoreCase("CS")) t.set("type", "character-style");
                if (ft.get(1).equalsIgnoreCase("LK")) t.set("type", "link");
                start++;
            } else if (ft.matches("FE")) {
                //Get field type
                String type = ft.get(start);
                if (type.equalsIgnoreCase("TX")) type = "text";
                else if (type.equalsIgnoreCase("DT")) type = "date";
                else if (type.equalsIgnoreCase("TM")) type = "time";
                else if (type.equalsIgnoreCase("IR")) type = "integer";
                else if (type.equalsIgnoreCase("FP")) type = "decimal";
                t.set("fieldType", type);
                t.set("type", "field");
                start++;

                //Optional
                if (start < ft.count() && ft.wasQuoted(start)) {
                    t.set("format", ft.get(start));
                    start++;
                }
                //Optional
                if (start < ft.count() && ft.get(start).equalsIgnoreCase("IX")) {
                    start++;

                    List<String> ops = ft.getOptionsArray();
                    String indexOptions = "";
                    while (true) {
                        if (start >= ops.size()) {
                            break;
                        }
                        if (TokenUtils.fastMatchesNonCached("TF|PF|TE|NO|PR|DT|FP|SW", ops.get(start))) {
                            indexOptions += ops.get(start) + ",";
                            start++;
                        } else break;
                    }
                    t.set("indexOptions", indexOptions);
                }

            }
            //LE, PD, and PA (level, highlighter, and paragraph definitions) don't have any special options between the name and attributes.

            List<String> opts = ft.getOptionsArray();
            opts = opts.subList(start, opts.size());

            //Parse the folio arguments
            StringWriter css = new StringWriter(50);
            int stoppedBefore = FolioCssUtils.tryParseAll(0, opts, css);

            if (stoppedBefore < opts.size())
                throw new InvalidMarkupException("Failed to parse all options on " + ft.text + ". Remaining text: " + join(opts.subList(stoppedBefore, opts.size())));

            t.set("style", css.toString());
            return t;

        }
        //LN level definition
        if (ft.matches("LN")) {
            List<String> withoutCommas = new ArrayList<String>();
            for(String s: ft.getOptionsArray()){
                withoutCommas.add(s.replace(',', ' '));
            }
            return new SlxToken("<infobase-meta type=\"levels\"/>").set("content", join(withoutCommas));

        }
        //Object definition.
        if (ft.matches("OD")) {
            return FolioObjectUtils.translateObjectDefinition(ft);

        }

        //TODO: implement DF! Sets the default font and size. 
        //DF, PR, DQ defaults

        //HL Hit Lst

        //QT Query Template
        //RC Reconcile shadow file

        //TP Title page
        //WP:TOC|DOC|REF:"bitmapobject",1(fixed), 0(scroll)
        t = tryCommentOut(ft, "PR|HL|QT|RC|WP|TP"); //Hit list, Query Template, Reconcile Shadow File, Wallpaper, Title Page
        if (t != null) return t;
        return null;
    }

    public static SlxToken tryTables(FolioToken ft) throws InvalidMarkupException {
        if (!ft.matches("TA|CE|RO")) return null;

        //Handle closing tags first.
        if (ft.isClosing()) {
            if (ft.count() > 0)
                throw new InvalidMarkupException("Closing </TA> </RO>, and </CE> tags cannot have attributes.", ft);
            if (ft.matches("TA")) return new SlxToken("</table>");
            if (ft.matches("RO")) return new SlxToken("</tr>");
            if (ft.matches("CE")) return new SlxToken("</td>");

        }
        //Now we only have opening tags to deal with.
        if (ft.matches("RO")) {
            /*<RO> is the Row Delimiter. <RO> defines the start of a row. The current row is ended when a new row begins (or you may explicitly end the row with </RO>).
    		 * A table may have any number of rows. Any row in the table may be a header row. See Row Attributes for more information.
    		 * Row attributes affect single rows in the table. Row attributes include:
    		 * HE � Marks the row as a Header Row. Tables may have more than one header row.
    		 */

            boolean isHeader = "HE".equalsIgnoreCase(ft.get(0));

            if (ft.count() > 1 || (ft.count() > 0 && !isHeader))
                throw new InvalidMarkupException("RO tags can only have one attribute - HE (header row).", ft);


            SlxToken t = new SlxToken("<tr>");
            if (isHeader) t.set("rowIsHeader", "true");
            return t;
        }

        if (ft.matches("CE")) {
            SlxToken t = new SlxToken("<td>");

            List<String> opts = ft.getOptionsArray();
            StringWriter css = new StringWriter(50);


            int n = 0;
            int i = n;

            while (true) {
                if (i >= opts.size()) break;


                if (i == n) {
                    if ("HE".equalsIgnoreCase(opts.get(i))) {
                        css.append("-folio-cell-header:true;");
                        i++;
                    }
                }

                if (i == n) i = FolioCssUtils.tryParseCellStyleAttribute(i, opts, css);
                if (i == n) i = tryParseOtherCellAttr(i, opts, t); //MD (rowspan), MR (colspan), HC (columnIsHeader)

                if (i == n) {
                    throw new InvalidMarkupException("Failed to parse all options on " + ft.text + ". Remaining text: " + join(opts.subList(i, opts.size())));
                } else n = i;
            }

            t.set("style", css.toString());
            return t;
        }
        if (ft.matches("TA")) {
            SlxToken t = new SlxToken("<table>");

            t.set("cellspacing", "0");

            List<String> opts = ft.getOptionsArray();
            StringWriter css = new StringWriter(50);
            //So align works properly... we must init the margin-left and margin-right values appropriately.
            //This allows the true margins (IN) to work in conjunction with JU
            //Set default padding-verical and padding-horizontal, default padding-top and padding-bottom.
            css.append("-folio-vertical-gap:0.022in; -folio-horizontal-gap:0.075in; margin-top:0.022in; margin-botton:0.022in; border-collapse: collapse; margin-left:0px; margin-right:0px;");

            int n = 0;
            int i = n;

            if (opts.size() > 0) {
                if (FolioCssUtils.isFolioUnit(opts.get(0))) {
                    //Column widths are specified.
                    int cols = Integer.parseInt(opts.get(0));

                    assert (opts.size() > cols); //There must be the same number of widths specified as columns.

                    StringBuilder widths = new StringBuilder();
                    for (int k = 0; k < cols; k++) {
                        //Gotta allow percents here...
                        //soo...
                        String u = opts.get(k + 1).trim();
                        if (u.endsWith("%")) {
                            if (!FolioCssUtils.isNumber(u.substring(0, u.length() - 1)))
                                throw new InvalidMarkupException("Invalid percentage unit " + u, ft);
                        } else {
                            u = FolioCssUtils.fixUnits(u);
                        }

                        widths.append(u);
                        if (k < cols - 1) widths.append(",");
                    }
                    t.set("colWidths", widths.toString());
                    //Update pointer
                    n = cols + 1;
                    i = n;
                }
            }


            while (true) {
                if (i >= opts.size()) break;

                if (i == n) i = FolioCssUtils.tryParseTableStyleAttribute(i, opts, css);

                if (i == n) {
                    throw new InvalidMarkupException("Failed to parse all options on " + ft.text + ". Remaining text: " + join(opts.subList(i, opts.size())));
                } else n = i;
            }

            t.set("style", css.toString());
            return t;
        }
        return null;
       
        /* Tables:
         * folioxml.core.InvalidMarkupException: Unknown tag: <TA:6,1,1,1.4,1.4,1.4,0.4; JU:CN; HG:0; VG:0; BP:0; AP:0>
	at folioxml.translation.FolioSlxTranslator.convertTag(FolioSlxTranslator.java:287)
		
         */
    	/*
    	<TA:Number of Columns,Widths for Each Column,Table Attributes><RO:Row Attributes><CE:Cell Attributes>Cell Text</TA>
    	
    	Parameters
    	Tables must be contained within a single record. It is recommended that any single record contain only one table.
    	Tables are comprised of three primary codes: the Table Definition, the Row Delimiters, and the Cell Delimiters.
    	
    	<TA> is the Table Definition. TA has two optional parameters and set of optional attributes:
    	Number of Columns � Columns is an integer and specifies the number of columns for the table. (One column is required for each cell; cells may be merged if necessary.) Number of defined columns may not exceed 32. Note: Use this parameter only if you need fixed width cells.
    	Widths for Each Column � Width of each column in the table, separated by commas. Widths may be specified in inches or by percentage of the window. The total number of widths specified must equal the number of columns (if you have 3 columns, you must have 3 widths). Total width for columns may not exceed 22 inches (including the horizontal gap between columns). Note: Use this parameter only if you need fixed width cells.
    	
    	Note: The number of columns and the widths for the columns are optional. If these parameters are not included, the table will be generated from the number of cells actually used in the table. Each cell will have the same width. (22 inch total maximum width; up to 32 columns.)
    	Table Attributes � Tables may contain several attributes. These attributes affect the entire table and set default borders for the cells in the table. See Table Attributes for more information.
    	
    	
    	�Cells may contain any formatting codes, text, and objects allowed within the main body of an infobase, with the exception of levels and record codes. Cells may contain multiple paragraphs.

    	As you can see, you do not need to support everything in your SGML file or add a huge attribute list to every table you create. Use the attributes that are necessary for your tables, and then map them to the appropriate Folio flat file codes (using OmniMark or some other pattern matching utility).
    	Note: If you export an infobase containing a table with no fixed width columns, an extra semi-colon is added to the export code. This does not cause problems on import. The code comes out as <TA:; BR:�(rest of table definition)>
    	Examples
    	<RD>A basic table (4 rows, 4 columns, no formatting, auto-defined column widths)
    	<RD><TA>
    	<RO><CE>row 1 column 1<CE>row 1 column 2<CE>row 1 column 3<CE>row 1 column 4
    	<RO><CE>row 2 column 1<CE>row 2 column 2<CE>row 2 column 3<CE>row 2 column 4
    	<RO><CE>row 3 column 1<CE>row 3 column 2<CE>row 3 column 3<CE>row 3 column 4
    	<RO><CE>row 4 column 1<CE>row 4 column 2<CE>row 4 column 3<CE>row 4 column 4
    	</TA>
    	<RD>Same table as the first example, with borders around the table and between cells.
    	<RD><TA:VG:0.075;HG:0.075;BR:AL:0.022,0.03,FC:0,0,128,HZ:0.015,0.03,FC:0,128,0,VT:0.015,0.03,FC:0,128,0>
    	<RO><CE>row 1 column 1<CE>row 1 column 2<CE>row 1 column 3<CE>row 1 column 4
    	<RO><CE>row 2 column 1<CE>row 2 column 2<CE>row 2 column 3<CE>row 2 column 4
    	<RO><CE>row 3 column 1<CE>row 3 column 2<CE>row 3 column 3<CE>row 3 column 4
    	<RO><CE>row 4 column 1<CE>row 4 column 2<CE>row 4 column 3<CE>row 4 column 4
    	</TA>
    	<RD>Same table as the second example, with fixed column widths, a different colored cell border, and paragraph formatting codes in a set of cells.
    	<RD><TA:4,1,1.25,1.25,1; VG:0.075; BR:AL:0.022,0.03,FC:0,0,128,HZ:0.015,0.03,FC:0,128,0,VT:0.015,0.03,FC:0,128,0>
    	<RO><CE>row 1 column 1<CE: BR:LF:0.021,0.03,FC:255,0,0,TP:0.021,0.03,FC:0,0,128,RT:0.021,0.03,FC:255,0,0,BT:0.021,0.03,FC:255,0,0>row 1 column 2<CE>row 1 column 3<CE>row 1 column 4
    	<RO><CE><JU:CN><BR:AL:0.021,0.03,FC:255,0,255><SD:192,192,192>row 2 column 1<CE><JU:CN><BR:AL:0.021,0.03,FC:255,0,255><SD:192,192,192>row 2 column 2<CE>row 2 column 3<CE>row 2 column 4
    	<RO><CE>row 3 column 1<CE>row 3 column 2<CE>row 3 column 3<CE>row 3 column 4
    	<RO><CE>row 4 column 1<CE>row 4 column 2<CE>row 4 column 3<CE>row 4 column 4
    	</TA>
    	*/


    }

    /**
     * Tries to parse one of: MD, MR, HI, and HC
     *
     * @param startIndex
     * @param opts
     * @param t
     * @return
     * @throws InvalidMarkupException
     */
    public static int tryParseOtherCellAttr(int startIndex, List<String> opts, SlxToken t) throws InvalidMarkupException {
        String name = opts.get(startIndex);
        if (name.equalsIgnoreCase("HC")) {
            t.set("columnIsHeader", "true"); //TODO: probably still needs to be transformed to a <th> tag...
            return startIndex + 1;
        }
    	
    	/*
    	MD:Integer � Number of cells to merge down.
    	MR:Integer � Number of cells to merge right.
    	HC � Marks the cell as a header cell. If this code is used in any cell, all cells in the column become header cells. (For consistency and readability, this code should be used in the first cell in a column; however, this is not required.)
    	*/
        if (TokenUtils.fastMatches("MD|MR", name)) {
            assert (opts.size() > startIndex + 1); //1 argument required for this option
            String value = opts.get(startIndex + 1);

            //MD, MR integer
            try {
                int val = Integer.parseInt(value);
                if (name.equalsIgnoreCase("MD"))
                    t.set("rowspan", Integer.toString(val + 1)); //We have to add one, since folio doesn't count the current cell
                else
                    t.set("colspan", Integer.toString(val + 1)); //We have to add one, since folio doesn't count the current cell
            } catch (NumberFormatException e) {
                throw new InvalidMarkupException("Failed to parse number " + value + " ... CE attribute " + name);
            }

            return startIndex + 2;
        }

        return startIndex;
    }


    /**
     * Joins the strings together with commas.
     *
     * @param data
     * @return
     */
    public static String join(List<String> data) {
        StringWriter sw = new StringWriter(data.size() * 10);
        for (int i = 0; i < data.size(); i++) {
            sw.append(data.get(i));
            if (i < data.size() - 1) sw.append(',');
        }
        return sw.toString();
    }

    /**
     * Alphanumeric, or quoted string. Quoted string can contain paired quotes
     */
    // private static String rStr = "[A-Za-z0-9]|\"(?:[^\"]*(?:\"\")?)*\"";


    //<FE:"Name",Type,"Field Format","IX:Index Options,Character Based Formatting Codes>
    //<TT:"Title Text"> Infobase Title
    //<TP:"Folio Object Name"> Title page (Splash screen, bitmap|metafile). Must be placed after referenced object is defined
    //<SU><CR></SU>
    //<RM><CR></RM>
    //<AU><CR></AU>
    //<AS><CR></AS>
    //<RE:"Date Text">
    //
    //<ST:Name,Type,Character based formatting codes> Character styles and link styles.
    //RP option - replace definition


    //<RC:Merge Type, Merge Apption>
    //Merge Type always == AP (Append Merge)
    //<QT:"TemplateName","File Name">
    //Query Template
    //<PR:"Level Name"> The default search partition. One of the levels. Default is record. Results are matching records. If "Book", results would be matching books.
    //<PD:"Pen Name", Character Based Formatting Codes> Highlighter pen
    //<PA:Name,Paragraph formatting, Character formatting> paragraph styles
    //<OD:FO:"Object name", Object Handler,"File Name", RP, File Type>
    //<LN:Name1,"Name2, comma",Name3,Name4> Level definitions. Order of <LE> codes is used instead if this is omitted. Not required for 4.x
    //<LE:LevelName,Paragraph attrs, Char attrs> Level definition and style.
    //<HL:Hit List Options>
    //<DQ:"Query"> max 2000 chars. The default query
    //<DF:FT:"Font Name;PT:Size"> The default font for input text boxes.


    /**
     * For single, zero-option tags. Can have SLX options. The output tag inherits the closing slash from the input tag. This allows concise translation for both.
     *
     * @param t
     * @param folioName
     * @param slxToken
     * @return
     * @throws InvalidMarkupException
     */
    public static SlxToken tryRename(FolioToken t, String folioName, String slxToken) throws InvalidMarkupException {
        if (t.matches(folioName) && t.assertCount(0)) {
            SlxToken st = new SlxToken(slxToken);
            if (t.isClosing()) st.tagType = SlxToken.TagType.Closing;
            else st.tagType = SlxToken.TagType.Opening;
            st.updateMarkup();
            return st;
        }
        return null;
    }

    /**
     * Comments out the tag if it is a match.
     *
     * @param ft
     * @param regex
     * @return
     * @throws InvalidMarkupException
     */
    public static SlxToken tryCommentOut(FolioToken ft, String regex) throws InvalidMarkupException {
        //TODO: we need to mark automatic comment-outs so they can automatically be re-inserted when going back to FFF
        if (ft.matches(regex))
            return new SlxToken(SlxToken.TokenType.Comment, "<!--" + ft.text.replaceAll("--", "-&#x002D") + "-->"); //Added -- encoding, since that is a banned combination within XML comments.
        return null;
    }

    /**
     * Searches the specified collection for the specified string, case-insensitive. Returns -1 if not found.
     *
     * @param collection
     * @param value
     * @return
     */
    protected static int getIndexOfCaseInsensitive(List<String> collection, String value) {
        for (int i = 0; i < collection.size(); i++) {
            String v = collection.get(i);
            if (value == v) {
                return i;
            }
            if (v != null && v.equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }


}