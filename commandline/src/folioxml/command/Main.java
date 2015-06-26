package folioxml.command;

import folioxml.config.InfobaseSet;
import folioxml.config.TestConfig;
import folioxml.config.YamlInfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.ExportRunner;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

public class Main {

	
	public static Options getOptions(){
		Options options = new Options();
		//yaml file
		//which configuration
		//inventory, index, or export

        Option config = new Option("config", true, "Path to yaml configuration");
        config.setRequired(true);
        Option export = new Option("export", true, "Name of config set to export");
        export.setRequired(true);

        options.addOption(config);
        options.addOption(export);

		return options;
	}
	/**
	 * @param args
	 */
    public static void main(String[] args) throws IOException {
        run(args);
    }
	public static int run(String[] args) throws IOException {
	    // create the parser
	    CommandLineParser parser = new PosixParser();
	    try {
	        // parse the command line arguments
	        CommandLine line = parser.parse(getOptions(), args);

            String configPath = line.getOptionValue("config");
            String configName = line.getOptionValue("export");


            String fullConfigPath = Paths.get(configPath).toFile().getCanonicalPath();
            if (!Paths.get(fullConfigPath).toFile().exists()){
                System.err.println( "Failed to locate yaml file " +fullConfigPath);
                return 2;
            }

            InputStream privateYaml =  new FileInputStream(fullConfigPath);
            String workingDir = new File(".").getCanonicalPath();
            Map<String,InfobaseSet> configs = YamlInfobaseSet.parseYaml(workingDir, privateYaml);

            if (!configs.containsKey(configName)){
                System.err.println("The yaml file does not contain a configuration '" + configName + "'.");
                return 3;
            }
            System.out.println("Indexing...");
            new ExportRunner(configs.get(configName)).Index();
            System.out.println("Exporting...");
            new ExportRunner(configs.get(configName)).Export();
            return 0;
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            return 1;
	    } catch (InvalidMarkupException e) {
            e.printStackTrace();
            return 4;
        }
    }

}
