package folioxml.lucene.analysis.folio;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;

/**
 * FYI FolioEnu*Analyzer doesn't support chinese, japanese, or korean.
 *
 * @author davidlinde
 */
public final class FolioEnuAnalyzer extends Analyzer {


    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer t = new FolioEnuTokenizer();
        return new TokenStreamComponents(t, t);
    }
}
