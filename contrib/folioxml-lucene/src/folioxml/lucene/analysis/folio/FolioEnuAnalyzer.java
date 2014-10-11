package folioxml.lucene.analysis.folio;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 *  FYI FolioEnu*Analyzer doesn't support chinese, japanese, or korean. 
 * @author davidlinde
 *
 */
public final class FolioEnuAnalyzer extends Analyzer {
  public final TokenStream tokenStream(String fieldName,
                                 final Reader reader) {
    return new FolioEnuTokenizer(reader);
  }
    @Override
  public final TokenStream reusableTokenStream(String fieldName,
                                         final Reader reader) throws IOException {
    Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
    if (tokenizer == null) {
      tokenizer = new FolioEnuTokenizer(reader);
      setPreviousTokenStream(tokenizer);
    } else
      	tokenizer.reset(reader);
    return tokenizer;
  }
}
