package folioxml.directexport;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.config.TestConfig;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.InfobaseSetVisitor;
import folioxml.export.html.FixHttpLinks;
import folioxml.export.html.MultiRunner;
import folioxml.export.html.RenameImages;
import folioxml.export.html.ResolveQueryLinks;

import folioxml.export.plugins.ExportInventory;
import folioxml.export.plugins.ExportMappingsFiles;
import folioxml.export.plugins.RenameFiles;
import folioxml.lucene.InfobaseSetIndexer;
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



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SimultaneousTest {


    @Test @Ignore
    public void IndexSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new InfobaseSetIndexer());
        //plugins.add(new ExportMappingsFiles());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("testset"),plugins);

        visitor.complete();
    }


    @Test @Ignore
    public void InventorySet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new RenameFiles());
        plugins.add(new ExportInventory());
        //plugins.add(new ExportMappingsFiles());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("testset"),plugins);

        visitor.complete();

    }




}


