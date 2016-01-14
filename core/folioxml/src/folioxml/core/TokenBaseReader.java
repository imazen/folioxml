package folioxml.core;

import folioxml.core.TokenBase.TokenType;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a series of FolioToken instances from the specififed Reader input stream.
 * Fetches DI and FI preprocessor includes using the specified IIncludeResolutionService.
 *
 * @author nathanael
 */
public class TokenBaseReader<T extends TokenBase> extends folioxml.core.TokenReaderBase {

    /**
     * Uses the UTF-8 encoding
     */
    public TokenBaseReader(File path) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        this(new InputStreamReader(new FileInputStream(path), "UTF-8"));

    }

    public TokenBaseReader(Reader reader) {
        this(reader, READ_SIZE_DEFAULT);
    }

    public TokenBaseReader(Reader reader, int readBlockSize) {
        super(reader, readBlockSize);

    }


    private static Pattern rComment = Pattern.compile("^" + TokenBase.RegexComment, Pattern.DOTALL);
    private static Pattern rText = Pattern.compile("^" + TokenBase.RegexText);
    private static Pattern rTag = Pattern.compile("^" + TokenBase.RegexTag);
    private static Pattern rEntity = Pattern.compile("^" + TokenBase.RegexEntity);

    /**
     * An array of the patterns we look for, in the correct order.
     */
    private static Pattern[] tokenPatterns = new Pattern[]{rText, rEntity, rTag, rComment};

    protected Pattern[] getTokenPatterns() {
        return tokenPatterns;
    }

    @SuppressWarnings("unchecked")
    public T read(T blankToken) throws IOException, InvalidMarkupException {


        //Store current position. After getNextMatch() is called, these values will be incremented to the *next* token.
        TokenInfo ti = tracker.getTokenInfo();

        //Or read from main stream
        Matcher m = getNextMatch();

        if (m == null) return null; //eof

        TokenBase t = blankToken;

        //Set the matched markup
        t.markup = m.group();

        //Set the type
        if (m.pattern() == rComment) {
            t.type = TokenType.Comment;
        } else if (m.pattern() == rText) {
            t.type = TokenType.Text;
        } else if (m.pattern() == rEntity) {
            t.type = TokenType.Entity;
        } else if (m.pattern() == rTag) {
            t.type = TokenType.Tag;
            t.parseTagFromMatcher(m);
        }

        //Save debugging info
        //t.info = ti;
        //t.info.length = m.end() - m.start();
        //TODO: Xml parsing needs line numbers too... We should probably add .info to TokenBase. 

        index = m.end();

        return (T) t;

    }

}