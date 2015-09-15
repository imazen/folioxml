package folioxml.lucene;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.LogStreamProvider;
import folioxml.lucene.analysis.AnalyzerPicker;
import folioxml.lucene.analysis.DynamicAnalyzer;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxContextStack;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import folioxml.xml.XmlRecord;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class InfobaseSetIndexer implements InfobaseSetPlugin, AnalyzerPicker {


    public InfobaseSetIndexer() {

    }

    InfobaseFieldOptsSet conf;
    IndexWriter w;

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException {

        Path folder = export.getLocalPath("lucene_index", AssetType.LuceneIndex, FolderCreation.None);
        w = new IndexWriter(FSDirectory.open(folder), new IndexWriterConfig(new DynamicAnalyzer(this)).setOpenMode(IndexWriterConfig.OpenMode.CREATE));

    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) {
        conf = null;
        currentInfobase = infobase;
    }

    InfobaseConfig currentInfobase;

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    Document doc = null;

    @Override
    public void onSlxRecordParsed(SlxRecord r) throws InvalidMarkupException {
        boolean isRoot = r.isRootRecord();

        //Create lucene document, add some fields.
        doc = new Document();
        //Add level, groups, infobase
        if (r.getLevelType() == null) {
            doc.add(addNonTokenizedField("level", "Normal"));
        } else {
            doc.add(addNonTokenizedField("level", r.getLevelType()));
        }
        doc.add(addAnalyzedField("groups", r.get("groups")));
        doc.add(addNonTokenizedField("infobase", currentInfobase.getId()));

        if (!isRoot) {
            //Iterate all tokens and stream to applicable fields so a query can be evaluated later
            FieldCollector coll = new FieldCollector(doc, conf);

            StringBuilder contentSb = new StringBuilder();
            SlxContextStack stack = new SlxContextStack(false, false);
            List<String> destinations = new ArrayList<String>();
            stack.process(r);
            String spacing = TokenUtils.entityDecodeString(" &#x00A0; ");
            for (SlxToken t : r.getTokens()) {
                stack.process(t);// call this on each token.

                //Hidden to indexing, not to view. This is totally separate from what ExportHiddenText does.
                boolean hidden = coll.collect(t, stack, r);

                if (!hidden && t.isTextOrEntity()) { //Changed dec 17 to include whitespace... was causing indexing errors.. fields separated by whitespace were being joined.
                    String s = t.markup;
                    if (t.isEntity()) s = TokenUtils.entityDecodeString(s);
                    contentSb.append(s);
                }
                if (t.matches("p|br|td|th|note") && !t.isOpening()) {
                    contentSb.append(spacing);
                }
                if (t.isTag() && t.matches("bookmark")) {
                    //Add bookmarks as-is
                    doc.add(new StringField("destinations", t.get("name"), Field.Store.YES));
                }
            }

            doc.add(new TextField(conf.getDefaultField(), contentSb.toString(), Field.Store.YES));

            String folioSectionHeading = TokenUtils.entityDecodeString(r.getFullHeading(",", false, 20)).trim();
            doc.add(new TextField("folioSectionHeading", folioSectionHeading, Field.Store.YES));


            doc.add(new StoredField("title", r.getFullHeading(" - ", true, 2)));
            doc.add(new StoredField("heading", r.get("heading")));

            coll.flush();
        }
    }


    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        return null;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {
        //Add URI
        if (xr.get("uri") != null) doc.add(new StoredField("uri", xr.get("uri")));
        //Add

        String relative_path = file.getAttributes().get("relative_path");
        String uri_fragment = file.getAttributes().get("uri_fragment");

        if (relative_path == null || uri_fragment == null) {
            throw new InvalidMarkupException("Both relative_path and uri_fragment must be defined on the FileNode for indexing");
        }

        doc.add(new StoredField("relative_path", relative_path));
        doc.add(new StoredField("uri_fragment", uri_fragment));

        if (xr.isRootRecord()) {
            //Configure field indexing based on the .DEF file.
            conf = new InfobaseFieldOptsSet(xr);
            doc.add(new StoredField("xml", xr.toXmlString(false)));
        }

        w.addDocument(doc);
    }

    @Override
    public void onRecordTransformed(XmlRecord r, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public void endInfobase(InfobaseConfig infobase) {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {
        try {
            w.commit();
        } finally {
            w.close();
        }
    }


    private Field addNonTokenizedField(String name, String value) {
        return new StringField(name, value.toLowerCase(Locale.ENGLISH).trim(), Field.Store.YES);
    }


    private Field addAnalyzedField(String name, String value) {
        if (value == null) value = ""; //Some records have no groups... causing null
        return new TextField(name, value, Field.Store.YES);
    }

    @Override
    public Analyzer getAnalyzer(String fieldName) {
        return conf.getAnalyzer(fieldName);
    }
}
