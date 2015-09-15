package folioxml.export.html;

import folioxml.config.ExportLocations;
import folioxml.core.InvalidMarkupException;
import folioxml.export.ExportingNodeListProcessor;
import folioxml.export.FileNode;
import folioxml.export.LogStreamProvider;
import folioxml.export.NodeListProcessor;
import folioxml.xml.ElementFilter;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.*;


public class HtmlTidy implements NodeListProcessor, ExportingNodeListProcessor {


    private static Set<String> validAttributes = new HashSet<String>(Arrays.asList("class", "id", "alt", "style", "href", "src", "name", "onclick", "cellspacing", "title", "colspan", "rowspan"));

    private static Set<String> invalidAttributes = new HashSet<String>();

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {

        //Ensure that there are no nodes other than table|td|tr|th|a|img|br|p|span|div

        for (Node n : nodes.filterRecursive(new ElementFilter()).list()) {
            if (!n.matches("table|tr|td|th|a|img|br|p|span|div")) {
                provider.getNamedStream("tidy_invalid_elements").append(n.toXmlString(true)).append("\n");
            }


            List<Map.Entry<String, String>> attrs = new ArrayList<Map.Entry<String, String>>(n.getAttributes().entrySet());
            //First check for invalid attributes, and print them.
            for (Map.Entry<String, String> pair : attrs) {
                if (!validAttributes.contains(pair.getKey()) && !invalidAttributes.contains(pair.getKey())) {
                    provider.getNamedStream("tidy_invalid_attributes").append("\n").append(pair.getKey()).append("\n").append(n.toXmlString(true)).append("\n");
                    invalidAttributes.add(pair.getKey());

                }
            }
            //Now, fix them
            for (Map.Entry<String, String> pair : attrs) {
                if (!validAttributes.contains(pair.getKey())) {
                    n.removeAttr(pair.getKey()).set("data-" + pair.getKey(), pair.getValue());
                }
            }
        }


        //and no attributes other than class, id, style, href, src,
        return nodes;

    }

    LogStreamProvider provider;

    @Override
    public void setFileNode(FileNode fn) {

    }

    @Override
    public void setLogProvider(LogStreamProvider provider) {
        this.provider = provider;
    }

    @Override
    public void setExportLocations(ExportLocations el) {

    }
}
