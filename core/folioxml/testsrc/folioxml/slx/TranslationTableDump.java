package folioxml.slx;

import folioxml.config.TestConfig;
import folioxml.core.FolioToSlxDiagnosticTool;
import folioxml.core.InvalidMarkupException;
import folioxml.folio.FolioTokenReader;
import folioxml.translation.SlxTranslatingReader;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TranslationTableDump {


    @Test
    public void FolioHelp() throws UnsupportedEncodingException, FileNotFoundException, IOException, InvalidMarkupException {
        export("folio_help");
    }

    private void export(String configName) throws UnsupportedEncodingException, FileNotFoundException, IOException, InvalidMarkupException {
        //Create token reader
        FolioTokenReader ftr = new FolioTokenReader(new File(folioxml.config.TestConfig.getFolioHlp().getFlatFilePath()));

        //Create translating reader. Wrap with the diagnostic layer
        FolioToSlxDiagnosticTool diag = new FolioToSlxDiagnosticTool(new SlxTranslatingReader(ftr));

        //Process the whole stream.
        while (diag.canRead()) diag.read();

        //Export the file
        String file = TestConfig.getFirst(configName).getExportFile("TranslationTable.txt", true);
        diag.outputDataFiles(file);

        //Close the original file
        ftr.close();
    }

}
