package folioxml.xml;

import folioxml.core.InvalidMarkupException;

/**
 * One of the node's ancestors much match this filter's criteria
 * @author nathanael
 *
 */
public class AncestorFilter implements IFilter {
	
	private IFilter[] filters = null;

	/**
	 * Allows an array of filters, or a list
	 * @param filters
	 */
	public AncestorFilter(Object ... filters){
		if (filters.length > 0){
			if (filters[0].getClass().isArray()){
				filters = (Object[]) filters[0];
			}
		}
		this.filters = new IFilter[filters.length];
		for (int i =0; i < filters.length; i++) this.filters[i] = (IFilter) filters[i];
	}
	public AncestorFilter(IFilter[] filters){
		this.filters = filters;
	}
	
	public boolean matches(Node n) throws InvalidMarkupException {
		Node p = n.parent;
		
		while (p != null){
			boolean matches = true;
			for (IFilter nf:filters) if (!nf.matches(p)) matches = false;
			if (matches) return true;
			p = p.parent;
		}
		return false;
	}

}
