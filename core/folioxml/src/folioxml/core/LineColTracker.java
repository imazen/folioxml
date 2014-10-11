package folioxml.core;

/**
 * Implements unicode-compliant line number and column counting. Since some line terminators span multiple characters, this requires state tracking. 
 * Lines and columns are 0-based. Used by TokenReaderBase to parse completed matches. 
 * @author nathanael
 */
public class LineColTracker{
    
    public LineColTracker(){
        
    }
    
    private long _line = 0;
    private long _col = 0;
    private long _char = 0;
    
    public TokenInfo getTokenInfo(){
    	TokenInfo t =new TokenInfo();
    	t.col = col();
    	t.line = line();
    	t.charIndex = chars();
    	return t;
    	
    }
    /**
     * The index of the current line. 0-based
     * @return
     */
    public long line(){ return _line;}
    public LineColTracker line(long newValue){_line = newValue; return this;}
    /**
     * The character index from the last line terminator. First character on a line = 0
     * @return
     */
    public long col(){return _col;}
    public LineColTracker col(long newValue){_col = newValue; return this;}
    
    /**
     * Character index from the beginning of the file, including line terminators. 0-based
     * @return
     */
    public long chars(){return _char;}
    public LineColTracker chars(long newValue){_char = newValue; return this;}
    
    private boolean skipLF = false;
    
    public LineColTracker add(CharSequence cs){
        return add(cs,0,cs.length());
    }
    
    public LineColTracker add (CharSequence text, int startIndex, int endIndex){
        char c;
        for (int i = startIndex; i < endIndex; i++){
            c = text.charAt(i);
            _char++; //always increment this guy
            
            //Skip the LF: ON CR-LF we count the CR and skip the immediately subsequent LF
            if (c == '\n' && skipLF) {
                skipLF = false;
                continue;
            }
            
            skipLF = (c == '\r'); //If it's a CR, skip the following LF
            
            //Check all unicode spec newline chars.
            if (c == '\r' || c == '\n' || c == '\u0085' || c == '\u000c' || c == '\u2028' || c == '\u2029'){
                _line++;
                _col = 0;
            }else{
                _col++;
            }
        }
        return this;
    }
    
    
}