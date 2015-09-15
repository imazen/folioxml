package folioxml.xml;

import folioxml.core.InvalidMarkupException;

import java.io.IOException;

public interface IXmlTokenReader {
    public XmlToken read() throws IOException, InvalidMarkupException;

    public void close() throws IOException;

    public boolean canRead();
}
