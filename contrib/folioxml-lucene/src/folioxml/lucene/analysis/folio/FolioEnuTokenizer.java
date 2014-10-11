package folioxml.lucene.analysis.folio;

import java.io.Reader;

public class FolioEnuTokenizer extends LookAroundCharTokenizer {

	/*
	alphanum = 0-9A-Za-z  + 138, 140, 154, 156, 159, 192�214, 216�246, 248�255

	single quotes, commas, minus sign, periods, and forward slashes are permitted in special contexts

	' Must come between two alpha characters.
	, Must come between two numeric characters, or immediately precede a numeric character
	- Must come between two numeric characters, or precede or follow a numeric character.
	- Must come between two between alpha characters or precede by alpha characters with a numeric 
	. Must come between two alpha-numeric characters or immediately precede a numeric character.
	/ Must come between two numeric characters.
*/
	
	public FolioEnuTokenizer(Reader input) {
		super(input);
	}

	@Override
	protected boolean isTokenChar(int p, int c, int n) {
		if (isAlphaNumeric(c)) return true;
		if (c == '\'' && isAlpha(p) && isAlpha(n)) return true;
		if (c == ',' &&  isNumeric (n)) return true;
		if (c == '-' &&  (isNumeric (n) || isNumeric (p))) return true;
		if (c == '-' &&  (isAlpha(n) && isNumeric(p))) return true;
		if (c == '/' &&  isNumeric (n) &&  isNumeric (p)) return true;
		if (c == '.' &&  (isNumeric (n) || (isAlphaNumeric(p) && isAlphaNumeric(n)))) return true;
		return false;
	}
	protected boolean isAlphaNumeric(int c){
		return isAlpha(c) || isNumeric(c);
	}
	protected boolean isNumeric(int c){
		return (c >= '0' && c <= '9');
	}
	protected boolean isAlpha(int c){
		return   (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				c == 138 || c == 140 || c == 154 || c == 156 || c == 159 || 
				(c >= 192 && c <= 214) || (c >= 216 && c <= 246) || ( c >= 248 && c <= 255);
	}

	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}

	
	  

}
