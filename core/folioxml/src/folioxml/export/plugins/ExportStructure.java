package folioxml.export.plugins;


import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.*;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.Node;
import folioxml.xml.XmlRecord;

import java.io.IOException;


public class ExportStructure implements InfobaseSetPlugin {


    private NodeInfoProvider p;

    public ExportStructure(NodeInfoProvider p) {
        this.p = p;
    }

    InfobaseSet set;

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.set = set;
    }

    InfobaseConfig currentInfobase;
    boolean useRootAsParentNode = false;

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        currentInfobase = infobase;
        useRootAsParentNode = p.separateInfobases(infobase, set);
    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        String heading = clean_slx.getHeading();
        if (heading != null) {
            heading = heading.replaceAll("[ \t\r\n]+", " ").trim();
        }
        if (heading != null && heading.length() > 0) {
            clean_slx.set("heading", heading);
        }

    }

    int recordIndex = 0;

    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }


    StaticFileNode current = null;


    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        if (xr.isRootRecord()) {
            xr.set("infobaseId", currentInfobase.getId());
        }
        if (!xr.isRootRecord() && current != null)
            if (!p.startNewFile(xr)) return current;


        StaticFileNode parent = null;
        //Locate the node's parents
        if (current != null) {
            XmlRecord commonAncestor = ((XmlRecord) current.getBag().get("record")).getCommonAncestor(xr, true);
            if (commonAncestor != null && commonAncestor.isRootRecord() && !useRootAsParentNode) commonAncestor = null;
            if (commonAncestor != null) {
                StaticFileNode candidateParent = current;
                while (candidateParent != null) {
                    if (((XmlRecord) candidateParent.getBag().get("record")) == commonAncestor) {
                        parent = candidateParent;
                        break;
                    } else {
                        candidateParent = (StaticFileNode) candidateParent.getParent();
                    }
                }
            }
        }


        StaticFileNode next = new StaticFileNode(parent);
        next.getBag().put("record", xr);
        if (xr.getLevelType() != null) {
            next.getAttributes().put("level", xr.getLevelType());
        }
        p.PopulateNodeInfo(xr, next);
        next.setRelativePath(p.getRelativePathFor(next));
        current = next;
        return next;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {


        String rid = "r" + Integer.toString(recordIndex);
        recordIndex++;
        xr.set("id", rid);

        //Many browsers/ebooks require an anchor tag, and can't navigate to a div ID.
        Node c = new Node("<a id=\"a" + rid + "\" ></a>");
        xr.addChild(c, 0);


        String path = file.getRelativePath();

        String fragment = "#a" + xr.get("id");
        //We set the URI so it can be indexed, and used for hyperlink resolution.
        xr.set("uri", path + fragment);

        //Store the path bits so we can re-create a relative path
        if (!file.getAttributes().containsKey("relative_path"))
            file.getAttributes().put("relative_path", path);
        if (!file.getAttributes().containsKey("uri_fragment"))
            file.getAttributes().put("uri_fragment", "#a" + xr.get("id"));

    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
