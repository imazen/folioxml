package folioxml.config;


import java.util.List;

public interface InfobaseConfig extends InfobaseConfigBase {

    public String getId();
    public String getFlatFilePath();

    @Deprecated
    public String getIndexDir();
    @Deprecated
    public String getExportDir(boolean create);

    @Deprecated
    public String getExportFile(String filename, boolean createFolders);
    @Deprecated
    public String generateExportBaseFile();

    public List<String> getAliases();

}
