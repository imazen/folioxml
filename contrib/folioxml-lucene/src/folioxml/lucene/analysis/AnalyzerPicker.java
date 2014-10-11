package folioxml.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;

public interface AnalyzerPicker {
	public Analyzer getAnalyzer(String fieldName);
}
