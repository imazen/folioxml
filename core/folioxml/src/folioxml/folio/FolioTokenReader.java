package folioxml.folio;

import folioxml.core.FileIncludeResolver;
import folioxml.core.IIncludeResolutionService;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a series of FolioToken instances from the specififed Reader input stream.
 * Fetches DI and FI preprocessor includes using the specified IIncludeResolutionService.
 *
 * @author nathanael
 */
public class FolioTokenReader extends folioxml.core.TokenReaderBase {


    /**
     * @param reader
     * @param readBlockSize Should (optimally) be the length of the largest comment or text segment in the file.
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public FolioTokenReader(Reader reader, int readBlockSize) {
        super(reader, readBlockSize);

    }

    /**
     * Uses the Windows-1252 encoding
     */
    public FolioTokenReader(File path) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        this(new InputStreamReader(new FileInputStream(path), "Windows-1252"), new FileIncludeResolver(path.getAbsolutePath()));

    }


    /**
     * @param reader
     * @param readBlockSize Should (optimally) be the length of the largest comment or text segment in the file.
     */
    public FolioTokenReader(Reader reader, IIncludeResolutionService referenceResolver) throws IOException {
        this(reader, READ_SIZE_DEFAULT, referenceResolver, null);

    }

    /**
     * @param reader
     * @param readBlockSize Should (optimally) be the length of the largest comment or text segment in the file.
     */
    public FolioTokenReader(Reader reader, int readBlockSize, IIncludeResolutionService referenceResolver) throws IOException {
        this(reader, readBlockSize, referenceResolver, null);
    }

    /**
     * @param reader              A FileReader or BufferedReader containing Folio Flat File document or definition codes.
     * @param readBlockSize       How much data to add to the buffer from 'reader' when more data is needed. The buffer is not fixed size, and will
     *                            strech as needed for a large token (such as a massive comment). The buffer will first clean out used-up space/
     * @param referenceResolver
     * @param parentDocumentPaths
     * @throws java.io.IOException
     */
    public FolioTokenReader(Reader reader, int readBlockSize, IIncludeResolutionService referenceResolver, List<String> parentDocumentPaths) throws IOException {
        super(reader, readBlockSize);
        this.resolver = referenceResolver;
        this.parentDocumentPaths = parentDocumentPaths;
        //Add the default element to parentDocumentPaths
        if (this.resolver != null) {
            //Cannot be null if resolver exists
            if (this.parentDocumentPaths == null) this.parentDocumentPaths = new ArrayList<String>();
            //Add base document path always - we don't want 2nd level files to re-reference the first.
            if (!this.parentDocumentPaths.contains(resolver.getHash())) {
                this.parentDocumentPaths.add(resolver.getHash());
            }
        }
    }

    public FolioTokenReader(Reader reader) {
        this(reader, READ_SIZE_DEFAULT);
    }

    /**
     * If initialized, this class will be used to perform on-the-fly file includes.
     */
    private IIncludeResolutionService resolver = null;

    /**
     * Used to track and prevent circular references
     */
    private List<String> parentDocumentPaths = null;

    /**
     * Used to read in included files. Null when finished.
     */
    private FolioTokenReader currentInnerReader = null;

    /**
     * Jan 20, 09. Can't use posessive quantifiers here - sorry.
     */
    public static String CommentRegex = "<CM>(.*?)</CM>";
    /**
     * Matches a comment tag and any intermediate comments. Lazy, of course.
     */
    private static Pattern rComment = Pattern.compile("^" + CommentRegex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Jan 20, 09 : Added possessive quantifiers throughout. previously (?:[^<]+|<[^A-Za-z/])+
     */
    public static String TextRegex = "(?:[^<]++|<[^A-Za-z/])++";
    /**
     * Matches text that doesn't contain any open brackets that are directly followed by a letter or a closing slash.
     */
    private static Pattern rText = Pattern.compile("^" + TextRegex); //non <, expect doubles


    /**
     * Matches any two-letter tag (and +/-), and captures (optional) options. group 1 and 2, respectively.
     * Tag options must have matching quote pairs, (single quotes are encoded like "").
     * Opening brackets can be entered by entering two.
     * Opening and closing brackets can be used literally as long as they exist in pairs, are not nested, and don't contain quotes.
     * Opening and closing brackets can be used arbitrarily within a quoted string.
     * <BR:AL:0.15,0.0291667,FC:255,255, caused problems.
     * Jan 20, 09. Added posessive quantifiers throughout regex.
     */
    public static String TagRegex = "<(/)?+([A-Z-a-z][A-Z-a-z][\\+\\-]?+)(?:\\s*+[:,;]++\\s*+((?:[^><\"]++|<<|\"(?:[^\"]|(?:\"\"))*+\"|<[^<>\"]*+>)++))?+>";

    /* old regex 86 seconds on <BR:AL:0.15,0.0291667,FC:255,255,
      public static String TagRegex = "<(/)?([A-Z-a-z][A-Za-z][\\+\\-]?)(?:\\s*[:,;]+\\s*((?:[^><\"]+|<<|\"(?:[^\"]|(?:\"\"))*\"|<[^<>\"]*>)+?))?>";
    */


    private static Pattern rTag = Pattern.compile("^" + TagRegex);

    /**
     * An array of the patterns we look for, in the correct order.
     */
    private static Pattern[] tokenPatterns = new Pattern[]{rText, rComment, rTag}; //rComment should come before rTag, since rTag matches opening comment tags.

    protected Pattern[] getTokenPatterns() {
        return tokenPatterns;
    }

    /**
     * Matches any single open bracket. Uses negative lookahead and lookbehind assertions
     */
    private static Pattern rSingleBracket = Pattern.compile("^(?<!\\<)<(?!\\<)");

    public long tokensRead = 0;

    public FolioToken read() throws IOException, InvalidMarkupException {
        tokensRead++;

        //Delegate if ready. Delete reference when done
        if (this.currentInnerReader != null) {
            FolioToken st = this.currentInnerReader.read();
            if (st != null) return st;
            else {
                this.currentInnerReader.close();
                this.currentInnerReader = null;
            }
        }

        //Store current position. After getNextMatch() is called, these values will be incremented to the *next* token.
        TokenInfo ti = tracker.getTokenInfo();


        //Or read from main stream
        Matcher m = getNextMatch();

        if (m == null) return null; //eof

        FolioToken ft = null;


        //Build comment tokens
        if (m.pattern() == rComment) {
            ft = new FolioToken(FolioToken.TokenType.Comment);
            ft.text = m.group(1);

            //Build text tokens
        } else if (m.pattern() == rText) {
            ft = new FolioToken(FolioToken.TokenType.Text);
            ft.text = m.group();

            //Check for single brackets (not pairs). They shouldn't be in text, so while we parse them, we call a warning.
            Matcher msb = rSingleBracket.matcher(ft.text);
            //Uncommented originally
            //while (msb.find()){
            //   msb.start();//TODO warning
            //}

        } else if (m.pattern() == rTag) {
            ft = new FolioToken(FolioToken.TokenType.Tag);
            if (m.group(1) != null) {
                ft.isClosing(true);
            }
            ft.text = m.group();
            ft.tagName = m.group(2);
            //ft.stackID = ft.tagName;
            ft.tagOptions = m.group(3);
        }

        //Save debugging info
        ft.info = ti;
        ft.info.length = m.end() - m.start();
        ft.info.parentService = this.resolver;
        if (m.pattern() == rComment)
            ft.info.text = m.group();
        else
            ft.info.text = ft.text; //it's already parsed

        index = m.end();

        //Check for stray comment tags.
        if (ft.matches("CM")) {
            throw new InvalidMarkupException("Comment tags cannot specify options and must be present in pairs.", ft);
        }

        //We may have locking issues here...
        //Check for includes (if we have a resolver)
        if (ft.type == FolioToken.TokenType.Tag) {
            //Insert both Definition and Flat File includes inline. We parse them the same
            if (ft.matches("DI|FI")) {
                if (this.resolver == null) {
                    throw new InvalidMarkupException("File include requested, but no IncludeResolutionService was specified.", ft);
                } else {
                    assert (ft.count() == 1);
                    String path = ft.get(0);
                    IIncludeResolutionService child = this.resolver.getChild(path);
                    //Check for circular references!!!
                    String hash = child.getHash();
                    if (this.parentDocumentPaths.contains(hash)) {
                        //That's right, the child is the circular reference that is also the parent.
                        throw new InvalidMarkupException("Circular reference: " + this.resolver.getDescription() + " contains a reference to parent document " + child.getDescription() + "... which is including " + this.resolver.getDescription());

                    } else {
                        Reader r = child.getReader();
                        List<String> newPathChain = new ArrayList<String>();
                        newPathChain.addAll(this.parentDocumentPaths);
                        newPathChain.add(hash);
                        this.currentInnerReader = new FolioTokenReader(r, this.readSize, child, newPathChain);
                        return this.read(); //Recursive - we've set up the delegation reader, so re-call this function. If the file is empty, it will start where it leftoff.
                    }
                }
            }
        }


        return ft;

    }

    @Override
    public boolean canRead() {
        if (this.currentInnerReader != null && this.currentInnerReader.canRead()) return true;
        return super.canRead();
    }

    @Override
    public void close() throws IOException {
        if (this.currentInnerReader != null) {
            this.currentInnerReader.close();
            this.currentInnerReader = null;
        }

        super.close();
    }
}