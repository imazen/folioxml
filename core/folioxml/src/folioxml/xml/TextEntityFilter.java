package folioxml.xml;

import folioxml.core.InvalidMarkupException;

/**
 * Created by nathanael on 6/18/15.
 */
public class TextEntityFilter implements  IFilter {

    int minimumLength = 0;

    public TextEntityFilter(int minimumLength){
        this.minimumLength = minimumLength;
    }
    @Override
    public boolean matches(Node n) throws InvalidMarkupException {
        return n.isTextOrEntity() && n.markup.length() >= minimumLength;
    }
}
