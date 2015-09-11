package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.Not;

import java.util.EnumSet;

/**
 * Pulls pagebraks, se, pp, program links, recordHeading spans
 * Removes groups attr from record.
 * Removes the type attribute from all elements (except object-def).
 * Renames record -> div.
 * 
 * @author nathanael
 *
 */
public class CleanupSlxStuff implements NodeListProcessor {

    private EnumSet<CleanupOptions> opts;

    public CleanupSlxStuff(EnumSet<CleanupOptions> options){
        this.opts = options;
    }

    public CleanupSlxStuff(){
        this.opts = EnumSet.of(CleanupOptions.PullProgramLinks,CleanupOptions.PullMenuLinks, CleanupOptions.DropGroupsAttr, CleanupOptions.DropTypeAttr, CleanupOptions.RenameRecordToDiv);
    }


    public enum CleanupOptions{
        PullProgramLinks,
        PullMenuLinks,
        DropTypeAttr,
        DropGroupsAttr,
        RenameRecordToDiv,
        RenameLinkToA,
        RenameBookmarks
    }
	
	
	public NodeList process(NodeList nodes) throws InvalidMarkupException {
		
		//Remove pagebreaks, proximity markers
		nodes.search(new NodeFilter("pagebreak|se|pp")).pull(); 
		
		//Remove program links
		if (opts.contains(CleanupOptions.PullProgramLinks)) nodes.search(new NodeFilter("link|a","program",null)).pull();
        if (opts.contains(CleanupOptions.PullMenuLinks)) nodes.search(new NodeFilter("link|a","menu",null)).pull();


        //Remove headings
		nodes.search(new NodeFilter("span","type","recordHeading")).pull();
		
		//Remove groups
        if (opts.contains(CleanupOptions.DropGroupsAttr)) nodes.search(new NodeFilter("record")).removeAttr("groups");
		
		//Removes the type attribute from everything... 
		//nov-30-09 NDJ: Except object-defs!!! Huge hard bug... stopped object refs from resolving. ?What about style-defs? Other stuff that relies on the 
		//type attribute after a record is procesed.
        if (opts.contains(CleanupOptions.DropTypeAttr)) nodes.search(new NodeFilter("type",null), new Not(new NodeFilter("object-def"))).removeAttr("type");
		
		//Rename the record
        if (opts.contains(CleanupOptions.RenameRecordToDiv)) nodes.searchOuter(new NodeFilter("record")).setTagName("div"); //convert records to divs.

        if (opts.contains(CleanupOptions.RenameLinkToA)) nodes.filterByTagName("link", true).setTagName("a");

        if (opts.contains(CleanupOptions.RenameBookmarks)) nodes.filterByTagName("bookmark", true).setTagName("a").removeAttr("name");



        return nodes;
	}
	
}
