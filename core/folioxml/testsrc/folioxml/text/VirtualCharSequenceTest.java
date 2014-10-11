package folioxml.text;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import folioxml.core.InvalidMarkupException;
import folioxml.xml.NodeList;
import folioxml.xml.XmlToStringWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.fail;

public class VirtualCharSequenceTest {

	@Test
	public void testRepair() throws Exception {
		//Test all permutations of a array with 10 elements (2^10 combinations)
		VirtualCharSequence vcs = new VirtualCharSequence(new ArrayList<ITextToken>());
		permuteRepair(vcs, new int[10],0 );
		permuteRepair(vcs, new int[2],0 );
		permuteRepair(vcs, new int[1],0 );
	}
	private void permuteRepair(VirtualCharSequence vcs, int[] array, int startFrom) throws Exception{
		if (startFrom >= array.length){
			vcs.indexes = array;
			vcs.nodes = new ITextToken[array.length];
			vcs.count = array.length;
			vcs._firstDeletedTokenIndex = 0; //We don't test when this is inaccurate, we know it fails. 
			vcs.repair();
			for (int i = 0; i < vcs.count; i++){
				if (!(vcs.indexes[i] > -1)){
					//2-4-2010. Caught bug where multiple holes in a row were not repaired.
					throw new Exception("Repair failed to remove all holes! " + ArrayUtils.toString(ArrayUtils.subarray(vcs.indexes, 0, vcs.count)));
				}
			}
		}else{
			int[] a1 = ArrayUtils.subarray(array, 0, array.length);
			int[] a2 = ArrayUtils.subarray(array, 0, array.length);
			a1[startFrom] = 0;
			a2[startFrom] = -1;
			permuteRepair(vcs,a1,startFrom +1);
			permuteRepair(vcs,a2,startFrom +1);
		}
	}


	@Test
	public void testMultiReplace() throws InvalidMarkupException, IOException {
		testMultiReplaceCore(26);
		testMultiReplaceCore(5);
		testMultiReplaceCore(1);
		testMultiReplaceCore(2);
	}

	protected void testMultiReplaceCore(int letters) throws InvalidMarkupException, IOException{
		//Build the alphabet with each letter in a node.
		String s = "";
		for (int i = 0; i <= letters; i++){
			s += "<n>" + (char)((int)'A' + i) + "</n>";
		}
		//We need to test using multiReplace in non-sequential and overlapping mode. 
		VirtualCharSequence vcs = new XmlToStringWrapper(new NodeList(s));
		//Will shift all the text together.
		for (int i =letters-1; i > -1; i--){
			for (int j = 1; j <= letters -i;j++){
				//Try with different snap values
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(), -1, true);
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(), -1, false); //False 
				//Try with incorrect start values (should find it anyway)
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(),j, true);
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(), i, false);
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(), 30, true);
				vcs.multiReplace(i, j, vcs.subSequence(i, i +j).toString(), -20, false);
			}
		}
		assert(new NodeList(s).getTextContents().equals(vcs.toString())); //The text-only result should be identical to the beginning.
		
		//We need to test using multiReplace in non-sequential and overlapping mode. 
		NodeList nodes = new NodeList(s);
		vcs = new XmlToStringWrapper(nodes);
		//Replace only one character at a time, with itself
		for (int i =0; i < letters; i++){
			vcs.multiReplace(i, 1, vcs.subSequence(i, i + 1).toString(), -1, true);
		}
		//The XML should be identical
		assert(s.equals(nodes.toXmlString(false))); //The text-only result should be identical to the beginning.
		
	}


	@Test
	public void testLastItemSmallerLinear() throws InvalidMarkupException {
		VirtualCharSequence vcs = new XmlToStringWrapper(new NodeList());
		
		//Old failure cause: startAt pointing to a larger number than the search value.
		int[] l = new int[]{1,5,7,9};
		
		
		assert (vcs.lastItemSmallerLinear(l, 2, 3, 4, true) == 0);
		assert (vcs.lastItemSmallerLinear(l, 2, 3, 4, false) == 0);
		assert (vcs.lastItemSmallerLinear(l, 2, 3, 4, true) == 0);
		assert (vcs.lastItemSmallerLinear(l, 2, 3, 4, true) == 0);
		
		//Old failure: grabbing an equal value when orEquals is false. Must be followed by larger number.
		assert (vcs.lastItemSmallerLinear(l, 5, 0, 4, false) == 0);
		
		
		//Failure cases: startAt pointing to hole, followed by larger number.
		l = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,-1,13,14,15};
		assert (vcs.lastItemSmallerLinear(l, 12, 12, 16, false) == 11);
		assert (vcs.lastItemSmallerLinear(l, 12, 12, 16, true) == 11);
		
		
		//Failure cases: startAt pointing to last item, which is a hole.
		l = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,-1};
		assert (vcs.lastItemSmallerLinear(l, 15, 15, 16, false) == 14);
		
		//Failure cases: 1, [25 holes], 26. Count = 27, startAt = 0; snapNext = false;
		l = new int[]{0,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,26};
		assert (vcs.lastItemSmallerLinear(l, 0, 0, 27, false) == -1); //With orEqual false, there is no item smaller than 0. This should return -1
		assert (vcs.lastItemSmallerLinear(l, 0, 0, 27, true) == 0); //It should return 0 with orEqual true
		
		
		//Verify the right index is returned on duplicates.
		l = new int[]{0,1,2,3,4,5,5,5,5,9,10,11,12,13,14,-1};
		assert (vcs.lastItemSmallerLinear(l, 5, 15, 16, true) == 8);
		assert (vcs.lastItemSmallerLinear(l, 6, 15, 16, false) == 8);
		assert (vcs.lastItemSmallerLinear(l, 5, 0, 16, true) == 8);
		assert (vcs.lastItemSmallerLinear(l, 6, 0, 16, false) == 8);
		assert (vcs.lastItemSmallerLinear(l, 5, 7, 16, true) == 8);
		assert (vcs.lastItemSmallerLinear(l, 6, 7, 16, false) == 8);
		
		//Verify invalid startSearch values are OK
		assert (vcs.lastItemSmallerLinear(l, 5, 2000, 16, true) == 8);
		assert (vcs.lastItemSmallerLinear(l, 6, -1000, 16, false) == 8);
		
		//TODO: test with 1 and 2 item arrays.
	}
	
	@Test
	public void testLastItemSmaller() throws InvalidMarkupException {
		
///lastItemSmaller
		
		//Expects no holes and no duplicates.
		//Entries must be sorted.
		
		VirtualCharSequence vcs = new XmlToStringWrapper(new NodeList());
		
		for (int i = 1; i < 100; i++){
			//Generate a new array of length 'i'. All elements are sequential, none are duplicates, and the differences between subsequent values is random.
			int[] a = new int[i];
			int counter = 0;
			for (int j = 0; j  < i; j++){
				a[j] = counter;
				counter += 1 + (int)Math.round(Math.random() * 10);
			}
			//Verify the linear and binary versions have identical results when called with every existing number in the array in the search, and with every index of the array before the item as the starting point.
			//Binary search doesn't work if you lie about the starting point.
			for (int j = 0; j < counter + 10; j++){
				for (int k = 0; k < vcs.lastItemSmallerLinear(a, j, 0, a.length, false); k++){
					//With orEqual true.
					int resultA = vcs.lastItemSmaller(a, j, k, a.length, true);
					int resultB = vcs.lastItemSmallerLinear(a, j, k, a.length, true);
					assert (resultA == resultB);
					
					//Now with orEqual false.
					resultA = vcs.lastItemSmaller(a, j, k, a.length, false);
				    resultB = vcs.lastItemSmallerLinear(a, j, k, a.length, false);
					assert (resultA == resultB);
				}
			}
		}
	}
	
	private String readFile( String file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new FileReader (file));
	    String line  = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    String ls = System.getProperty("line.separator");
	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }
	    return stringBuilder.toString();
	 }
	
	@Test
	public void testReplaceAllRegexStringBoolean() throws InvalidMarkupException, IOException {
		fail("Not yet implemented");
		
	}
	
	
	
	/*
	
	@Test
	public void testReplaceIntIntString() {
		//fail("Not yet implemented");
	}

	@Test
	public void testReplaceIntIntStringBoolean() {
		//fail("Not yet implemented");
	}

	
	@Test
	public void testReplaceCore() {
		
	}



	@Test
	public void testReplaceSmartIntIntString() {
		//fail("Not yet implemented");
	}

	@Test
	public void testReplaceSmartIntIntStringIntBoolean() {
		//fail("Not yet implemented");
	}

	@Test
	public void testReplaceAllPatternStringBoolean() {
		//fail("Not yet implemented");
	}

	@Test
	public void testReplaceMatch() {
		//fail("Not yet implemented");
	}
*/
}
