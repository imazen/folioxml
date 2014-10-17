/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package folioxml;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.junit.*;

import folioxml.core.InvalidMarkupException;
import folioxml.directexport.SimultaneousTest;
import folioxml.utils.ConfUtil;
import folioxml.utils.YamlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author dlinde
 */
public class SearchTest {

    public SearchTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    	new SimultaneousTest().IndexHelp();
    	new SimultaneousTest().ExportHelp();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class Indexer.
     */
    
    public void main() throws Exception {

        //dir.close();
     
    }
    @Test
    public void smallTest() throws IOException{
        runBenchmark(100,1000);
    }
    @Test
    public void mediumlTest() throws IOException{
        runBenchmark(-1,10);
    }
    @Test
    public void fullTestTest() throws IOException{
        runBenchmark(-1,1000);
    }

    public void runBenchmark(int maxKeywords, int maxHits) throws IOException{
        System.out.println();
        System.out.println("Running test with " + maxKeywords + " keywords, " + maxHits + " maximum hits per query");
        long start;
         start = new Date().getTime();
        //Directory dir = FSDirectory.getDirectory(PathProvider.getDataDir());
        // Now search the index. If you pass in a directory instance, you are responsible for closing it. If you pass in a path, it will open and close the directory instance itself.
         String indexDir = YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath()).replace(".", "_");
        IndexSearcher isearcher = new IndexSearcher(FSDirectory.open(new File(indexDir)));
        System.out.println("creating index searcher took " + ( new Date().getTime() - start) + " milliseconds");
        
        start = new Date().getTime();
        TermEnum te = isearcher.getIndexReader().terms();
        ArrayList<String> keywords = new ArrayList<String>();
        
        int maxk = 0;
        while (te.next() && (maxk < maxKeywords || maxKeywords < 1)){
           keywords.add(te.term().text());
           maxk++;
           
        }
        System.out.println("reading " + keywords.size() + " keywords took " + ( new Date().getTime() - start) + " milliseconds");
        
        
       
        //first run 72, next 19
        start = new Date().getTime();
        for (int i = 0; i < keywords.size();i++){
           isearcher.search(new TermQuery(new Term("contents",keywords.get(i))), isearcher.maxDoc());
            
        }
        
        System.out.println("searching on " + keywords.size() + " keywords took " + ( new Date().getTime() - start) + " milliseconds");
        
        //first run 29, next 20
        start = new Date().getTime();
        for (int i = 0; i < keywords.size();i++){
            ScoreDoc[] h = isearcher.search(new TermQuery(new Term("contents",keywords.get(i))), isearcher.maxDoc()).scoreDocs;
            for (int j = 0; j < h.length && j < maxHits; j++){
                Document d = isearcher.doc(h[j].doc);
            }
        }
        
        System.out.println("searching and reading " + maxHits + " hits on " + keywords.size() + " keywords took " + ( new Date().getTime() - start) + " milliseconds");
        
        
        isearcher.close();
    }
  

}