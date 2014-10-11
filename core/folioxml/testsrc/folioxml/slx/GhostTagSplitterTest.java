package folioxml.slx;

import org.junit.Test;
import folioxml.core.InvalidMarkupException;
import folioxml.translation.SlxTranslatingReader;
import folioxml.xml.SlxToXmlTransformer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class GhostTagSplitterTest {

	@Test
	public void testProcessISlxTokenReaderISlxTokenWriter() throws UnsupportedEncodingException, InvalidMarkupException, IOException {
		SlxRecordReader reader = new SlxRecordReader(new SlxTranslatingReader(FolioSnippets.get2MBSampleReader()));
		
		SlxToXmlTransformer s = new SlxToXmlTransformer();
		
		StringBuilder sb = new StringBuilder();
		
		while(true){
			SlxRecord r = reader.read();
			if (r == null) break;
			
			String temp= s.convert(r).toXmlString(true);
			
			sb.append(temp);
		}
		
		String str = sb.toString();
		int i =0;
		i++;
	}

}
