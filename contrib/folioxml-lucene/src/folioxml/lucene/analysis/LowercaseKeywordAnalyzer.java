package folioxml.lucene.analysis;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;

/**
 * Lowercase normalized: "Tokenizes" the entire stream as a single token. This is useful
 * for data like zip codes, ids, and some product names.
 */

/**
 * Lowercase normalized: "Tokenizes" the entire stream as a single token. This is useful
 * for data like zip codes, ids, and some product names.
 */

public final class LowercaseKeywordAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer t = new KeywordTokenizer();
        return new TokenStreamComponents(t, new LowerCaseFilter(t));
    }
}