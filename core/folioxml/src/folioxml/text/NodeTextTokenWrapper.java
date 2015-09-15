package folioxml.text;

import folioxml.core.TokenBase;
import folioxml.xml.Node;

/**
 * Created by nathanael on 6/18/15.
 */
public class NodeTextTokenWrapper implements ITextToken {
    private Node n;

    public NodeTextTokenWrapper(Node n) {
        this.n = n;
    }

    public String getText() {
        return n.markup;
    }

    public void setText(String s) {
        n.markup = s;
        n.type = TokenBase.TokenType.Text;
        if (s.length() == 0) {
            //Delete
            n.remove(true); //Delete token from parent.
        }
    }
}