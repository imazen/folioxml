package folioxml.export.html;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RenameImages extends FixImagePaths {

    public RenameImages(InfobaseSet infobase_set) {
        this.infobase_set = infobase_set;
        signatures = AllFileSignatures();
    }
    private ArrayList<Pair<String, byte[]>> signatures;

    private InfobaseSet infobase_set;

    //Flags
    //Drop extension in XML - assumes our uploader also drops the extension and replaces it with a content type
    //Convert BMP files to PNG.

    protected static Pattern object_file = Pattern.compile("\\A([^\\\\/]+)[\\\\/]FFF([0-9]+).(OB|OLE|BMP)\\Z");
    protected static Pattern data_file = Pattern.compile("\\A([^\\\\/]+)[\\\\/]Data[\\\\/]([^\\\\/]+)\\Z");


    private HashMap<String, String> renamed = new HashMap<String, String>();
    public HashMap<String, String> to_copy = new HashMap<String, String>();
    public HashMap<String, String> to_compress = new HashMap<String, String>();
    public HashMap<String, String> failed = new HashMap<String, String>();

    public void CopyFiles() throws IOException{
        for(Map.Entry<String,String> pair: to_copy.entrySet()){
            Path target = Paths.get(pair.getValue());
            Path source = Paths.get(pair.getKey());
            //If the destination file doesn't exist, copy
            if (!Files.exists(target)){
                if (!Files.isDirectory(target.getParent())){
                    Files.createDirectory(target.getParent());
                }
                try{
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }catch(IOException e){
                    System.err.println("Failed to copy to " + pair.getValue());
                    e.printStackTrace(System.err);
                }

            }
        }
    }
    public void CompressFiles() throws IOException{
        for(Map.Entry<String,String> pair: to_compress.entrySet()){
            Path target = Paths.get(pair.getValue());
            Path source = Paths.get(pair.getKey());
            //If the destination file doesn't exist, copy
            if (!Files.exists(target)){
                if (!Files.isDirectory(target.getParent())){
                    Files.createDirectory(target.getParent());
                }
                try{
                    ConvertToPng(source.toFile(),target.toFile());
                }catch(IOException e){
                    System.err.println("Failed to compress to " + pair.getValue());
                    e.printStackTrace(System.err);
                }

            }
        }
    }
    public void ConvertToPng(File input, File output) throws IOException{
        //Read the file to a BufferedImage
        BufferedImage image = ImageIO.read(input);

        //Write the image to the destination as a PNG
        ImageIO.write(image, "png", output);
    }




    private String failure(String path, String message){
        failed.put(path, message);
        System.err.println(path);
        System.err.println(message);
        return path;
    }
    @Override
    public String modifyImageUrl(String path) {
        //If we have renamed this file before, reuse result
        if (renamed.containsKey(path))
            return renamed.get(path);

        //Did we fail to access or categorize this file?
        if (failed.containsKey(path))
            return path;

        InfobaseConfig ib = null;

        Matcher m = data_file.matcher(path);
        if (!m.find()) m = object_file.matcher(path);

        if (!m.find()){
            //Not a data link or object/ole? skip.
            return failure(path, "Path is not a Data link or OB/OLE file.");
        }


        //Which infobase does it correspond with
        ib = infobase_set.byName(m.group(1));
        if (ib == null){
            return failure(path, "Failed to find corresponding InfobaseConfig for '" + m.group(1) + "'");
        }

        //What's the full source file path
        java.nio.file.Path image = Paths.get(ib.getFlatFilePath()).resolveSibling(path.replace("\\", File.separator));


        boolean recompress = false;
        String filename = null;
        if (m.pattern() == data_file){
            filename = m.group(2).toLowerCase(Locale.ENGLISH); //Use existing filename
        }else{
            //object or OLE file
            byte[] buffer = null;
            try{
                buffer = getFileSignature(image.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                return failure(path, image.toString() + "\n" + e.toString());
            }
            String ext = getExtensionForSignature(buffer);
            if (ext == null){
                return failure(path, "Unknown file type; unrecognized signature in " + path);
            }

            if (ext.equalsIgnoreCase("bmp")){
                recompress = true;
                ext = "png";
            }
            //m.group(3) == OB or OLE
            filename = m.group(2) + "." + ext;
        }

        String relative = Paths.get(ib.getId()).resolve(filename).toString();
        renamed.put(path, relative);
        (recompress ? to_compress : to_copy).put(image.toAbsolutePath().toString(), Paths.get(ib.getExportDir(false)).resolve(relative).toString());
        return relative;
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
