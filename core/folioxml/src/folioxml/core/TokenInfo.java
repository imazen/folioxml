package folioxml.core;

/**
 * Used within FolioToken as a storage device for information about where the token was located in the source file.
 * Populated from FolioTokenReader
 * @author nathanael
 *
 */
public class TokenInfo{

    public TokenInfo(){}
    public long line = 0;
    public long col = 0;
    public long charIndex = 0;
    public long length = 0;
    public String text = null;
    public IIncludeResolutionService parentService;

    public String toString(){
        if (parentService == null)
            return "Line " + Long.toString(line + 1) + " col " + Long.toString(col + 1);
        else
            return "Line " + Long.toString(line + 1) + " col " + Long.toString(col + 1) + " in " + parentService.getDescription();
    }
}