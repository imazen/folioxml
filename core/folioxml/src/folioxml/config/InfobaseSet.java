package folioxml.config;

import java.util.List;


public interface InfobaseSet extends InfobaseConfigBase {

    public String getId();

    public InfobaseConfig getFirst();

    public List<InfobaseConfig> getInfobases();

    @Deprecated
    public String getExportDir(boolean create);

    @Deprecated
    public String getExportFile(String filename, boolean createFolders);

    @Deprecated
    public String generateExportBaseFile();


    public InfobaseConfig byName(String name);

    @Deprecated
    public String getIndexDir();

}
