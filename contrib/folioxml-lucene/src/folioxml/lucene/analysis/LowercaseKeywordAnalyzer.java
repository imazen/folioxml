package folioxml.lucene.analysis;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Lowercase normalized: "Tokenizes" the entire stream as a single token. This is useful
 * for data like zip codes, ids, and some product names.
 */
public final class LowercaseKeywordAnalyzer extends Analyzer {
  public TokenStream tokenStream(String fieldName,
                                 final Reader reader) {
    return new LowercaseKeywordTokenizer(reader);
  }
    @Override
  public TokenStream reusableTokenStream(String fieldName,
                                         final Reader reader) throws IOException {
    Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
    if (tokenizer == null) {
      tokenizer = new LowercaseKeywordTokenizer(reader);
      setPreviousTokenStream(tokenizer);
    } else
      	tokenizer.reset(reader);
    return tokenizer;
  }
}
