package folioxml.slx;

import folioxml.core.StringIncludeResolver;
import folioxml.folio.FolioTokenReader;
import folioxml.utils.ConfUtil;
import folioxml.utils.YamlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;

public class FolioSnippets {
	
	public static final String folioHelpFFF = YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath());
	public static final String folioHelpDEF = folioHelpFFF.replace(".FFF", ".DEF");
	
	
	public static FolioTokenReader get2MBSampleReader() throws UnsupportedEncodingException, IOException{
		return new FolioTokenReader(new InputStreamReader(new FileInputStream(folioHelpFFF)),
				new StringIncludeResolver().add(new StringIncludeResolver("FolioHlp.DEF",getResource(folioHelpDEF))));
	}
	
	private static String getResource(String s) throws IOException{
		InputStreamReader r = new InputStreamReader(new FileInputStream(new File(folioHelpDEF)));
		CharBuffer cb = CharBuffer.allocate(2048);
		StringBuilder sb = new StringBuilder();
		
		while( r.read(cb) >= 0 ) {
			  sb.append( cb.flip() );
			  cb.clear();
		}
		return sb.toString();
	}
	
	
	
    
    
	

}
