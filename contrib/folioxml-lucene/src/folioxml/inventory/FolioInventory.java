package folioxml.inventory;

import folioxml.core.InvalidMarkupException;
import folioxml.directexport.DirectSlxExporter;
import folioxml.directexport.DirectXhtmlExporter;
import folioxml.directexport.DirectXmlExporter;
import folioxml.export.html.*;
import folioxml.folio.FolioTokenReader;
import folioxml.lucene.QueryResolverInfo;
import folioxml.lucene.SlxIndexer;
import folioxml.lucene.SlxIndexingConfig;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.tools.OutputRedirector;
import folioxml.translation.SlxTranslatingReader;
import folioxml.xml.NodeList;
import folioxml.xml.SlxToXmlTransformer;
import folioxml.xml.XmlRecord;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FolioInventory {

//Check for overlapping classes
//Check units are inches


    public void Inventory(String fffPath) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        File f = new File(fffPath);
        String baseFile = f.getParent() + File.separatorChar + f.getName()+ new SimpleDateFormat("-dd-MMM-yy-(s)").format(new Date());

        Inventory(fffPath, baseFile + ".log.txt", baseFile + ".report.txt");
    }
    public void Inventory(String fffPath, String logPath, String reportPath) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        File f = new File(fffPath);
        FolioTokenReader ftr = new FolioTokenReader(f);
        SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr),null, false);
        srr.silent = false;
        OutputRedirector redir = new OutputRedirector(logPath);
        redir.open();
        InventoryNodes inventory = new InventoryNodes();

        XmlRecord root = null;
        int i =0;
        try{
            while(true){
                SlxRecord r = srr.read();
                if (r == null) break;//loop exit

                XmlRecord rx= new SlxToXmlTransformer().convert(r);

                if  ("root".equals(r.getLevelType())){
                    root = rx;
                }else{
                    inventory.process((new NodeList(rx)));
                    i++;
                }

            }


            System.out.println();
            System.out.println("Root node");
            System.out.println(root.toXmlString(true));

        }finally{

            try{
                redir.close();
                redir = new OutputRedirector(reportPath);
                redir.open();
                System.out.println("Read " + Integer.toString(i) + " records, " + Long.toString(ftr.tokensRead) + " tokens.");
                inventory.PrintStats();
            }finally{
                srr.close();
            }
            inventory.PrintUniques();
            redir.close();
        }
    }


}
