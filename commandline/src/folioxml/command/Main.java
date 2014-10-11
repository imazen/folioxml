package folioxml.command;

import org.apache.commons.cli.*;

public class Main {

	
	public static Options getOptions(){
		Options options = new Options();
		options.addOption("type", true, "xml for raw xml, xhtml for xhtml, and simple-xhtml for simplified");
		options.addOption("exportfolder", true, " A subfolder to export the files to");
		return options;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    // create the parser
	    CommandLineParser parser = new PosixParser();
	    try {
	        // parse the command line arguments
	        CommandLine line = parser.parse( getOptions(), args );
	    //    line.getArgs()
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	    }

	}

}
