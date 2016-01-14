package folioxml.xml;

import folioxml.core.InvalidMarkupException;

public class Or implements IFilter {

    private IFilter[] filters = null;

    /**
     * Allows an array of filters, or a list
     *
     * @param filters
     */
    public Or(Object... filters) {
        if (filters.length > 0) {
            if (filters[0].getClass().isArray()) {
                filters = (Object[]) filters[0];
            }
        }
        this.filters = new IFilter[filters.length];
        for (int i = 0; i < filters.length; i++) this.filters[i] = (IFilter) filters[i];
    }

    public Or(IFilter[] filters) {
        this.filters = filters;
    }

    public boolean matches(Node n) throws InvalidMarkupException {
        boolean matches = false;
        for (IFilter nf : filters) if (nf.matches(n)) matches = true;
        return matches;
    }

}
