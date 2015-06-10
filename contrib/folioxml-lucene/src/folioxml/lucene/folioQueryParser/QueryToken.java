package folioxml.lucene.folioQueryParser;


import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenInfo;
import folioxml.core.TokenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Opening angle brackets must be paired. Closing angle brackets cannot be present at all! (&lt;/example>)
 *
 * @author nathanael
 */
public class QueryToken{
    

    /*et [, the backslash \, the caret ^, the dollar sign $, the period or dot ., 
     * the vertical bar or pipe symbol |, the question mark ?, the asterisk or star *,
     *  the plus sign +, the opening round bracket ( and the closing round bracket ). And {}
     * */
  

    public enum TokenType{
    	None, OpenGroup, CloseGroup, OpenField, CloseField, Whitespace, Or, Xor, Not, FieldDelimiter, Term, TermSuffix
    }
    public static Pattern rGrouping = Pattern.compile("^[\\(\\)\\[\\]]");
    // & and and are whitespace, and whitespace = &/and
    public static Pattern rWhitespace = Pattern.compile("^(?:\\&|and|\\s+)" , Pattern.CASE_INSENSITIVE);
    public static Pattern rOr = Pattern.compile("^(?:\\||or)", Pattern.CASE_INSENSITIVE);
    public static Pattern rXor = Pattern.compile("^(?:\\~|xor)", Pattern.CASE_INSENSITIVE);
    public static Pattern rNot = Pattern.compile("^(?:\\^|not)", Pattern.CASE_INSENSITIVE);
    public static Pattern rColon = Pattern.compile("^\\:{1,2}");
    //Unquoted strings may contain apostrophes, but they cannot start with them. 
    public static Pattern rTerm = Pattern.compile("^(?:,|" +
    						       "\"(?:[^\"]++|\"\")++\"|" + 
    						       "'(?:[^']++|'')++'|" +
                                    "[^'\",\\s~:/@#\\|\\^\\&\\[\\]\\(\\)\\\\\\{\\}][^\"\\s~:\\|\\^\\&\\[\\]\\(\\)\\\\\\{\\}]*+)");
    public static Pattern rTermSuffix = Pattern.compile("^[/#@][0-9Ss]+");
    
    /**
     * An array of the patterns we look for, in the correct order.
     */
    public static Pattern[] tokenPatterns = new Pattern[]{rGrouping, rWhitespace,rNot, rOr,rXor,rColon, rWhitespace, rTermSuffix,rTerm };
    
    public QueryToken(Pattern p, String text) throws InvalidMarkupException{
    	this.text = text;
    	if (p == rGrouping){
    		if (text.equals("(")) type = TokenType.OpenGroup;
    		if (text.equals(")")) type = TokenType.CloseGroup;
    		if (text.equals("[")) type = TokenType.OpenField;
    		if (text.equals("]")) type = TokenType.CloseField;
    	}
    	if (p == rWhitespace) type = TokenType.Whitespace;
    	if (p == rOr) type = TokenType.Or;
    	if (p == rXor) type = TokenType.Xor;
    	if (p == rNot) type = TokenType.Not;
    	if (p == rColon) type = TokenType.FieldDelimiter;
    	if (p == rTerm) type = TokenType.Term;
    	if (p == rTermSuffix) type = TokenType.TermSuffix;
    	if (this.type == TokenType.None) throw new InvalidMarkupException("Invalid token, could not be classified: " + text);
    }
    public QueryToken(TokenType type, String text) throws InvalidMarkupException{
        this.text = text;
        this.type = type;
    }

      /**
     * Comment, text, or tag
     */
    public TokenType type = TokenType.None;

    public String text = null;
    public TokenInfo info = null;
    
    public String fieldName = null;
    public void setFieldNameRecursive(String name){
    	fieldName= name;
    	if (children != null){
    		for (int i =0; i < children.size(); i++){
    			children.get(i).setFieldNameRecursive(name);
    		}
    	}
    }

    public List<QueryToken> children;
    
    public List<QueryToken> headers; //For a field

    
    public void add(QueryToken child){
    	if (children == null) children  = new ArrayList<QueryToken>();
    	children.add(child);
    }
    
    public void ParseChildrenIntoTree() throws InvalidMarkupException{
    	/* Order of operations
    	 * 2) Proximity suffixes. Throw exception if the previous child is not a Term
    	 * 1) Groups and Fields, build them.
    	 * 3) Drop whitespace
    	 * 4) do not grouping
    	 * 5) do or grouping
    	 * 6) do xor grouping
    	 * 7) If an OpenParen, OpenField, or None token has more than one child, they are implicitly in an AND clause.
    	 */
    	applyTermSuffixes();
    	createGroup(false);
    	stripWhitespace();
    	applyNot();
    	applyOrs();
    }
    /*
     * Finds each term suffix and ensures the previous token is a term. Then it moves the term to be a child of the term suffix.
     */
    protected void applyTermSuffixes() throws InvalidMarkupException{
		if (children == null) return;
		for (int i = 0; i < children.size(); i++){
			if (children.get(i).type == TokenType.TermSuffix){
				if (i == 0 || children.get(i -1).type != TokenType.Term) throw new InvalidMarkupException("Term suffixes must be preceeded by a term", children.get(i).info);
				children.get(i).add(children.get(i -1));
				children.remove(i -1);
				i--;
			}
		}
    }
    /*
     * Behaves as if this token is a '(', and 'children' contains all the remaining tokens. If no matching ')' is found, it doesn't complain. 
     * calls createField when it hits '['. Returns all tokens that don't fit into the current group.
     */
    protected List<QueryToken> createGroup(boolean strict) throws InvalidMarkupException{
		if (children == null) return new ArrayList<QueryToken>();
		for (int i = 0; i < children.size(); i++){
			QueryToken t= children.get(i);
			if (t.type == TokenType.CloseGroup){
				List<QueryToken> remainder = new ArrayList<QueryToken>(children.subList(i+1, children.size()));
				children = new ArrayList<QueryToken>(children.subList(0, i));
				return remainder;
			}
			if (t.type == TokenType.CloseField) throw new InvalidMarkupException("Found closing ']' without matching opening '['",t.info);
			if (t.type == TokenType.OpenGroup || t.type == TokenType.OpenField){ //Recursive
				t.children = new ArrayList<QueryToken>(children.subList(i + 1, children.size()));
				List<QueryToken> newchildrenlist = new ArrayList<QueryToken>(children.subList(0,i + 1));
				newchildrenlist.addAll(t.type == TokenType.OpenGroup ? t.createGroup(true) : t.createField());
				children = newchildrenlist;
			}
		}
		if (strict) throw new InvalidMarkupException("Failed to find closing ')'", info);
		else return new ArrayList<QueryToken>();
    }
    
    protected List<QueryToken> createField() throws InvalidMarkupException{
		if (children == null) return new ArrayList<QueryToken>();
		//Remove all leading whitespace from a field declaration.
		while(children.size() > 0 && children.get(0).type == TokenType.Whitespace) children.remove(0);
		
		
		headers = new ArrayList<QueryToken>();
		//All child tokens prior to ':' are considered part of the heading, except [, ], (, )
		//If there is no ':', then they're all headers
		//Process the children.
		boolean foundDelimiter = false;
		
		//For popup and note searches, the field is predefined, we want everything to be contents.
		if (children.size() > 0 && TokenUtils.fastMatches("popup|note",children.get(0).text)) {
			foundDelimiter = true;
			headers.add(children.get(0)); children.remove(0);
		}
		
		for (int i = 0; i < children.size(); i++){
			QueryToken t= children.get(i);
			if (t.type == TokenType.CloseField){
				List<QueryToken> remainder = new ArrayList<QueryToken>(children.subList(i+1, children.size()));
				children = new ArrayList<QueryToken>(children.subList(0, i));
				return remainder;
			}
			else if (t.type == TokenType.CloseGroup) throw new InvalidMarkupException("Found closing ')'  without matching opening '(",t.info);
			else if (t.type == TokenType.OpenGroup || t.type == TokenType.OpenField){ //Recursive
				t.children = new ArrayList<QueryToken>(children.subList(i + 1, children.size()));
				List<QueryToken> newchildrenlist = new ArrayList<QueryToken>(children.subList(0,i + 1));
				newchildrenlist.addAll(t.type == TokenType.OpenGroup ? t.createGroup(true) : t.createField());
				children = newchildrenlist;
			} else if (!foundDelimiter && t.type == TokenType.FieldDelimiter){
				foundDelimiter = true;
				children.remove(i); i--;
			} 
			else if (!foundDelimiter){
				headers.add(t);
				children.remove(i);
				i --;
			}
		}
		throw new InvalidMarkupException("Failed to find closing ']'", info);
    }
    
    
    protected void stripWhitespace() throws InvalidMarkupException{
		if (children == null) return;
		for (int i = 0; i < children.size(); i++){
			if (children.get(i).type == TokenType.Whitespace){
				children.remove(i); 
				i --;
			}else children.get(i).stripWhitespace(); //recursive.
		}
    }
    protected void applyNot()throws InvalidMarkupException{
		if (children == null) return;
		for (int i = 0; i < children.size(); i++){
			if (children.get(i).type == TokenType.Not){
				if (i >= children.size() - 1) throw new InvalidMarkupException("No term found for not operator.", children.get(i).info);
				children.get(i).add(children.get(i + 1));
				children.remove(i + 1);
			}
			children.get(i).applyNot(); //If the user double nots, this will cause an exception (good!). 
		}
    }
    protected void applyOrs() throws InvalidMarkupException{
		if (children == null) return;
		//grandchildren first
		for (int i = 0; i < children.size(); i++){
			children.get(i).applyOrs();
		}
		//Then we do the or stuff.
		for (int i = 0; i < children.size(); i++){
			if (children.get(i).type == TokenType.Or || children.get(i).type == TokenType.Xor){
				if (i == 0 || i >= children.size() -1) throw new InvalidMarkupException("OR and XOR operators require at least two operands", children.get(i).info);
				children.get(i).add(children.get(i -1));
				children.get(i).add(children.get(i + 1));
				children.remove(i + 1);
				children.remove(i - 1);
				i--;
			}
		}
    }
    
}