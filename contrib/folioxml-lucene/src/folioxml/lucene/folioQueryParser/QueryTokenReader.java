package folioxml.lucene.folioQueryParser;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenInfo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 
 * @author nathanael
 */
public class QueryTokenReader extends folioxml.core.TokenReaderBase{

	public QueryTokenReader(String s) {
		this(new StringReader(s),s.length());
	}
    public QueryTokenReader(Reader reader) {
        this(reader,READ_SIZE_DEFAULT);
    }
    public QueryTokenReader(Reader reader, int readBlockSize) {
        super(reader,readBlockSize);
    }
    protected Pattern[] getTokenPatterns(){
        return QueryToken.tokenPatterns;
    }

    public QueryToken read() throws IOException, InvalidMarkupException{
    	//Store current position. After getNextMatch() is called, these values will be incremented to the *next* token.
        TokenInfo ti = tracker.getTokenInfo();
        //Or read from main stream
        Matcher m = getNextMatch();
        if (m == null) return null; //eof
        QueryToken qt = new QueryToken(m.pattern(), m.group());
        //Save debugging info
        qt.info = ti;
        qt.info.length = m.end() - m.start();
        index = m.end();
        return qt;
    }
    public List<QueryToken> readAll() throws IOException, InvalidMarkupException{
    	ArrayList<QueryToken> items = new ArrayList<QueryToken>();
    	QueryToken t = null;
    	do{
    		t = read();
    		if (t != null) items.add(t);
    	}while(t != null);
    	return items;
    }
}