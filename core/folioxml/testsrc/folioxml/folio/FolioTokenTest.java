package folioxml.folio;

import org.junit.*;

import static org.junit.Assert.assertTrue;

/**
 * @author dlinde
 */
public class FolioTokenTest {

    public FolioTokenTest() {

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

    /**
     * Test of main method, of class Indexer.
     */


    @Test
    public void TestAttributeSplitting() throws Exception {
        FolioToken ft = new FolioToken("<RD,ID:theid:levelType;CH>");
        assertTrue(ft.get(0).equals("ID"));
        assertTrue(ft.get(1).equals("theid"));
        assertTrue(ft.get(2).equals("levelType"));
        assertTrue(ft.get(3).equals("CH"));
        assertTrue(ft.count() == 4);
        assertTrue(ft.hasOption("CH"));
        assertTrue(ft.hasOption("ID"));

        assertTrue(ft.getOptionAfter("iD").equals("theid"));

    }

    @Test
    public void TestAttributeEncoding() throws Exception {
        FolioToken ft = new FolioToken("<RD,ID:the<<id:\"level<<\"\"\"\"Type\",CH>");
        assertTrue(ft.get(0).equals("ID"));
        assertTrue(ft.get(1).equals("the&lt;id"));
        assertTrue(ft.get(2).equals("level&lt;&quot;&quot;Type"));
        assertTrue(ft.get(3).equals("CH"));
        assertTrue(ft.getOptionAfter("iD").equals("the&lt;id"));
    }

    @Test
    public void TestAttributeValidation() throws Exception {
        ensureFails("<RD,single<bracket>");
        ensureFails("<RD,single\"quote>");
        ensureFails("<RD,\"noclosingQuote>");
        ensureFails("<RD,closing>angle bracket>");
        //ensureFails("<RD,..invalidcharsunqouted>");

    }

    public void ensureFails(String s) {
        try {
            FolioToken ft = new FolioToken(s);
            ft.getOptionsArray();
            assertTrue(false);//Should have failed
        } catch (Exception e) {
        }

    }
}