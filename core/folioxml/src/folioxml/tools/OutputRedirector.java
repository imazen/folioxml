package folioxml.tools;

import java.io.*;

public class OutputRedirector {
	PrintStream stdout = null;
	PrintStream stderr = null;
	String filename = null;
	protected FileOutputStream out = null;
	public OutputRedirector(String filename){
		this.filename = filename;
	}

	public void open() throws FileNotFoundException, UnsupportedEncodingException {
		//Save so we can restore them later.
        stdout = System.out;                                        
        stderr = System.err;   
        //Make the dir if it is missing
		if (!new java.io.File(filename).getParentFile().exists()) new java.io.File(filename).getParentFile().mkdirs();
		
		out  = new FileOutputStream(filename);
		System.out.println("Redirecting output to " + filename);
		System.setOut(new PrintStream(out,true, "UTF-8"));
		System.setErr(new PrintStream(out,true, "UTF-8"));
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
