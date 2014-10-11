package folioxml.slx;

import org.junit.Test;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.folio.FolioTokenReader;
import folioxml.translation.SlxTranslatingReader;
import folioxml.utils.ConfUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TranslationTableDump {
	
	
	@Test
	public void FolioHelp() throws UnsupportedEncodingException, FileNotFoundException, IOException, InvalidMarkupException{
		export("folio-help");
	}
	
	private void export(String configName) throws UnsupportedEncodingException, FileNotFoundException, IOException, InvalidMarkupException{
	    //Create token reader
	    FolioTokenReader ftr = new FolioTokenReader(new File(ConfUtil.getFFFPath(configName)));
		
	    //Create translating reader. Wrap with the diagnostic layer
	    FolioToSlxDiagnosticTool diag = new FolioToSlxDiagnosticTool(new SlxTranslatingReader(ftr));
	    
	    //Process the whole stream.
	    while (diag.canRead()) diag.read(); 
	    
	    //Export the file
	    diag.outputDataFiles(ConfUtil.getExportPath(configName) + "TranslationTable.txt");
	
	    
		//Close the original file
		ftr.close();
	}

}
