package folioxml.xml;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.slx.SlxToken;


public class SlxToXmlTest {

    @Test
    public void TestSlxToXmlNode() throws IOException, InvalidMarkupException{
       SlxToken s = new SlxToken("</selfclosing with=\"attribute\">");
       new Node(s,true);//Deep copy attrs
       //Verify that the Node constructor doesn't remove the attributes from the SlxToken.
       Assert.assertEquals("attribute",s.get("with"));
    }

	@Test
	public void TestHelpForCorruption() throws IOException, InvalidMarkupException{
		TestForCorruption("folio-help");
	}
	
    private void TestForCorruption(String configName)throws IOException, InvalidMarkupException{
	    System.out.println("Starting");
	    
	    //Create SLX valid reader
	    SlxRecordReader srr = new SlxRecordReader(new File(folioxml.config.TestConfig.getFolioHlp().getFlatFilePath()));
	
	    while(true){
			SlxRecord r = srr.read();
		    if (r == null) break;//loop exit

		    String original = r.toSlxMarkup(false);
		    
		    XmlRecord rx= new SlxToXmlTransformer().convert(r);
            //The SLX output should be identical before and after. If not, SlxToXmlTransformer is modifying the tokens/attributes
		    Assert.assertEquals(original, r.toSlxMarkup(false));
	    }
	    
		//Close the original file
		srr.close();
		
    }
}
