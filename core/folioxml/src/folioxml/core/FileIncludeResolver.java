package folioxml.core;

import java.io.*;


/**
 * An implementation of IIncludeResolutionService for the filesystem. FolioTokenReader knows nothing about the location of the data it is reading, so it accepts a 'resolution service' instance while allows it to retrieve 'include' files.
 *
 * @author nathanael
 */
public class FileIncludeResolver implements IIncludeResolutionService {
    /**
     * Creates a file resolver using the specified base path.
     *
     * @param baseDocument Must be an absolute path
     */
    public FileIncludeResolver(String baseDocument) {
        //Parse base document string
        this.baseDocument = new File(baseDocument);
        assert (this.baseDocument.isAbsolute());

        //Calculate base dir
        if (this.baseDocument.isFile()) this.baseDirectory = this.baseDocument.getParentFile();
        else this.baseDirectory = this.baseDocument;
    }

    private File baseDocument;
    private File baseDirectory;


    /**
     * Returns a file reference from the specified (possibly relative) path
     *
     * @param fileReference
     * @return
     * @throws java.io.FileNotFoundException
     */
    public Reader getReader() throws FileNotFoundException {
        try {
            return new InputStreamReader(new FileInputStream(baseDocument), "Windows-1252");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


    /**
     * This returns the hash for the base document, so it can be excluded from circular references.
     * This is a hashing function that the circular reference tracking uses.
     * Creats and absolute string that should be a function of the referenced file, not just the string.
     *
     * @return
     */
    public String getHash() throws IOException {
        return baseDocument.getCanonicalPath().toLowerCase();

    }

    public String getDescription() {
        if (baseDocument == null) return "null baseDocument";
        else return baseDocument.toString();
    }

    /**
     * Takes an absolute or relative path and returns an absolute File instance.
     *
     * @param path
     * @return
     */
    private File resolve(String path) {
        File f = new File(path);
        //assert(f.isFile());
        if (f.isAbsolute()) return f; //nothing needed.
        else {
            //relative
            return new File(baseDirectory, path);
        }
    }

    /**
     * Returns an instance of the class with the specified path as the new base location. Unlike the constructor, this function accepts relative paths.
     *
     * @param fileReference
     * @return
     * @throws java.io.IOException
     */
    public IIncludeResolutionService getChild(String fileReference) throws IOException {
        return new FileIncludeResolver(resolve(fileReference).getCanonicalPath());
    }
}
    