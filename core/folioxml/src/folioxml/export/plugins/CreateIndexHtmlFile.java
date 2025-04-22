package folioxml.export.plugins;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.export.*;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxRecord;
import folioxml.xml.XmlRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateIndexHtmlFile implements InfobaseSetPlugin {

    private ExportLocations exportLocations;
    private LogStreamProvider logs;
    // Use a LinkedHashMap to store FileNodes, keyed by relative path to ensure uniqueness
    // while preserving a reasonable order (though we'll sort later).
    private Map<String, FileNode> generatedFileNodes = new LinkedHashMap<>();
    private FileNode lastFileNode = null; // Optimization to reduce map lookups/puts

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.exportLocations = export;
        this.logs = logs;
        this.generatedFileNodes.clear(); // Ensure clean state for new run
        this.lastFileNode = null;
    }

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        // No action needed per infobase start
    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        // No reader wrapping needed
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        // No action needed per SLX record
    }

    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        // No action needed per transformed record
    }

    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        // Does not assign file nodes itself, just collects them
        return null;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {
        // Collect unique FileNode objects as they are completed
        if (file != null && file != lastFileNode) { // Check if it's a new file node instance
            String relativePath = file.getRelativePath();
            if (relativePath != null) {
                 // Put/update the map entry for this path. Overwriting is fine.
                generatedFileNodes.put(relativePath, file);
                lastFileNode = file;
            }
        }
    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {
        // No action needed per infobase end
    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException, InvalidMarkupException {
        if (exportLocations == null) {
            logs.getNamedStream("CreateIndexHtmlFile").append("Error: ExportLocations not configured. Cannot create index file.\n");
            return;
        }
        String filename = "index";
        Path indexFilePath = exportLocations.getLocalPath(filename, AssetType.Html, FolderCreation.None);

        if (generatedFileNodes.isEmpty()) {
            logs.getNamedStream("CreateIndexHtmlFile").append("Warning: No FileNodes collected. Index file will be empty or not created.\n");
            // Optionally create an empty index file or just return
             try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFilePath.toFile()), StandardCharsets.UTF_8))) {
                 writeEmptyIndex(writer, "Generated HTML Files Index (No files found)");
                 return;
             } catch (IOException e) {
                 logs.getNamedStream("CreateIndexHtmlFile").append("Error writing empty index file " + indexFilePath + ": " + e.getMessage() + "\n");
                 throw e;
             }
        }

        // Convert map values to list and sort alphabetically by relative path
        List<FileNode> sortedNodes = new ArrayList<>(generatedFileNodes.values());
        sortedNodes.sort(Comparator.comparing(FileNode::getRelativePath));


        // Generate the index.html file
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFilePath.toFile()), StandardCharsets.UTF_8))) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("  <meta charset=\"utf-8\" />\n");
            writer.write("  <title>Generated HTML Files Index</title>\n");
            writer.write("  <style>\n");
            writer.write("    body { font-family: sans-serif; }\n");
            writer.write("    ul { list-style: none; padding-left: 1em; }\n"); // Indent list slightly
            writer.write("    li { margin-bottom: 5px; }\n");
            writer.write("    a { text-decoration: none; color: #0000EE; }\n"); // Standard link blue
            writer.write("    a:hover { text-decoration: underline; }\n");
            writer.write("    .filepath { font-size: 0.8em; color: #555; margin-left: 1em; }\n"); // Style for showing path
            writer.write("  </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("  <h1>Generated HTML Files Index</h1>\n");
            writer.write("  <ul>\n");

            for (FileNode node : sortedNodes) {
                String targetRelativePath = node.getRelativePath();
                String title = node.getAttributes().get("heading");
                if (title == null || title.trim().isEmpty()) {
                    title = targetRelativePath; // Fallback to relative path if heading is missing
                }

                // Calculate the relative URI from the index file to the target file
                // getUri needs the *directory* of the source file (index.html)
                String href = exportLocations.getUri(targetRelativePath, AssetType.Html, indexFilePath);

                // Basic sanitization for title
                title = title.replace("<", "&lt;").replace(">", "&gt;");

                writer.write("    <li><a href=\"" + href + "\">" + title + "</a> <span class=\"filepath\">(" + targetRelativePath + ")</span></li>\n");
            }

            writer.write("  </ul>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
            logs.getNamedStream("CreateIndexHtmlFile").append("Successfully generated index file: " + indexFilePath + " with " + sortedNodes.size() + " entries.\n");
        } catch (IOException e) {
            logs.getNamedStream("CreateIndexHtmlFile").append("Error writing index file " + indexFilePath + ": " + e.getMessage() + "\n");
            throw e; // Rethrow to signal failure
        }
    }

    // Helper to write a basic empty index page
    private void writeEmptyIndex(BufferedWriter writer, String title) throws IOException {
         writer.write("<!DOCTYPE html>\n");
         writer.write("<html>\n");
         writer.write("<head>\n");
         writer.write("  <meta charset=\"utf-8\" />\n");
         writer.write("  <title>" + title + "</title>\n");
         writer.write("  <style>body { font-family: sans-serif; }</style>\n");
         writer.write("</head>\n");
         writer.write("<body>\n");
         writer.write("  <h1>" + title + "</h1>\n");
         writer.write("  <p>No HTML files were generated or found to include in this index.</p>\n");
         writer.write("</body>\n");
         writer.write("</html>\n");
    }
} 