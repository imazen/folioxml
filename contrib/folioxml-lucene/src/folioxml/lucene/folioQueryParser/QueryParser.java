package folioxml.lucene.folioQueryParser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.lucene.folioQueryParser.QueryToken.TokenType;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/*
 * Supports field and group searches. Boolean  Does not support wild
 */
public class QueryParser {
	
	public QueryParser(Analyzer analyzer, String defaultField){
		this.analyzer = analyzer;
		this.defaultField = defaultField;
	}
	
	public Query parse(String s) throws IOException, InvalidMarkupException, ParseException{
		return parse(new QueryTokenReader(s));
	}
	public Query parse(QueryTokenReader r) throws IOException, InvalidMarkupException, ParseException{
		return parse(r.readAll());
	}
	public Query parse(List<QueryToken> tokens) throws InvalidMarkupException, ParseException, IOException{
		QueryToken t = new QueryToken(TokenType.None,"");
		t.children = tokens;
		t.ParseChildrenIntoTree();
		return Convert(t);
	}
	Analyzer analyzer;
	String defaultField = "contents";

	protected Query Convert(QueryToken t) throws InvalidMarkupException, ParseException, IOException{
		if (t.type == TokenType.None || t.type == TokenType.OpenGroup){
			if (t.children == null || t.children.size() == 0) return null;
			if (t.children.size() == 1) return Convert(t.children.get(0));
			//Otherwise, make a boolean query.
			BooleanQuery q = new BooleanQuery();
			for (int i =0; i < t.children.size(); i++){
				Query c = Convert(t.children.get(i));
				if (c != null) q.add(c, Occur.MUST);
			}
			if (q.clauses().size() > 0) return q; 
			else return null;
		}
		if (t.type == TokenType.OpenField){ 
			//Parse the field type and name out.
			String type = t.headers.get(0).text;
			
			if (TokenUtils.fastMatches("contents|headings|partition|rank|weight|server",type))
				throw new InvalidMarkupException("Support for [" + type + " ...] in queries is not yet implemented.",t.headers.get(0));
			
			if (TokenUtils.fastMatches("field|group|highlighter|level|popup|note",type)){
				t.headers.remove(0);
			}else{
				type = "Field";
			}
			
			if (TokenUtils.fastMatches("field|highlighter|level|popup|note",type)){
				//Concatenate the text from the tokens to find the field/highlighter/level/popup/note 'field name' to search on.
				String header = TokenUtils.fastMatches("popup|note",type) ? (type + "s") : "";
				for(int i = 0; i < t.headers.size(); i++){
					QueryToken h = t.headers.get(i);
					header += h.text;
					if (h.children != null && h.children.size() > 0) throw new InvalidMarkupException("Invalid character in field, level, or highlighter name - #, @ or /");
				}
				//So, now we have a field name. Pass it down to all children so they can be created properly.
				if (!TokenUtils.fastMatches("level",type))
					t.setFieldNameRecursive(header.trim());
				
				//Piggyback off the () query creation
				QueryToken n = new QueryToken(TokenType.OpenGroup,"(");
				n.children = t.children;
				Query q = Convert(n);
				if (!TokenUtils.fastMatches("level",type)){
					//Now, we have to do something special if there are no children.
					if (q == null) return new PrefixQuery(new Term(t.fieldName,"*"));
					else return q;
				}else{
					//Level queries are really an AND query, they don't change the field name.
					if (q == null) return null;
					BooleanQuery bq = new BooleanQuery();
					bq.add(q, Occur.MUST);
					bq.add(new TermQuery(new Term("level", header.trim())), Occur.MUST);
					return bq;
				}
				
			}else if (TokenUtils.fastMatches("group",type)){
				t.setFieldNameRecursive("groups");
				String header = "";
				for(int i = 0; i < t.headers.size(); i++){
					QueryToken h = t.headers.get(i);
					header += h.text;
					if (h.children != null && h.children.size() > 0) throw new InvalidMarkupException("Invalid character in field header - #, @ or /");
				}
				return parseSimpleQuery("groups", header.trim());
			}
			

		}
		if (t.type == TokenType.Term){
			//+ - && || ! ( ) { } [ ] ^ " ~ * ? : \
			
			return parseSimpleQuery(t.fieldName != null ? t.fieldName : defaultField, t.text);
			
		}
		
		if (t.type == TokenType.TermSuffix){ //For proximity searches
			//TODO: Implement proximity searches
			return Convert(t.children.get(0));
		}

		if (t.type == TokenType.Not){
			Query c = Convert(t.children.get(0));
			if (c == null) return null;
			BooleanQuery q = new BooleanQuery();
			q.add(c,  Occur.MUST_NOT);
			return q;
		}
		if (t.type == TokenType.Or){
			BooleanQuery q = new BooleanQuery();
			for (int i =0; i < t.children.size(); i++){
				Query c = Convert(t.children.get(i));
				if (c != null) q.add(c, Occur.SHOULD);
			}
			if (q.clauses().size() > 0) return q; 
			else return null;
		}
		if (t.type == TokenType.Xor){
			Query c1 = Convert(t.children.get(0));
			Query c2 = Convert(t.children.get(1));
			if (c1 == null && c2 != null) return c2;
			if (c1 != null && c2 == null) return c1;
			if (c1 == null && c2 == null) return null;
			
			
			BooleanQuery qa = new BooleanQuery();
			qa.add(c1, Occur.MUST);
			qa.add(c2, Occur.MUST_NOT);
			BooleanQuery qb = new BooleanQuery();
			qb.add(c2, Occur.MUST);
			qb.add(c1, Occur.MUST_NOT);
			
			BooleanQuery q = new BooleanQuery();
			q.add(qa, Occur.SHOULD);
			q.add(qb, Occur.SHOULD);
			return q;
		}
		return null;
	}
	
	
	public Query parseSimpleQuery(String fieldName, String text) throws IOException{
		//Fix doubled single quotes, strip outer single quotes.
		if (text.startsWith("'") && (text.endsWith("'"))) {
			text = text.replace("''", "'"); //Fix doubled apostrohes
			text = text.substring(1, text.length() -1); //Remove the apostrophes.
		}
		boolean phraseQuery = text.startsWith("\"");
		if (phraseQuery) text = text.substring(1, text.length() -1); //Remove the quotes, we don't need them anymore.
	
		
		TokenStream s = analyzer.tokenStream(fieldName, new StringReader(text));
		try{
			if (phraseQuery){
				PhraseQuery q = new PhraseQuery();
				
				while (s.incrementToken()){ 
					String term = s.getAttribute(CharTermAttribute.class).toString();
					if (term != null && term.length() > 0) q.add(new Term(fieldName,term));
				}
				return q;
			}else{
				BooleanQuery q = new BooleanQuery();
				
				while (s.incrementToken()){ 
					String term = s.getAttribute(CharTermAttribute.class).toString();
					if (term != null && term.length() > 0) q.add(new TermQuery(new Term(fieldName,term)),Occur.MUST);
				}
				return q;
			}
		}finally{
			s.end();
		}
		
	}
}
