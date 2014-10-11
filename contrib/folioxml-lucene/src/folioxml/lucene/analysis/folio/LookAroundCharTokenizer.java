package folioxml.lucene.analysis.folio;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;



public abstract class LookAroundCharTokenizer extends Tokenizer {
	
	  public LookAroundCharTokenizer(Reader input) {
	    super(input);
	  }
	  
	  /**
	   * Returns true iff a codepoint should be included in a token. This tokenizer
	   * generates as tokens adjacent sequences of codepoints which satisfy this
	   * predicate. Codepoints for which this is false are used to define token
	   * boundaries and are not included in tokens.
	   * 
	   * Values previous and next may be -1 at the beginning or end of a stream, respectively. 
	   */
	  protected abstract boolean isTokenChar(int previous, int c, int next);

	  /**
	   * Called on each token character to normalize it before it is added to the
	   * token. The default implementation does nothing. Subclasses may use this to,
	   * e.g., lowercase tokens.
	   * <p>
	   * As of Lucene 3.1 the char based API ({@link #isTokenChar(char)} and
	   * {@link #normalize(char)}) has been depreciated in favor of a Unicode 4.0
	   * compatible int based API to support codepoints instead of UTF-16 code
	   * units. Subclasses of {@link CharTokenizer} must not override the char based
	   * methods if a {@link Version} >= 3.1 is passed to the constructor.
	   * <p>
	   * <p>
	   * NOTE: This method will be marked <i>abstract</i> in Lucene 4.0.
	   * </p>
	   */
	  protected abstract int normalize(int c);

	  
	  
	  private static final int MAX_WORD_LEN = 1024;
	  private static final int IO_BUFFER_SIZE = 4096;
	  
	  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	  
	  private int offset = 0, finalOffset = 0;
	  private int cPrev = -1;
	  private int c = -1;
	  private int cNext = -1;
	  
	  @Override
	  public final boolean incrementToken() throws IOException {
	    clearAttributes();
	    int length = 0;
	    int start = -1; // We will set this to the index of the first valid token char we find.
	    char[] buffer = termAtt.buffer(); //Destination buffer
	    while (true) {
			cPrev = c;
			c = cNext;
			cNext = input.read(); //No support for surrogates here!
			offset++;
	    	
			if (length > 1 && c == -1) break; //We hit the end of the input for this token
			if (offset > 1 && c == -1) return false; //We don't have a token, nothing to do.
			
			if (offset > 1 && isTokenChar(cPrev, c , cNext)) {               // if it's a token char
				if (length == 0) {                // start of token
				  assert start == -1;
				  start = offset - 2;
				} else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
				  buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
				}
				length += Character.toChars(normalize(c), buffer, length); // buffer it, normalized
				if (length >= MAX_WORD_LEN) break; // buffer overflow! make sure to check for >= surrogate pair could break == test
			} else if (length > 0){             // at non-Letter w/ chars
				break;                           // return 'em
			}
	    }

	    termAtt.setLength(length);
	    assert start != -1;
	    offsetAtt.setOffset(correctOffset(start), finalOffset = correctOffset(start+length));
	    return true;
	    
	  }
	  
	  
	  
	  
	  @Override
	  public final void end() {
	    // set final offset
	    offsetAtt.setOffset(finalOffset, finalOffset);
	  }

	  @Override
	  public void reset(Reader input) throws IOException {
	    super.reset(input);
	    offset = 0;
	    finalOffset = 0;
	    cPrev = -1;
	    c = -1;
	    cNext = -1;
	  }


	
}


