package folioxml.config;

/**
 * Created by nathanael on 6/16/15.
 */
public interface InfobaseConfigBase {

    public ExportLocations generateExportLocations();

    public String getStringAsPath(String key, FolderCreation pathOptions);
    public String getString(String key);
    public boolean getBool(String key);
    public long getInteger(String key);

    public  Object getObject(String key);
}
