package folioxml.folio;

import folioxml.xml.SlxToXmlTransformer;
import folioxml.xml.XmlRecord;
import org.junit.Test;
import folioxml.core.IIncludeResolutionService;
import folioxml.core.InvalidMarkupException;
import folioxml.core.StringIncludeResolver;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxRecordReader;
import folioxml.translation.SlxTranslatingReader;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class FolioSlxTransformerTest{


    public FolioSlxTransformerTest(){

    }

    @Test
    public void TestOverlappingField() throws InvalidMarkupException, IOException {
        String fff = "<RD>\n" +
                "<FD:\"field 1\"><FD:\"field 2\">field 1 and field 2 </FD:\"field 1\"> field 2</FD:\"field 2\">\n" +
                "<FD:\"field 1\"> field 1 <FD:\"field 2\"> field 1 and field 2</FD:\"field 1\"> field 2</FD:\"field 2\">";

        FolioTokenReader ftr = new FolioTokenReader(new StringReader(fff), new StringIncludeResolver());
        SlxRecordReader srr = new SlxRecordReader(new SlxTranslatingReader(ftr));

        SlxRecord root = srr.read();
        SlxRecord r = srr.read();

        assertEquals("<record class=\"NormalLevel\">\n" +
                "<p><span class=\"field_1\" type=\"field 1\"><span class=\"field_2\" type=\"field 2\">field 1 and field 2 </span type=\"field 1\"> field 2</span type=\"field 2\">\n" +
                "<span class=\"field_1\" type=\"field 1\"> field 1 <span class=\"field_2\" type=\"field 2\"> field 1 and field 2</span type=\"field 1\"> field 2</span type=\"field 2\"></p></record>", r.toSlxMarkup(false));

        SlxToXmlTransformer gts = new SlxToXmlTransformer();
        XmlRecord xml = gts.convert(r);

        assertEquals("<record class=\"NormalLevel\">\n" +
                "<p><span class=\"field_1\" type=\"field 1\"><span class=\"field_2\" type=\"field 2\">field 1 and field 2 </span></span><span class=\"field_2\" type=\"field 2\"> field 2</span>\n" +
                "<span class=\"field_1\" type=\"field 1\"> field 1 <span class=\"field_2\" type=\"field 2\"> field 1 and field 2</span></span><span class=\"field_2\" type=\"field 2\"> field 2</span></p></record>", xml.toXmlString(false));

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