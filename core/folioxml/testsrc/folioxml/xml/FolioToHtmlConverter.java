package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;

import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;


public class FolioToHtmlConverter {

    @Test
    public void TestConversion() throws IOException, InvalidMarkupException{

        String from  = folioxml.config.TestConfig.getFolioHlp().getFlatFilePath();



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
