package folioxml.export;

import com.sun.xml.internal.bind.api.impl.NameConverter;
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class InfobaseSetVisitor implements LogStreamProvider {

    public InfobaseSetVisitor(InfobaseSet set, List<InfobaseSetPlugin> plugins){
        this.set = set;
        this.plugins = plugins;

        if (set.getObject("logs") != null) {
            Map<String, Object> logs = (Map<String, Object>) set.getObject("logs");
            for(Map.Entry<String,Object> pair: logs.entrySet()){
                if (pair.getValue() instanceof  Boolean){
                    if ((Boolean)pair.getValue() == false) log_stream_merges.put(pair.getKey(), null);
                }else if (pair.getValue() instanceof  String){
                    log_stream_merges.put(pair.getKey(),(String)pair.getValue());

                }
            }
        }
    }

    Map<String,String> log_stream_merges = new HashMap<String, String>();

    List<InfobaseSetPlugin> plugins;

    InfobaseSet set;


    Path baseLogPath;

    Map<String,Writer> openLogs = new HashMap<String,Writer>();



    public void complete() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {

        closeLogs();

        ExportLocations export = set.generateExportLocations();

        baseLogPath = export.getLocalPath("log_", AssetType.Text, FolderCreation.CreateParents);
        Path logPath =export.getLocalPath("log.txt", AssetType.Text, FolderCreation.CreateParents);

        OutputRedirector redir = new OutputRedirector(logPath.toString());
        redir.open();
        //Plugin hooks
        for(InfobaseSetPlugin p: plugins)
            p.beginInfobaseSet(set,export, this);


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

                getNamedStream("rootnode")
                        .append("\nRoot node\n")
                        .append(root.toXmlString(true));


                //endInfobase
                for(InfobaseSetPlugin p: plugins)
                    p.endInfobase(conf);


                tokenCount += ftr.tokensRead;
                recordCount += recordsRead;
                System.out.printf("Visited %s records and %s tokens from %s.\n" , recordCount,tokenCount,conf.getFlatFilePath());

            }

            System.out.printf("Read %s records and %s tokens from %s infobases.\n", recordCount, tokenCount, set.getInfobases().size());
            //Plugin hooks
            for(InfobaseSetPlugin p: plugins)
                p.endInfobaseSet(set);

        }finally{
            if (slxReader != null) slxReader.close();

            redir.close();
            closeLogs();
        }

    }

    @Override
    public Appendable getNamedStream(String name) throws IOException {
        if (log_stream_merges.containsKey(name)){
            name = log_stream_merges.get(name);
            if (name == null) return new NilAppendable();
        }
        if (!openLogs.containsKey(name)){
            BufferedWriter bw = Files.newBufferedWriter(Paths.get(baseLogPath + name + ".txt"), Charset.forName("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            openLogs.put(name, bw);

        }
        return openLogs.get(name);
    }

    private class NilAppendable implements Appendable{

        @Override
        public Appendable append(CharSequence csq) throws IOException {
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            return this;
        }

        @Override
        public Appendable append(char c) throws IOException {
            return this;
        }
    }


    void closeLogs() throws IOException {
        for(Map.Entry<String,Writer> e: openLogs.entrySet()){
            e.getValue().close();
        }
        openLogs.clear();
    }
}
