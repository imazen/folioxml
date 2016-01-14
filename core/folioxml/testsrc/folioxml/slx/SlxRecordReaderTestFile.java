package folioxml.slx;

import folioxml.config.TestConfig;
import folioxml.core.FileIncludeResolver;
import folioxml.core.IIncludeResolutionService;
import folioxml.core.InvalidMarkupException;
import folioxml.core.StringIncludeResolver;
import folioxml.folio.FolioTokenReader;
import folioxml.translation.SlxTranslatingReader;
import folioxml.utils.Stopwatch;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class SlxRecordReaderTestFile {


    public SlxRecordReaderTestFile() {

    }

    public static void main(String[] args) throws Exception {
        new SlxRecordReaderTestFile().ConvertFile();
    }


    public void Test100() throws InvalidMarkupException, IOException {
        // for (int i = 0; i < 100; i++) TestSegment();
    }


    public void TestSegment() throws InvalidMarkupException, IOException {

        String defFile = "<LN:Year,Tape,Chapter,Section>";
        t(
                "<CM> ***********************************************\n" +
                        "** Folio Flat File Identifier and Version Info **\n" +
                        "*********************************************** </CM>\n" +
                        "<VI:Folio,FFF,4.6.1.0>\n\n\n" +
                        "<CM> ***********************************************\n" +
                        "     **        Definition File Include            **\n" +
                        "     *********************************************** </CM>\n" +
                        "<DI:\"def.def\"><TT:\" Another Title\">\n" +
                        "<RE:\"3/20/2005 5:57:07 PM\">\n" +
                        "<AU> Company Name</AU>\n" +
                        "<SU> Another Description </SU>\n" +
                        "<AS> Description part 1<CR> Description part 2</AS>\n" +
                        "<RM> Description Part 2<CR> Another Title Software - 2000</RM>\n" +
                        "<HE><JU:LF><AP:0.125><IN:FI:0><TS:Right,RT,NO><BR:BT:0.00972222,0><SD:NO><GI><TB><FT:\"Times New Roman\",SR>Page:  <GP><FT></HE>\n" +
                        "<FO></FO>\n" +
                        "\n" +
                        "\n" +
                        "<CM> ***********************************************\n" +
                        "     **              Record Text                  **\n" +
                        "     *********************************************** </CM>\n\n" +
                        "<RD,ID:EC000019:Year,CH><GR:\"non indexed info group\"><GR:\"group 1\"><GR:\"group 2\"><GR:\"group 3\">" +
                        "<BH><JU:CN><BR:AL:0.15,0.0291667,FC:255,255,0><SD:255,0,0><FD:\"non indexed field\"><BD+><UN:2+><PT:48><FC:255,255,0>" +
                        "<BC:255,0,0>WARNING!<UN><PT><FC:0,0,255><BC><CR><PT:12><FC:255,255,0><BC:255,0,0>You have inadvertently opened the infobase: <UN+>Book911.NFO<UN><CR><CR><PT:16>" +
                        "",
                new StringIncludeResolver().add(new StringIncludeResolver("def.def", defFile)));

    }

    public void t(String s, IIncludeResolutionService service) throws InvalidMarkupException, IOException {
        //try{
        FolioTokenReader ftr = new FolioTokenReader(new StringReader(s), service);
        SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr));

        SlxRecord r;
        while (true) {
            r = srr.read();
            if (r == null) break;
        }
        /*}catch (InvalidMarkupException ime ){
            System.out.println(ime.getMessage());
        }*/

    }


    @Test
    public void ConvertFile() throws IOException, InvalidMarkupException {
        core(true);
    }

    @Test
    public void ReadFile() throws IOException, InvalidMarkupException {
        core(false);
    }

    public void core(boolean convert) throws IOException, InvalidMarkupException {

        File f = new File(folioxml.config.TestConfig.getFolioHlp().getFlatFilePath());
        System.out.println("Starting");

        FileReader fr = new FileReader(f);
        BufferedWriter writer = null;
        File file = new File(TestConfig.getFolioHlp().getExportDir(true), "folio-help-out.txt");
        if (convert) writer = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"));
        try {
            FolioTokenReader ftr = new FolioTokenReader(new FileReader(f), new FileIncludeResolver(f.getAbsolutePath()));
            SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr));

            int records = 0;
            Stopwatch s = new Stopwatch();
            s.start();
            while (true) {
                records++;
                if (records % 2000 == 0) {
                    s.stop();
                    //There are 9.76 million tokens in the full file
                    System.out.println(", records: " + records +

                            " with bufferMore=" + ftr.bufferTime.toString() + ", matchTime=" + ftr.matchTime.toString() + ", and getNextMatchTime=" + ftr.getNextMatchTime.toString() + " of " + s.toString());
                    // and " + ftr.getNextMatchLoops + " getNextMatch() calls.");
                    ftr.bufferTime.reset();
                    ftr.matchLoops = 0;
                    ftr.getNextMatchLoops = 0;
                    ftr.matchTime.reset();
                    ftr.getNextMatchTime.reset();
                    s.reset();
                    s.start();
                }
                SlxRecord r = srr.read();

                if (r == null) break;

                if (convert) writer.write(r.toSlxMarkup(true));
                //System.out.println(ftr.bufferTime.toString() + ": " + r.getText());
            }
        } finally {
            if (convert) writer.close();
            fr.close();
        }
    }


}