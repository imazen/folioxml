package folioxml.directexport;

import folioxml.export.html.ResolveQueryLinks;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.junit.Ignore;
import org.junit.Test;

import folioxml.core.InvalidMarkupException;
import folioxml.lucene.QueryResolverInfo;
import folioxml.lucene.SlxIndexer;
import folioxml.lucene.SlxIndexingConfig;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.tools.OutputRedirector;
import folioxml.utils.ConfUtil;
import folioxml.utils.YamlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimultaneousTest {
	
	@Test @Ignore
	public void IndexHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
		Index(YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath()));
	}
	
	@Test @Ignore
	public void ExportHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
		Export(YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath()));
		
		
	}

    @Test @Ignore
    public void IndexCustomFile() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        Index(YamlUtil.getProperty(YamlUtil.getConfiguration().getCustomFile().getPath()));
    }

    @Test @Ignore
    public void ExportCustomFile() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        Export(YamlUtil.getProperty(YamlUtil.getConfiguration().getCustomFile().getPath()));


    }

	public void Index(String fffPath) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
    
	    //Create SLX valid reader
	    SlxRecordReader srr = new SlxRecordReader(new File(fffPath));
	    srr.silent = true;
	    //Index the data to the index location
		new SlxIndexer(srr, SlxIndexingConfig.FolioQueryCompatible()).indexAll(fffPath.replace(".", "_"));
		
		//Close the original file
		srr.close();
	}
	
	
	
	public void Export(String sourceFile) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
		File f = new File(sourceFile);
		SlxRecordReader srr = new SlxRecordReader(f);
		srr.silent = false;
		String xmlFile = f.getParent() + File.separatorChar + f.getName()+ new SimpleDateFormat("-dd-MMM-yy-(s)").format(new Date()) + ".xml";
	    DirectXmlExporter x = new DirectXmlExporter(srr,xmlFile);
	    DirectXhtmlExporter xh = new DirectXhtmlExporter(srr,xmlFile.replace(".xml", ".xhtml"));
	    DirectSlxExporter xs = new DirectSlxExporter(srr,xmlFile.replace(".xml", ".slx"));
	    
	    OutputRedirector redir = new OutputRedirector(xmlFile.replace(".xml", ".log.txt"));
	    redir.open();
	    
	    //Support query link resolution if the lucene index exists.
	    IndexSearcher searcher = null;
	    File index = new File(sourceFile.replace(".", "_"));
	    SlxIndexingConfig indexConfig = null;
        QueryResolverInfo queryInfo = null;
	    if (index.isDirectory()) {
	    	searcher = new IndexSearcher(FSDirectory.open(index));
	    	indexConfig = SlxIndexingConfig.FolioQueryCompatible();
            queryInfo = new QueryResolverInfo(searcher, indexConfig.getRecordAnalyzer(), indexConfig.textField);

	    	xh.queryLinkResolver = new ResolveQueryLinks(queryInfo);
	    }
	    //x.maxRecords = 250;
	    //xh.maxRecords = 250;
	    //TODO: I gotta 
	    int i =0;
	    try{
	    while(true){
			SlxRecord r = srr.read();
		    if (r == null) break;//loop exit
		    if (i == 0) indexConfig.UpdateFieldIndexingInfo(r);
		    //do slx first
		    if (xs != null) xs.processRecord(r);
		    xh.processRecord(r);
		    //x.processRecord(r); //This will be broken - Html exporter corrupts the tokens
		    
		    
		    i++;
		    if (queryInfo != null && i % 1000 == 0) System.out.println("Query links resolved: " + queryInfo.workingQueryLinks + ", " +
                    queryInfo.noresultQueryLinks + " with no results, and " +
                    queryInfo.invalidQueryLinks + " with invalid syntax.");
		    
	    }
	    }finally{
	    	if (searcher != null) searcher.close();
	    	srr.close();
	    	x.close();
	    	xh.close();
	    	if (xs != null) xs.close();
	    }
	    System.out.println("Invalid query links (for syntax reasons): " + queryInfo.invalidQueryLinks);
	    System.out.println("No result query links: " + queryInfo.noresultQueryLinks);
	    System.out.println("Working query links: " + queryInfo.workingQueryLinks);
	    System.out.println("Cross-infobase query links: " + queryInfo.crossInfobaseQueries);
	    System.out.println("Read " +Integer.toString(i) + " records.");
	    redir.close();
	}

}


