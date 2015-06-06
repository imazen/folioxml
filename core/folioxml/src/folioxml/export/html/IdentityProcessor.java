package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeList;

import java.io.IOException;

public class IdentityProcessor implements NodeListProcessor {

    public IdentityProcessor(){}

    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
        return nodes;
    }

}
