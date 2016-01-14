package apache.lucene;

import folioxml.lucene.analysis.folio.FolioEnuTokenizer;
import folioxml.lucene.analysis.folio.TokenCombiner;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class CharTokenizer {


    @Test
    public void TestBufferingCodeOnFolio() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2046; i++) {
            sb.append(" ");
        }
        String token = "thisisasingletokenandshouldnotbebroken";
        sb.append(token);
        Tokenizer lt = new FolioEnuTokenizer();
        lt.reset();
        lt.setReader(new StringReader(sb.toString()));
        lt.incrementToken();
        assert (lt.getAttribute(CharTermAttribute.class).toString().equals(token));

    }

    @Test
    public void TestBufferingCodeOnFolioPhrase() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2046; i++) {
            sb.append(" ");
        }
        String token = "thisisasingletokenandshouldnotbebroken";
        sb.append(token);
        Tokenizer t = new FolioEnuTokenizer();
        t.setReader(new StringReader(sb.toString()));
        TokenStream lt = new TokenCombiner(t, ' ');
        lt.incrementToken();
        assert (lt.getAttribute(CharTermAttribute.class).toString().equals(token));

    }
}
