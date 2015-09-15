package folioxml.lucene.folioQueryParser;

import folioxml.core.InvalidMarkupException;
import folioxml.lucene.analysis.folio.FolioEnuAnalyzer;
import folioxml.lucene.analysis.folio.FolioEnuPhraseAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Query;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;

public class QueryParserTest {


    @Test
    @Ignore
    public void TestQueryList() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        TestQueryList(new StandardAnalyzer(), "queries-fixed.txt");
    }

    @Test
    @Ignore
    public void TestQueryListFolio() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        TestQueryList(new FolioEnuAnalyzer(), "queries-fixed.txt");
    }

    @Test
    @Ignore
    public void TestQueryListFolioPhrase() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        TestQueryList(new FolioEnuPhraseAnalyzer(), "queries-fixed.txt");
    }

    @Test
    public void TestQueryContents() throws IOException, InvalidMarkupException {

        QueryParser qp = new QueryParser(new StandardAnalyzer(), "contents");
        qp.parse("[Contents 'hello']");
        qp.parse("[Contents a,b,c]");
        qp.parse("[Contents 'hello (abc)']");
        qp.parse("[Contents 'hello (QTE)']");
        qp.parse("[Contents 'hello (abc)','hello (QTE) '  ]");
        qp.parse("[Contents 'Query Template Editor Reference','Overview of the Query Template Editor (QTE)']");
    }

    public void TestQueryList(Analyzer textAnalyzer, String sourceFile) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {


        QueryParser qp = new QueryParser(textAnalyzer, "contents");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(sourceFile));
            String line = null;
            double failures = 0;
            double success = 0;
            while ((line = br.readLine()) != null && failures < 100000) {
                try {

                    Query q = qp.parse(line.trim());
                    if (q == null) {
                        System.out.println("Null result for: " + line);
                        failures++;
                    } else if (q.toString().trim().length() == 0) {
                        System.out.println("Parsing this made an empty query: " + line);
                    } else {
                        success++;

                    }
                    if (success % 1000 == 0) System.out.println(success + " sucesses.");
                } catch (InvalidMarkupException ex) {
                    System.out.println("Failed on: " + line);
                    System.out.println(ex.getMessage());
                    failures++;
                }
            }
            double total = failures + success;
            System.out.println("Failure on " + failures + " of " + total + " (" + (Math.round((failures / success) * 10000) / 100) + "%)");
        } finally {
            br.close();
        }
    }


}
