package folioxml.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.Paths.*;


public class YamlInfobaseSet implements InfobaseSet{

    public static Map<String,InfobaseSet> parseYaml(String workingDir, InputStream s){
        Yaml yaml = new Yaml();
        HashMap<String,InfobaseSet> results = new HashMap<String,InfobaseSet>();
        Map<String,Object> yml;
        yml = (Map<String,Object>)yaml.load(s);
        for(Map.Entry<String,Object> set: yml.entrySet()){
            InfobaseSet is = new YamlInfobaseSet(set.getKey(), (Map<String, Object>)(set.getValue()), workingDir);
            results.put(set.getKey(),is);
        }
        return results;
    }

    public static Map<String,InfobaseSet> parseYamlFile(String path) throws FileNotFoundException {
        return parseYaml(new File(path).getParent(), new FileInputStream(new File(path)));
    }

    String name;

    String basedir;
    Map<String,Object> data;
    List<InfobaseConfig> infobases;
    Map<String, InfobaseConfig> infobasesByAlias;

    @Override
    public InfobaseConfig getFirst() {
        return infobases.get(0);
    }

    @Override
    public List<InfobaseConfig> getInfobases() {
        return infobases;
    }

    @Override
    public String getExportDir(boolean create) {
        String exportPath = getStringAsPath("export_dir", create ? FolderCreation.CreateAsDir : FolderCreation.None);

        if (exportPath == null){
            exportPath = Paths.get(getFirst().getFlatFilePath()).resolveSibling("export").toString();
        }
        return exportPath;

    }

    public String getInfobaseDir() {
        String dir =  getStringAsPath("infobase_dir", FolderCreation.None);
        if (dir == null){
            dir = resolvePath(basedir,null);
        }
        return dir;
    }

    @Override
    public String getExportFile(String filename, boolean createFolders) {
        return Paths.get(getExportDir(createFolders)).resolve(filename).toAbsolutePath().toString();
    }

    @Override
    // Generates a base path that can be used for logs, reports, etc.
    public String generateExportBaseFile() {
        return getExportFile(name.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9a-zA-Z_-]", "") + "-all" + new SimpleDateFormat("-dd-MMM-yy-(s)").format(new Date()), true);
    }

    @Override
    public InfobaseConfig byName(String name) {
        return infobasesByAlias.get(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public String getStringAsPath(String key,  FolderCreation pathOptions) {
        return getStringAsPath(key, basedir, pathOptions);
    }

    public String getStringAsPath(String key, String base_path, FolderCreation pathOptions) {
        String value = getString(key);
        if (value == null) return null;
        String path = resolvePath(value, base_path);
        if (pathOptions != FolderCreation.None) YamlInfobaseConfig.createFoldersInPath(path, pathOptions);
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

    public YamlInfobaseSet(String name, Map<String,Object> map, String workingDir){
        this.basedir = workingDir;
        this.data = map;
        this.name = name;
        this.infobases = new ArrayList<InfobaseConfig>();
        this.infobasesByAlias = new HashMap<String, InfobaseConfig>();
        for(Object o: ((List<Object>)data.get("infobases"))){
            Map<String,Object> infobase = (Map<String,Object>)o;
            InfobaseConfig c = new YamlInfobaseConfig(this, infobase);
            this.infobases.add(c);
            for (String a: c.getAliases()){
                this.infobasesByAlias.put(a, c);
            }
        }

    }

    private String slash(){
        return System.getProperty("file.separator");
    }

    @Override
    public String getIndexDir() {
        String indexPath =  getStringAsPath("index_dir", FolderCreation.None);
        if (indexPath == null){
            indexPath = Paths.get(getExportDir(true)).resolve("combined_lucene_index").toString();
        }
        return indexPath;
    }

    private String fixSlashes(String path){
        char otherSlash = slash().charAt(0) == '/' ? '\\' : '/';
        return path.replace(otherSlash, slash().charAt(0));
    }
    private String joinPath(String a, String b){
        a = fixSlashes(a);
        b = fixSlashes(b);

        //Trim slashes
        while (a.endsWith(slash())) a = a.substring(0,a.length() -1);
        while (b.startsWith(slash())) b = b.substring(1);

        return a + slash() + b;
    }

    public String resolvePath(String path, String base_path) {
        if (path == null) return null;
        //Deal with user directories
        path = path.replaceFirst("^~",System.getProperty("user.home"));

        path = fixSlashes(path);

        Path p_path = Paths.get(path);

        if (p_path.isAbsolute()){
            return p_path.toAbsolutePath().toString();
        }else{
            if (base_path != null){
                //Clean the base path, possibly resolving it
                String clean_base = resolvePath(base_path, null);
                return Paths.get(clean_base).resolve(p_path).toAbsolutePath().toString();
            }else{
                return path; //Sad path, lost path,
            }

        }
    }


}
