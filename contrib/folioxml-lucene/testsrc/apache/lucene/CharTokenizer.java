package apache.lucene;

import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;
import folioxml.lucene.analysis.folio.FolioEnuTokenizer;
import folioxml.lucene.analysis.folio.TokenCombiner;

import java.io.IOException;
import java.io.StringReader;

public class CharTokenizer {

	@Test
	public void TestBufferingCode() throws IOException{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2046; i++){
			sb.append(" ");
		}
		String token = "thisisasingletokenandshouldnotbebroken";
		sb.append(token);
		LowerCaseTokenizer lt = new LowerCaseTokenizer(Version.LUCENE_33, new StringReader(sb.toString()));
		lt.incrementToken();
		assert(lt.getAttribute(CharTermAttribute.class).toString().equals(token));
		
	}
	
	@Test
	public void TestBufferingCodeOnFolio() throws IOException{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2046; i++){
			sb.append(" ");
		}
		String token = "thisisasingletokenandshouldnotbebroken";
		sb.append(token);
		TokenStream lt = new FolioEnuTokenizer(new StringReader(sb.toString()));
		lt.incrementToken();
		assert(lt.getAttribute(CharTermAttribute.class).toString().equals(token));
		
	}
	@Test
	public void TestBufferingCodeOnFolioPhrase() throws IOException{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2046; i++){
			sb.append(" ");
		}
		String token = "thisisasingletokenandshouldnotbebroken";
		sb.append(token);
		TokenStream lt = new TokenCombiner(new FolioEnuTokenizer(new StringReader(sb.toString())),' ');
		lt.incrementToken();
		assert(lt.getAttribute(CharTermAttribute.class).toString().equals(token));
		
	}
}
