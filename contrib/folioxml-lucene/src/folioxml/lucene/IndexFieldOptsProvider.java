package folioxml.lucene;

/**
 * Created by nathanael on 6/9/15.
 */
public interface IndexFieldOptsProvider {
    IndexFieldOpts getFieldOptions(String fieldName);
    String getDefaultField();
}
