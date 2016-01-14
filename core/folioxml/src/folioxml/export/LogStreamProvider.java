package folioxml.export;

import java.io.IOException;

/**
 * Created by nathanael on 6/25/15.
 */
public interface LogStreamProvider {

    Appendable getNamedStream(String name) throws IOException;
}
