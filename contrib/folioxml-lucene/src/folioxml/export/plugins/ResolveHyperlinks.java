package folioxml.export.plugins;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.html.ResolveQueryLinks;
import folioxml.lucene.InfobaseFieldOptsSet;
import folioxml.lucene.QueryResolverInfo;
import folioxml.lucene.SlxIndexingConfig;
import folioxml.lucene.analysis.DynamicAnalyzer;
import folioxml.lucene.folioQueryParser.QueryParser;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

//Fixes jump links and query links across infobase boundaries.
public class ResolveHyperlinks implements InfobaseSetPlugin {

    IndexSearcher searcher = null;

    InfobaseSet infobaseSet = null;

    Map<String, DynamicAnalyzer> analyzersPerInfobase = new HashMap<String, DynamicAnalyzer>();


    private DynamicAnalyzer loadAnalyzerFromLucene(InfobaseConfig ic) throws IOException, InvalidMarkupException {
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("infobase", ic.getId())), BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term("level", "root")), BooleanClause.Occur.MUST);
        ScoreDoc[] hits = searcher.search(q,1).scoreDocs;
        if (hits.length > 0){
            //info.workingQueryLinks++;
            String rootXml = searcher.doc(hits[0].doc).get("xml");
            XmlRecord root = new XmlRecord(rootXml);
            return new DynamicAnalyzer(new InfobaseFieldOptsSet(root));
        }else{
            //Infobase in set not indexed
            throw new IOException("Infobase " + ic.getId() + " is present in set, but root record is missing from lucene index.");
        }
    }

    private void loadAnalyzers() throws IOException, InvalidMarkupException {
        for(InfobaseConfig i: infobaseSet.getInfobases()){
            analyzersPerInfobase.put(i.getId(),loadAnalyzerFromLucene(i));
        }
    }

    @Override
    public void beginInfobaseSet(InfobaseSet set, String exportBaseName) throws IOException, InvalidMarkupException {
        infobaseSet = set;
        searcher = null;

        File index = Paths.get(set.getExportDir(true)).resolve("lucene_index").toFile();
        QueryResolverInfo queryInfo = null;
        if (index.isDirectory()) {
            searcher = new IndexSearcher(FSDirectory.open(index));
            //Load and parse all infobase root nodes
            //query infoabse="x" && level="root", then load and parse slx to load the query resolver.
            loadAnalyzers();
        }
    }

    InfobaseConfig currentInfobase = null;
    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        currentInfobase = infobase;
    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public void onRecordTransformed(SlxRecord dirty_slx, XmlRecord r) throws InvalidMarkupException, IOException {

        NodeList nodes = new NodeList(r);

        //All jump destinations must be an anchor tag, or we can't link to them.
        for (Node n:nodes.search(new NodeFilter("bookmark")).list())
            n.setTagName("a").set("id", hashDestination(currentInfobase.getId(), n.get("name")));



        //Convert local and remote jump and query links

        NodeList queryLinks = nodes.search(new NodeFilter("link","query",null));
        for (Node n:queryLinks.list()){
            String result = TryGetResultUri(n.get("infobase"), n.get("query"));
            if (result != null){
                n.set("href", result);
                n.setTagName("a");
            }else{
                //Broken query link
            }
        }




        //Convert local jump links, delete cross-infobase links
        NodeList jumpLinks = nodes.search(new NodeFilter("link","jumpDestination",null));
        for (Node n:jumpLinks.list()){
            String result = TryGetDestinationUri(n.get("infobase"), n.get("jumpDestination"));
            if (result == null){
                //broken jump link
            }else{
                n.setTagName("a");
                n.set("href", result);
            }
        }

    }

    public  String hashDestination(String infobase, String name)  {
        //Normalize destination infobase ID.
        String iid = infobaseSet.byName(infobase).getId();

        //Ids may not begin with a number, prefix
        return "d" + Integer.toHexString(iid.hashCode()) + "_" + Integer.toHexString(name.hashCode());
    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        if (searcher != null) searcher.close();
    /*
        System.out.println("Invalid query links (for syntax reasons): " + queryInfo.invalidQueryLinks);
        System.out.println("No result query links: " + queryInfo.noresultQueryLinks);
        System.out.println("Working query links: " + queryInfo.workingQueryLinks);
        System.out.println("Cross-infobase query links: " + queryInfo.crossInfobaseQueries);
        */
    }


    public String TryGetResultUri(String infobase, String query){
        InfobaseConfig targetConfig = infobaseSet.byName(infobase);
        if (targetConfig == null){
            return null; //Infobase external to set
        }
        try{
            //Lookup analyzer based on infobase
            QueryParser qp = new QueryParser(this.analyzersPerInfobase.get(targetConfig.getId()), InfobaseFieldOptsSet.getStaticDefaultField());
            Query q = qp.parse(query);
            if (q == null) {
                System.out.println("Failed to convert query: " + query);
                //info.invalidQueryLinks ++;
            }
            else {
                String newQuery = q.toString() ;
                ScoreDoc[] hits = searcher.search(q,1).scoreDocs;
                if (hits.length > 0){
                    //info.workingQueryLinks++;
                    return searcher.doc(hits[0].doc).get("uri");
                }else {
                    //info.noresultQueryLinks++;
                    System.out.println("No results for " + newQuery +  "     (Previously " + query + ")");
                }
                return null;
            }
        }catch(InvalidMarkupException ex){
            System.out.println("Failed on: " + query);
            System.out.println(ex.getMessage());
            //info.invalidQueryLinks++;
        } catch (IOException e) {
            System.out.println("Failed on: " + query);
            // TODO Auto-generated catch block
            e.printStackTrace();
           // info.invalidQueryLinks++;
        } catch (ParseException e) {
            System.out.println("Failed on: " + query);
            // TODO Auto-generated catch block
            e.printStackTrace();
            //info.invalidQueryLinks++;
        }
        return null;
    }

    public String TryGetDestinationUri(String infobase, String jumpDestination) throws IOException {
        InfobaseConfig targetConfig = infobaseSet.byName(infobase);
        if (targetConfig == null){
            return null; //Infobase external to set
        }

        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("infobase", targetConfig.getId())), BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term("destinations", jumpDestination)), BooleanClause.Occur.MUST);
        ScoreDoc[] hits = searcher.search(q,1).scoreDocs;
        if (hits.length > 0){
            //info.workingQueryLinks++;
            return searcher.doc(hits[0].doc).get("uri");
            //TODO: improve by modifying uri fragment to link directly to bookmark
        }else {
            //TODO; broken jump link!
        }
        return null;
    }


}
