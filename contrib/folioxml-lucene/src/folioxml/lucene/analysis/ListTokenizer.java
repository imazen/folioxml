package folioxml.lucene.analysis;


import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;

/** Splits into tokens only on delimiting characters  */

public class ListTokenizer extends CharTokenizer {
  /** Construct a new LetterTokenizer. */
  public ListTokenizer(char[] delims) {
    super();
    this.delimiters = delims;
            
  }
  protected char[] delimiters;
    /** Collects only characters which satisfy
   * {@link Character#isLetter(char)}.*/
    @Override
  protected int normalize(int c) {
    return Character.toLowerCase(c);
  }
  
  /** Collects only characters which satisfy
   * {@link Character#isLetter(char)}.*/
  protected boolean isTokenChar(int c) {
      for (int i = 0; i < delimiters.length; i++){
          if (c == delimiters[i]) return false;
      }
      return true;
  }
}
