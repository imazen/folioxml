package folioxml.xml;

import folioxml.core.InvalidMarkupException;

public class Not implements IFilter{
	public Not(IFilter filter) {
		this.filter = filter;
	}
	private IFilter filter = null;
	
	public boolean matches(Node n) throws InvalidMarkupException{
		return !filter.matches(n);
	}
}
