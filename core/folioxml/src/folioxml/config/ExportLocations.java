package folioxml.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public interface ExportLocations {

    public Path getLocalPath(String relativePath, AssetType assetType, FolderCreation folderCreation) throws IOException;
    public URL getPublicUrl(String relativePath, AssetType assetType) throws MalformedURLException;

    // Attempts to get the public URL - if not available, uses a relative URL based on the difference between the current document_base and the asset on the filesystem.
    public String getUri(String relativePath, AssetType assetType, Path document_base) throws IOException;
}
