package folioxml.lucene.analysis.folio;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class TokenCombinerTest {

	@Test
	public void TestWithStandardAnalyzer() throws IOException{
		String text = "token1 token2 token3";
		TestCombiner(new StandardAnalyzer().tokenStream("field", new StringReader(text)),text.replace(' ', '-'));
	}
	
	@Test
	public void TestWithFolioEnu() throws IOException{
		String text = "token1 token2 token3";
		TestCombiner(new FolioEnuAnalyzer().tokenStream("field", new StringReader(text)),text.replace(' ', '-'));
	}
	
	@Test
	public void ShouldDie() throws IOException{
		String text = "token1* token2 token3";
		TestCombiner(new FolioEnuAnalyzer().tokenStream("field", new StringReader(text)),"token1-token2-token3");
	}
	
	
	public void TestCombiner(TokenStream s, String expected) throws IOException{
		TokenCombiner tc = new TokenCombiner(s, '-');
        tc.reset();
		int i =0;
		while (tc.incrementToken()){ 
			String term = tc.getAttribute(CharTermAttribute.class).toString();
			Assert.assertEquals(expected, term);
			assert(i ==0);
			i++;
		}
        tc.end();
        tc.close();
	}
	
	@Test
	public void TestSA() throws IOException{
		String text = "agg bgg cgg";
		TokenStream s = new StandardAnalyzer().tokenStream("field", new StringReader(text));
        s.reset();
		int i =0;
		while (true){
			boolean eos = !s.incrementToken(); //We have to process tokens even if they return end of file.
			String term = s.getAttribute(CharTermAttribute.class).toString();
			if (i == 0) Assert.assertEquals("agg", term);
			if (i == 1) Assert.assertEquals("bgg", term);
			if (i == 2) Assert.assertEquals("cgg", term);
			if (i == 3) Assert.assertEquals("", term);
			i++;
			if (eos) break;
		}
        s.end();
        s.close();
	}
	
	
	@Test
	public void TestFolioEnu() throws IOException{
		String text = "agg bgg cgg";
		TokenStream s = new FolioEnuAnalyzer().tokenStream("field", new StringReader(text));
        s.reset();
		int i =0;
		while (s.incrementToken()){ 
			String term = s.getAttribute(CharTermAttribute.class).toString();
			if (i == 0) Assert.assertEquals("agg", term);
			if (i == 1) Assert.assertEquals("bgg", term);
			if (i == 2) Assert.assertEquals("cgg", term);
			if (i == 3) Assert.assertEquals("", term);
			i++;
		}
        s.end();
        s.close();
	}
}
