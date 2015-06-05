package folioxml.config;


import java.util.List;

public interface InfobaseConfig {

    public String getId();
    public String getFlatFilePath();
    public String getIndexDir();
    public String getExportDir(boolean create);

    public String getExportFile(String filename, boolean createFolders);
    public String generateExportBaseFile();

    public List<String> getAliases();

    public String getStringAsPath(String key, FolderCreation pathOptions);
    public String getString(String key);
    public boolean getBool(String key);
    public long getInteger(String key);
}
