package folioxml.core;

import java.io.*;
import java.util.ArrayList;


/**
 * An implementation of IIncludeResolutionService for testing. You 'pre-fill' the class with avaialble includes by calling the add() method. 
 * The collection of available includes is shared with each Resolver that .add() is called on. That way .getChild() doesn't affect the possibilities.
 * @author nathanael
 */
public class StringIncludeResolver implements IIncludeResolutionService{
    
    public StringIncludeResolver(){
        this("test.fff","");
    }
    /**
     * Creates a StringIncludeResolver. 
     */
    public StringIncludeResolver(String filename, String data){
        //Parse base document string
        this.baseDocument = new File(filename).getAbsoluteFile();
        assert(this.baseDocument.isAbsolute());
        this.data = data;
        
        //Calculate base dir
         this.baseDirectory = this.baseDocument.getParentFile();
        
    }
    
    private File baseDocument;
    private File baseDirectory;
    private String data;

    /**
     * Returns a reader
     * @param fileReference
     * @return
     * @throws java.io.FileNotFoundException
     */
    public Reader getReader() throws FileNotFoundException {
        return new StringReader(data);
    }
    
    private ArrayList<StringIncludeResolver> files = new ArrayList<StringIncludeResolver>();

    public StringIncludeResolver add(StringIncludeResolver sir){
        files.add(sir);
        sir.files = files; 
        return this;
    }
    /**
     * This returns the hash for the base document, so it can be excluded from circular references. 
     * This is a hashing function that the circular reference tracking uses.
     * Creats and absolute string that should be a function of the referenced file, not just the string.
     * @return
     */
    public String getHash() throws IOException {
        return baseDocument.getCanonicalPath().toLowerCase();
                
    }
    
    public String getDescription(){
        return baseDocument.toString();
    }
    /**
     * Takes an absolute or relative path and returns an absolute File instance.
     * @param path
     * @return
     */
    private File resolve(String path){
        File f = new File(path);
        
        if (f.isAbsolute()) return f; //nothing needed.
        else{
            //relative
            return new File(baseDirectory,path);
        }
    }

    /**
     * Gets a child for the specified sub-reference. In this implementation, the only available childern are those added by the .add() method.
     * The collection is shared among all children, so recursive includes are possibile as long as the filenames are added once at the root level.
     * @param fileReference
     * @return
     * @throws java.io.IOException
     */
    public IIncludeResolutionService getChild(String fileReference) throws IOException {
        String hash = resolve(fileReference).getCanonicalPath().toLowerCase();
        for (int i = 0; i < files.size(); i++){
            if (files.get(i).getHash().equals(hash)) return files.get(i);
        }
        throw new IOException("The specified include \"" + fileReference + "\" was not found - please add it using .add() before tryingt to include it.");
    }
}
    