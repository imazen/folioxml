package folioxml.xml;

import folioxml.core.InvalidMarkupException;

// Matches elements, but not text, comments, or entities.
public class ElementFilter implements  IFilter {
    @Override
    public boolean matches(Node n) throws InvalidMarkupException {
        return n.isTag();
    }
}
