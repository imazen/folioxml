package folioxml.export.html;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.core.Pair;
import folioxml.core.TokenUtils;
import folioxml.export.FileNode;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RenameImages {

    ExportLocations export;

    public RenameImages(InfobaseSet infobase_set, ExportLocations export) {
        this.infobase_set = infobase_set;
        this.export = export;
        signatures = AllFileSignatures();

        nextAssetId = infobase_set.getInteger("asset_start_index");
        if (nextAssetId == null) nextAssetId = 0;

        asset_use_index_in_url = infobase_set.getBool("asset_use_index_in_url");
        if (asset_use_index_in_url == null) asset_use_index_in_url = false;

    }

    Boolean asset_use_index_in_url;

    public void process(NodeList nodes, FileNode document) throws InvalidMarkupException, IOException {

        Path document_base = export.getLocalPath(document.getRelativePath(), AssetType.Html, FolderCreation.None);

        //
        NodeList images = nodes.filterByTagName("img|object|link", true);
        for (Node n:images.list()){
            //If it's an image, convert it to an img tag, if it's a data link, convert to 'a'.


            //If type="ole" - oops!
            //if type="folio" - we can fix this.

            //link, dataLink
            //link, objectName,
            //object, type="folio"
            //object type="data-link"


            boolean isImage = (n.matches("object") && TokenUtils.fastMatches("bitmap|metafile|picture", n.get("handler"))) || n.matches("img");


            boolean isImageLink = n.matches("link|a") && TokenUtils.fastMatches("bitmap|metafile|picture", n.get("handler"));


            boolean isObjectLink = n.matches("link") && (n.get("objectName") != null || n.get("dataLink") != null);

            if (n.matches("link") && !isObjectLink) continue; //We don't care about web, program, query, popup, jump, or menu links.
            /*boolean isLinkToImage


            if (TokenUtils.fastMatches("folio|data-link", n.get("type"))){
                continue;
            }

            boolean isDataLink =

            if (TokenUtils.fastMatches("bitmap|metafile|picture",handler)){ //Convert these three types to "img" tags immediately.


            }*/


            String attr = n.matches("img|object") ? "src" : "href";

            //Fix path. It will be relative, since it is from a local embedded object.
            String src = n.get(attr);
            if (src != null) {
                //Parse the file signature (cached on 'src'
                BundledAsset b = getAsset(src);

                if (b.success){
                    String resultUri = export.getUri(asset_use_index_in_url ? Long.toString(b.assetId) : b.targetPath, AssetType.Image, document_base);
                    n.set(attr, resultUri);
                    n.set("resolved", "true");
                    if (isImage){
                        n.setTagName("img");
                        n.removeAttr("type");
                        n.removeAttr("handler");
                        b.alt = n.get("name");
                        n.set("alt", n.get("name")); //The alt tag can use the name
                        n.removeAttr("name");
                    }else if (isImageLink){
                        n.setTagName("a");
                        n.removeAttr("type");
                        n.removeAttr("handler");
                        b.alt = n.get("objectName");
                        n.set("alt", n.get("objectName")); //The alt tag can use the name
                        n.removeAttr("objectName");
                    }else if (isObjectLink){
                        n.setTagName("a");
                        b.alt = n.get("dataLink");
                        n.set("alt", b.alt);
                        if (n.get("mime") != null) n.set("data-mime", n.get("mime"));
                        if (n.get("dataLink") != null) n.set("data-linkname", n.get("dataLink"));
                        n.removeAttr("mime");
                        n.removeAttr("dataLink");

                    }
                }else{
                    //Unscucessfully.

                }

            }


            //TODO: catch the rest
        }
    }


    private ArrayList<Pair<String, byte[]>> signatures;

    private InfobaseSet infobase_set;

    public Integer nextAssetId;

    public class BundledAsset{

        public BundledAsset(){}
        public Path originalDiskLocation;
        public String originalFileExtension;
        public String originalPath;
        public Path targetDiskLocation;
        public String targetFileExtension;
        public String targetPath;
        public String alt;

        public int assetId;
        public InfobaseConfig infobase;
        public boolean dataLink;

        public boolean success;
        public String error_message;


    }

      BundledAsset fail(String path, String message) {
         BundledAsset b = new BundledAsset();
         b.success = false;
         b.error_message = path;
         b.originalPath = path;
         System.err.println(path);
         System.err.println(message);
         return b;
     }


    //Flags
    //Drop extension in XML - assumes our uploader also drops the extension and replaces it with a content type
    //Convert BMP files to PNG.

    protected static Pattern object_file = Pattern.compile("\\A([^\\\\/]+)[\\\\/]FFF([0-9]+).(OB|OLE|BMP)\\Z");
    protected static Pattern data_file = Pattern.compile("\\A([^\\\\/]+)[\\\\/]Data[\\\\/]([^\\\\/]+)\\Z");


    private HashMap<String, BundledAsset> assets = new HashMap<String, BundledAsset>();


    public void CopyConvertFiles() throws IOException{
        for(Map.Entry<String,BundledAsset> pair: assets.entrySet()){
            BundledAsset target = pair.getValue();
            if (!target.success) continue;

            //If the destination file doesn't exist, copy
            if (!Files.exists(target.targetDiskLocation)){
                if (!Files.isDirectory(target.targetDiskLocation.getParent())){
                    Files.createDirectory(target.targetDiskLocation.getParent());
                }
                try{
                    if ("bmp".equals(target.originalFileExtension) && "png".equals(target.targetFileExtension)){
                        ConvertToPng(target.originalDiskLocation.toFile(), target.targetDiskLocation.toFile());
                    }else {
                        Files.copy(target.originalDiskLocation,  target.targetDiskLocation, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }catch(IOException e){
                    System.err.println("Failed to copy/compress to " + pair.getValue().targetDiskLocation);
                    e.printStackTrace(System.err);
                }

            }
        }
    }

    public void ExportAssetInventory() throws IOException, InvalidMarkupException {
        Path xmlPath = export.getLocalPath("AssetInventory.xml", AssetType.Xml,FolderCreation.CreateParents);


        BufferedWriter out  = Files.newBufferedWriter(xmlPath, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW);


        out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

        Node assetsNode = new Node("<assets />");


        for(Map.Entry<String,BundledAsset> pair: assets.entrySet()){
            BundledAsset target = pair.getValue();
            if (!target.success) continue;

            Node n = new Node("<asset />");

            n.set("asset_id", Long.toString(target.assetId));
            if (target.alt != null) n.set("alt", target.alt);
            n.set("dataLink", Boolean.toString(target.dataLink));

            n.set("originalPath", target.originalPath.toString());
            n.set("orginalDiskLocation", target.originalDiskLocation.toString());
            if (target.originalFileExtension != null) n.set("originalFileType", target.originalFileExtension.toString());
            n.set("targetPath", target.targetPath.toString());
            n.set("targetDiskLocation", target.targetDiskLocation.toString());
            if (target.targetFileExtension != null) n.set("targetFileType", target.targetFileExtension.toString());

            assetsNode.addChild(n);
        }
        out.append(assetsNode.toXmlString(true));
        out.close();
    }
    public void ConvertToPng(File input, File output) throws IOException{
        //Read the file to a BufferedImage
        BufferedImage image = ImageIO.read(input);

        //Write the image to the destination as a PNG
        ImageIO.write(image, "png", output);
    }




    private BundledAsset parse(String path) throws IOException {

        BundledAsset b = new BundledAsset();
        b.success = true;
        b.originalPath = path;

        Matcher m = data_file.matcher(path);
        b.dataLink = true;
        if (!m.find()) {
            m = object_file.matcher(path);
            b.dataLink = false;

            if (!m.find()){
                //Not a data link or object/ole? skip.
                return fail(path, "Path is not a Data link or OB/OLE file.");
            }
        }


        //Which infobase does it correspond with
        b.infobase = infobase_set.byName(m.group(1));
        if (b.infobase == null){
            return fail(path, "Failed to find corresponding InfobaseConfig for '" + m.group(1) + "'");
        }

        //What's the full source file path
        b.originalDiskLocation = Paths.get(b.infobase.getFlatFilePath()).resolveSibling(path.replace("\\", File.separator)).toAbsolutePath();



        String filename = null;
        if (m.pattern() == data_file){
            b.targetPath = m.group(2).toLowerCase(Locale.ENGLISH); //Use existing filename
        }else{
            //object or OLE file
            byte[] buffer = null;
            try{
                buffer = getFileSignature(b.originalDiskLocation.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                return fail(path, b.originalDiskLocation.toString() + "\n" + e.toString());
            }
            String ext = getExtensionForSignature(buffer);
            if (ext == null){
                return fail(path, "Unknown file type; unrecognized magic byte signature.");
            }
            b.originalFileExtension = ext;
            //Convert bmp to png
            b.targetFileExtension = ext.equalsIgnoreCase("bmp") ? "png" : ext;
            //m.group(3) == OB or OLE
            b.targetPath = m.group(2) + "." + b.targetFileExtension;
        }

        b.targetPath = Paths.get(b.infobase.getId()).resolve(Paths.get(b.targetPath)).toString();
        b.targetDiskLocation = export.getLocalPath(b.targetPath, AssetType.Image, FolderCreation.None);
        return b;
    }

    public BundledAsset getAsset(String path) throws IOException {

        BundledAsset b;
        //If we have renamed this file before, reuse result
        if (assets.containsKey(path)) {
            b = assets.get(path);
        } else {
            b = parse(path);
            b.assetId = nextAssetId;
            nextAssetId++;
            assets.put(path, b);
        }
        return b;
    }
    public String modifyImageUrl(String path, FileNode document_base) throws IOException {

        BundledAsset b = getAsset(path);
        if (!b.success) return path;
        //TODO: log failure path into attributes, perhaps?

        Path document = export.getLocalPath(document_base.getRelativePath(), AssetType.Html, FolderCreation.None);

        return export.getUri(b.targetPath,AssetType.Image,document);
    }


    public static byte[] getFileSignature(File file)  throws IOException{
        InputStream ios = null;
        try {
            byte[] buffer = new byte[12];
            ios = new FileInputStream(file);
            int bytesRead = ios.read(buffer);
            return buffer;
        } finally {
            if (ios != null)
                ios.close();
        }
    }


    public String getExtensionForSignature(byte[] buffer) {
        for(Pair<String,byte[]> sig: signatures){
            byte[] bsig = sig.getSecond();
            boolean match = true;
            for (int i = 0; i < bsig.length && i < buffer.length; i++){
                if (bsig[i] != buffer[i]) {
                    match = false; //not a match
                    break;
                }
            }
            if (match) return sig.getFirst();
        }
        return null;
    }

    //https://en.wikipedia.org/wiki/List_of_file_signatures
    private static byte[] jpeg = new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0};
    private static byte[] gif87 = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38, (byte)0x37, (byte)0x61};
    private static byte[] gif89 = new byte[]{(byte)0x47,(byte)0x49,(byte)0x46,(byte)0x38, (byte)0x39, (byte)0x61};
    private static byte[] ico = new byte[]{(byte)0,(byte)0,(byte)1,(byte)0};
    private static byte[] png = new byte[]{(byte)0x89,(byte)0x50,(byte)0x4e,(byte)0x47, (byte)0x0D, (byte)0x0a, (byte)0x1A, (byte)0x0A};
    private static byte[] pdf = new byte[]{(byte)0x25,(byte)0x50,(byte)0x44,(byte)0x46};
    private static byte[] tiff_little = new byte[]{(byte)0x49,(byte)0x49,(byte)0x2A,(byte)0x00};
    private static byte[] tiff_big = new byte[]{(byte)0x4D,(byte)0x4D,(byte)0x00,(byte)0x2A};
    private static byte[] bmp = new byte[]{(byte)0x42,(byte)0x4D};
    private static ArrayList<Pair<String, byte[]>> AllFileSignatures(){
        ArrayList<Pair<String, byte[]>> signatures = new ArrayList<Pair<String, byte[]>>();
        signatures.add(new Pair<String,byte[]>("jpg",jpeg));
        signatures.add(new Pair<String,byte[]>("tiff",tiff_big));
        signatures.add(new Pair<String,byte[]>("tiff",tiff_little));
        signatures.add(new Pair<String,byte[]>("pdf",pdf));
        signatures.add(new Pair<String,byte[]>("png",png));
        signatures.add(new Pair<String,byte[]>("ico",ico));
        signatures.add(new Pair<String,byte[]>("gif",gif87));
        signatures.add(new Pair<String,byte[]>("gif",gif89));
        signatures.add(new Pair<String,byte[]>("bmp",bmp));
        return signatures;
    }
}
