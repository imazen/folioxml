package folioxml.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nathanael on 6/17/15.
 */
public class YamlExportLocations implements  ExportLocations {


    String config_id;
    Date export_date;
    Map<String, Object> locations_config;
    Path basedir;

    public YamlExportLocations(Path basedir, String config_id, Date export_date, Map<String, Object> locations_config) {
        this.basedir = basedir;
        this.config_id = config_id;
        this.export_date = export_date == null ? new Date() : export_date;
        this.locations_config = locations_config;
    }

    private String getStamp(){
        return new SimpleDateFormat("dd-MMM-yy-(S)").format(export_date);
    }

    private String applyRegexes(String input, AssetType kind){
        String regex = getString("find", kind, null);
        String replacement = getString("replace", kind, null);
        if (regex == null || replacement == null) return input;

        return input.replaceAll(regex,replacement);
    }

    private String expandPath(String path, String input){
        return path.replaceAll("\\{id\\}", Matcher.quoteReplacement(config_id)).replaceAll("\\{stamp\\}", Matcher.quoteReplacement(getStamp())).replaceAll("\\{input\\}", Matcher.quoteReplacement(input)).replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")));
    }


    private String getString(String key, AssetType category, String defaultValue){
        Object cat = locations_config.get(category.toString().toLowerCase());
        if (cat == null) cat = locations_config.get(category.toString());
        if (cat == null) return defaultValue;

        Map<String,Object> conf = (Map<String,Object>)cat;

        Object value = conf.get(key);
        if (value == null) value = conf.get(key.toLowerCase());

        return value == null ? defaultValue : (String)value;
    }

    private Path createFoldersInPath(Path path, FolderCreation creationOptions) throws IOException {
        if (creationOptions == FolderCreation.None) return path;
        File to_create = creationOptions == FolderCreation.CreateParents   ? path.getParent().toFile() : path.toFile();
        if (!to_create.exists() && !to_create.mkdirs()){
            throw new IOException("Failed to create directories in " + to_create.toString());
        }
        return path;
    }


    private String replace_slashes(String path, char use_slash ){
        char otherSlash = use_slash == '/' ? '\\' : '/';
        return path.replace(otherSlash, use_slash);
    }


    private Path resolvePath(String path) {
        if (path == null) return null;
        //Fix slashes
        path = replace_slashes(path, File.separatorChar);

        return basedir.resolve(Paths.get(path)).toAbsolutePath();
    }


    @Override
    public Path getLocalPath(String relativePath, AssetType assetType, FolderCreation folderCreation) throws IOException {
        String path_pattern = getString("path", assetType, getString("path",AssetType.Default, null));
        if (path_pattern == null) throw new IOException("yaml configuration error: No path value configured for export_locations: " + assetType.toString().toLowerCase());

        String input = applyRegexes(relativePath,assetType);

        String expanded = expandPath(path_pattern, input);

        Path result =  resolvePath(expanded);

        return createFoldersInPath(result, folderCreation);
    }




    @Override
    public URL getPublicUrl(String relativePath, AssetType assetType) throws MalformedURLException {
        String url_pattern = getString("url", assetType, getString("url",AssetType.Default, null));
        if (url_pattern == null) return null;

        String input = applyRegexes(replace_slashes(relativePath, '/'),assetType);

        String expanded = expandPath(url_pattern, input);

        return new URL(expanded);
    }

    @Override
    public String getUri(String relativePath, AssetType assetType, Path document_base) throws IOException {
        URL pub = getPublicUrl(relativePath,assetType);
        if (pub != null) return pub.toString();

        Path assetPath = getLocalPath(relativePath,assetType, FolderCreation.None);

        String relative_physical = document_base.getParent().relativize(assetPath).toString();
        return replace_slashes(relative_physical, '/');
    }
}
