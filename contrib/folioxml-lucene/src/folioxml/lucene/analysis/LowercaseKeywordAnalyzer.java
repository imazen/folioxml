package folioxml.lucene.analysis;


import com.sun.javafx.fxml.expression.Expression;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Lowercase normalized: "Tokenizes" the entire stream as a single token. This is useful
 * for data like zip codes, ids, and some product names.
 */
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;

import java.io.Reader;

public final class LowercaseKeywordAnalyzer extends Analyzer
{
    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer t = new KeywordTokenizer();
        return new TokenStreamComponents(t, new LowerCaseFilter(t));
    }
}