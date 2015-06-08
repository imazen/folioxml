package folioxml.export.html;

import org.junit.Test;

import static org.junit.Assert.*;

public class FixHttpLinksTest {

    @Test
    public void TestIncompleteScheme(){
       assertEquals(new FixHttpLinks().repairUrl("http:/test.com"),  "http://test.com");
       assertEquals(new FixHttpLinks().repairUrl("https:/domain.is"),  "https://domain.is");
    }

    @Test
    public void TestMissingScheme(){
        assertEquals(new FixHttpLinks().repairUrl("test.com"),  "http://test.com");
        assertEquals(new FixHttpLinks().repairUrl("www.domain.is"),  "http://www.domain.is");
    }


    @Test
    public void TestPreserve(){
        assertEquals(new FixHttpLinks().repairUrl("C:\\file.txt"),  "C:\\file.txt");
        assertEquals(new FixHttpLinks().repairUrl("mailto:n@domain.com"),  "mailto:n@domain.com");
    }
}
