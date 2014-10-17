package folioxml.utils;

import java.io.File;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.representer.Representer;

public class YamlUtil {

	// folioxml.version = 1
	// folioxml.path = path/to/fff/sample.fff
	// folioxml.index = path/to/index/lucene_index
	// folioxml.export = path/to/index/export
	//
	// folio-help.path = files/folio-help/FolioHlp.FFF
	// folio-help.index = files/indexes/folio-help/
	// folio-help.export = files/folio-help/export/

	private static Yaml yaml;
	private static Configuration configuration;
	private static String workingDir;
	static {
		PropertyUtils propUtils = new PropertyUtils();
		propUtils.setAllowReadOnlyProperties(true);
		Representer repr = new Representer();
		repr.setPropertyUtils(propUtils);
		yaml = new Yaml(new Constructor(Configuration.class), repr);
		configuration = (Configuration) yaml.load(YamlUtil.class
				.getResourceAsStream("conf.yaml"));
		workingDir = System.getProperty("user.dir");
		if(workingDir!=null) {
			workingDir = workingDir.substring(0,workingDir.lastIndexOf(slash()));
			workingDir = workingDir.substring(0,workingDir.lastIndexOf(slash()));	
	    }
	}

	public static void main(String[] args) {
		System.out.println(configuration);
		System.out.println(getProperty(configuration.getFolioHelp().getPath()));
		System.out.println(YamlUtil.getProperty(YamlUtil.getConfiguration().getFolioHelp().getPath()));
	}

	public static Configuration getConfiguration() {
		return configuration;
	}
	
	public static String slash(){
    	return System.getProperty("file.separator");
    }
	
	public static String getProperty(String name) {
		if (name.startsWith(slash())) {
			return workingDir + name;
		} else {
			return workingDir + slash() + name;
		}
	}

	public static String getProperty(String name, boolean makeParentDir) {
		if (makeParentDir) {
			File file = new File(name);
			file.getParentFile().mkdirs();
		}
		return getProperty(name);
	}
	
	public static String getExport(String configName, String fileName) {
		Dirs struct;
	    if ("folio-help".equals(configName)) {
	    	struct = YamlUtil.getConfiguration().getFolioHelp();
	    } else {
	    	struct = YamlUtil.getConfiguration().getFolioXml();
	    }
	    String file = YamlUtil.getProperty(struct.getExport());
	    if (!file.endsWith(YamlUtil.slash())) {
	     file = file + YamlUtil.slash();
	    }
	    file = YamlUtil.getProperty(file) + fileName;
		return file;
	}
}
