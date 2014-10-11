import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTestHarness {

    ///folio/FolioTokenReader.java
    public static String CommentRegex = "<CM>(.*?)</CM>";
    //Try putting the ^ back in... And calling .region(index, end) 
    //instead if passing in index to the find() method. Leave the find() method blank, but re-create the matchers each time...
    public static String RegexPrefix = "^";
    /**
     * Matches a comment tag and any intermediate comments. Lazy, of course.
     */
    private static Pattern rComment = Pattern.compile(RegexPrefix + CommentRegex,
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static String TextRegex = "(?:[^<]+|<[^A-Za-z/])+";
    /**
     * Matches text that doesn't contain any open brackets that are directly
     * followed by a letter or a closing slash.
     */
    private static Pattern rText = Pattern.compile(RegexPrefix + TextRegex); // non <,
    // expect
    // doubles

    /**
     * Matches any two-letter tag (and +/-), and captures (optional) options.
     * group 1 and 2, respectively. Tag options must have matching quote pairs,
     * (single quotes are encoded like ""). Opening brackets can be entered by
     * entering two. Opening and closing brackets can be used literally as long
     * as they exist in pairs, are not nested, and don't contain quotes. Opening
     * and closing brackets can be used arbitrarily within a quoted string.
     */
    public static String TagRegex = "<(/)?([A-Z-a-z][A-Za-z][\\+\\-]?)(?:\\s*[:,;]+\\s*((?:[^><\"]+|<<|\"(?:[^\"]|(?:\"\"))*\"|<[^<>\"]*>)+?))?>";

    private static Pattern rTag = Pattern.compile(RegexPrefix + TagRegex);

    /**
     * An array of the patterns we look for, in the correct order.
     */
    private static Pattern[] tokenPatterns = new Pattern[]{rText, rComment,
            rTag}; // rComment should come before rTag, since rTag matches

    // opening comment tags.

    protected Pattern[] getTokenPatterns() {
        return tokenPatterns;
    }

    @Test
    public void testCharBuffer() throws Exception {
        testRegex(cbData, 1);
    }

    @Test
    public void testStringBuffer() throws Exception {
        testRegex(sbData, 1);
    }

    @Test
    public void testStringBuilder() throws Exception {
        testRegex(sbuData, 1);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    private static void testRegex(CharSequence text, int iterations) {
        // start time
        int index = 0;
        long start = System.nanoTime();
        Matcher[] matchers = new Matcher[tokenPatterns.length];
        for (int i = 0; i < tokenPatterns.length; i++) {
            matchers[i] = tokenPatterns[i].matcher(text);
        }

        for (int i = 0; i < iterations; i++) {
            index = 0;// reset window

            boolean found = false;
            do {
                found = false;
                for (int j = 0; j < matchers.length; j++) {
                    Matcher m = matchers[j];
                    m.reset();
                    m = m.region(index, text.length());
                    if (m.find()) {
                        index = m.end();
                        found = true;
                        //System.out.println(m.group());
                        break;
                    }
                }
            } while (found);

            if (index != text.length()) {

                if (index < 0){
                    throw new Error();
                }
                CharSequence next = null;
                if (index + 51 > text.length())
                    next = text.subSequence(index, text.length());
                else
                    next = text.subSequence(index, index + 50);
//		
                throw new Error("Failed to parse token at " + next);
            }


        }
        // end time
        long end = System.nanoTime();
        System.err.println(end - start + "| Time Elapsed in milliseconds for "
                + text.getClass().getSimpleName() + " implementation");
    }

    private static CharBuffer cbData;
    private static StringBuffer sbData;
    private static StringBuilder sbuData;

    @BeforeClass
    public static void setUp() throws Exception {
        // Here's the actual code used to perform the matching at a certain
        // character index.

        // filter(pFilter, filename, printOutput);
        cbData = useCharBuffer();
        sbData = useStringBuffer();
        sbuData = useStringBuilder();
    }

    @After
    public void tearDown() throws Exception {

    }

    private static CharBuffer useCharBuffer() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(
                folioxml.utils.ConfUtil.getFFFPath("folio-help")));
        char[] whole = new char[0]; //bug if file < 8192 chars
        char[] chars = new char[32000];
        int numRead = 0;
        while ((numRead = reader.read(chars)) > -1) {
            char[] newWhole = new char[whole.length + numRead];
            System.arraycopy(whole, 0, newWhole, 0, whole.length);
            System.arraycopy(chars, 0, newWhole, whole.length, numRead);
            whole = newWhole;
        }

        reader.close();
        return CharBuffer.wrap(whole);
    }

    private static StringBuffer useStringBuffer() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(
                folioxml.utils.ConfUtil.getFFFPath("folio-help")));
        StringBuffer sb = new StringBuffer(8192);
        char[] chars = new char[8192];
        int numRead = 0;
        while ((numRead = reader.read(chars)) > -1) {
            sb.append(String.valueOf(chars,0,numRead));
        }

        reader.close();
        return sb;
    }

    private static StringBuilder useStringBuilder() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(
                folioxml.utils.ConfUtil.getFFFPath("folio-help")));
        StringBuilder sb = new StringBuilder(8192);
        char[] chars = new char[8192];
        int numRead = 0;
        while ((numRead = reader.read(chars)) > -1) {
            sb.append(String.valueOf(chars,0,numRead));
        }

        reader.close();
        return sb;
    }

}
