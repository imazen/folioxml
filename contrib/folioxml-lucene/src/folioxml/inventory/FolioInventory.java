package folioxml.inventory;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.config.TestConfig;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.directexport.DirectSlxExporter;
import folioxml.directexport.DirectXhtmlExporter;
import folioxml.directexport.DirectXmlExporter;
import folioxml.export.html.*;
import folioxml.folio.FolioTokenReader;
import folioxml.lucene.QueryResolverInfo;
import folioxml.lucene.SlxIndexer;
import folioxml.lucene.SlxIndexingConfig;
import folioxml.slx.ISlxTokenReader;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FolioInventory {

//Check for overlapping classes
//Check units are inches


    public void Inventory(InfobaseConfig ibase) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        String baseFile = ibase.generateExportBaseFile();
        Inventory(ibase.getFlatFilePath(), baseFile + ".log.txt", baseFile + ".report.txt");
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

    public void InventorySet(InfobaseSet set) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        String basePath = set.getInfobases().size() > 1 ? set.generateExportBaseFile() : set.getFirst().generateExportBaseFile();
        String reportPath = basePath + ".report.txt";
        String logPath = basePath + ".log.txt";

        OutputRedirector redir = new OutputRedirector(logPath);
        redir.open();
        InventoryNodes inventory = new InventoryNodes();

        boolean dumpMappings = true;

        List<SlxRecordReader> open_readers = new ArrayList<SlxRecordReader>();


        int recordCount =0;
        long tokenCount = 0;
        try{
            for(InfobaseConfig conf : set.getInfobases()){


                System.out.println("Opening infobase " + conf.getFlatFilePath());
                //Open reader
                FolioTokenReader ftr = new FolioTokenReader(new File(conf.getFlatFilePath()));
                ISlxTokenReader slxReader = new SlxTranslatingReader(ftr);
                FolioToSlxDiagnosticTool diagTool = dumpMappings ? new FolioToSlxDiagnosticTool(slxReader) : null;
                if (diagTool != null) slxReader = diagTool;
                SlxRecordReader srr = new SlxRecordReader(slxReader,null, false);
                open_readers.add(srr);
                srr.silent = false;

                XmlRecord root = null;
                SlxRecord r = null;
                int recordsRead = 0;
                while((r = srr.read()) != null){
                    XmlRecord rx = new SlxToXmlTransformer().convert(r);
                    if  ("root".equals(r.getLevelType())){
                        root = rx;
                    }else{
                        inventory.process((new NodeList(rx)));
                        recordsRead++;
                    }
                }


                System.out.println();
                System.out.println("Root node");
                System.out.println(root.toXmlString(true));


                if (diagTool != null) diagTool.outputDataFiles(conf.generateExportBaseFile() + ".mappings.txt");

                tokenCount += ftr.tokensRead;
                recordCount += recordsRead;
                System.out.printf("Read %s records and %s tokens from %s.\n" , recordCount,tokenCount,conf.getFlatFilePath());
            }

        }finally{

            try{
                redir.close();
                redir = new OutputRedirector(reportPath);
                redir.open();
                System.out.printf("Read %s records and %s tokens from %s infobases.\n", recordCount, tokenCount, set.getInfobases().size());
                inventory.PrintStats();
            }finally{
                for(SlxRecordReader r: open_readers)
                    r.close();
            }
            inventory.PrintExternalInfobases(set);
            inventory.PrintUniques(); 


            redir.close();
        }
    }




}
