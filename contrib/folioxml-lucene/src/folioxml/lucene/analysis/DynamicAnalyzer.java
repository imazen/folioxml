package folioxml.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;


public final class DynamicAnalyzer extends DelegatingAnalyzerWrapper {
    AnalyzerPicker picker = null;

    public DynamicAnalyzer(AnalyzerPicker callback) {
        super(PER_FIELD_REUSE_STRATEGY);
        this.picker = callback;
    }

    protected Analyzer getWrappedAnalyzer(String fieldName) {
        return picker.getAnalyzer(fieldName);
    }
}

