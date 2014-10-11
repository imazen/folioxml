package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBaseReader;

import java.io.*;

public class XmlTokenReader extends TokenBaseReader<XmlToken> implements IXmlTokenReader{

    public XmlTokenReader(Reader reader){
        this(reader,READ_SIZE_DEFAULT);
     }

    public XmlTokenReader(Reader reader, int readBlockSize){
        super(reader,readBlockSize);
    }

	public XmlTokenReader(File path) throws UnsupportedEncodingException,
			FileNotFoundException, IOException {
		super(path);
	}

	public XmlToken read() throws IOException, InvalidMarkupException {
		return super.read(new XmlToken());
		
	}

}