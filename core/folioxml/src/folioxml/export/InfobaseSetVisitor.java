package folioxml.export;

import folioxml.config.*;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.export.html.IdentityProcessor;
import folioxml.folio.FolioTokenReader;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.tools.OutputRedirector;
import folioxml.translation.SlxTranslatingReader;
import folioxml.xml.NodeList;
import folioxml.xml.SlxToXmlTransformer;
import folioxml.xml.XmlRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class InfobaseSetVisitor {

    public InfobaseSetVisitor(InfobaseSet set, List<InfobaseSetPlugin> plugins){
        this.set = set;
        this.plugins = plugins;
    }

    List<InfobaseSetPlugin> plugins;

    InfobaseSet set;


    public void complete() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {

        ExportLocations export = set.generateExportLocations();

        Path reportPath = export.getLocalPath("report.txt", AssetType.Text, FolderCreation.CreateParents);
        Path logPath =export.getLocalPath("log.txt", AssetType.Text, FolderCreation.CreateParents);

        OutputRedirector redir = new OutputRedirector(logPath.toString());
        redir.open();
        //Plugin hooks
        for(InfobaseSetPlugin p: plugins)
            p.beginInfobaseSet(set,export);


        ISlxTokenReader slxReader = null;
        int recordCount =0;
        long tokenCount = 0;
        try{
            for(InfobaseConfig conf : set.getInfobases()){
                System.out.println("Opening infobase " + conf.getFlatFilePath());

                //Plugin hooks
                for(InfobaseSetPlugin p: plugins)
                    p.beginInfobase(conf);

                //Open reader
                FolioTokenReader ftr = new FolioTokenReader(new File(conf.getFlatFilePath()));


                //Folio-to-xml translating reader may need to be wrapped
                slxReader = new SlxTranslatingReader(ftr);
                for(InfobaseSetPlugin p: plugins)
                    slxReader = p.wrapSlxReader(slxReader);

                //Record reader
                SlxRecordReader srr = new SlxRecordReader(slxReader,null);

                XmlRecord root = null;
                SlxRecord r = null;
                int recordsRead = 0;
                while((r = srr.read()) != null){

                    r.getHeading(); //Causes Slx to be evaluated, caches result in heading="" attribute

                    //onSlxRecordParsed
                    for(InfobaseSetPlugin p: plugins)
                        p.onSlxRecordParsed(r);

                    XmlRecord rx = new SlxToXmlTransformer().convert(r);


                    //onRecordTransformed
                    for(InfobaseSetPlugin p: plugins)
                        p.onRecordTransformed(rx,r);


                    FileNode fn = null;
                    //assignFileNode
                    for(InfobaseSetPlugin p: plugins){
                        FileNode temp = p.assignFileNode(rx,r);
                        if (temp != null) {
                            fn = temp;
                            break;
                        }
                    }

                    //onRecordComplete
                    for(InfobaseSetPlugin p: plugins)
                        p.onRecordComplete(rx,fn);

                    if  (r.isRootRecord()){
                        root = rx;
                    }else{
                        recordsRead++;
                    }
                }


                System.out.println();
                System.out.println("Root node");
                System.out.println(root.toXmlString(true));

                //endInfobase
                for(InfobaseSetPlugin p: plugins)
                    p.endInfobase(conf);


                tokenCount += ftr.tokensRead;
                recordCount += recordsRead;
                System.out.printf("Visited %s records and %s tokens from %s.\n" , recordCount,tokenCount,conf.getFlatFilePath());

            }

        }finally{
            if (slxReader != null) slxReader.close();
            redir.close();
        }

        try{
            redir = new OutputRedirector(reportPath.toString());
            redir.open();
            System.out.printf("Read %s records and %s tokens from %s infobases.\n", recordCount, tokenCount, set.getInfobases().size());
            //Plugin hooks
            for(InfobaseSetPlugin p: plugins)
                p.endInfobaseSet(set);

        }finally{
            redir.close();
        }

    }
}
