package folioxml.export;

import folioxml.config.ExportLocations;

/**
 * Created by nathanael on 6/25/15.
 */
public interface ExportingNodeListProcessor extends NodeListProcessor {

    void setFileNode(FileNode fn);

    void setLogProvider(LogStreamProvider provider);

    void setExportLocations(ExportLocations el);
}
