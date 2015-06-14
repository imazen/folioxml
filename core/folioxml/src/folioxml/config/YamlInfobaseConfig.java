package folioxml.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


public class YamlInfobaseConfig implements InfobaseConfig {


    Map<String,Object> data;

    YamlInfobaseSet parent;


    public YamlInfobaseConfig(YamlInfobaseSet parent, Map<String,Object> configData){
        this.parent = parent;
        this.data = configData;
    }

    @Override
    public String getId() {
        return getString("id") == null ? getAliases().get(0) : getString("id");
    }

    @Override
    public String getFlatFilePath() {
        return getStringAsPath("path", parent.getInfobaseDir(),  FolderCreation.None);
    }

    @Override
    public String getIndexDir() {
        String indexPath =  getStringAsPath("index_dir", FolderCreation.None);
        if (indexPath == null){
            indexPath = Paths.get(getExportDir(true)).resolve(getId() + "_lucene_index").toString();
        }
        return indexPath;
    }

    @Override
    public String getExportDir(boolean create) {
        return getStringAsPath("export_dir", FolderCreation.CreateAsDir) == null ? parent.getExportDir(create) : getStringAsPath("export_dir", FolderCreation.CreateAsDir);
    }

    @Override
    public String getExportFile(String filename, boolean createFolders) {
        String path = Paths.get(getExportDir(createFolders)).resolve(filename).toAbsolutePath().toString();
        YamlInfobaseConfig.createFoldersInPath(path, createFolders ? FolderCreation.CreateParents : FolderCreation.None);
        return path;
    }

    @Override
    // Generates a base path that can be used for logs, reports, etc.
    public String generateExportBaseFile() {

        String exportFolderName =  getId() + new SimpleDateFormat("-dd-MMM-yy-(s)").format(new Date());
        return getExportFile(Paths.get(exportFolderName).resolve(getId()).toString(), true);

    }

    @Override
    public List<String> getAliases() {
        ArrayList<String> aliases = new ArrayList<String>();
        File fff = new File(getFlatFilePath());
        String nameWithoutExtension = fff.getName().replaceAll("\\.FFF","").toLowerCase(Locale.ENGLISH);
        String sanitizedName = nameWithoutExtension.replaceAll("[^0-9a-zA-Z_-]|^[0-9_-]", "");
        aliases.add(sanitizedName);
        aliases.add(nameWithoutExtension);
        aliases.add(nameWithoutExtension + ".nfo");

        Object aliasList = data.get("aliases");
        if (aliasList != null){
            List<Object> configAliases = (List<Object>)aliasList;

            for(Object o: configAliases){
                aliases.add(o.toString().toLowerCase(Locale.ENGLISH));
            }
        }
        return aliases;
    }

    @Override
    public String getStringAsPath(String key,  FolderCreation pathOptions) {
        return getStringAsPath(key, null, pathOptions);
    }

    public String getStringAsPath(String key, String base_path, FolderCreation pathOptions) {
        String value = getString(key);
        if (value == null) return null;
        String path = parent.resolvePath(value, base_path);
        if (pathOptions != FolderCreation.None) createFoldersInPath(path,pathOptions);
        return path;
    }

    public static String createFoldersInPath(String path, FolderCreation creationOptions){
        if (creationOptions == FolderCreation.None) return path;
        File to_create = creationOptions == FolderCreation.CreateParents   ? new File(path).getParentFile() : new File(path);
        if (!to_create.exists() && !to_create.mkdirs()){
           //throw new IOException("Failed to create directories in " + to_create.toString());
        }
        return path;
    }

    @Override
    public String getString(String key) {
        Object o = data.get(key);
        return o == null ? null : o.toString();
    }
    @Override
    public boolean getBool(String key) {
        return (Boolean)data.get(key);
    }

    @Override
    public long getInteger(String key) {
        return (Long)data.get(key);
    }
}
