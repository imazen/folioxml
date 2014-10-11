/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package folioxml.lucene.tests;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.util.Date;

/**
 *
 * @author dlinde
 */
public class Searcher {

    public static void main(String[] args) throws Exception{
        if (args.length != 2){
            throw new Exception("Usage: java " + Searcher.class.getName() + 
                    " <index dir> <query> ");
        }
        File indexDir = new File(args[0]);
        String q = args[1];
         if (!indexDir.exists() || !indexDir.isDirectory()){
                throw new Exception(indexDir + "does not exist or is not a directory.");
         }
        search(indexDir,q);

    }
    
    public static void search(File indexDir, String q) throws Exception{
  
        Directory fsDir = FSDirectory.open(indexDir);
        IndexSearcher is = new IndexSearcher(fsDir);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);
        QueryParser parser = new QueryParser(Version.LUCENE_33,"contents", analyzer);
        Query query = parser.parse(q);
        
        long start = new Date().getTime();
        ScoreDoc[] hits = is.search(query, is.maxDoc()).scoreDocs;
        long end = new Date().getTime();
        
        System.err.println("Found " + hits.length + " document(s) ( in " 
                + (end - start) + " milliseconds) that matched query '" + q 
                + "':");
        for (int i = 0; i < hits.length; i++){
            Document doc = is.doc(hits[i].doc);
            System.out.println(doc.get("filename"));
        }
    }
}
