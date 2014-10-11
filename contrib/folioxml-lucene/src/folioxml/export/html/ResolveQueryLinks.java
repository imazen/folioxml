package folioxml.export.html;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.lucene.QueryResolverInfo;
import folioxml.lucene.folioQueryParser.QueryParser;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.io.IOException;

public class ResolveQueryLinks implements NodeListProcessor {
	
	public ResolveQueryLinks(QueryResolverInfo info){
		this.info = info;
		this.searcher = info.searcher;
		qp = new QueryParser(info.analyzer, info.defaultField);
	}
	QueryResolverInfo info;
	IndexSearcher searcher;
	QueryParser qp;

	public String TryGetResultHash(Query query, String original) throws CorruptIndexException, IOException{
		String newQuery = query.toString() ;
		ScoreDoc[] hits = searcher.search(query,1).scoreDocs;
		if (hits.length > 0){
			info.workingQueryLinks++;
			return searcher.doc(hits[0].doc).get("recordId");
		}else {
			info.noresultQueryLinks++;
			System.out.println("No results for " + newQuery +  "     (Previously " + original + ")");
		}
		return null;
	}
	
	public String TryGetResultHash(String query) {
 	   try{  
		   Query q = qp.parse(query);
		   if (q == null) {
			   System.out.println("Failed to convert query: " + query);
			   info.invalidQueryLinks ++;
		   }
		   else {
			   return TryGetResultHash(q,query);
		   }
	   }catch(InvalidMarkupException ex){
		   System.out.println("Failed on: " + query);
		   System.out.println(ex.getMessage());
		   info.invalidQueryLinks++;
	   } catch (IOException e) {
		   System.out.println("Failed on: " + query);
		// TODO Auto-generated catch block
			e.printStackTrace();
			info.invalidQueryLinks++;
		} catch (ParseException e) {
			System.out.println("Failed on: " + query);
			// TODO Auto-generated catch block
			e.printStackTrace();
			info.invalidQueryLinks++;
		}
		   return null;
	}

	public NodeList process(NodeList nodes) throws InvalidMarkupException {

		
		//Convert local jump links, delete cross-infobase links
		NodeList queryLinks = nodes.search(new NodeFilter("link","query",null));
		for (Node n:queryLinks.list()){
			if (n.get("infobase") != null && n.get("infobase").trim().length() > 0){
				//Whoa... cross infobase links not supported.
				info.crossInfobaseQueries++;
			}else{
				n.setTagName("a");
				String result = TryGetResultHash(n.get("query"));
				if (result != null)
					n.set("href", "#rid" + result);
				else
					n.set("href", "#noresult");
			}
		}
		return nodes;
	}
	


}
