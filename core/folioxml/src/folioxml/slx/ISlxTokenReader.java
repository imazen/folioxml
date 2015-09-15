package folioxml.slx;

import folioxml.core.InvalidMarkupException;

import java.io.IOException;

public interface ISlxTokenReader {
    public SlxToken read() throws IOException, InvalidMarkupException;

    public void close() throws IOException;

    public boolean canRead();
}