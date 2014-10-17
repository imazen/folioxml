package folioxml.slx;

import org.junit.Test;

import folioxml.core.InvalidMarkupException;
import folioxml.folio.FolioTokenReader;
import folioxml.translation.SlxTranslatingReader;
import folioxml.utils.ConfUtil;
import folioxml.utils.YamlUtil;
import folioxml.xml.SlxToXmlTransformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SlxTransformerTest {

	@Test
	public void FolioHelp() throws IOException, InvalidMarkupException{
		XmlExport("folio-help");
	}
	
	public void XmlExport(String configName)throws IOException, InvalidMarkupException{
    	
	    System.out.println("Starting");
	    FolioTokenReader ftr = new FolioTokenReader(new File(YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath()))); 
	    File file  = new File(YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getExport(), true), "Translation.slx");
	    FileOutputStream fos = new FileOutputStream(file); 
    	
	    try{
	    	SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr));
		    
	    	
	    	
	    	OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
	    	out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<infobase>");
		    while (true){
				SlxRecord r = srr.read();
				SlxToXmlTransformer gts = new SlxToXmlTransformer();
				if (r==null) break;
				
				out.write(gts.convert(r).toXmlString(true));
			}
		    out.write("\n</infobase>");
		    out.close();
	    }finally{
	    	
	    	ftr.close();
	    	fos.close();
	    }

    }
	   
	   
}
