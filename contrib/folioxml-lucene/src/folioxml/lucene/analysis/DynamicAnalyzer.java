package folioxml.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Fieldable;

import java.io.IOException;
import java.io.Reader;

public final class DynamicAnalyzer extends Analyzer {
	
	AnalyzerPicker picker = null;
	public DynamicAnalyzer(AnalyzerPicker callback){
		this.picker = callback;
	}
	

	
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    Analyzer analyzer = picker.getAnalyzer(fieldName);
    return analyzer.tokenStream(fieldName, reader);
  }
  
  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    Analyzer analyzer = picker.getAnalyzer(fieldName);
    return analyzer.reusableTokenStream(fieldName, reader);
  }
  
  /** Return the positionIncrementGap from the analyzer assigned to fieldName */
  @Override
  public int getPositionIncrementGap(String fieldName) {
    Analyzer analyzer = picker.getAnalyzer(fieldName);
    return analyzer.getPositionIncrementGap(fieldName);
  }

  /** Return the offsetGap from the assigned to field 
   * copied below code directly from lucene 3.5 Analayzer class for getOffsetGap
   * */
  
  public int getOffsetGap(Fieldable field)
  {
    if (field.isTokenized()) {
      return 1;
    }
    return 0;
  }
 
}
