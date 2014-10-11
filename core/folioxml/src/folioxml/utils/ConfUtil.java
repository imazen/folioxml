package folioxml.utils;


import java.io.*;
import java.util.*;
import java.util.Map.Entry;



public class ConfUtil{
    
    private static Properties props = null;
    
    private static boolean loadedLock = false;
    
    private static String workingDir = null;
    public static void main(String[] args) {
    	printConfigs();
    }
    /**
     * init class handles loading class and initializing the properties file
     */
    private static void init(){
		props = new Properties();
		InputStream in = null;
		
		try {
			//this will work fine as long as the configuration.properties file is not to a different package.
			in = ConfUtil.class.getResourceAsStream("conf.properties");
			props.load(in);
			in.close();
			workingDir = System.getProperty("user.dir");
			if(workingDir!=null) {
				//removes the top 2 subfolders from the working directory
				workingDir = workingDir.substring(0,workingDir.lastIndexOf(slash()));
				workingDir = workingDir.substring(0,workingDir.lastIndexOf(slash()));
				System.out.println("Getting workingDir :"+ workingDir);
				
		    }else {
		    	throw new IOException("Unable to retrieve System.getProperties(\"user.dir\")");
		    }
		    
		    loadedLock = true;
		    
		}catch(IOException ioe) {
	    	ioe.printStackTrace();
	    	System.err.println("Error loading " + ConfUtil.class.getSimpleName() +" ");
	    }
    }
    
    public static String slash(){
    	return System.getProperty("file.separator");
    }
    
    public static void printConfigs(){
    	if(!loadedLock)init();
    	Set<Entry<Object,Object>> pairs = props.entrySet();
    	
    	ArrayList<String> c = new ArrayList<String>();
    	
    	for(Entry<Object,Object> p:pairs){
    		System.out.println(p.getKey().toString() + ": " + p.getValue().toString());
    	}
    	
    }
    /**
     * Converts path1 and path2 to the current OS path style, then joins using the current separator char.
     * @param path1
     * @param path2
     * @return
     */
    public static String join(String path1, String path2){
    	//TODO: handle .. and . 
    	char otherSlash = slash().charAt(0) == '/' ? '\\' : '/';
    	path1 = path1.replace(otherSlash, slash().charAt(0));
    	path2 = path2.replace(otherSlash, slash().charAt(0));
    	//Trim slashes
    	while (path1.endsWith(slash())) path1 = path1.substring(0,path1.length() -1);
    	while (path2.startsWith(slash())) path2 = path2.substring(1); 
    	
    	return path1 + slash() + path2;
    }
    
    public static String resolve(String path){
    	if (path.startsWith("/")) return path; //It's already absolute.
    	if(!loadedLock)init();
    	return join(workingDir,path);
    }
    
    private static String getProp(String s){
    	String val = props.getProperty(s);
    	assert(val != null): "Property " + s + " not found.";
    	return val;
    }
    
    
    boolean hasIncrementedVersion = false;
    
    public static int getIncrementedVersionNumber(String configName){
    	if(!loadedLock)init();
		return getVersionNumber(configName)+1;
    }
    
    public static int getVersionNumber(String configName){
    	if(!loadedLock)init();
    	return Integer.valueOf(getProp(configName + ".version")).intValue();
    }
    
    public static String getFFFPath(String configName){
    	if(!loadedLock)init();
    	return resolve(getProp(configName + ".path"));  
    }
    public static String getIndexPath(String configName){
    	if(!loadedLock)init();
    	return resolve(getProp(configName + ".index"));  
    }
    
    public static String getExportPath(String configName){
    	if(!loadedLock)init();
    	return resolve(getProp(configName + ".export"));  
    }
   
	public static String getExportFile(String configName, String filename) {
		return join( getExportPath(configName), filename);
	}
	
	public static String getExportFile(String configName, String filename, boolean makeParentDir) {
		return getExportFile(configName,null,filename,makeParentDir);
	}
	
	public static String getExportFile(String configName, Map<String,String> settings, String filename, boolean makeParentDir) {
		if(!loadedLock)init();
		if(settings == null){//djl 08-25-2010 attempt to fix null pointer error while running the eob export 
			settings = new HashMap<String,String> ();
		}
		String fileName =  join(( settings.get("export") != null ? resolve(settings.get("export")) : getExportPath(configName)), filename);
		if (makeParentDir) if (!new File(fileName).getParentFile().exists()) new File(fileName).getParentFile().mkdir();
		return fileName;
	}
	
	
	
	/**
	 * Returns an array of maps from the property. 
	 * @param configName
	 * @param property
	 * @return
	 */
	public static List<Map<String,String>> getArray(String configName, String property){
		if(!loadedLock)init();
	    /* example where property = "export.oeb" and configName = folioxml
	     * 
	     * folioxml.export.oeb.defaults.tocsAtEach = root, Chapter
		
            folioxml.paths = /123, /124, /125,

		 
	     */
		String baseProperty = configName + "." + property + ".";
		Map<Integer,Map<String,String>> items = new HashMap<Integer,Map<String,String>>();
		
		//Defaults.
		Map<String,String> defaults = getMap(baseProperty + "defaults.");
		
		//Get array items
		Map<String,String> allItems = getMap(baseProperty);
		
		Set<Entry<String,String>> pairs = allItems.entrySet();
    	for(Entry<String,String> p:pairs){
    		int firstDot = p.getKey().indexOf(".");
    		if (firstDot > 0){
    			String first = p.getKey().substring(0,firstDot);
    			if (first.matches("[0-9]+")){
    				//Base namespace
    				String subName = baseProperty + first + ".";
    				//Get the index
    				int index = Integer.parseInt(first);
    				if (items.containsKey(index)) continue; //Skip - never process the same number twice
    				
    				//Get the subitems
    				Map<String,String> subitems = new HashMap<String,String>();
    				subitems.putAll(defaults); //Insert defaults first
    				subitems.putAll(getMap(subName));
    				
    				items.put(index, subitems);
    			}
    		}
    		
    	}
    	
    	//Convert map from integer->map to a ordered list of maps.
    	List<Integer> indexes = asSortedList(items.keySet());
    	List<Map<String,String>> itemList = new ArrayList<Map<String,String>>();
    	for (Integer i:indexes){
    		itemList.add(items.get(i));
    	}
    	
		return itemList;
	}
	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}
	
	public static Map<String, String> getMap(String namespace){
		if(!loadedLock)init();
		HashMap<String,String> map = new HashMap<String,String>();
		
		Set<Entry<Object,Object>> pairs = props.entrySet();
    	for(Entry<Object,Object> p:pairs){
    		if (p.getKey().toString().startsWith(namespace)){
    			map.put(p.getKey().toString().substring(namespace.length()),p.getValue().toString());
    		}
    	}
		
		return map;
	}


    public static String slurp(final InputStream is, final int bufferSize)
    {
      final char[] buffer = new char[bufferSize];
      final StringBuilder out = new StringBuilder();
      try {
        final Reader in = new InputStreamReader(is, "UTF-8");
        try {
          for (;;) {
            int rsz = in.read(buffer, 0, buffer.length);
            if (rsz < 0)
              break;
            out.append(buffer, 0, rsz);
          }
        }
        finally {
          in.close();
        }
      }
      catch (UnsupportedEncodingException ex) {
        /* ... */
    	  ex.printStackTrace();
      }
      catch (IOException ex) {
    	  ex.printStackTrace();
          /* ... */
      }
      return out.toString();
    }


	
	
}