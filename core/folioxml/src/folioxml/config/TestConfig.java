package folioxml.config;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by nathanael on 5/25/15.
 */
public class TestConfig {

    public static String slash(){
        return System.getProperty("file.separator");
    }

    public static Map<String,InfobaseSet> getAllUncached(){


        InputStream foo = TestConfig.class.getResourceAsStream("/test.yaml");

        String classDir = TestConfig.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String workingDir = Paths.get(classDir).getParent().getParent().getParent().getParent().toAbsolutePath().toString();

        return YamlInfobaseSet.parseYaml(workingDir,foo);
    }

    public static Map<String,InfobaseSet> getAll(){
        if (configs == null){
            configs = getAllUncached();
        }
        return configs;
    }

    static Map<String,InfobaseSet> configs;

    public static InfobaseConfig getFolioHlp(){
        return getAll().get("folio_help").getFirst();
    }

    public static InfobaseSet get(String configName) {
        return getAll().get(configName);
    }

    public static InfobaseConfig getFirst(String configName) {
        return getAll().get(configName).getFirst();
    }
}
