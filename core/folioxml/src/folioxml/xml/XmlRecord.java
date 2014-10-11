package folioxml.xml;

import org.apache.commons.lang.NotImplementedException;
import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;

import java.io.IOException;

public class XmlRecord extends Node {
	
	public XmlRecord(String xml) throws IOException, InvalidMarkupException{
		super(xml);
	}
	protected XmlRecord(SlxRecord r, boolean copyChildren){
		if (copyChildren) throw new NotImplementedException();
		r.copyTo(this, true);
		
		//Then deal with parent reference
		if (r.parent != null) parent = r.parent.slxXmlRecordTag; //The tag is applied by SlxToXmlTransformer to each record, so future records can translate their ancestry
	}

	public XmlRecord parent = null;
	
	
	
}
