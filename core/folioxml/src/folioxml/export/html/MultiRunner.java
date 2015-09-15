package folioxml.export.html;

import folioxml.config.ExportLocations;
import folioxml.core.InvalidMarkupException;
import folioxml.export.ExportingNodeListProcessor;
import folioxml.export.FileNode;
import folioxml.export.LogStreamProvider;
import folioxml.export.NodeListProcessor;
import folioxml.xml.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiRunner implements NodeListProcessor, ExportingNodeListProcessor {

    private NodeListProcessor[] filters = null;
    private ExportingNodeListProcessor[] efilters = null;

    public MultiRunner(NodeListProcessor... filters) {

        this.filters = filters;
        List<ExportingNodeListProcessor> ef = new ArrayList<ExportingNodeListProcessor>();

        for (NodeListProcessor p : filters) {
            if (p instanceof ExportingNodeListProcessor) {
                ef.add((ExportingNodeListProcessor) p);
            }
        }
        efilters = ef.toArray(new ExportingNodeListProcessor[ef.size()]);
    }


    public NodeList process(NodeList nodes) throws InvalidMarkupException, IOException {
        for (NodeListProcessor p : filters)
            nodes = p.process(nodes);

        return nodes;
    }

    public static NodeList process(NodeList nodes, NodeListProcessor... filters) throws InvalidMarkupException, IOException {
        for (NodeListProcessor p : filters)
            nodes = p.process(nodes);

        return nodes;
    }

    @Override
    public void setFileNode(FileNode fn) {
        for (ExportingNodeListProcessor p : efilters) {
            p.setFileNode(fn);
        }
    }

    @Override
    public void setLogProvider(LogStreamProvider provider) {
        for (ExportingNodeListProcessor p : efilters) {
            p.setLogProvider(provider);
        }
    }

    @Override
    public void setExportLocations(ExportLocations el) {
        for (ExportingNodeListProcessor p : efilters) {
            p.setExportLocations(el);
        }
    }
}
