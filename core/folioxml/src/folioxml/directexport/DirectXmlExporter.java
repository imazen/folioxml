package folioxml.directexport;

import folioxml.core.InvalidMarkupException;
import folioxml.css.StylesheetBuilder;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.xml.SlxToXmlTransformer;
import folioxml.xml.XmlRecord;

import java.io.*;

public class DirectXmlExporter {
	
	protected SlxRecordReader reader;
	protected OutputStreamWriter out;
	protected String encoding;
	
	public DirectXmlExporter(SlxRecordReader reader, OutputStreamWriter out){
		this.reader = reader;
		this.out = out;
	}
	
	public DirectXmlExporter(SlxRecordReader reader, String outputFile) throws UnsupportedEncodingException, FileNotFoundException{
		this.reader = reader;
		//Make the dir if it is missing
		if (!new java.io.File(outputFile).getParentFile().exists()) new java.io.File(outputFile).getParentFile().mkdirs();
		
		encoding = "utf-8";
		this.out  = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8");
	}
	public void close() throws IOException{
		if (!bottomWritten) writeBottom();
		if (out != null) out.close();
	}
	
	public void processAll() throws IOException, InvalidMarkupException{
		while(true){
			SlxRecord r = reader.read();
		    if (r == null) break;//loop exit
		    processRecord(r);
		}
		
	}
	

	
	protected String css = null;
	protected  String levels = null;
	protected SlxRecord root = null;
	public boolean indentXml = true;
	
	public int maxRecords = -1;
	
	public int recordsProcessed = 0;
	public void processRecord(SlxRecord r) throws InvalidMarkupException, IOException{
		boolean isRoot = "root".equals(r.getLevelType());
		if (isRoot){
			css = new StylesheetBuilder(r).getCss();
			levels = r.get("levels") != null ? r.get("levels") : r.get("levelDefOrder");
			root = r;
		}
		if (!topWritten) writeTop();
		if (maxRecords > -1 && recordsProcessed >= maxRecords) return;
		recordsProcessed++;
		writeRecord(r);
		
	}
	
	public void writeRecord(SlxRecord r) throws InvalidMarkupException, IOException{
   
		
		XmlRecord rx= new SlxToXmlTransformer().convert(r);
		out.write(rx.toXmlString(indentXml));

	}
	
	protected boolean topWritten = false;
	protected boolean bottomWritten = false;
	public void writeTop() throws IOException, InvalidMarkupException{
		topWritten = true;
		out.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
		out.append("<infobase>\n");
	}
	public void writeBottom() throws IOException{
		bottomWritten = true;
		out.write("\n</infobase>");
	}
	
	

	 

}
