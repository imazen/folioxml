package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenBase;
import org.junit.Assert;
import org.junit.Test;

public class SlxTokenTest {

    @Test
    public void testSlxTokenString() throws InvalidMarkupException {
        new SlxToken("<p>");
    }


    @Test
    public void testIsComment() throws InvalidMarkupException {
        assert (new SlxToken("<!-- comment -->").isComment());
    }

    @Test
    public void testIsTextOrEntity() throws InvalidMarkupException {
        //Should always fail: assert(new SlxToken("text &amp; a few &lt; &lt;entities... ").isTextOrEntity());
        assert (new SlxToken("&amp;").isTextOrEntity());
        assert (new SlxToken("text").isTextOrEntity());
    }

    @Test
    public void testGetText() throws InvalidMarkupException {
        Assert.assertEquals("<encodetest att=\"&quot;font name&quot;\" />", new SlxToken("<encodetest />").set("att", "\"font name\"").toString());

        Assert.assertEquals("\"font name\"", new SlxToken("<decodetest att=\"&quot;font name&quot;\" />").get("att"));

    }

    @Test
    public void testDetectsTokenType() throws InvalidMarkupException {
        Assert.assertEquals(TokenBase.TagType.SelfClosing, new SlxToken("<encodetest />").tagType);
        Assert.assertEquals(TokenBase.TagType.Opening, new SlxToken("<encodetest>").tagType);
        Assert.assertEquals(TokenBase.TagType.Closing, new SlxToken("</encodetest>").tagType);

    }
    /*
	@Test
	public void testSlxTokenTokenTypeString()throws InvalidMarkupException  {
		fail("Not yet implemented");
	}

	
	@Test
	public void testIsContent()throws InvalidMarkupException  {
		fail("Not yet implemented");
	}

	@Test
	public void testIsTag() throws InvalidMarkupException {
		fail("Not yet implemented");
	}

	@Test
	public void testIsOpening() throws InvalidMarkupException {
		fail("Not yet implemented");
	}

	@Test
	public void testIsClosing() throws InvalidMarkupException {
		fail("Not yet implemented");
	}

	@Test
	public void testMatches() throws InvalidMarkupException {
		fail("Not yet implemented");
	}



	@Test
	public void testGet() throws InvalidMarkupException {
		fail("Not yet implemented");
	}

	@Test
	public void testRemove() {
		fail("Not yet implemented");
	}
	*/

}
