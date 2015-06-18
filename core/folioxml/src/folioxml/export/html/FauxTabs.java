package folioxml.export.html;

import com.sun.tools.corba.se.idl.constExpr.ShiftRight;
import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.core.TokenUtils;
import folioxml.css.CssUtils;
import folioxml.css.StylesheetBuilder;
import folioxml.export.NodeListProcessor;
import folioxml.text.ITextToken;
import folioxml.text.TextLinesBuilder;
import folioxml.text.TextLinesSequencer;
import folioxml.text.VirtualCharSequence;
import folioxml.translation.FolioCssUtils;
import folioxml.xml.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FauxTabs implements NodeListProcessor {

    public FauxTabs(int minLineWidth, int maxLineWidth){
        this.minWidthChars = minLineWidth;
        this.maxWidthChars = maxLineWidth;
    }

    //We are going to assume left-aligned text, since Folio Views doesn't seem to support other text alignments.
    //We are going to assume fixed-width font.

    public class TabStop{
        public String leaderPattern;
        public TabStopPosition position;
        public double inches;
        public TabStopJustify justification;
    }

    public enum TabStopPosition{
        CustomFromLeft,
        Center,
        Right
    }
    public enum TabStopJustify {
        Left,
        Center,
        Right,
        Decimal
    }


    public class FixedTabStop{
        public int location;
        public TabStopJustify just;
        public String leader;
    }


    private int minWidthChars;
    private int maxWidthChars;


    private String getLeaderOfLength(FixedTabStop ts, int lengthRequired){
        int patternRepetitions = (int)Math.ceil((double)lengthRequired / (double)ts.leader.length());
        StringBuilder leader = new StringBuilder(patternRepetitions * ts.leader.length());
        for (int i = 0; i < patternRepetitions; i++) leader.append(ts.leader);
        return leader.substring(0,lengthRequired);
    }

    private void FakeTabsInLine(VirtualCharSequence line, List<FixedTabStop> tabStops, int paragraphWidth, int defaultTabSize){
        int tabAt = line.indexOf("\t",0);
        if (tabAt  < 0) return;

        int nextTab = line.indexOf("\t",tabAt + 1);
        boolean moreTabs = true;
        if (nextTab < 0) {
            nextTab = line.length();
            moreTabs = false;
        }

        //Find first tab stop with a location > tabAt.
        FixedTabStop next = null;
        for(FixedTabStop ts: tabStops){
            if (ts.location > tabAt){
                next = ts;
                break;
            }
        }
        if (next == null){
            //Default tab stops
            next = new FixedTabStop();
            int lastTabSet = tabStops.size() > 0 ? tabStops.get(tabStops.size() - 1).location : 0;

            int offset = (int)Math.ceil((double)(tabAt + 1 - lastTabSet) / (double)defaultTabSize) * defaultTabSize;

            next.location = lastTabSet + offset;
            next.leader = TokenUtils.entityDecodeString(" &#160;");
            next.just = TabStopJustify.Left;
        }

        int offset = 0;
        int textLength = (nextTab - tabAt - 1);
        if (next.just == TabStopJustify.Center)
            offset = textLength / -2;
        if (next.just == TabStopJustify.Right)
            offset = -textLength;
        if (next.just == TabStopJustify.Decimal){
            int decimalAt = line.indexOf(".", tabAt + 1);
            if (decimalAt < nextTab && decimalAt > -1){
                offset = decimalAt - nextTab - 1;
            }
        }

        int leaderLength = Math.max(0, (next.location - tabAt) + offset);
        String leader = getLeaderOfLength(next, leaderLength);
        line.replace(tabAt,1,leader);

        //Recurse until all tabs are gone.
        if (moreTabs){
            FakeTabsInLine(line,tabStops,paragraphWidth,defaultTabSize);
        }
    }

    private void FakeTabs(Node paragraph, IFilter exclude) throws InvalidMarkupException {

        ParagraphInfo pi = getParagraphStyle(paragraph);
        if (pi == null) pi = new ParagraphInfo();
        Double charWidth = pi.getCharWidthInches();

        int defaultTabSize = (int)Math.ceil(0.5 / charWidth);



        //Abstract tree as a flat set of tokens to edit, grouped by line.
        List<VirtualCharSequence> lines = new TextLinesSequencer(new NodeList(paragraph),exclude).getLines(true);

        //Determine our fixed width
        int paragraphWidth = minWidthChars;
        for(VirtualCharSequence line: lines){
            paragraphWidth = Math.max(minWidthChars, Math.min(maxWidthChars, line.length()));
        }

        //Get tab stops
        List<TabStop> tabs = pi.stops;

        //Assign inches to center/right tabs
        List<FixedTabStop> fixedTabs = fixTabStops(tabs, charWidth,paragraphWidth);

        //Replace tabs with leaders
        for(VirtualCharSequence line: lines){
            FakeTabsInLine(line,fixedTabs,paragraphWidth,defaultTabSize);
        }

    }

    private List<FixedTabStop> fixTabStops(List<TabStop> tabStops, double charWidthInches, int maxChars){
        if (tabStops == null) return new ArrayList<FixedTabStop>();
        List<FixedTabStop> results = new ArrayList<FixedTabStop>(tabStops.size());
        for(TabStop ts: tabStops){
            FixedTabStop fts = new FixedTabStop();
            fts.leader = ts.leaderPattern;
            fts.just = ts.justification;
            fts.location = (int)Math.ceil(ts.inches / charWidthInches);
            if (ts.position == TabStopPosition.Center)
                fts.location = maxChars / 2;
            if (ts.position == TabStopPosition.Right)
                fts.location = maxChars;
            results.add(fts);
        }
        return results;
    }


    private XmlRecord getInfobaseRootFor(Node p) throws InvalidMarkupException {
        Node recordRoot = p.rootNode();
        if (recordRoot instanceof  XmlRecord){
            return ((XmlRecord)recordRoot).getRoot();
        }else{
            throw new InvalidMarkupException("The root Node is not an XmlRecord!");
        }
    }



    private Map<String,ParagraphInfo> getInfobaseStyles(XmlRecord root) throws InvalidMarkupException {

        Map<String,ParagraphInfo> results = new HashMap<String,ParagraphInfo>();
        NodeList styleDefs = root.children.filterByTagName("style-def",true);
        for (Node t:styleDefs.list()){
            String cls = t.get("class");
            String style = t.get("style");
            String type = t.get("type");
            if (cls != null && style != null && "paragraph".equalsIgnoreCase(type)) {
                results.put(cls, infoFromStyle(style));
            }
        }
        return results;
    }

    private Map<XmlRecord, Map<String,ParagraphInfo>> infobaseStyleCache = new HashMap<XmlRecord, Map<String,ParagraphInfo>>();

    private Map<String,ParagraphInfo> getInfobaseStylesCached(XmlRecord root) throws InvalidMarkupException {
        if (infobaseStyleCache.containsKey(root)){
            return infobaseStyleCache.get(root);
        }else{
            Map<String,ParagraphInfo> data = getInfobaseStyles(root);
            infobaseStyleCache.put(root,data);
            return data;
        }
    }
    private ParagraphInfo getParagraphStyle(Node p) throws InvalidMarkupException {

        ParagraphInfo pi = infoFromStyle(p.get("style"));

        String[] classes = p.get("class") == null ? null : p.get("class").split("\\s+");
        if (classes == null || classes.length == 0) return pi;

        Map<String,ParagraphInfo> definedStyles = getInfobaseStylesCached(getInfobaseRootFor(p));

        for(String cls:classes){
            if (definedStyles.containsKey(cls)){
                pi = cascadeInfo(pi, definedStyles.get(cls));
            }
        }

        return pi;
    }



    public class ParagraphInfo{
        public List<TabStop> stops;
        public String fontSize;
        public String fontFamilies;
        public Double cssWidth;
        public void copyFrom(ParagraphInfo second){
            if (second == null) return;
            if (second.fontFamilies != null) this.fontFamilies = second.fontFamilies;
            if (second.fontSize != null) this.fontSize = second.fontSize;
            if (second.cssWidth != null) this.cssWidth = second.cssWidth;
            if (second.stops != null && second.stops.size() > 0) this.stops = second.stops; //SHALLOW COPY of tab stops

        }

        public Double getCharWidthInches() {
            double fontInches = FolioCssUtils.toInches(StylesheetBuilder.DEFAULT_FONT_SIZE);
            if (fontSize != null) fontInches = FolioCssUtils.toInches(fontSize,fontInches);

            //We need to expand the tab positions to help account for the growth from variable-width to fixed-width font.
            //Thus the 0.8
            return fontSizeToCharWidthRatio() * fontInches * 0.8;
        }
    }

    private Pattern tabStopSpec = Pattern.compile("\\A(Center|Right|[\\d.]+) (NM|CN|RT|CA) (NO|DS|DO|DA|UN)\\Z", Pattern.CASE_INSENSITIVE);

    private TabStop parseTabStop(String folioSpec) throws InvalidMarkupException {

        Matcher m = tabStopSpec.matcher(folioSpec);
        if (!m.find()) throw new InvalidMarkupException("Invalid tab stop specification: " + folioSpec);

        TabStop ts = new TabStop();

        if ("Center".equalsIgnoreCase(m.group(1)))
            ts.position = TabStopPosition.Center;
        else if ("Right".equalsIgnoreCase(m.group(1)))
            ts.position = TabStopPosition.Right;
        else{
            ts.position = TabStopPosition.CustomFromLeft;
            ts.inches = Double.parseDouble(m.group(1));
        }


        if ("NM".equalsIgnoreCase(m.group(2)))
            ts.justification = TabStopJustify.Left;
        if ("CN".equalsIgnoreCase(m.group(2)))
            ts.justification = TabStopJustify.Center;
        if ("RT".equalsIgnoreCase(m.group(2)))
            ts.justification = TabStopJustify.Right;
        if ("CA".equalsIgnoreCase(m.group(2)))
            ts.justification = TabStopJustify.Decimal;


        if ("NO".equalsIgnoreCase(m.group(3)))
            ts.leaderPattern = TokenUtils.entityDecodeString(" &#160;");
        if ("DS".equalsIgnoreCase(m.group(3)))
            ts.leaderPattern = TokenUtils.entityDecodeString(" .");
        if ("DO".equalsIgnoreCase(m.group(3)))
            ts.leaderPattern = TokenUtils.entityDecodeString(".");
        if ("DA".equalsIgnoreCase(m.group(3)))
            ts.leaderPattern = TokenUtils.entityDecodeString(" -");
        if ("UN".equalsIgnoreCase(m.group(3)))
            ts.leaderPattern = TokenUtils.entityDecodeString("_");

        return ts;
    }

    /*
    var id = 0;

var sum = 0;
var fontname = "Courier New";

//This finds the ratio between inches (in font size) and character width of 'x' for a given font.
for (var i = 0.09; i < 1; i += 0.001){
    document.write("<span style='font-family: " + fontname + ";font-size:" + i + "in' id='x" + id + "'>x</span>");
    var width = document.getElementById("x" + id).offsetWidth
    document.write(i.toString() + " -> " + width +  "   (" + (width / i) + ")");
    sum +=  (width / i);
    document.write("<br>");
    id++;
}

document.write("<strong>Avg: " + (sum / id) + "px, " + (sum / id / 96) + "in</strong>");
     */

    private double fontSizeToCharWidthRatio(){
        //See the javascript above for calculating this magic value for any font
        //This values was CourierNew on Chrome
        return 0.6138741700947206;
    }

    private ParagraphInfo infoFromStyle(String style) throws InvalidMarkupException {
        if (style == null) return null;
        ParagraphInfo pi = new ParagraphInfo();

        List<TabStop> stops = new ArrayList<TabStop>();

        List<Pair<String,String>> css = CssUtils.parseCssAsList(style, false);

        for(Pair<String,String> p: css){
            if (p.getFirst().equals("width")){
                pi.cssWidth = FolioCssUtils.toInches(p.getSecond(), null);
            }
            if (p.getFirst().equals("font-size")){
                pi.fontSize = p.getSecond();
            }
            if (p.getFirst().equals("font-family")){
                pi.fontFamilies = p.getSecond();
            }
            if (p.getFirst().equals("-folio-tab-set")){
                stops.add(parseTabStop(p.getSecond()));
            }
        }
        pi.stops = stops;

        return pi;
    }

    private ParagraphInfo cascadeInfo(ParagraphInfo first, ParagraphInfo second) {
        ParagraphInfo pi = new ParagraphInfo();
        pi.copyFrom(first);
        pi.copyFrom(second);
        return pi;
    }

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {

        //We need to exclude any popups from being adjusted.
        IFilter exclusions = new NodeFilter("popup|table|td|th|note|div|record");

        //For indentation-only use, we could apply a leader of " &nbsp; " (decoded) and leave the class name unchanged.
        NodeList paragraphs = nodes.searchOuter(new NodeFilter("p"));
        for (Node p: paragraphs.list()) {
            List<StringBuilder> lines = new TextLinesBuilder().generateLines(new NodeList(p));
            //Analyze tab usage
            TextLinesBuilder.TabUsage tabs = new TextLinesBuilder().analyzeTabUsage(lines);
            if (tabs == TextLinesBuilder.TabUsage.Indentation){
                FakeTabs(p,exclusions);
                p.addClass("faux_tabs_indentation");
            }else if (tabs == TextLinesBuilder.TabUsage.Tabulation){
                FakeTabs(p,exclusions);
                p.addClass("faux_tabulation");
            }

            if (tabs == TextLinesBuilder.TabUsage.Tabulation){
                //Do before/after per line
                List<StringBuilder> newLines = new TextLinesBuilder().generateLines(new NodeList(p));
                for(int i =0; i < lines.size() && i < newLines.size(); i++){
                    //System.out.print("O:");
                    //System.out.println(lines.get(i));
                    ///System.out.print("N:");
                    System.out.println(newLines.get(i));
                }

            }
        }

        return nodes;
    }
}
