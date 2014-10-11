package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.folio.FolioToken;
import folioxml.folio.FolioTokenReader;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxToken;

import java.io.IOException;


public class SlxTranslatingReader implements ISlxTokenReader{
    public SlxTranslatingReader(FolioTokenReader reader){
        this.reader = reader;
    }
    protected FolioTokenReader reader;
    //protected FolioSlxTranslator translator = new FolioSlxTranslator();
    /**
     * Reads the next translated token from the stream
     * @return
     * @throws java.io.IOException
     * @throws folioxml.core.InvalidMarkupException
     */
    public SlxToken read() throws IOException, InvalidMarkupException{
        FolioToken ft = reader.read();
        if (ft == null) return null;
        return FolioSlxTranslator.translate(ft);
        
    }
    public boolean canRead(){
        return reader.canRead();
    }
    /**
     * Closes the underlying reader
     */
    public void close() throws IOException{
        reader.close();
        reader = null;
    }
}