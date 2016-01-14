package folioxml.xml;

import folioxml.core.InvalidMarkupException;

public interface IFilter {
    public boolean matches(Node n) throws InvalidMarkupException;
}
