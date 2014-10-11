package folioxml.slx;

import folioxml.core.StringIncludeResolver;
import folioxml.folio.FolioTokenReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;

public class FolioSnippets {
	
	
	
	public static FolioTokenReader get2MBSampleReader() throws UnsupportedEncodingException, IOException{
		return new FolioTokenReader(new InputStreamReader(FolioSnippets.class.getResourceAsStream("resources/sample.fff"),"utf-8"),
				new StringIncludeResolver().add(new StringIncludeResolver("sample.def",getResource("/testsrc/resources/sample.def"))));
	}
	
	private static String getResource(String s) throws IOException{
		InputStreamReader r = new InputStreamReader(FolioSnippets.class.getResourceAsStream("resources/sample.def"),"utf-8");
		CharBuffer cb = CharBuffer.allocate(2048);
		StringBuilder sb = new StringBuilder();
		
		while( r.read(cb) >= 0 ) {
			  sb.append( cb.flip() );
			  cb.clear();
		}
		return sb.toString();
	}
	
	
	
    
    
	

}
