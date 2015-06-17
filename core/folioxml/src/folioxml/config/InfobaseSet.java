package folioxml.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface InfobaseSet extends  InfobaseConfigBase {

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
