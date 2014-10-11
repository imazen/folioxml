package folioxml.text;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import folioxml.core.TokenUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


	/**
	 * Wraps a collection of text tokens, exposing the joined text as a char sequence. Changing this instance changes the underlying text tokens, but not vice versa. If you externally modify the text tokens,
	 * you must re-create this class. (And maybe re-create the token list also, if any have been inserted or deleted).
	 * Exposes replace, replaceAll, insert, delete, setCharAt, flattenArea...
	 * Text tokens may contain entities after using methods of this class. Empty tokens may remain, unless the ITextToken instances automatically delete themself when setText("") is called.
	 * 
	 * Warning! If all text is deleted from this instance, you will no longer be able to edit it! 
	 * No operation will succeed if length() == 0.
	 * @author nathanael
	 *
	 */
	public class VirtualCharSequence implements CharSequence {

		public VirtualCharSequence(List<ITextToken> textTokens){
			this(textTokens,true);
		}
		/**
		 * 
		 * @param textTokens A list of text tokens to expose as a single CharSequence for reading, searching, replacing, and modification.
		 * @param decodeEntities If true, decodes entities found in text and entity tokens. Re-encodes special characters in modified tokens. If false, your regexes must account for &apos; as well as ' . Replacement text must be careful to encode chars properly
		 */
		public VirtualCharSequence(List<ITextToken> textTokens, boolean decodeEntities){
			this.encodeEntities = this.decodeEntities = decodeEntities; //Converts all entities encountered into text, instead of just replaced ones.
			
			//Calculate required size. (estimated, since we aren't decoding entities. Decoded entities are usually smaller than their codes, so we should be OK)
			int estimatedTextSize = 0;
			//How many empty tokens exist.
			int emptyTokens = 0;
			for (int i = 0; i < textTokens.size(); i++){
				int len = textTokens.get(i).getText().length();
				if (len== 0) emptyTokens++;
				else estimatedTextSize += len;
			}
			
			//Init the arrays to the right size.
			text = new StringBuilder(estimatedTextSize);
			nodes = new ITextToken[textTokens.size() - emptyTokens];
			indexes = new int[nodes.length];
			count = nodes.length; //Init the length var (how much of the array is active)
			//Fill them
			int ix = 0;
			int charIndex = 0;
			for (int i = 0; i < textTokens.size(); i++){
				ITextToken t = textTokens.get(i);
				String s = t.getText();
				if (s.length() > 0){
					//Only add non-empty nodes
					nodes[ix] = t;
					indexes[ix] = charIndex;
					//Increment index and char index
					ix++; charIndex += s.length();
					//Add text
					text.append(s);
				}		
			}
			
		}
		
		
		
		/* stores all unique text being deleted*/
		public HashSet<String> deletedText = null;
		
		
		/**
		 * The text cache for fast searching.
		 */
		StringBuilder text = null;
		/**
		 * Entity decoding/encoding settings
		 */
		boolean decodeEntities = false;
		boolean encodeEntities = false;
		/**
		 * This array is sorted. Develops holes (val=-1) where tokens are deleted. However, binary searches can't run with holes. 
		 */
		/**
		 * Exposed for testing purposes only
		 */
		 int[] indexes = null;
		 /**
			 * Exposed for testing purposes only
			 */
		 ITextToken[] nodes = null;
		 /**
			 * Exposed for testing purposes only
			 */
		 int count = 0;
		
		
		/**
		 * For faster cleanup. This is where repair() has to start working.
		 */
		int _firstDeletedTokenIndex = -1;
		
		/**
		 * Used by addNode when populating the arrays. Only used during construction of the class.
		 */
		int filledTo = 0;
		
		

		/**
		 * Fills holes in the array, so binary search can be used again.  Decreases 'count' accordingly.
		 */
		protected void repair(){
			if (this._firstDeletedTokenIndex < 0) return; //Nothing to do.
			
			
			int offset = 0;
			int ix = _firstDeletedTokenIndex; //startAt
			
			while (ix < count){
				//If this is a hole, increase the offset
				if (indexes[ix] == -1) {
					offset++;
					count--; //Decrease the count (used part of the array)
				}
				//Shift down //if (ix == count) break; //The last item was a hole. Quit here. (not needed, will exit anyway)
				if (offset > 0 && ix < count){
					indexes[ix] = indexes[ix + offset];
					nodes[ix] = nodes[ix + offset];
				}
				//Go to next, unless we placed a new hole here (2-4-2010. Patched bug where holes were skipped if adjacent. Prior code incremented unconditionally)
				if (ix >= count || indexes[ix] != -1) ix++;
			}
			//We're cleaned up
			this._firstDeletedTokenIndex = -1;
		}
		/**
		 * Returns a list of the strings corresponding to the non-empty text tokens that make up this virtual char sequence. A debugging tool.
		 * @return
		 */
		public List<String> getComponentStrings(){
			List<String> list = new ArrayList<String>(this.nodes.length);
			for (int i = 0; i < count; i++)
			{
				if (this.indexes[i] > -1){
					String s = this.nodes[i].getText();
					if (s != null && !s.isEmpty()) list.add(s);
				}
			}
			return list;
		}

		/**
		 * Returns the [index]th non-empty text token. 0-based
		 * @param index
		 * @return
		 */
		public ITextToken getTokenAt(int index){
			int ix = 0;
			for (int i = 0; i < count; i++)
			{
				if (this.indexes[i] > -1){
					String s = this.nodes[i].getText();
					if (s != null && !s.isEmpty()) {
						if (ix == index) return this.nodes[i];
						ix++;
					}
				}
			}
			return null;
		}

		
		public char charAt(int index) {
			return text.charAt(index);
		}

		public VirtualCharSequence setCharAt(int index, char c){
			//untested
			this.replace(index, 1, String.valueOf(c)); //There's a faster way, but we have to run repair, binary lookup, decode, and encode again.
			return this;
		}
		
		public int length() {
			return text.length();
		}
		
		
		public CharSequence subSequence(int start, int end) {
			return text.subSequence(start, end);
		}
		public String toString(){
			return text.toString();
		}
		/**
		 * Warning - if you delete all text, you won't be able to re-insert any text (all tokens will be gone). 
		 * Use replace instead.
		 * @param start
		 * @param newText
		 */
		public void insert(int start, String newText){
			replace(start,0,newText);
		}
		/**
		 * Warning - if you delete all text, you won't be able to re-insert any text (all tokens will be gone). 
		 * Use replace instead.
		 * @param start
		 * @param length
		 */
		public void delete(int start, int length){
			replace(start,length,"");
		}
		

		public VirtualCharSequence replace(int start, int length, String newText){
			repair(); //So we can use binary search, instead of linear search. Only does work if work is needed (_firstDeletedTokenIndex)
			
			//Replace, finding initial node using binary search lastItemSmallerOrEqual
			replaceCore(start,length,newText,lastItemSmaller(indexes,start,0,count,true));	
			return this;
		}
		
		/**
		 * 
		 * @param start
		 * @param length
		 * @param newText
		 * @param snapNext If true, and start is the index where one token stops and another starts, the second token is modified. If false, the first token is modified.
		 */
		public VirtualCharSequence replace(int start, int length, String newText, boolean snapNext){
			repair(); //So we can use binary search, instead of linear search. Only does work if work is needed (_firstDeletedTokenIndex)
			
			//Replace, finding initial node using binary search lastItemSmallerOrEqual
			replaceCore(start,length,newText,lastItemSmaller(indexes,start,0,count,snapNext));	
			return this;
		}
		/**
		 * Allows multiple replaces to be done efficiently without the cleanup overhead. Should only be done if replaces are sequential and non-overlapping.
		 * You should call .repair() after you are done.
		 * For good performance, pass the result of the last call in as startSearchAt. The first call after repair() ignores startSearchAt, and uses binary search. 
		 * Subsequent calls use a slow, linear search, starting at startSearchAt. 
		 * @param start
		 * @param length
		 * @param newText
		 * @param startSearchAt You can pass 0 for the first call, but you should use the result of the last multiReplace call for subsequent calls.
		 * @return
		 */
		protected int multiReplace(int start, int length, String newText, int startSearchAt, boolean snapNext){
			assert(length + newText.length() > 0); //Assert this call has a point. It's protected, so bad usage should be found.
			
			//SnapNext must be true when 'start' == 0, because there are no previous tokens (Fixed 2-4-2010)
			if (start == 0) snapNext = true;
			
			int firstTokenIndex  = -1;
			if (this._firstDeletedTokenIndex > -1){
				//We use a linear search if there are array holes, starting at token startSearchAt 
				
				firstTokenIndex = lastItemSmallerLinear(indexes, start, startSearchAt >= 0 ? startSearchAt : 0, count,snapNext);
			}else{
				//Use a binary search if the arrays are clean.
				firstTokenIndex = lastItemSmaller(indexes,start,0,count,snapNext);
			}
			if (firstTokenIndex < 0){
				assert(firstTokenIndex >= 0);
			}
			
			//Returns the index of the last modified token
			return replaceCore(start,length,newText,firstTokenIndex);	
		}
		

		/**
		 * Starts modifying tokens as needed, starting at firstTokenIndex. Returns the index of the last modified token.
		 * @param start Character index in main text to start replacing/inserting/deleting
		 * @param length The amount of text to delete/replace
		 * @param newText
		 * @param firstTokenIndex
		 * @return
		 */
		int replaceCore(int start, int length, String newText,int firstTokenIndex){
			if (firstTokenIndex < 0 && start == 0) firstTokenIndex = 0; //If snapNext isn't on, sometimes we get -1 when start == 0.
			
			if (length == 0 && newText.length() == 0) return firstTokenIndex; //Nothing to do. return 
			
			assert(count != 0):"You may not modify an empty VirtualCharSequnce. It has no underlying tokens to write to.";
			assert(start >= 0);
			assert(length <= text.length() - start);
			assert(firstTokenIndex >= 0 && firstTokenIndex < count): "First token index = " + firstTokenIndex;
//TODO: BUG - what if we delete all the text from the VirtualCharSequnce? Count will == 0... We still need to be able to insert...somewhere..
			
			//Change the text locally
			
			//System.out.println(text.subSequence(start, start + length));
			
			//String oldText = text.subSequence(start, start + length).toString();
			
			//lazy initilzation
			//if(deletedText == null) deletedText = new HashSet<String>();
			//deletedText.add(text.subSequence(start, start + length).toString().trim().toLowerCase());
			
			text.replace(start, start + length, newText);
			
			//What is the offset for subsequent text?
			int offset = newText.length() - length;
			
			//The index of the first node to modify. 
			int startIndex = firstTokenIndex; 
			
			//So we can know where to start repairing
			int firstDeletedTokenIndex = -1;
			
			//So we know where to start looking in a future replace call
			int lastModifiedToken = -1;
			
			//Change the underlying XML
			for (int i = startIndex; i < count; i++){
				if (indexes[i] == -1) continue; //Skip holes in array
				
				//Is this entry after the modified text? Just offset it.
				if (indexes[i] > start + length){
					indexes[i] += offset; //If we insert text, all nodes shift down.
				}else{
					//It's within the text we are replacing, and needs modifying
					String oldNodeText  = this.decodeEntities ? TokenUtils.entityDecodeString(nodes[i].getText()) : nodes[i].getText();
					
					
					//The first node gets all the new text inserted (replacing the old text)
					if (i == startIndex){
						
						//Where inside the node do we start?
						int nodeLocalIndex = start - indexes[i]; //This must be inside the node if firstTokenIndex is correct. Can also be the character after the last..
						//What part of the node text are we replacing?
						int nodeLocalLength = Math.min(length, oldNodeText.length() - nodeLocalIndex);
						//Splice in new text
						String newNodeText = oldNodeText.substring(0,nodeLocalIndex) + newText + oldNodeText.substring(nodeLocalIndex + nodeLocalLength);
						//Save to node
						nodes[i].setText(this.encodeEntities ? TokenUtils.lightEntityEncode(newNodeText) : newNodeText);
						//Patched 2-3-2010. If the replacement string is empty, we need to know how to handle it.
						if (newNodeText.length() == 0){
							indexes[i] = -1; //Mark for skipping.
							//So we know where to start repairing later
							if (firstDeletedTokenIndex == -1) firstDeletedTokenIndex = i;
							if (i == count -1) count--; //Don't let the last one be a hole. Causes problems.
						}
						
					}else{
						//Other nodes just get deleted/partially deleted.
						int deleteCount = start + length - indexes[i]; //How many chars to delete from the start of the token. May be more than the token contains.
						if (deleteCount >= oldNodeText.length()){
							//We are deleting this...
							nodes[i].setText("");
							
							indexes[i] = -1; //Mark for skipping.
							//So we know where to start repairing later
							if (firstDeletedTokenIndex == -1) firstDeletedTokenIndex = i;
							if (i == count -1) count--; //Don't let the last one be a hole. Causes problems.
						}else{
							String newNodeText = oldNodeText.substring(deleteCount);
							//Remove and reencode.
							nodes[i].setText(this.encodeEntities ? TokenUtils.lightEntityEncode(newNodeText) : newNodeText);
							//They all get shoved to the end of the new text.
							indexes[i] = start + newText.length();
						}
						
					}
					lastModifiedToken = i;
				}
			}
			//Record for repair purposes.
			if (firstDeletedTokenIndex > -1 && (this._firstDeletedTokenIndex < 0 || this._firstDeletedTokenIndex > firstDeletedTokenIndex)){
				this._firstDeletedTokenIndex = firstDeletedTokenIndex;
			}
			return lastModifiedToken;
		}
		/**
		 * Binary searches the array for the last item smaller than value. Expects the array to be sorted and have no holes. All values must be unique.
		 * startAt and endAt must be accurate if you want accurate results. Won't look before 'startAt' for a match, unlike the linear equivalent of this algorithm.
		 * @param l
		 * @param value
		 * @param startAt
		 * @param endAt
		 * @return
		 */
		protected int lastItemSmaller(int[] l, int value, int startAt, int endAt, boolean orEqual){
			int low = startAt;
			int high = endAt-1;

			while (low <= high) {
			    int mid = (low + high) >>> 1;
			    int mval = l[mid]; //The current value

			    if (mval < value + (orEqual ? 1 : 0))
			    	low = mid + 1;
			    else
			    	high = mid - 1;

			}
			if (low != high + 1){
				assert (low == high - 1): low + " " + high;
			}
			//This loop should always exit with low = high -1. 
			
			if (high == startAt - 1) return -1; //All items are smaller.
			else return high; //This index is smaller
		}
		/*
		 * Does a linear search for the last item smaller (or equal, if orEqual==true) to the specified search 'value'. Allows, holes (-1 values), and non-unique values.
		 * If startAt AND all subsequent slots are holes, the index of the first non-hole prior to startAt will be returned. (If forward search fails, reverse search occurs.) 
		 * Search won't progress past endAt, but may backtrack all the way to index 0, before startAt.
		 * orEqual determines whether valid matches include exact matches, (i.e, the first character in a text segment). 
		 * When true, and asked for the text segment corrsponding to a character index that lies perfectly at the intersection of to paragraphs, the second one will be given. 
		 * Returns -1 if no values found. Searching for 0 in an array of zeroes will return false of orEqual==false
		 */
		protected int lastItemSmallerLinear(int[] l, int value, int startAt, int endAt, boolean orEqual){
			//Don't allow starting after the ending. Change the starting value - this will force the backtracking code path immediately.
			if (startAt > endAt) startAt = endAt; 
			//Don't allow starting before 0
			if (startAt < 0) startAt = 0;
			
			assert(endAt <= l.length);
			
			//Test searching for '3' in a list of '3s' with orEqual=true
			
			int lastIx = -1;
			for (int i = startAt; i < endAt; i++){
				int val = l[i];
				if (val == -1) continue; //Skip holes.
				if (orEqual){
					//Save the index if it's less than or equal to what we are searching for. If it is larger, return the last value
					if (val <= value) 
						lastIx = i; 
					else if (lastIx > -1)
						return lastIx;
				}else{
					//Save the index if it's less than what we are searching for. If it is larger or equal, return the last value
					if (val < value) 
						lastIx = i; 
					else if (lastIx > -1)
						return lastIx;
				}
			}
			//Previously possible Bug. If startSearchAt and subsequent entries are holes, this will fail.
			if (lastIx == -1){		
				for (int i = startAt - 1; i >= 0; i--){
					int val = l[i]; if (val == -1) continue;
					//First valid value, we return. We're going backwards, remember.
					if (orEqual && val <= value) return i;
					if (!orEqual && val < value) return i;
				}
			}
			return lastIx; //Changed 2-3-2010. If working at the end of the list, we might hit the end without encountering a larger value.  
		}
		
		/**
		 * Only modifies the tokens that actually have changed text... Allows S&R over fielded text without losing the fields on unchanged data.
		 * 
		 * @param start
		 * @param length
		 * @param newText
		 */
		public void replaceSmart(int start, int length, String newText){
			replaceSmart(start,length,newText,30,false);
		}
		/**
		 * Only modifies the tokens that actually have changed text... Allows S&R over fielded text without losing the fields on unchanged data.
		 * 
		 * @param start
		 * @param length
		 * @param newText
		 */
		public void replaceSmart(int start, int length, String newText, int secondsTimeout, boolean enableLineOptimization){
			repair(); //Allow binary search. Fixed 2-4-2010. This omission was allowing multiple sets of multiReplace to run.
			//TODO: implement with DIFF
			String oldText = text.substring(start, start + length);
			//Diff
			diff_match_patch diff = new diff_match_patch();
			diff.Diff_Timeout = secondsTimeout;
			//diff.Match_Threshold = 0;
			//diff.Match_Distance = 20;
			//http://code.google.com/p/google-diff-match-patch/wiki/API
			//Perform diff
			LinkedList<Diff> diffs = diff.diff_main(oldText, newText,enableLineOptimization); //Turn of line-based optimization
			
			//Add a terminator item... 
			Diff terminator = new Diff(null, null);
			diffs.add(terminator);
			
			//Our operation queue... we only flush operations when we hit 'equal' or terminator. 
			List<Diff> queuedOps = new ArrayList<Diff>(50);
			
			int ix = start; //char index
			int lastChangedToken = -1; //token index
			
			StringBuilder temp = new StringBuilder(100); //Temp array used to concat inserts.
			
			//The first diff segment inserted or changed gets different treatment
			boolean firstOp = true; //For smart snapping...
			
			for (Diff d:diffs){
				
				//on EQUAL or END, flush queuedOps, joining them all into a single replace.
				if (queuedOps.size() > 0 && (d.operation == Operation.EQUAL || d == terminator)){
					
					//Track how much we need to delete
					int delLength = 0;
					 //Clear string buffer.
					temp.delete(0, temp.length());
					
					for (Diff qi:queuedOps){
						if (qi.operation == Operation.INSERT){
							temp.append(qi.text); //Concat all our inserts into a single replace statement. Usually there will only be one insert in  queuedOps list.
						}else if (qi.operation == Operation.DELETE){
							delLength += qi.text.length();
						}
					}
					//Do real work. All work is sequential, so we can use multiReplace() for much better performance.
					//Snapping is important for inserts and change... 
					//If we are replacing text, we put it in the same starting token. 
					//If we are inserting text, we place it in the previous token (unless it is the first segment)
					lastChangedToken = this.multiReplace(ix, delLength, temp.toString(), lastChangedToken,firstOp || delLength > 0);
					
					//Increment ix, using the summed length of the inserts.
					ix += temp.length();
					//Clear queuedOpts
					queuedOps.clear();
				}
				
				if (d.operation == Operation.EQUAL){
					ix += d.text.length(); //Increment index.
					firstOp = false; //After the first matching segment, we turn OFF snapNext for insertions (not replaces)
				}else{
					//Queue to queuedOps.
					queuedOps.add(d);
				}
				
			}
		}
		/**
		 * Pulls all the text in the specified range into a single token, at the hierarchy location of the first character. The finished token may have original text at the beginning or end that existed before.
		 * Calls this.replace(start, length, text.substring(start, start + length));
		 * @param start
		 * @param length
		 */
		public void flattenArea(int start, int length){
			this.replace(start, length, text.substring(start, start + length));
		}
		
		
		/*S&R is difficult when you have tags mixed in with text. A misapplied field can cause a relevant piece of text to not match. 
		 * Entities are also separate tokens, as per XML... but this also makes it impossible to run regexes on individual text tokens.
	Here's my idea for implementation.
	Build a CharSequence? implementation that concatenates all the text and entities within an XML node 
	(probably at the record level, unless the S&R specifies a CSS selector, like span.editorNotes.... in which case that will be the context.
	A table of char index->XmlToken? references will be kept alongside.
	Standard Java Regex instances can be used for searching, but replacing will require a custom method.

	The custom replace method will take the old text, the new text, and diff them. Only portions that 
	differ will be modified. Each differing section will be deleted and replaced. 

	If there is a table entry at the first char of the section, it will be kept. Subsequent 
	entries will be shifted to after the last character of the new segment. Differences in 
	length have to be applied to the other later indexes of course.
	Cycling back through the table, it is easy to modify the attached XmlTokens? to use 
	the new contents... The contents for a section extend from the start index for it, until the next index. 
	A pass to delete empty text/entity tokens should be made ASAP.
	*/
		
		
		
		





		
		
		/**
	     * Returns a literal replacement <code>String</code> for the specified
	     * <code>String</code>.
	     *
	     * This method produces a <code>String</code> that will work
	     * as a literal replacement <code>s</code> in the
	     * <code>appendReplacement</code> method of the {@link Matcher} class.
	     * The <code>String</code> produced will match the sequence of characters
	     * in <code>s</code> treated as a literal sequence. Slashes ('\') and
	     * dollar signs ('$') will be given no special meaning.
	     *
	     * @param  s The string to be literalized
	     * @return  A literal string replacement
	     * @since 1.5
	     */
	    public static String quoteReplacement(String s) {
	        return Matcher.quoteReplacement(s);
	    }

	    /**
	     * Replaces every subsequence of the input sequence that matches the
	     * pattern with the given replacement string. 
	     *
	     * <p> Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	     * the replacement string may cause the results to be different than if it
	     * were being treated as a literal replacement string. Dollar signs may be
	     * treated as references to captured subsequences as described above, and
	     * backslashes are used to escape literal characters in the replacement
	     * string.
	     *
	     * <p> Given the regular expression <tt>a*b</tt>, the input
	     * <tt>"aabfooaabfooabfoob"</tt>, and the replacement string
	     * <tt>"-"</tt>, an invocation of this method on a matcher for that
	     * expression would yield the string <tt>"-foo-foo-foo-"</tt>.
	     *
	     *
	     * @param  replacement
	     *         The replacement string
	     *
	     * @return  itself. 
	     */
	    public VirtualCharSequence replaceAll(Pattern p, String replacement, boolean smartMerging) {
	    	Matcher m = p.matcher(this);
	        m.reset();
	        boolean result = m.find();
	        if (result) {
	            do {
	                int startAt = replaceMatch(m, replacement,smartMerging); //This modifies the underying CharSequence. We need to clear
	                m.reset(); //Cleanup
	                result = m.find(startAt); //Start where we left off.
	            } while (result);
	        }
	        return this;
	    }
	    public VirtualCharSequence replaceAll(String regex, String replacement, boolean smartMerging) {
	    	VirtualCharSequence sw = replaceAll(Pattern.compile(regex),replacement, smartMerging);
	    	/*if(deletedText!= null){
	    		System.out.println();
				for(String t : deletedText){
					System.out.print(t + ",\t");
				}
			}*/
	    	return sw;
	    }
	    
	    /**

	     * <p> This method performs the following actions: </p>
	     *
	     * <ol>
	     *
	     *   <li><p> It computes the replacement string for the current match. </p></li>
	     *
	     *   <li><p> It replaces the match with the replacement string in this instance using .replace.
	     *   </p></li>
	     *
	     *   <li><p> It returns the index of the end of the match *after* replacement... i.e, where to start looking for the next match, with changes in length factored in.
	     *   </p></li>
	     *
	     * </ol>
	     *
	     * <p> The replacement string may contain references to subsequences
	     * captured during the previous match: Each occurrence of
	     * <tt>$</tt><i>g</i><tt></tt> will be replaced by the result of
	     * evaluating {@link #group(int) group}<tt>(</tt><i>g</i><tt>)</tt>. 
	     * The first number after the <tt>$</tt> is always treated as part of
	     * the group reference. Subsequent numbers are incorporated into g if
	     * they would form a legal group reference. Only the numerals '0'
	     * through '9' are considered as potential components of the group
	     * reference. If the second group matched the string <tt>"foo"</tt>, for
	     * example, then passing the replacement string <tt>"$2bar"</tt> would
	     * cause <tt>"foobar"</tt> to be appended to the string buffer. A dollar
	     * sign (<tt>$</tt>) may be included as a literal in the replacement
	     * string by preceding it with a backslash (<tt>\$</tt>).
	     *
	     * <p> Note that backslashes (<tt>\</tt>) and dollar signs (<tt>$</tt>) in
	     * the replacement string may cause the results to be different than if it
	     * were being treated as a literal replacement string. Dollar signs may be
	     * treated as references to captured subsequences as described above, and
	     * backslashes are used to escape literal characters in the replacement
	     * string.
	     *

	     */
	    public int replaceMatch(Matcher m, String replacement, boolean smartMerging ) {
	    	int testMatch = m.start(); //Throws an IllegalStateException if no match has been found.
	    	
	    	String newText = computeReplacementString(m,replacement);
	    	
	    	if (smartMerging)
	    		this.replaceSmart(m.start(), m.end() - m.start(), newText);
	    	else
	    		this.replace(m.start(), m.end() - m.start(), newText);
	    	
	    	return m.start() + newText.length();
	    }

	    /**
	     * Borrowed from the Matcher class and modified.
	     * @param m
	     * @param replacement
	     * @return
	     */
	    private String computeReplacementString(Matcher m, String replacement){
	        int cursor = 0;
	        String s = replacement;
	        StringBuffer result = new StringBuffer();

	        while (cursor < replacement.length()) {
	            char nextChar = replacement.charAt(cursor);
	            if (nextChar == '\\') {
	                cursor++;
	                nextChar = replacement.charAt(cursor);
	                result.append(nextChar);
	                cursor++;
	            } else if (nextChar == '$') {
	                // Skip past $
	                cursor++;

	                // The first number is always a group
	                int refNum = (int)replacement.charAt(cursor) - '0';
	                if ((refNum < 0)||(refNum > 9))
	                    throw new IllegalArgumentException(
	                        "Illegal group reference");
	                cursor++;

	                // Capture the largest legal group string
	                boolean done = false;
	                while (!done) {
	                    if (cursor >= replacement.length()) {
	                        break;
	                    }
	                    int nextDigit = replacement.charAt(cursor) - '0';
	                    if ((nextDigit < 0)||(nextDigit > 9)) { // not a number
	                        break;
	                    }
	                    int newRefNum = (refNum * 10) + nextDigit;
	                    if (m.groupCount() < newRefNum) {
	                        done = true;
	                    } else {
	                        refNum = newRefNum;
	                        cursor++;
	                    }
	                }

	                // Append group
	                if (m.group(refNum) != null)
	                    result.append(m.group(refNum));
	            } else {
	                result.append(nextChar);
	                cursor++;
	            }
	        }
	        return result.toString();
	    }
	    /**
	     * Trims whitespace from the beginning. The char sequence may be empty after this call, and may throw an exception if you try to insert or replace anything.
	     */
		public VirtualCharSequence trimStart() {
			this.replaceAll("\\A\\s+", "", false);
			return this;
		}
	    /**
	     * Trims whitespace from the end. The char sequence may be empty after this call, and may throw an exception if you try to insert or replace anything.
	     */
		public VirtualCharSequence trimEnd() {
			this.replaceAll("\\s+\\z", "", false);
			return this;
		}

		/**
		 * Trims whitespace from beginning and end. The char sequence may be empty after this call, and may throw an exception if you try to insert or replace anything.
		 */
		public VirtualCharSequence trim(){
			trimStart();
			trimEnd();
			return this;
		}
		public VirtualCharSequence append(String newText) {
			this.insert(this.length(), newText);
			return this;
		}
		
	}


