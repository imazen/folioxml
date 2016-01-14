package folioxml.folio;

import folioxml.core.FileIncludeResolver;
import folioxml.utils.Stopwatch;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;

/**
 * @author dlinde
 */
public class FolioTokenReaderTestFile {

    @Test
    public void baselineRead() throws Exception {
        File f = new File(folioxml.config.TestConfig.getFolioHlp().getFlatFilePath());
        System.out.println("Starting");

        FileReader fr = new FileReader(f);
        Stopwatch s = new Stopwatch();
        s.start();
        char[] buffer = new char[8096];
        while (fr.read(buffer) > 0) {
        }
        s.stop();
        System.out.println("Baseline read: " + s.toString());
    }

    @Test
    public void TestFile() throws Exception {
        File f = new File(folioxml.config.TestConfig.getFolioHlp().getFlatFilePath());
        System.out.println("Starting");

        FileReader fr = new FileReader(f);
        FolioTokenReader ftr = new FolioTokenReader(new FileReader(f), new FileIncludeResolver(f.getAbsolutePath()));

        int tokens = 0;
        Stopwatch s = new Stopwatch();
        s.start();
        while (true) {
            tokens++;
            if (tokens % 200000 == 0) {
                s.stop();
                //There are 9.76 million tokens in the full file
                System.out.println(", " + tokens +

                        " with bufferMore=" + ftr.bufferTime.toString() + ", matchTime=" + ftr.matchTime.toString() + ", and getNextMatchTime=" + ftr.getNextMatchTime.toString() + " of " + s.toString() + " for " + ftr.matchLoops + " regex compares");
                // and " + ftr.getNextMatchLoops + " getNextMatch() calls.");
                ftr.bufferTime.reset();
                ftr.matchLoops = 0;
                ftr.getNextMatchLoops = 0;
                ftr.matchTime.reset();
                ftr.getNextMatchTime.reset();
                s.reset();
                s.start();
            }
            FolioToken r = ftr.read();
            if (r == null) break;
            //System.out.println(ftr.bufferTime.toString() + ": " + r.getText());
        }
    }

}
