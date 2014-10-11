package folioxml.lucene.folioQueryParser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.junit.Test;
import folioxml.lucene.analysis.folio.FolioEnuAnalyzer;
import folioxml.lucene.analysis.folio.FolioEnuPhraseAnalyzer;
import folioxml.core.InvalidMarkupException;

import java.io.*;

public class QueryParserTest {


	@Test
	public void TestQueryList() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException, ParseException{
		TestQueryList(new StandardAnalyzer(Version.LUCENE_33), "queries-fixed.txt");
	}
	
	@Test
	public void TestQueryListFolio() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException, ParseException{
		TestQueryList(new FolioEnuAnalyzer(), "queries-fixed.txt");
	}
	
	@Test
	public void TestQueryListFolioPhrase() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException, ParseException{
		TestQueryList(new FolioEnuPhraseAnalyzer(), "queries-fixed.txt");
	}
	
	public void TestQueryList(Analyzer textAnalyzer, String sourceFile) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException, ParseException{
		

		QueryParser qp = new QueryParser(textAnalyzer, "contents");
	    BufferedReader br  = null;
	    try{
	    	br = new BufferedReader(new FileReader(sourceFile));  
	    	String line = null;  
	    	double failures = 0;
	    	double success = 0;
	    	while ((line = br.readLine()) != null && failures < 100000)  
	    	{  
	    	   try{
	    		   
	    		   Query q = qp.parse(line.trim());
	    		   if (q == null) {
	    			   System.out.println("Null result for: " + line);
	    			   failures++;
	    		   }
	    		   else if (q.toString().trim().length() == 0){
	    			   System.out.println("Parsing this made an empty query: "  + line);
	    		   }else{
	    			   success ++;
	    			   
	    		   }
	    		   if (success % 1000 == 0) System.out.println(success + " sucesses.");
	    	   }catch(InvalidMarkupException ex){
	    		   System.out.println("Failed on: " + line);
	    		   System.out.println(ex.getMessage());
	    		   failures ++;
	    	   }
	    	} 
	    	double total = failures + success;
	    	System.out.println("Failure on " + failures + " of " + total +  " (" + (Math.round((failures / success) * 10000) / 100) + "%)");
	    }finally{
	    	br.close();
	    }
	}

	
}
