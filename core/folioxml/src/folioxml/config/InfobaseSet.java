package folioxml.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface InfobaseSet {

    public InfobaseConfig getFirst();

    public List<InfobaseConfig> getInfobases();
    public String getExportDir(boolean create);
    public String getExportFile(String filename, boolean createFolders);
    public String generateExportBaseFile();

    public InfobaseConfig byName(String name);

    public String getStringAsPath(String key, FolderCreation pathOptions);
    public String getString(String key);
    public boolean getBool(String key);
    public long getInteger(String key);
}
