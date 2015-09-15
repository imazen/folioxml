package folioxml.export.html;


import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfigBase;
import folioxml.core.InvalidMarkupException;
import folioxml.export.ExportingNodeListProcessor;
import folioxml.export.FileNode;
import folioxml.export.LogStreamProvider;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class LinkMapper implements NodeListProcessor, ExportingNodeListProcessor {

    public InfobaseConfigBase c;

    private HashMap<String, Object> infobaseToUriMap;


    private HashMap<String, Object> uriToUriMap;

    public LinkMapper(InfobaseConfigBase c) {
        this.c = c;

        Map<String, Object> mapperConfig = c.getObject("link_mapper") != null ? (Map<String, Object>) c.getObject("link_mapper") : new HashMap<String, Object>();

        if (mapperConfig.get("infobases") != null) {
            infobaseToUriMap = (HashMap<String, Object>) mapperConfig.get("infobases");
        }
        if (mapperConfig.get("urls") != null) {
            uriToUriMap = (HashMap<String, Object>) mapperConfig.get("urls");
        }
    }

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {

        NodeList links = nodes.filterByTagName("a|link", true);
        for (Node n : links.list()) {
            String newUrl = null;

            String infobase = n.get("infobase");
            if (infobase != null && infobaseToUriMap != null && infobaseToUriMap.containsKey(infobase)) {
                newUrl = (String) infobaseToUriMap.get(infobase);
            }
            String href = n.get("href");
            if (href != null && uriToUriMap != null && uriToUriMap.containsKey(href)) {
                newUrl = (String) uriToUriMap.get(href);
            }

            if (newUrl != null) {
                String old = n.toXmlString(true);
                n.setTagName("a");
                n.removeAttr("infobase");
                n.set("href", newUrl);
                provider.getNamedStream("mapped_links").append(" in record ").append(n.rootNode().get("folioId")).append("\n").append(old).append(" ... to:\n").append(n.toXmlString((true))).append("\n");
            }

        }
        return nodes;
    }

    @Override
    public void setFileNode(FileNode fn) {

    }

    LogStreamProvider provider;

    @Override
    public void setLogProvider(LogStreamProvider provider) {
        this.provider = provider;
    }

    @Override
    public void setExportLocations(ExportLocations el) {

    }

}
