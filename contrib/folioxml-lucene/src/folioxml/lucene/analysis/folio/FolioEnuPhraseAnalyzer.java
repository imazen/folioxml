package folioxml.lucene.analysis.folio;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import java.io.Reader;


public final class FolioEnuPhraseAnalyzer extends Analyzer {
  public final TokenStream tokenStream(String fieldName,
                                 final Reader reader) {
    return new TokenCombiner( new FolioEnuTokenizer(reader),' ');
  }
   
}
