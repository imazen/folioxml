package folioxml.text;

import folioxml.core.InvalidMarkupException;
import folioxml.xml.IFilter;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.util.ArrayList;
import java.util.List;

//Only comprehends <br> tags, works if phrasingContentOnly()==true
public class TextLinesSequencer {

    public TextLinesSequencer(NodeList nl, IFilter exclusionFilter) throws InvalidMarkupException {

        NodeList linebreaks = nl.search("br");
        //TODO: throw exception if there is a block level element present after we exclude it.

        linesOfTOkens = new ArrayList<List<ITextToken>>(linebreaks.count());

        Node prev = null;
        for(Node b:linebreaks.list()){
            NodeList tokens = nl.textEntityNodesBetween(prev,b,exclusionFilter);
            addLineFromTextNodeList(tokens);
            prev = b;
        }
        NodeList tokens = nl.textEntityNodesBetween(prev,null,exclusionFilter);
        addLineFromTextNodeList(tokens);
    }

    private void addLineFromTextNodeList(NodeList tokens){
        ArrayList<ITextToken> wrapped = new ArrayList<ITextToken>(tokens.count());
        for(Node text: tokens.list()){
            wrapped.add(new NodeTextTokenWrapper(text));
        }
        linesOfTOkens.add(wrapped);
    }

    public List<List<ITextToken>> linesOfTOkens;


    public List<VirtualCharSequence> getLines(boolean decodeEntities){
        List<VirtualCharSequence> lines = new ArrayList<VirtualCharSequence>(linesOfTOkens.size());
        for(List<ITextToken> line: linesOfTOkens){
            lines.add(new VirtualCharSequence(line,decodeEntities));
        }
        return lines;
    }
}
