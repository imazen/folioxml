package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.folio.FolioToken;
import org.junit.Test;

import static org.junit.Assert.*;

public class FolioLinkUtilsTest {


    @Test
    public void TestProgramLinkConversion() throws InvalidMarkupException{
        assertEquals("<link class=\"Class\" program=\"program.exe\">", FolioLinkUtils.translate(new FolioToken("<PL:Class:\"program.exe\">")).toTokenString());

        assertEquals("<link class=\"Class\" href=\"www.program.exe\">", FolioLinkUtils.translate(new FolioToken("<PL:Class:\"www.program.exe\">")).toTokenString());

        assertEquals("<link class=\"Class\" href=\"mailto:hello\">", FolioLinkUtils.translate(new FolioToken("<PL:Class:\"mailto:hello\">")).toTokenString());

        assertEquals("<link class=\"c\" href=\"www.domain.com\">", FolioLinkUtils.translate(new FolioToken("<PL:c:\"www.domain.com\">")).toTokenString());
        assertEquals("<link class=\"c\" program=\"www.temp\\hello.exe\">", FolioLinkUtils.translate(new FolioToken("<PL:c:\"www.temp\\hello.exe\">")).toTokenString());
        assertEquals("<link class=\"c\" program=\"www.exe\">", FolioLinkUtils.translate(new FolioToken("<PL:c:\"www.exe\">")).toTokenString());

        assertEquals("<link class=\"c\" program=\"C:\\temp.txt\">", FolioLinkUtils.translate(new FolioToken("<PL:c:\"C:\\temp.txt\">")).toTokenString());

    }


    @Test
    public void TestWebLinkConversion() throws InvalidMarkupException{
        assertEquals("<link class=\"Class\" href=\"domain.com\">", FolioLinkUtils.translate(new FolioToken("<WW:Class:\"  domain.com  \">")).toTokenString());

        assertEquals("<link class=\"Class\" href=\"img\\test.exe\">", FolioLinkUtils.translate(new FolioToken("<WW:Class:\"img\\test.exe\">")).toTokenString());

    }
}
