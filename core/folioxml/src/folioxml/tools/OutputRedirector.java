package folioxml.tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class OutputRedirector {
	PrintStream stdout = null;
	PrintStream stderr = null;
	String filename = null;
	protected FileOutputStream out = null;
	public OutputRedirector(String filename){
		this.filename = filename;
	}

	public void open() throws FileNotFoundException{
		//Save so we can restore them later.
        stdout = System.out;                                        
        stderr = System.err;   
        //Make the dir if it is missing
		if (!new java.io.File(filename).getParentFile().exists()) new java.io.File(filename).getParentFile().mkdirs();
		
		out  = new FileOutputStream(filename);
		System.out.println("Redirecting output to " + filename);
		System.setOut(new PrintStream(out,true));
		System.setErr(new PrintStream(out,true));
	}
	
	public void close() throws IOException{
		if (out != null) {
			System.setOut(stdout);
			System.setErr(stderr);
			
			out.close();
		}
		out = null;
	}

}
