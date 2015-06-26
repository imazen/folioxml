package folioxml.config;

/**
 * Created by nathanael on 6/16/15.
 */
public interface InfobaseConfigBase {

    public ExportLocations generateExportLocations();

    public String getStringAsPath(String key, FolderCreation pathOptions);
    public String getString(String key);
    public Boolean getBool(String key);
    public Long getInteger(String key);

    public  Object getObject(String key);
}
