package folioxml.folio;

import org.junit.*;
import folioxml.core.InvalidMarkupException;
import folioxml.utils.Stopwatch;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author nathanael
 */
public class FolioTokenReaderTest {

    public FolioTokenReaderTest() throws Exception {
    /*         try{
    assert false;
    throw new Exception("Assertions must be enabled for tests and software to run correctly!");
    }catch(AssertionError e){}*/
    }

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }
    private static FolioToken.TokenType comment = FolioToken.TokenType.Comment;
    private static FolioToken.TokenType text = FolioToken.TokenType.Text;
    private static FolioToken.TokenType tag = FolioToken.TokenType.Tag;
    FolioTokenReader r;
    FolioToken ft;
    /**
     * Test of main method, of class Indexer.
     */
    private int currentReadSize = 1;

    public FolioTokenReader read(String s) {
        return read(s, currentReadSize);
    }

    public FolioTokenReader read(String s, int readSize) {
        return new FolioTokenReader(new StringReader(s), readSize);
    }

    @Test
    public void TestSingleTag() throws Exception {

        r = read("<RD,ID:the<<id:\"level\"\"Type\",CH>");
        ft = r.read();
        assertTrue(ft.get(0).equals("ID"));
        assertTrue(ft.get(1).equals("the&lt;id"));
        assertTrue(ft.get(2).equals("level&quot;Type"));
        assertTrue(ft.get(3).equals("CH"));
        assertTrue(ft.getOptionAfter("iD").equals("the&lt;id"));
        assertTrue(r.read() == null);
    }

    @Test
    public void TestComments() throws Exception {


        r = read("<<<CM><CM><<<CM></CM> <cM></cm>");
        ft = r.read();
        assertTrue(ft.type == text);
        assertTrue(ft.text.equals("<<"));
        ft = r.read();

        assertTrue(ft.type == comment);
        assertTrue(ft.text.equals("<CM><<<CM>"));
        ft = r.read();
        assertTrue(ft.text.equals(" "));
        assertTrue(ft.type == text);
        ft = r.read();
        assertTrue(ft.type == comment);
        assertTrue(ft.text.length() == 0);


        assertTrue(r.read() == null);
        assertTrue(r.canRead() == false);

        //Attributes not allowed
        afail("<CM,attr>");
        //Unexpected closing
        afail("</CM>");

    }

    @Test
    public void TestText() throws Exception {
        r = read(" here's <<>>some invalid but < allowed markup with < extra single brackets because they aren't followed by a slash or aphabetic");
        r.read();
        assertTrue(r.read() == null && !r.canRead());
//       generateCode("Can't have <PARTIALTAGS");

        afail("Can't have <PARTIALTAGS");

        afail("or </ bits and pieces");
        afail("Because <IT knows there should be a tag");

    }

    @Test
    public void TestTags() throws Exception {
        r = read("</CT>");
        assertTrue(r.read().isClosing());
        assertTrue(read("<UN>").read().tagName.equals("UN"));
        assertTrue(read("<FD:\"field << name\">").read().tagOptions.equals("\"field << name\""));

        //^<(/)?([A-Z-a-z][A-Za-z][\\+\\-]?)(?:[:,;](([^<]+|<<)+))>

        afail("This invalid markup won't match any of the tokens due to the number, and will cause an assertion failure: <A0>");
        afail("It has to match one of them <FD:noSingle<Brackets>");

        afail("Tags can only be two characters: <LONGTAG> will fail");

    }


    @Test
    public void TestSequences() throws Exception {
        String txt =
                "<CM> ***********************************************\n" +
                "** Folio Flat File Identifier and Version Info **\n" +
                "*********************************************** </CM>\n" +
                "<VI:Folio,FFF,4.6.1.0>\n\n\n" +
                "<CM> ***********************************************\n" +
                "     **        Definition File Include            **\n" +
                "     *********************************************** </CM>\n" +
                "<TT:\" Another Title \">\n" +
                "<RE:\"3/20/2005 5:57:07 PM\">\n" +
                "<AU> Company Name Inc.</AU>\n" +
                "<SU> Some Description </SU>\n" +
                "<AS> First part of description<CR> second part of description</AS>\n" +
                "<RM> The Product<CR> Product Description Details</RM>\n" +
                "<HE><JU:LF><AP:0.125><IN:FI:0><TS:Right,RT,NO><BR:BT:0.00972222,0><SD:NO><GI><TB><FT:\"Times New Roman\",SR>Page:  <GP><FT></HE>\n" +
                "<FO></FO>\n" +
                "\n" +
                "\n" +
                "<CM> ***********************************************\n" +
                "     **              Record Text                  **\n" +
                "     *********************************************** </CM>\n\n" +
                "<RD,ID:911:Book,CH><GR:\"non indexed groups\"><GR:\"group 1\"><GR:\"group 2\"><GR:\"group 3\">" +
                "<BH><JU:CN><BR:AL:0.15,0.0291667,FC:255,255,0><SD:255,0,0><FD:\"non indexed field\"><BD+><UN:2+><PT:48><FC:255,255,0>" +
                "<BC:255,0,0>WARNING!<UN><PT><FC:0,0,255><BC><CR><PT:12><FC:255,255,0><BC:255,0,0>You have inadvertently opened the infobase: <UN+>Book911.NFO<UN><CR><CR><PT:16>";

        //generateCode(txt);
        this.TestMessageTop();

    }

    public void main() throws Exception {
        BlockSizeVariations();
    }
    @Test
    public void PerfTest()throws Exception {
        //1000 loops of BlockSizeVariations in 10816 ms
        //down from 27,00 with output statements.
        Stopwatch s = new Stopwatch();
        s.start();
        for (int i = 0; i < 1000;i++) BlockSizeVariations();
        s.stop();
        System.out.println("1000 loops of BlockSizeVariations in " + s.toString());
    }

    public void BlockSizeVariations() throws Exception {
        TestWithBlockSize(1);
        TestWithBlockSize(3);
        TestWithBlockSize(10);
        TestWithBlockSize(2 ^ 16);

    }

    @Test
    public void BlockSize1() throws Exception {
        TestWithBlockSize(1);
    }

    @Test
    public void BlockSize3() throws Exception {
        TestWithBlockSize(3);
    }

    @Test
    public void BlockSize10() throws Exception {
        TestWithBlockSize(10);
    }

    @Test
    public void BlockSize216() throws Exception {
        TestWithBlockSize(216);
    }

    public void generateCode(String s) throws IOException, InvalidMarkupException {
        PrintStream o = System.out;
        r = read(s);

        //Print string
        o.print("\t\tr = read(");
        printStringLiteral(s, "\t\t\t");
        o.println(");");
        //Generate asserts for each tag
        while (r.canRead()) {
            o.println();

            ft = r.read();
            if (ft == null) {
                ga("r.canRead()");
                ga("r.read() == null;");
            } else {
                ga("r.canRead()");
                o.println("ft = r.read();");
                ga("ft.type == FolioToken.TokenType." + ft.type.toString());
                ga("ft.isClosing() == " + Boolean.toString(ft.isClosing()));
                assertEquals("ft.text", ft.text);

                if (ft.type == FolioToken.TokenType.Tag) {
                    assertEquals("ft.tagName", ft.tagName);
                    assertEquals("ft.tagOptions", ft.tagOptions);
                }
                List<String> opts = ft.getOptionsArray();
                for (int i = 0; i < opts.size(); i++) {
                    o.print("assertTrue(ft.get(" + Integer.toString(i) + ").equals(");
                    printStringLiteral(opts.get(i), "\t\t\t\t");
                    o.println("));");
                }
            }

        }

        ga("r.read() == null");
        ga("r.canRead() == false");

    }

    public void ga(String s) {
        System.out.println("assertTrue(" + s + ");");
    }

    public void assertEquals(String var, String value) {
        if (value == null) {
            ga(var + " == null");
        } else {
            System.out.print("assertTrue(" + var + ".equals(");
            printStringLiteral(value, "\t\t\t\t");
            System.out.println("));");
        }
    }

    public void printStringLiteral(String s, String tabPrefix) {
        PrintStream o = System.out;
        int index = 0;
        while (index < s.length()) {
            if (index > 0) {
                o.println("\" + ");
                o.print(tabPrefix);
            }

            int nextNewline = s.indexOf('\n', index) + 1;

            //We +1, so no result will return 0 instead of -1
            if (nextNewline < 1) {
                nextNewline = s.length();
            }

            //wrap at 60 chars
            if (nextNewline - index > 60) {
                nextNewline = index + 60;
            }


            //System.out.println(index + "," + nextNewline);
            String substr = escape(s.substring(index, nextNewline));
            index = nextNewline;

            o.print("\"");
            o.print(substr);
        }
        o.print("\"");
    }

    public String escape(String s) {
        StringWriter sw = new StringWriter((int) (s.length() * 1.1));
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                sw.append("\\\\");
            } else if (s.charAt(i) == '\t') {
                sw.append("\\t");
            } else if (s.charAt(i) == '\"') {
                sw.append("\\\"");
            } else if (s.charAt(i) == '\n') {
                sw.append("\\n");
            } else {
                sw.append(s.charAt(i));
            }
        }
        return sw.toString();
    }

    public void TestWithBlockSize(int size) throws Exception {
        this.currentReadSize = size;
        TestSingleTag();
        TestComments();
        TestText();
        TestTags();
        TestMessageTop();
    }

    public void afail(String s) throws IOException {

        boolean suceeded = false;
        try {
            r = read(s);
            while (r.canRead()) {
                r.read();
            }
            suceeded = true;
        } catch (Exception e) {

        } catch (junit.framework.AssertionFailedError e) {

        }
        assertTrue(!suceeded); //Should have thrown an exception
    }

    public void TestMessageTop() throws InvalidMarkupException, IOException {

        r = read("<CM> ***********************************************\n" +
                "** Folio Flat File Identifier and Version Info **\n" +
                "*********************************************** </CM>\n" +
                "<VI:Folio,FFF,4.6.1.0>\n" +
                "\n" +
                "\n" +
                "<CM> ***********************************************\n" +
                "     **        Definition File Include            **\n" +
                "     *********************************************** </CM>\n" +
                "<TT:\" Another Title \">\n" +
                "<RE:\"3/20/2005 5:57:07 PM\">\n" +
                "<AU> Company Name Inc.</AU>\n" +
                "<SU> Some Description </SU>\n" +
                "<AS> First part of description<CR> second part of description</AS>\n" +
                "<RM> The Product<CR> Product Description Details</RM>\n" +
                "<HE><JU:LF><AP:0.125><IN:FI:0><TS:Right,RT,NO><BR:BT:0.00972" +
                "222,0><SD:NO><GI><TB><FT:\"Times New Roman\",SR>Page:  <GP><FT" +
                "></HE>\n" +
                "<FO></FO>\n" +
                "\n" +
                "\n" +
                "<CM> ***********************************************\n" +
                "     **              Record Text                  **\n" +
                "     *********************************************** </CM>\n" +
                "\n" +
                "<RD,ID:911:Book,CH><GR:\"non indexed groups\"><GR:\"group 1\"><GR:\"group 2\"><GR:\"group 3\"><BH><JU:C" +
                "N><BR:AL:0.15,0.0291667,FC:255,255,0><SD:255,0,0><FD:\"non indexed field\"><BD+><UN:2+><PT:48><FC:255,255,0><BC:255,0,0>W" +
                "ARNING!<UN><PT><FC:0,0,255><BC><CR><PT:12><FC:255,255,0><BC:" +
                "255,0,0>You have inadvertently opened the infobase: <UN+>Book" +
                "911.NFO<UN><CR><CR><PT:16>");

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Comment);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" ***********************************************\n" +
                "** Folio Flat File Identifier and Version Info **\n" +
                "*********************************************** "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<VI:Folio,FFF,4.6.1.0>"));
        assertTrue(ft.tagName.equals("VI"));
        assertTrue(ft.tagOptions.equals("Folio,FFF,4.6.1.0"));
        assertTrue(ft.get(0).equals("Folio"));
        assertTrue(ft.get(1).equals("FFF"));
        assertTrue(ft.get(2).equals("4.6.1.0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n" +
                "\n" +
                "\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Comment);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" ***********************************************\n" +
                "     **        Definition File Include            **\n" +
                "     *********************************************** "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

              assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<TT:\" Another Title \">"));
        assertTrue(ft.tagName.equals("TT"));
        assertTrue(ft.tagOptions.equals("\" Another Title \""));
        assertTrue(ft.get(0).equals(" Another Title "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<RE:\"3/20/2005 5:57:07 PM\">"));
        assertTrue(ft.tagName.equals("RE"));
        assertTrue(ft.tagOptions.equals("\"3/20/2005 5:57:07 PM\""));
        assertTrue(ft.get(0).equals("3/20/2005 5:57:07 PM"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<AU>"));
        assertTrue(ft.tagName.equals("AU"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" Company Name Inc."));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</AU>"));
        assertTrue(ft.tagName.equals("AU"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<SU>"));
        assertTrue(ft.tagName.equals("SU"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" Some Description "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</SU>"));
        assertTrue(ft.tagName.equals("SU"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<AS>"));
        assertTrue(ft.tagName.equals("AS"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" First part of description"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<CR>"));
        assertTrue(ft.tagName.equals("CR"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" second part of description"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</AS>"));
        assertTrue(ft.tagName.equals("AS"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<RM>"));
        assertTrue(ft.tagName.equals("RM"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" The Product"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<CR>"));
        assertTrue(ft.tagName.equals("CR"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" Product Description Details"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</RM>"));
        assertTrue(ft.tagName.equals("RM"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<HE>"));
        assertTrue(ft.tagName.equals("HE"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<JU:LF>"));
        assertTrue(ft.tagName.equals("JU"));
        assertTrue(ft.tagOptions.equals("LF"));
        assertTrue(ft.get(0).equals("LF"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<AP:0.125>"));
        assertTrue(ft.tagName.equals("AP"));
        assertTrue(ft.tagOptions.equals("0.125"));
        assertTrue(ft.get(0).equals("0.125"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<IN:FI:0>"));
        assertTrue(ft.tagName.equals("IN"));
        assertTrue(ft.tagOptions.equals("FI:0"));
        assertTrue(ft.get(0).equals("FI"));
        assertTrue(ft.get(1).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<TS:Right,RT,NO>"));
        assertTrue(ft.tagName.equals("TS"));
        assertTrue(ft.tagOptions.equals("Right,RT,NO"));
        assertTrue(ft.get(0).equals("Right"));
        assertTrue(ft.get(1).equals("RT"));
        assertTrue(ft.get(2).equals("NO"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BR:BT:0.00972222,0>"));
        assertTrue(ft.tagName.equals("BR"));
        assertTrue(ft.tagOptions.equals("BT:0.00972222,0"));
        assertTrue(ft.get(0).equals("BT"));
        assertTrue(ft.get(1).equals("0.00972222"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<SD:NO>"));
        assertTrue(ft.tagName.equals("SD"));
        assertTrue(ft.tagOptions.equals("NO"));
        assertTrue(ft.get(0).equals("NO"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GI>"));
        assertTrue(ft.tagName.equals("GI"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<TB>"));
        assertTrue(ft.tagName.equals("TB"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FT:\"Times New Roman\",SR>"));
        assertTrue(ft.tagName.equals("FT"));
        assertTrue(ft.tagOptions.equals("\"Times New Roman\",SR"));
        assertTrue(ft.get(0).equals("Times New Roman"));
        assertTrue(ft.get(1).equals("SR"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("Page:  "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GP>"));
        assertTrue(ft.tagName.equals("GP"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FT>"));
        assertTrue(ft.tagName.equals("FT"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</HE>"));
        assertTrue(ft.tagName.equals("HE"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FO>"));
        assertTrue(ft.tagName.equals("FO"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == true);
        assertTrue(ft.text.equals("</FO>"));
        assertTrue(ft.tagName.equals("FO"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n" +
                "\n" +
                "\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Comment);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals(" ***********************************************\n" +
                "     **              Record Text                  **\n" +
                "     *********************************************** "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("\n" +
                "\n"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<RD,ID:911:Book,CH>"));
        assertTrue(ft.tagName.equals("RD"));
        assertTrue(ft.tagOptions.equals("ID:911:Book,CH"));
        assertTrue(ft.get(0).equals("ID"));
        assertTrue(ft.get(1).equals("911"));
        assertTrue(ft.get(2).equals("Book"));
        assertTrue(ft.get(3).equals("CH"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GR:\"non indexed groups\">"));
        assertTrue(ft.tagName.equals("GR"));
        assertTrue(ft.tagOptions.equals("\"non indexed groups\""));
        assertTrue(ft.get(0).equals("non indexed groups"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GR:\"group 1\">"));
        assertTrue(ft.tagName.equals("GR"));
        assertTrue(ft.tagOptions.equals("\"group 1\""));
        assertTrue(ft.get(0).equals("group 1"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GR:\"group 2\">"));
        assertTrue(ft.tagName.equals("GR"));
        assertTrue(ft.tagOptions.equals("\"group 2\""));
        assertTrue(ft.get(0).equals("group 2"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<GR:\"group 3\">"));
        assertTrue(ft.tagName.equals("GR"));
        assertTrue(ft.tagOptions.equals("\"group 3\""));
        assertTrue(ft.get(0).equals("group 3"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BH>"));
        assertTrue(ft.tagName.equals("BH"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<JU:CN>"));
        assertTrue(ft.tagName.equals("JU"));
        assertTrue(ft.tagOptions.equals("CN"));
        assertTrue(ft.get(0).equals("CN"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BR:AL:0.15,0.0291667,FC:255,255,0>"));
        assertTrue(ft.tagName.equals("BR"));
        assertTrue(ft.tagOptions.equals("AL:0.15,0.0291667,FC:255,255,0"));
        assertTrue(ft.get(0).equals("AL"));
        assertTrue(ft.get(1).equals("0.15"));
        assertTrue(ft.get(2).equals("0.0291667"));
        assertTrue(ft.get(3).equals("FC"));
        assertTrue(ft.get(4).equals("255"));
        assertTrue(ft.get(5).equals("255"));
        assertTrue(ft.get(6).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<SD:255,0,0>"));
        assertTrue(ft.tagName.equals("SD"));
        assertTrue(ft.tagOptions.equals("255,0,0"));
        assertTrue(ft.get(0).equals("255"));
        assertTrue(ft.get(1).equals("0"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FD:\"non indexed field\">"));
        assertTrue(ft.tagName.equals("FD"));
        assertTrue(ft.tagOptions.equals("\"non indexed field\""));
        assertTrue(ft.get(0).equals("non indexed field"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BD+>"));
        assertTrue(ft.tagName.equals("BD+"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<UN:2+>"));
        assertTrue(ft.tagName.equals("UN"));
        assertTrue(ft.tagOptions.equals("2+"));
        assertTrue(ft.get(0).equals("2+"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<PT:48>"));
        assertTrue(ft.tagName.equals("PT"));
        assertTrue(ft.tagOptions.equals("48"));
        assertTrue(ft.get(0).equals("48"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FC:255,255,0>"));
        assertTrue(ft.tagName.equals("FC"));
        assertTrue(ft.tagOptions.equals("255,255,0"));
        assertTrue(ft.get(0).equals("255"));
        assertTrue(ft.get(1).equals("255"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BC:255,0,0>"));
        assertTrue(ft.tagName.equals("BC"));
        assertTrue(ft.tagOptions.equals("255,0,0"));
        assertTrue(ft.get(0).equals("255"));
        assertTrue(ft.get(1).equals("0"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("WARNING!"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<UN>"));
        assertTrue(ft.tagName.equals("UN"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<PT>"));
        assertTrue(ft.tagName.equals("PT"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FC:0,0,255>"));
        assertTrue(ft.tagName.equals("FC"));
        assertTrue(ft.tagOptions.equals("0,0,255"));
        assertTrue(ft.get(0).equals("0"));
        assertTrue(ft.get(1).equals("0"));
        assertTrue(ft.get(2).equals("255"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BC>"));
        assertTrue(ft.tagName.equals("BC"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<CR>"));
        assertTrue(ft.tagName.equals("CR"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<PT:12>"));
        assertTrue(ft.tagName.equals("PT"));
        assertTrue(ft.tagOptions.equals("12"));
        assertTrue(ft.get(0).equals("12"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<FC:255,255,0>"));
        assertTrue(ft.tagName.equals("FC"));
        assertTrue(ft.tagOptions.equals("255,255,0"));
        assertTrue(ft.get(0).equals("255"));
        assertTrue(ft.get(1).equals("255"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<BC:255,0,0>"));
        assertTrue(ft.tagName.equals("BC"));
        assertTrue(ft.tagOptions.equals("255,0,0"));
        assertTrue(ft.get(0).equals("255"));
        assertTrue(ft.get(1).equals("0"));
        assertTrue(ft.get(2).equals("0"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("You have inadvertently opened the infobase: "));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<UN+>"));
        assertTrue(ft.tagName.equals("UN+"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Text);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("Book911.NFO"));

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<UN>"));
        assertTrue(ft.tagName.equals("UN"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<CR>"));
        assertTrue(ft.tagName.equals("CR"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<CR>"));
        assertTrue(ft.tagName.equals("CR"));
        assertTrue(ft.tagOptions == null);

        assertTrue(r.canRead());
        ft = r.read();
        assertTrue(ft.type == FolioToken.TokenType.Tag);
        assertTrue(ft.isClosing() == false);
        assertTrue(ft.text.equals("<PT:16>"));
        assertTrue(ft.tagName.equals("PT"));
        assertTrue(ft.tagOptions.equals("16"));
        assertTrue(ft.get(0).equals("16"));

        assertTrue(r.read() == null);
        assertTrue(r.canRead() == false); //These were in the wrong order. canRead() returns true incorrectly if the last read block happens to grab the last bit of data without causing an EOF


    }
}