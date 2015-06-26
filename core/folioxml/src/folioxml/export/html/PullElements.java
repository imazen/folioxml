package folioxml.export.html;


import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfigBase;
import folioxml.config.YamlInfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.ExportingNodeListProcessor;
import folioxml.export.FileNode;
import folioxml.export.LogStreamProvider;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PullElements implements NodeListProcessor, ExportingNodeListProcessor {

    public InfobaseConfigBase c;

    public PullElements(InfobaseConfigBase c){
        this.c = c;

        Map<String,Object> pull = c.getObject("pull") != null ? (Map<String,Object>)c.getObject("pull") : new HashMap<String,Object>();

        if (pull.get("links_to_infobases") != null)
            infobasesToDrop = (List<String>)pull.get("links_to_infobases");

        pullProgramLinks = (Boolean)pull.get("program_links");
        pullMenuLinks = (Boolean)pull.get("menu_links");
        if (pullProgramLinks == null) pullProgramLinks = false;
        if (pullMenuLinks == null) pullMenuLinks = true;


    }

    private Boolean pullProgramLinks = null;
    private Boolean pullMenuLinks = null;
    private List<String> infobasesToDrop = new ArrayList<String>();

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
        NodeList objects = nodes.filterByTagName("img|object|a|link", true);
        for (Node n:objects.list()){
            String infobase = n.get("infobase");
            if (infobase == null) continue;
            if (infobasesToDrop.contains(infobase)){
                logAndPullNode(n,"External infobase reference");
            }
        }

        if (pullMenuLinks) logAndPullNodes(nodes.search(new NodeFilter("link", "menu", null)), "Menu link");
        if (pullProgramLinks) logAndPullNodes(nodes.search(new NodeFilter("link", "program", null)), "Program link");

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


    private void logAndPullNodes(NodeList list, String intro) throws InvalidMarkupException, IOException {
        for (Node n:list.list()){
            logAndPullNode(n,intro);
        }
    }
    private void logAndPullNode(Node n,String intro) throws InvalidMarkupException, IOException {

        provider.getNamedStream("pulled_elements").append(intro).append(" in record ").append(n.rootNode().get("folioId")).append("\n").append(n.toXmlString((true))).append("\n");

        n.pull();
    }


}
