package folioxml.export.html;


import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfigBase;
import folioxml.core.InvalidMarkupException;
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

    public PullElements(InfobaseConfigBase c) {
        this.c = c;

        Map<String, Object> pull = c.getObject("pull") != null ? (Map<String, Object>) c.getObject("pull") : new HashMap<String, Object>();

        if (pull.get("links_to_infobases") != null)
            infobasesToDrop = (List<String>) pull.get("links_to_infobases");

        pullProgramLinks = (Boolean) pull.get("program_links");
        pullMenuLinks = (Boolean) pull.get("menu_links");
        if (pullProgramLinks == null) pullProgramLinks = false;
        if (pullMenuLinks == null) pullMenuLinks = true;

        dropNotes = (Boolean) pull.get("drop_notes");
        if (dropNotes == null) dropNotes = false;

        dropPopups = (Boolean) pull.get("drop_popups");
        if (dropPopups == null) dropPopups = true;

        dropOle = (Boolean) pull.get("ole_objects");
        if (dropOle == null) dropOle = false;

        dropMetafile = (Boolean) pull.get("metafile_objects");
        if (dropMetafile == null) dropMetafile = false;
    }

    private Boolean pullProgramLinks = null;
    private Boolean pullMenuLinks = null;
    private Boolean dropNotes = null;
    private Boolean dropPopups = null;
    private Boolean dropOle = null;
    private Boolean dropMetafile = null;
    private List<String> infobasesToDrop = new ArrayList<String>();

    @Override
    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
        NodeList objects = nodes.filterByTagName("img|object|a|link", true);
        for (Node n : objects.list()) {
            String infobase = n.get("infobase");
            if (infobase == null) continue;
            if (infobasesToDrop.contains(infobase)) {
                logAndPullNode("pulled_elements", n, "External infobase reference");
            }
        }

        if (pullMenuLinks)
            logAndPullNodes("pulled_elements", nodes.search(new NodeFilter("link", "menu", null)), "Menu link");
        if (pullProgramLinks)
            logAndPullNodes("pulled_elements", nodes.search(new NodeFilter("link", "program", null)), "Program link");
        if (dropOle)
            logAndDropNodes("dropped_elements", nodes.search(new NodeFilter("object", "type", "ole")), "OLE Object");
        if (dropMetafile)
            logAndDropNodes("dropped_elements", nodes.search(new NodeFilter("object", "handler", "Metafile")), "Metafile Object");
        if (dropNotes) logAndDropNodes("dropped_elements", nodes.search(new NodeFilter("note")), "Notes");
        if (dropPopups) {
            objects = nodes.search(new NodeFilter("a|link", "type", "popup"));
            for (Node t : objects.list()) {
                Node popup = objects.search(new NodeFilter("popup")).first();
                if (popup == null) {
                    provider.getNamedStream("warnings").append("Malformed popup links").append(" in record ").append(t.rootNode().get("folioId")).append("\n").append(t.toXmlString((true))).append("\n");
                }
                if (popup != null) logNode("dropped_elements", popup, "Popup contents");
                logNode("pulled_elements", t, "Popup link");
                if (popup != null) popup.remove();
                t.pull();
            }
            logAndPullNodes("pulled_elements", nodes.search(new NodeFilter("link", "popupTitle", null)), "Named popup link");
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


    private void logAndPullNodes(String stream, NodeList list, String intro) throws InvalidMarkupException, IOException {
        for (Node n : list.list()) {
            logAndPullNode(stream, n, intro);
        }
    }

    private void logAndDropNodes(String stream, NodeList list, String intro) throws InvalidMarkupException, IOException {
        for (Node n : list.list()) {
            logNode(stream, n, intro);
            n.remove();
        }
    }

    private void logAndPullNode(String stream, Node n, String intro) throws InvalidMarkupException, IOException {
        logNode(stream, n, intro);
        n.pull();
    }

    private void logNode(String stream, Node n, String intro) throws InvalidMarkupException, IOException {
        provider.getNamedStream(stream).append(intro).append(" in record ").append(n.rootNode().get("folioId")).append("\n").append(n.toXmlString((true))).append("\n");
    }


}
