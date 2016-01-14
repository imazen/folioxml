package folioxml.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

public interface IIncludeResolutionService {
    public Reader getReader() throws FileNotFoundException;

    public String getHash() throws IOException;

    public String getDescription();

    public IIncludeResolutionService getChild(String fileReference) throws IOException;
}