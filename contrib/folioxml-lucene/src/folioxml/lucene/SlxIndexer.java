package folioxml.lucene;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxContextStack;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.utils.Stopwatch;

import java.io.File;
import java.io.IOException;

public class SlxIndexer {
	protected SlxRecordReader reader;
	protected SlxIndexingConfig conf;

    public static void IndexAll(String folioFilePath, String indexDir, SlxIndexingConfig config) throws IOException, InvalidMarkupException {
        SlxRecordReader srr = null;
        try{//Create SLX valid reader
            srr = new SlxRecordReader(new File(folioFilePath));

            //Index the data to the index location
            new SlxIndexer(srr, config).indexAll(indexDir);
        }finally{
            //Close the original file
            if (srr != null) srr.close();
        }
    }
	
	public SlxIndexer(SlxRecordReader r, SlxIndexingConfig conf){
		this.reader = r;
		this.conf = conf;
	}
	private long numDocsProccessed = 0; 
	
	public void indexAll(String indexDir) throws IOException, InvalidMarkupException{
	    IndexWriter w = new IndexWriter(FSDirectory.open(new File(indexDir)), new IndexWriterConfig(Version.LUCENE_33, conf.getRecordAnalyzer()).setOpenMode(OpenMode.CREATE));
	    StringBuilder sb = null;
	    int records = 0;
	    Stopwatch s = new Stopwatch();
	    s.start();
	    SlxContextStack stack = new SlxContextStack(false,true);
	    try {
		while (true){
		    records++;
		    if (!conf.Debug.contains(SlxIndexingConfig.DebugOptions.SilenceProgressInfo) && records % 2000 == 0) {
	        	s.stop();
	        	//there are 9.76 million tokens in the full file
	        	System.out.println(", records: " + records + " in " + s.toString() );
	        	s.reset();
	        	s.start();
		    }
		    SlxRecord r = reader.read();
		    
		    
		    if (r == null) break;//loop exit

		    //start indexing
		    stack.process(r);
		    
		    w.addDocument(new LuceneRecord(r,stack, conf).process());
		    numDocsProccessed++;
		}
		w.optimize();
		   
	    }catch (InvalidMarkupException ime) {
	    	System.err.println("Error occured in " + getClass().getSimpleName() + " because:" +ime.getCause());
	    	ime.printStackTrace();
	    }finally {
	    	System.out.println("Number of documents processed: " + numDocsProccessed);
			w.close();
	    }
	}


}
