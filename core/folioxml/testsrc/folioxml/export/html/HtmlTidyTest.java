package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.LogStreamProvider;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class HtmlTidyTest {

    private HtmlTidy htmlTidy;

    @Before
    public void Setup() {
        htmlTidy = new HtmlTidy();
        htmlTidy.setLogProvider(new NopLogStreamProvider());
    }
    @Test
    public void TestInvalidAttributesAreRenamed() throws IOException, InvalidMarkupException {
        XmlRecord record = new XmlRecord("<div folioId=\"1\" groups=\"2017,public\" id=\"r1\" level=\"Year\" uri=\"2017#ar1\">");
        htmlTidy.process(new NodeList(singletonList(record)));
        assertEquals("<div data-folioId=\"1\" data-groups=\"2017,public\" data-level=\"Year\" data-uri=\"2017#ar1\" id=\"r1\"></div>", record.toXmlString(false));
    }
}

class NopLogStreamProvider implements LogStreamProvider {

    @Override
    public Appendable getNamedStream(String name) throws IOException {
        return new NilAppendable();
    }
}

class NilAppendable implements Appendable {

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        return this;
    }
}