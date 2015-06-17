package folioxml.export.plugins;


import com.sun.org.apache.xalan.internal.lib.NodeInfo;
import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.NodeInfoProvider;
import folioxml.export.StaticFileNode;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;
import sun.swing.SwingUtilities2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;


public class ExportStructure implements InfobaseSetPlugin {


    private NodeInfoProvider p;
    public ExportStructure(NodeInfoProvider p){
        this.p = p;
    }


    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export) throws IOException, InvalidMarkupException {

    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {

    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        String heading =  clean_slx.getHeading();
        if (heading != null){
            heading = heading.replaceAll("[ \t\r\n]+", " ").trim();
        }
        clean_slx.set("heading", heading);
    }

    int recordIndex =0;
    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }



    StaticFileNode current = null;



    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {


        if (!p.startNewFile(xr)) return  current;

        StaticFileNode parent = null;
        //Locate the node's parents
        if (current != null){
            XmlRecord commonAncestor = ((XmlRecord)current.getBag().get("record")).getCommonAncestor(xr,true);
            if (commonAncestor.isRootRecord()) commonAncestor = null;
            if (commonAncestor != null){
                StaticFileNode candidateParent = current;
                while (candidateParent != null){
                    if (((XmlRecord)candidateParent.getBag().get("record")) == commonAncestor) {
                        parent = candidateParent;
                        break;
                    }else{
                        candidateParent = (StaticFileNode)candidateParent.getParent();
                    }
                }
            }
        }


        StaticFileNode next = new StaticFileNode(parent);
        next.getBag().put("record", xr);
        next.getAttributes().put("level", xr.get("level"));
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
        Node c = new Node("<a id=\"a" +rid + "\" ></a>");
        xr.addChild(c, 0);


        String path = file.getRelativePath();

        String fragment = "#a" + xr.get("id");
        //We set the URI so it can be indexed, and used for hyperlink resolution.
        xr.set("uri", path + fragment);

        //Store the path bits so we can re-create a relative path
        file.getAttributes().put("relative_path", path);
        file.getAttributes().put("uri_fragment", "#a" + xr.get("id"));

    }




    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
