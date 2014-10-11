package javabugs;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexBugs {

   
    
    protected static String simpleText = "eebbbbcbbbbd";
    protected static Pattern pLazyPossessive = Pattern.compile("^([ebc]*?)(c)?+d$");
    protected static Pattern pLazyGreedy = Pattern.compile("^([ebc]*?)(c)?d$");
    protected static Pattern pGreedyPossessive = Pattern.compile("^([ebc]*)(c)?+d$");
	
	@Test
	public void testLazyPossesiveSimple(){
		testRegex(pLazyPossessive,simpleText,1,2);
	}
	@Test
	public void testLazyGreedySimple(){
		testRegex(pLazyGreedy,simpleText,1,2);
	}
	@Test
	public void testGreedyPossessiveSimple(){
		testRegex(pGreedyPossessive,simpleText,1,2);
	}
	


	
	
	public void testRegex(Pattern p, String text, int groupA, int groupB){
		Matcher m = p.matcher(text);
		System.out.append("Text: " + text + "\n\n");
		System.out.append("Pattern: " + p.toString() + "\n");
		
		if (m.find()){
			int b = m.start(groupB);
			int a = m.end(groupA);
			System.out.append("Group " + groupB + " starts at " + b + ". Group " + groupA + " ends at " + a + "\n");

			assert b >= a || b < 0 :"There are no group repetitions... How is this happening?";
		}
	}
	

	/**
	 * The real world examples I discovered this with:
	 * 
	 */
    /**
     * group(1) closing slash
     * group(2) tag name
     * group(3) tag attributes
     * group(4) self closing slash
     **/
    protected static Pattern pTag = Pattern.compile("^<(/)?+([\\w\\-\\.:]++)(\\s++[^>]*?)?(/)?+>$");
    
    /*
     * Doesn't use possessive quantifier "?+"... uses "?"
     */
    protected static Pattern pTag2 = Pattern.compile("^<(/)?+([\\w\\-\\.:]++)(\\s++[^>]*?)?(/)?>$");
    
    /*
     * Grouping the rest of the regex separately from defective group #4
     */
    protected static Pattern pTag3 = Pattern.compile("^(?:<(/)?+([\\w\\-\\.:]++)(\\s++[^>]*?)?)(/)?+>$");
    
    
    protected static String text = "<record class=\"NormalLevel\" fullPath=\"/\" level=\"root\" " +
    "levelDefOrder=\"Year,Tape,Chapter,Section,Normal Level\"  " +
    "levels=\"Year,Tape,Chapter,Section\">";
	 
	@Test
	public void realWorld_testPossesive(){
		testRegex(pTag,text,3,4);
	}
	@Test
	public void realWorld_testGreedy(){
		testRegex(pTag2,text,3,4);
	}
	
	
	@Test
	public void realWorld_testGrouped(){
		testRegex(pTag3,text,3,4);
	}
	 
	
}
