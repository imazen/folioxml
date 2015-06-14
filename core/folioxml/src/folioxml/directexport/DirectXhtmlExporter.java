package folioxml.directexport;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.export.deprecated.BookmarksAndJumpLinks;
import folioxml.export.deprecated.RecordAnchorWriter;
import folioxml.export.html.*;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.xml.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class DirectXhtmlExporter extends DirectXmlExporter {
	
	public DirectXhtmlExporter(SlxRecordReader reader, OutputStreamWriter out) {
		super(reader, out);
	}
	
	public DirectXhtmlExporter(SlxRecordReader reader, String file) throws UnsupportedEncodingException, FileNotFoundException {
		super(reader, file);
	}
	
	/*
	 * Set 'queryLinkResolver' to a ResolveQueryLinks instance to allow translation of query links.
	 */
	public NodeListProcessor queryLinkResolver = null;


	@Override 
	public void writeRecord(SlxRecord r) throws IOException, InvalidMarkupException{
   
		boolean isRoot = "root".equals(r.getLevelType());
		if (isRoot) return; //Don't write the root record in XHTMl
		
		XmlRecord rx= new SlxToXmlTransformer().convert(r);
	
		NodeList nodes =  MultiRunner.process(new NodeList(rx),
				new RecordAnchorWriter(),
				new BookmarksAndJumpLinks(), //Drop x-infobase links, fix jump links and destinations
				new Images(), //Convert eligible object tags into img tags
				new Notes(),  //Adds js notes
				new Popups(), //Adds js popups
				new FixImagePaths(""), //Switch to forward slashes
				new SplitSelfClosingTags(),
				new CleanupSlxStuff()); //Removes pagebreak|ss|pp, program links, span.recordHeading, record.groups, and renames group to div.
		
		if (queryLinkResolver != null)
			nodes = queryLinkResolver.process(nodes);
		
		//What links should we keep? - object links, popup links, jump links... some others we should leave just so we know we need to fix them.
		//Rename all link tags to a. (popup links)
		nodes.filterByTagName("link", true).setTagName("a"); 
		
		
		out.write(nodes.toXmlString(true));

	}


	
	@Override
	public void writeTop() throws IOException, InvalidMarkupException{
		topWritten = true;
		out.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
		out.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		out.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");

		out.append("\t<head>\n");
		out.append("\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + encoding + "\" />\n");
		
		
		
		//Parse root xml
		Node n = new SlxToXmlTransformer().convert(root);
		
		/*<infobase-meta type="author">
			<p>Nathanael Jones</p>
		</infobase-meta>*/
		String author = new NodeList(n).search(new NodeFilter("infobase-meta","type","author")).getTextContents().trim();
		
		/*<infobase-meta content=" Imazen   ---  SHADOW File --- ( NFO created: 2005-0805 )  104,300 " type="title" />*/
		String title = new NodeList(n).search(new NodeFilter("infobase-meta","type","title")).first().get("content").trim();
		//TODO: null ref if title is missing
		
		//<infobase-meta content="8/17/2009 5:10:09 PM" type="revision-date" />
		String revised = new NodeList(n).search(new NodeFilter("infobase-meta","type","revision-date")).first().get("content").trim();
		
		/*<infobase-meta type="subject">
			<p> subject for metadata </p>
		</infobase-meta>
		*/
		String subject = new NodeList(n).search(new NodeFilter("infobase-meta","type","subject")).getTextContents().trim();
		
		/*<infobase-meta type="abstract">
			<p>
				description for metadata.
			</p>
		</infobase-meta>*/
		String abstr = new NodeList(n).search(new NodeFilter("infobase-meta","type","abstract")).getTextContents().trim();
		/*<infobase-meta type="remark">
			<p>
				remark for metadata
			</p>
		</infobase-meta>*/
		String remark = new NodeList(n).search(new NodeFilter("infobase-meta","type","remark")).getTextContents().trim();
		
		//Parse title override
		//title = overrideTitle(title,settings);
		
		//Parse author override
		//author = (settings.get("author") != null) ? settings.get("author") : author;
		
		//Write title tag.
		out.append("\t\t<title>" + TokenUtils.lightEntityEncode(title) + "</title>\n");
		
		//Write author
		out.append("\t\t<meta name=\"author\" content=\""+ TokenUtils.attributeEncode(author) + "\" />\n");
		
		
		
		//The folio stylesheet
		boolean dropstylesheet = false;
		if (!dropstylesheet){
			out.append("\n");
			out.append("<style type=\"text/css\">");
			//out.append("\t <!--/*--><![CDATA[/*><!-- */");
			out.append(css);
			//out.append("\t/*]]>*/-->");
			if (indentXml) out.append("\nbody{white-space:normal;}\n");
			out.append("\n</style>");
			out.append("\n\n");
		}
		
		//The custom stylesheet
		//if (settings.get("stylesheet") != null){
		//	out.append("\t\t<link rel=\"stylesheet\" href=\"" + settings.get("stylesheet") + "\" type=\"text/css\" />\n");
		//}
		
		//Start the body
		out.append("\t</head>\n\t<body>\n");
		
		out.append("<div><a name=\"start\" id=\"start\"></a></div>\n");
		
		
		
	}
	
	@Override
	public void writeBottom() throws IOException{
		bottomWritten = true;
		out.append("</body>\n</html>");
	}


	public static String hashPath(String path)  {
		
		//djl 09-22-2010 added in underscore since they can't start with #s or spaces
		return "z" + Integer.toHexString(path.hashCode());
	}
}
