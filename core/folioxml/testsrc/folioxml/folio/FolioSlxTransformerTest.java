package folioxml.folio;

import org.junit.Test;
import folioxml.core.IIncludeResolutionService;
import folioxml.core.InvalidMarkupException;
import folioxml.core.StringIncludeResolver;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.translation.SlxTranslatingReader;

import java.io.IOException;
import java.io.StringReader;

public class FolioSlxTransformerTest{


    public FolioSlxTransformerTest(){

    }

    public static void main(String[] args) throws Exception {
        new FolioSlxTransformerTest().TestSegment();
    }



    public void Test100()throws InvalidMarkupException, IOException{
       // for (int i = 0; i < 100; i++) TestSegment();
    }

    @Test
    public void TestSegment() throws InvalidMarkupException, IOException{

        String defFile = "<LN:Book:Chapter>";
        t(
                "<CM> ***********************************************\n" +
                "** Folio Flat File Identifier and Version Info **\n" +
                "*********************************************** </CM>\n" +
                "<VI:Folio,FFF,4.6.1.0>\n\n\n" +
                "<CM> ***********************************************\n" +
                "     **        Definition File Include            **\n" +
                "     *********************************************** </CM>\n" +
                "<DI:\"def.def\"><TT:\" Content Title \">\n" +
                "<RE:\"3/20/2001 5:57:07 PM\">\n" +
                "<AU> Imazen </AU>\n" +
                "<SU> </SU>\n" +
                "<AS> </AS>\n" +
                "<RM> Imazen</RM>\n" +
                "<HE><JU:LF><AP:0.125><IN:FI:0><TS:Right,RT,NO><BR:BT:0.00972222,0><SD:NO><GI><TB><FT:\"Times New Roman\",SR>Page:  <GP><FT></HE>\n" +
                "<FO></FO>\n" +
                "\n" +
                "\n" +
                "<CM> ***********************************************\n" +
                "     **              Record Text                  **\n" +
                "     *********************************************** </CM>\n\n" +
                "<RD,ID:19:Book,CH><GR:\"nonindexed info\"><GR:\"group 5\"><GR:\"group 2\"><GR:\"group 4\">" +
                "<BH><JU:CN><BR:AL:0.15,0.0291667,FC:255,255,0><SD:255,0,0><FD:\"non indexed field\"><BD+><UN:2+><PT:48><FC:255,255,0>" +
                "<BC:255,0,0>WARNING!<UN><PT><FC:0,0,255><BC><CR><PT:12><FC:255,255,0><BC:255,0,0>You have inadvertently opened the infobase: <UN+>Test.NFO<UN><CR><CR><PT:16>" +
                	"",
                new StringIncludeResolver().add(new StringIncludeResolver("def.def",defFile)));

    }


    public void t(String s, IIncludeResolutionService service) throws InvalidMarkupException, IOException{
        //try{
        FolioTokenReader ftr = new FolioTokenReader(new StringReader(s),service);
        SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr));

        SlxRecord r;
        while (true){
            r = srr.read();
            if (r == null) break;
        }
        /*}catch (InvalidMarkupException ime ){
            System.out.println(ime.getMessage());
        }*/

    }


}