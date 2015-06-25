package folioxml.export;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by nathanael on 6/25/15.
 */
public interface LogStreamProvider {

     Appendable getNamedStream(String name) throws IOException;
}
