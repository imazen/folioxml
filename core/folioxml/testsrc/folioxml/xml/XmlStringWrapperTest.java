package folioxml.xml;

import org.junit.Test;
import folioxml.core.InvalidMarkupException;

import java.io.IOException;


public class XmlStringWrapperTest {
	
	
	@Test
	public void test1() throws IOException, InvalidMarkupException{
		test("<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>",
				"<node>ABCD<node /><node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>"
		,0,4,"ABCD");
		
		test("<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>",
				"<node>ABC<node />ABCD<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>"
		,3,1,"ABCD");
		
		test("<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>",
				"<node>A<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>"
		,1,2,"");
		
		test("<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>",
				"<node><node /><node>FG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>"
		,0,5,"");
		
		
		//TWO bugs
		
		//1) If you use it twice over the same segment of text, the second time you may refill 'deleted' empty tokens, since they are still in the array. SHOULD BE FIXED. TEST 
		
		
		//2) Text and entity tokens are treated the same, so you may have entities in with plain text after using the wrapper. Set decodeEntities=true to search agains the decoded text.
		
		
		
	}
	
	@Test
	public void testForFirstTokenBug() throws IOException, InvalidMarkupException{
		Node n = new Node("<node><n>abcdef</n>ghij<n>kl</n>mn<n>op</n>qr</node>");
		XmlToStringWrapper xsw = new XmlToStringWrapper(n,false);
		xsw.replace(14, 2,"");
		xsw.replace(14, 0,"test");
		
		String newText = n.toXmlString(false);

		System.out.append(newText + "\n");
		String b = "<node><n>abcdef</n>ghij<n>kl</n>mn<n></n>testqr</node>";
		//System.out.append(b + "\n");
		assert(newText.equals(b)): "Unexepected result " + newText + " instead of " + b;
	}
	
	@Test
	public void testHoles() throws IOException, InvalidMarkupException{
		Node n = new Node("<node><n>abcdef</n>ghij<n>kl</n>mn<n>op</n>qr</node>");
		XmlToStringWrapper xsw = new XmlToStringWrapper(n,false);
		xsw.replace(12, 4,"[test]");
		xsw.replace(17, 3,"ing]");
		
		String newText = n.toXmlString(false);

		System.out.append(newText + "\n");
		//System.out.append(b + "\n");
		//assert(newText.equals(b)): "Unexepected result " + newText + " instead of " + b;
	}
	//TODO: test entities
	
	//TODO: Bugs inserting at begginning and end of token. 
	@Test 
	public void testInsert() throws IOException, InvalidMarkupException{
		String xml = "<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>";
		Node n = new Node(xml);
		
		//Test beginning (had a bug here once)
		n.getStringWrapper().insert(0, "(hi)");
		xml = xml.replace("ABC", "(hi)ABC");
		assertMatches(n,xml);
		
		//TODO - test end of token and of xml
	}
	
	@Test 
	public void testDelete() throws IOException, InvalidMarkupException{
		String xml = "<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>";
		Node n = new Node(xml);
		
		//Test beginning
		n.getStringWrapper().delete(0,2);
		xml = xml.replace("AB", "");
		assertMatches(n,xml);
		
		//TODO - test end of token and of xml
	}
	
	@Test
	public void testMerging() throws IOException, InvalidMarkupException{
		String xml = "<node>ABC<node />D<node>EFG</node>HIJK<node>L<node>MNOPQRST</node>UVW</node>XYZ</node>";
		String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		test(xml,xml,text,text,true);
		test(xml,xml,text,text,true);
		
		test(xml,xml.replace("HIJ", "!!!!!"),text,text.replace("HIJ", "!!!!!"),true);
		
		//We are doing single character -> multi charreplaces
		String result1 = "<node>-A--B--C-<node />-D-<node>-E--F--G-</node>-H--I--J--K-<node>-L-<node>-M--N--O--P--Q--R--S--T-</node>-U--V--W-</node>-X--Y--Z-</node>";
		

        test(xml,result1,"([A-Z])","-$1-",false);
        
        

		//Will be different... doesn't know to keep together... Not sure if it should matter?
        test(xml,result1,"([A-Z])","-$1-",true); //Had bug here once, (same one as testInsert, beginning)
	}
	
	public void test(String a, String b, String regex, String replacement, boolean smartMerging) throws IOException, InvalidMarkupException{
		Node n = new Node(a);
		//TODO: test entities.
		System.out.append("\n Replacing " + regex + " with " + replacement + " inside:\n" + a + "\n");
		XmlToStringWrapper xsw = new XmlToStringWrapper(n,false);
		xsw.replaceAll(regex,replacement,smartMerging);
		assertMatches(n,b);
	}
	
	public void assertMatches(Node n, String expectedResult) throws InvalidMarkupException{
		String newText = n.toXmlString(false);
		System.out.append("Expected: " + expectedResult + "\n");
		System.out.append("  Result: " + newText + "\n");	
		assert(newText.equals(expectedResult)): "Unexepected result " + newText + " instead of " + expectedResult;
	}
	
	public void test(String a, String b, int start, int length, String replacement) throws IOException, InvalidMarkupException{
		Node n = new Node(a);
		XmlToStringWrapper xsw = new XmlToStringWrapper(n,false);
		xsw.replace(start, length,replacement);
		assertMatches(n,b);
		
	}

}
