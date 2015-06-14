package folioxml.xml;

import org.apache.commons.lang.NotImplementedException;
import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;
import org.yaml.snakeyaml.util.ArrayStack;

import java.io.IOException;
import java.util.*;

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

    public boolean isRootRecord() throws InvalidMarkupException{
        return (this.getLevelType() != null && this.getLevelType().equalsIgnoreCase("root"));
    }

    /**
     * Returns true if there is a value for the 'level' attribute
     * @return
     */
    public boolean isLevelRecord() throws InvalidMarkupException{
        return getLevelType() != null;
    }
    /**
     * returns this.get("level"). Returns null if the string is empty.
     * @return
     */
    public String getLevelType() throws InvalidMarkupException{
        String s= this.get("level"); if (s == null || s.length() == 0) return null;
        return s;
    }

    public XmlRecord getRoot(){
        if (parent == null) return this;
        else return parent.getRoot();
    }

    public Deque<XmlRecord> getAncestors(boolean includeSelf){
        Deque<XmlRecord> parents = new ArrayDeque<XmlRecord>();

        XmlRecord current = this;
        if (includeSelf) parents.add(this);
        while (current.parent != null){
            parents.addLast(current.parent);
            current = current.parent;
        }
        return parents;
    }

    public XmlRecord getCommonAncestor(XmlRecord other, boolean includeSelves){
        Deque<XmlRecord> parents = this.getAncestors(includeSelves);
        Deque<XmlRecord> otherParents = other.getAncestors(includeSelves);
        XmlRecord common = null;
        while (!parents.isEmpty() && !otherParents.isEmpty()){
            XmlRecord c = parents.removeLast();
            if (c == otherParents.removeLast()) common = c;
            else break;
        }
        return common;
    }

}
