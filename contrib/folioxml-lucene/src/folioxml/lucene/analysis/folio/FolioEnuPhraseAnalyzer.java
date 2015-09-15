package folioxml.lucene.analysis.folio;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;


public final class FolioEnuPhraseAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer t = new FolioEnuTokenizer();
        return new TokenStreamComponents(t, new TokenCombiner(t, ' '));
    }

}
