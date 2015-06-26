package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.*;

import java.io.IOException;


public class ReplaceUnderline implements NodeListProcessor {
    private static final String text_underline = "text-decoration:underline;";

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {


        NodeList stylin = nodes.search(new And(new NodeFilter(null, "style", null), new Not(new AncestorFilter(new NodeFilter("a|link")))));
        for(Node n:stylin.list()){

            String style = n.get("style");
            if (style.indexOf(text_underline, 0) >= 0){
                n.addClass("replace_underline");
                style = style.replace(text_underline, "");
                n.set("style", style);
            }
        }
        return nodes;
    }
}
