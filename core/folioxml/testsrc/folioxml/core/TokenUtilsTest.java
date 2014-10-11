package folioxml.core;

import org.junit.Test;




public class TokenUtilsTest{
    
    
    
    @Test
    public void fastMatches(){
        assert(TokenUtils.fastMatches("hi|yo", "hi"));
        assert(TokenUtils.fastMatches("hi|yo", "yo"));
        assert(TokenUtils.fastMatches("[a-z]*", "hi"));
        assert(TokenUtils.fastMatches("hey", "hey"));
        assert(TokenUtils.fastMatches( "record|note|popup|namedPopup|td","record"));
    }

    
}