package folioxml.core;

import folioxml.utils.Stopwatch;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for tokenizing readers which need to operate on a stream instead of a s String. Provides line/col counting, buffering, and regex matching. Uses the .hitEnd() property of regexes to determine whether more data needs to be buffered
 * for a complete token match.
 * @author nathanael
 */
public abstract class TokenReaderBase{

    /**
     * Subclasses should override this, and return an array of the token patterns for getNextMatch() to iterate through. They must start with \\G. (using ^ instead bug issue with \\G!! in java.util.regex!! Order only matters if one of the patterns is a subset of another - like tags are with comments.
     * @return
     */
    protected abstract Pattern[] getTokenPatterns();

    /**
     * The number of chars that get transferred at a time when more data is needed. Should optimally contain the largest comment or plaintext segment in a file
     * This much contiguous memory must be available
     */
    protected int readSize = 0;
    protected static int READ_SIZE_DEFAULT = 2048;//32768;

    /**
     * The index within 'textWindow' that we are parsing at. Increases each time a token is parsed, resets to 0 on cleanup of textWindow.
     */
    protected int index = 0;
    /**
     * Dynamically growing parsing window. New data is pulled in when a regex hits the end (regardless of success).
     *
     */
    protected StringBuilder textWindow = null;
    /**
     * The underlying reader
     */
    protected Reader reader;


    /**
     * True if reader is at end of file. Becomes true when a reader.read() returns no data. Doesn't mean that there isn't data left in textWindow
     */
    protected boolean atEOF = false;

    /**
     * Used to transfer data from the underlying reader.
     */
    protected char[] readerToBufferBuffer = null;

    /**
     * Keeps track of our line/col position within the reader.
     */
    protected LineColTracker tracker = null;

    /**
     * The token regexps. Cached from getTokenPatterns()
     */
    private Pattern[] tokenPatterns = null;
    /**
     *
     * @param reader Should be at position 0. The token reader tracks line/col positions at the tokenizing level, so offset readers or any interference will throw that off.
     * @param readBlockSize Should (optimally) be the length of the largest comment or text segment in the file.
     */
    public TokenReaderBase(Reader reader, int readBlockSize){

        this.reader = reader;
        //Initialize buffers
        this.readSize = readBlockSize;
        this.textWindow = new StringBuilder(this.readSize * 2);
        this.readerToBufferBuffer = new char[this.readSize];
        this.tokenPatterns = getTokenPatterns();
        this.tracker = new LineColTracker();

    }

    public TokenReaderBase(Reader reader){
       this(reader,READ_SIZE_DEFAULT);
    }
    /**
     * Returns false after a read() call returns null. Sometimes returns false without a failed read() call, for example
     * if the last read() call discoveres the end-of-file but still succeeds. Always null-check read()
     */
    public boolean canRead(){
        if (atEOF && index >= textWindow.length()) return false;
        else return true;
    }

   /* public Reader getReader(){
        return reader;
    }*/
        /**
     * Closes the underlying reader.
     * @throws java.io.IOException
     */
    public void close() throws IOException{
        reader.close();
    }

    public Stopwatch bufferTime = new Stopwatch();
    public Stopwatch matchTime = new Stopwatch();
    public Stopwatch getNextMatchTime = new Stopwatch();
    public int getNextMatchLoops = 0;
    public int matchLoops = 0;

    /**
     * Returns true if more text was added to textWindow. Returns false if EOF.
     * Cleans up whenver a realloc is pending. Cleanup will cause index = 0;
     * @return
     */
    protected boolean bufferMore() throws IOException{
        bufferTime.start();

        //Fill as much of readerToBufferBuffer as possible, returns the number of characters filled.
        int result = reader.read(readerToBufferBuffer);
        //Check for end of file
        atEOF = (result < 1);
        if (!atEOF){
            //Determine the most efficient way. If we already have enough space, let StringBuilder do its thing.
            if (result <= textWindow.capacity() - textWindow.length()){
                //Add - we already have the allocated space.
                textWindow.append(readerToBufferBuffer,0,result);
            }else{
                //The StringBuilder would have to reallocate to add the new data
                //Let's try to avoid that.

                //Can we make enough room by cleaning up?
                if (result <= index){
                    textWindow.delete(0, index);
                    textWindow.append(readerToBufferBuffer,0,result);
                }else{
                    //Looks like we have to reallocate.
                    //Let's clean up at the same time, so we don't get an insanely long string

                    //calculate the minimum amount of space needed to hold the unparsed+new data
                    int minSize = textWindow.length() - index + result;

                    //Double it. Once a StringBuilder is big enough, this shouldn't run again - cleanup will happen as a delete.
                    StringBuilder newSB = new StringBuilder(minSize * 2);
                   // System.out.println("Reallocated StringBuilder from " + textWindow.capacity() + " to " + newSB.capacity());
                    newSB.append(textWindow, index, textWindow.length());
                    newSB.append(readerToBufferBuffer,0,result);
                    textWindow = newSB;
                }
                index = 0;
            }
        }
        bufferTime.stop();

        return atEOF;

    }




    /**
     * Returns null if we have already parsed the last token.
     * WATCH OUT!!!! textWindow is mutable, so Matcher instances will become corrupt if bufferMore() is called.
     * @return
     */
    protected Matcher getNextMatch() throws IOException, InvalidMarkupException{
        //If we're out of text in the reader and the buffer, return null
        if (!canRead())
            return null;
        
        getNextMatchTime.start();
        getNextMatchLoops++;
        try{
        	
            Matcher match = null;

            //If the buffer is empty, we really need to pull in more data
            boolean needsData = (index >= textWindow.length()) && !atEOF;

            do{
                if (needsData){
                    //if (atEOF) throw new Exception();
                    assert(!atEOF); //Should never be set true if we're at the end of the file.
                    bufferMore();
                    //If we're out of text in the reader and the buffer, return null
                    if (!canRead())
                        return null;
                    needsData = false;
                }
                //Loop through the token types, add data when needed.
                for(int i = 0; i < tokenPatterns.length; i++){
                    //Create a matcher for the current textWindow
                    Matcher m = tokenPatterns[i].matcher(textWindow);
                    //Optimizer note: Matchers are only cacheable if the pattern and text are the same. You're only saving on the initialization cost of 2 tiny int[] arrays. Not worth it.
                    //Seek for a match
                    //matchTime.reset();
                    
                    long start = matchTime.hasValue() ? matchTime.toValue() : 0;
                    matchTime.start();
                    matchLoops++;
                    m.reset();
                    m.region(index, textWindow.length());
                    boolean isMatch = m.find();

                    matchTime.stop();
                    if (matchTime.toValue() - start > 1000) {
                    	String region = textWindow.substring(index,textWindow.length());
                    	String all = textWindow.toString();
                    	System.out.println(region);
                    	System.out.println(all);
                    	System.out.println(m.pattern().toString());
                    	System.out.println(matchTime.toValue() + " ms");
                    	//assert(false);
                    }

                    //If the regex bumped into the end of textWindow, buffer more text and try again.
                    if (m.hitEnd() && !atEOF){
                       needsData = true;
                       break;

                    }
                    //Only return a successful match that doesn't hit the end (or hits eof)
                    if (isMatch){
                        match = m;
                        break;
                    }
                }

            }while(needsData);

            //invalid token
            if (match == null && canRead()){
                int end = index + 20;
                if (end >= textWindow.length()) end = textWindow.length();
                throw new InvalidMarkupException("Invalid token:" + textWindow.substring(index,end ));
                //assert(false); //Invalid token
            }
            if (!canRead()){
                assert(true);
            }

            
            //Increment line/col numbers
            if (match != null) tracker.add(textWindow, match.start(), match.end());
            
            return match;
        }finally{
        	getNextMatchTime.stop();
        }
    }


}