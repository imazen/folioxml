package folioxml.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;

public class QueryResolverInfo {
	
	public QueryResolverInfo(IndexSearcher searcher, Analyzer analyzer, String defaultField){
		this.searcher = searcher;
		this.defaultField = defaultField;
		this.analyzer = analyzer;
	}
	public IndexSearcher searcher;
	public String defaultField;
	public Analyzer analyzer;
	
	public int invalidQueryLinks = 0;
	public int noresultQueryLinks = 0;
	public int workingQueryLinks = 0;
	public int crossInfobaseQueries = 0;
}
