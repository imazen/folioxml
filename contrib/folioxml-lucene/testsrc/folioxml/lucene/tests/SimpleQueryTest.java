/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package folioxml.lucene.tests;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.File;

import static org.junit.Assert.assertEquals;

//import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author dlinde
 */
public class SimpleQueryTest {

    public SimpleQueryTest() {
        super();
    }

    public static void main(String[] args) throws Exception {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);

        // Store the index in memory:
        Directory directory = new RAMDirectory();
        // To store an index on disk, use this instead:
        
        //Directory directory = FSDirectory.open(new File("path-to-index"));
        IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_33, analyzer).setOpenMode(OpenMode.CREATE));
        Document doc = new Document();
        String text = "This is the text to be indexed.";
        doc.add(new Field("fieldname", text, Field.Store.YES,
                Field.Index.ANALYZED));
        
        iwriter.addDocument(doc);
        iwriter.optimize();
        iwriter.close();

        // Now search the index:
        IndexSearcher isearcher = new IndexSearcher(directory);
        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_33,"fieldname", analyzer);
        Query query = parser.parse("text");
        ScoreDoc[] hits = isearcher.search(query, isearcher.maxDoc()).scoreDocs;
        assertEquals(1, hits.length);
    
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
        }
        isearcher.close();
        directory.close();
    }
}
