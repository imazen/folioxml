package folioxml.core;

import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxToken;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


public class FolioToSlxDiagnosticTool implements ISlxTokenReader {


    /**
     * To verify that the markup is being converted correctly, we need some diagnostic tools.
     * <p>
     * At translation level -
     * Output a file containing pairs of SlxToken and FolioToken? objects.
     * - This should help us spot anomolies in tag formatting, and understand the diversity of incoming markup. Adding frequency data would also be helpful.
     * - Output a table of character, and entity use (with frequency). This should help up spot potential character encoding issues.
     * <p>
     * After Transformation -
     * A table of all unique Tag, Entity, and Comment SlxTokens? and their frequencies. Grouping by (tag|entity|comment) would be good.
     * Also, the original names -> new CSS names table would be useful.
     * <p>
     * I thought I would need it today, but I won't until next Monday. SlxToken objects have a FolioToken? property - this should make it very easy to build the tables.
     * Both offer .toString().
     * Your diagnostic table builder should work as an ISlxTokenReader than wraps another ISlxTokenReader. This will allow it to wrap SlxTranslatingReader?.
     * It should also have methods for processing a record.
     * Two instances will be needed to monitor both SLX Valid and SLX Transitional.
     * You can get the original names->css names from the SlxTokenReader?.cssCleaner instance.
     * Skip comments for now - I know there are 200,000 unique comments in the infobase. We gotta filter out PID and KPN comments first...
     * Add/Change #42 (Diagnostic tools)
     **/


    // table contains pairs
    private HashMap<Pair<String, String>, Long> tagTablePair = null;
    private HashMap<Pair<String, String>, Long> entityTablePair = null;
    //TODO: add comments back in
    //skipping comments for now
    //private HashMap<Pair<String,String>,Long> commentTablePair = null;

    public FolioToSlxDiagnosticTool(ISlxTokenReader slxTokenReader) {
        this.reader = slxTokenReader;
        tagTablePair = new HashMap<Pair<String, String>, Long>();
        entityTablePair = new HashMap<Pair<String, String>, Long>();
        //skipping comments for now
        // commentTablePair = new HashMap<Pair<String,String>,Long>();
    }


    public void outputDataFiles(String filename) throws InvalidMarkupException {
        exportTableMapToFile(tagTablePair, filename);
    }

    private boolean exportTableMapToFile(Map<Pair<String, String>, Long> map, String fileName) throws InvalidMarkupException {
        System.out.println(fileName);
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<Pair<String, String>, Long>> iterator = null;
        iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Pair<String, String>, Long> nextPair = iterator.next();

            sb.append(nextPair.getKey().getFirst());
            sb.append("	");
            sb.append(nextPair.getKey().getSecond());
            sb.append("	");

            SlxToken t = new SlxToken(nextPair.getKey().getSecond());

            sb.append(t.getTagName());
            sb.append("	");
            String ft = nextPair.getKey().getFirst();
            if (ft != null && ft.length() > 3) {
                if (ft.charAt(1) == '/') ft = ft.substring(2, 4);
                else ft = ft.substring(1, 3);
            }

            sb.append(ft);
            sb.append("	");
            sb.append(nextPair.getValue());

            //sb.append("|");
            sb.append("\n");

        }

        if (!new File(fileName).getParentFile().exists()) new File(fileName).getParentFile().mkdir();

        Writer fw = null;

        try {
            fw = Files.newBufferedWriter(Paths.get(fileName), Charset.forName("UTF-8"));

            fw.write(sb.toString());

            fw.close();
        } catch (IOException iox) {
            iox.printStackTrace();
            fw = null;
            return false;
        }
        return true;
    }

    protected ISlxTokenReader reader;
    //protected FolioSlxTranslator translator = new FolioSlxTranslator();

    /**
     * Reads the next translated token from the stream
     *
     * @return
     * @throws java.io.IOException
     * @throws folioxml.core.InvalidMarkupException
     */
    public SlxToken read() throws IOException, InvalidMarkupException {
        SlxToken st = reader.read();
        if (st == null) return null;

        if (!"record".equals(st.getTagName())) {


            //classify token type and add new pair or increment pair frequency
            if (st.isTag()) {

                //create FolioToken & SlxToken String pair
                Pair<String, String> pair = new Pair<String, String>(st.sourceToken.text, st.toString());
                tagTablePair.put(pair, (tagTablePair.containsKey(pair) ? tagTablePair.get(pair) + 1 : 1));

            } else if (st.isEntity()) {
                //entityTablePair.put(pair, (entityTablePair.containsKey(pair) ? entityTablePair.get(pair) + 1 : 1));

            } else if (st.isComment()) {
                // skipping comments for now
                //	commentTablePair.put(pair, (commentTablePair.containsKey(pair) ? commentTablePair.get(pair) + 1 : 1));

            } else if (st.isTextOrEntity()) {
                //ignore this branch
            } else {
                //this should never happen (I think?)
                throw new InvalidMarkupException("Error in SlxToken  " + "FolioToken:" + st.sourceToken.text + "\t\tSlxToken:" + st.toString());
            }

        }
        //System.err.println("FolioToken:" + st.sourceToken.text +"\nSLXToken:" +st.toString());

        return st;

    }

    public boolean canRead() {
        return reader.canRead();
    }

    /**
     * Closes the underlying reader
     */
    public void close() throws IOException {


        reader.close();
        reader = null;
    }
}
