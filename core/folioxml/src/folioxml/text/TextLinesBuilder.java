package folioxml.text;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nathanael on 6/18/15.
 */
public class TextLinesBuilder {

    private Pattern tabsAtTheStart = Pattern.compile("\\A\\s*?\\t+");

    private Pattern tabsInTheMiddle = Pattern.compile("\\A\\s*[^\\t\\r\\n]+\\t+");



    public TextLinesBuilder(){}


    public List<StringBuilder> generateLines(NodeList nl){
        List<StringBuilder> lines = new ArrayList<StringBuilder>();
        for(Node n: nl.list()){
            line_new(lines);
            add_lines(n, lines);
        }
        return lines;
    }

    public TabUsage analyzeTabUsage(List<StringBuilder> lines) throws InvalidMarkupException {
        TabUsage result = TabUsage.None;
        for(StringBuilder l:lines){
            Matcher m = tabsInTheMiddle.matcher(l);
            if (m.find()) return TabUsage.Tabulation; //Tabulation is a quick exit, no escalation from there
            m = tabsAtTheStart.matcher(l);
            if (m.find()){
                result = TabUsage.Indentation; //Escalate to indentation.
            }else if (l.indexOf("\t") > -1) {
                throw new InvalidMarkupException("Tab analysis missed a code path.");

            }
        }
        return result;
    }
    public TabUsage analyzeTabUsage(NodeList nl) throws InvalidMarkupException {
        List<StringBuilder> lines = generateLines(nl);
        return analyzeTabUsage(lines);
    }

    public enum TabUsage{
        Indentation,
        Tabulation,
        None
    }



    private void add_lines(Node n, List<StringBuilder> lines){
        if (n.matches("p|br|table|td|th|note|div|record")) line_new(lines);

        if (n.isTag() && n.children != null){
            for(Node c:n.children.list()){
                add_lines(c,lines);
            }
        }else if (n.isTextOrEntity()){
            String s = n.markup;
            if (n.isEntity()) s = TokenUtils.entityDecodeString(s);
            last_line(lines).append(s);
        }

        if (n.matches("p|br|table|td|th|note|div|record")) line_new(lines);

    }

    private StringBuilder last_line(List<StringBuilder> lines){
        if (lines.size() == 0){
            lines.add(new StringBuilder());
        }
        return lines.get(lines.size() - 1);
    }

    private void line_new(List<StringBuilder> lines){
        if (lines.size() == 0 || lines.get(lines.size() - 1).length() > 0){
            lines.add(new StringBuilder());
        }
    }
}
