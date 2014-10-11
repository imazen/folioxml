package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.css.CssClassCleaner;
import folioxml.folio.FolioToken;
import folioxml.slx.SlxToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Doesn't deal with Popup links (PW), although named popup links are handled. 
 * @author nathanael
 *
 */
public class FolioLinkUtils{

    //WW is a subset of PL, syntax the same.
    //<PL:Style,"url,file,exe">
    //<WW:Style,"address">
	//<QL:Style, query>
	
	//<JL:Jump,""> - added Jan 21,09. 
	//<PX:Style,NamedPopupID>
    //<OL:Style,ObjectName,(opt)Class Name, Infobase Name, ZM (scale to fit)
    //<DL:Style,DataObjectName> 
    //<ML:Style,"FolioMenuCommand">
    //<UL:Style, user defined>
	//<EN:Style Name,"Query",Width,Height,"Title"> End note link
    //<link href="" infobase="" jumpdestination="" popupId="" program="" dataobject="" objectname="" scaleobject="" objectclass="" userdefined="" query="" recordsWithHits="" popupQuery=""
    
    public static boolean isOpeningLinkTag(FolioToken t){
        return (!t.isClosing() && t.matches("^UL|QL|PL|OL|PX|WW|JL|EN|DL|ML$"));
    }
    public static boolean isClosingLinkTag(FolioToken t){
        if (t.matches("EL")) 
            return true;
        if (t.isClosing() && t.matches("^UL|QL|PL|OL|PX|WW|JL|EN|DL|ML$")) return true;
        
        return false;
    }
    public static SlxToken translate(FolioToken t) throws InvalidMarkupException{
    	if (t.matches("UL")) return FolioSlxTranslator.tryCommentOut(t, "UL"); //We comment out user links - we have no way to map them.
    	
    	
        if (isClosingLinkTag(t)) return new SlxToken("</link>");
        else if (isOpeningLinkTag(t)){
            if (t.count() < 2 && !t.matches("UL")) throw new InvalidMarkupException("All links (PL, WW, QL, JL, PX, OL, DL, ML) except User Link (UL) must provide at least 2 arguments.",t);
            
            SlxToken st = new SlxToken("<link>").set("class",t.get(0)); //Class is always first.
            
            
            if (t.matches("WW|PL")) {
            	//Web and program links. Program links are a superset of web links. Use Href for web addresses, program for exe's and documents.
            	String cmd  =t.get(1);
            	if (isUrl(cmd) || t.matches("WW")) 
            		st.set("href",cmd); //Href for these. PL may use a local path or .exe.... If it's a valid URL, use href.
            	else 
            		st.set("program",cmd); //TODO: path variable expansion "%%" = path to infobase. "%?" = path to folio views.
            	
            	if (t.count() > 2) throw new InvalidMarkupException("WW, PL, and DL links can only have 2 arguments.",t);
            }
            else if (t.matches("QL")) { //Query link
                st.set("query", t.get(1));
                
                int ix = 2;
                
                while(ix < t.count()){
                	String opt= t.get(ix);
                	if (opt.equalsIgnoreCase("RH")) 
                		st.set("showOnlyHitRecords", "true");
                	else if (st.get("infobase") == null) 
                		st.set("infobase", opt);
                	else
                		throw new InvalidMarkupException("Unrecognized option " + opt,t);
                	ix++;
                }
            }else if (t.matches("OL")){ //Object link
                st.set("objectName", t.get(1));
                
                int ix = 2;
                //May20,2011 - BUG: if Class name is blank, the infobase name will be used instead.
                //Added logic to use as infobase name if it contains '.nfo'.
                while(ix < t.count()){
                	String opt= t.get(ix);
                	if (opt.equalsIgnoreCase("ZM")) 
                		st.set("zoomFit", "true");
                	else{
                		if (st.get("infobase") == null && opt.toLowerCase().contains(".nfo"))
                			st.set("infobase", opt); //If it has .nfo, the class name must have been omitted.
	                	else if (st.get("className") == null) 
	                		st.set("className", opt); //Fill class name first
	                	else if (st.get("infobase") == null) 
	                		st.set("infobase", opt); //Then infobase
	                	else
	                		throw new InvalidMarkupException("Unrecognized option " + opt,t);
                	}
                	ix++;
                }
            //Data link
            } else if (t.matches("DL")){ 
            	//Data link. References object definition.
            	st.set("dataLink", t.get(1)); //Changed from objectName to dataLink Feb 2 , 2010. Conflicted with object links, and they don't have anything in common.
            	if (t.count() > 2) throw new InvalidMarkupException("WW, PL, and DL links can only have 2 arguments.",t);
            
            //Menu link
            } else if (t.matches("ML")){ 
            
            	st.set("menu", t.get(1));
            	if (t.count() > 2) throw new InvalidMarkupException("ML (Menu links) can only have 2 arguments.",t);
            
            	//Popup link
            }  else if (t.matches("PX")){ 
            
            	st.set("popupTitle", t.get(1));
            	if (t.count() > 2) throw new InvalidMarkupException("PX (Named popup links) can only have 2 arguments.",t);
            
            	//Jump link
            }else if (t.matches("JL")){ 
            	
            	st.set("jumpDestination", "_" + t.get(1));
            	
            	if (t.count() > 2)
            		st.set("infobase", t.get(2));
            	
            	if (t.count() > 3) throw new InvalidMarkupException("JL (Jump link) may only have 3 arguments." + t.text);
            	
            	
            	//End note link (popup query link)
            }else if (t.matches("EN")){ 
            	
            	st.set("query", t.get(1));

            	if (t.count() == 5){
            		st.set("popupWidth", t.get(2));
            		st.set("popupHeight", t.get(3));
            		st.set("title", t.get(4));
            	}else if (t.count() == 3){
            		st.set("title", t.get(2));
            	}else if (t.count() > 2){
            		throw new InvalidMarkupException("EN (End note link) may only have 2, 3, or 5 arguments" + t.text);
            	}
            	
            } else{
            	
            	throw new InvalidMarkupException("Link not supported: " + t.text);
            }
            
            
            return st;
        }
        return null;
    }
    /**
     * HAAACK. Not tested, not verified, not thought through. 
     * @param t
     * @param css
     * @return
     * @throws InvalidMarkupException
     */
    public static String translateToFolio(SlxToken t, CssClassCleaner css) throws InvalidMarkupException{
    	assert(t.matches("a|link"));
    	/* Incoming tokens will be 'resolved', merged with their definitions. 
    	 * I think this means that type="link" and style="?????" 
    	 */
		/*
    	<link> (ghost tag 2/2)

    	<link href="" infobase="" jumpdestination="" popupTitle="" program="" dataobject="" objectname="" scaleobject="" objectclass="" userdefined="" query="" recordsWithHits="" popupQuery="" />
    	Links cannot overlap or nest inside other links. Link tags can be either opening or closing, but </EL> is often used to close all types if links instead of the starting tag.

    	Program link	 <PL:Style,"url,file,exe">	 <link class="style" program="url, file, or exe">
    	Web link	 <WW:Style,"address">	 <link class="style" href="address">	 WW is a subset of PL, syntax the same.
    	Object link	 <OL:Style,ObjectName,(opt)Class Name, Infobase Name, ZM (scale to fit)
    	Data link	 <DL:Style,DataObjectName>
    	Named popup link	 <PX:Style,NamedPopupID>
    	Menu link	 <ML:Style,"FolioMenuCommand">
    	User Link	 <UL:Style, user defined>
    	Jump link	 <JL:Style,"jump destination">	 <link class="style" jumpdestination="jump destination">
    	Query link	 <QL:Style, query>	 <link class="style" query="query">
    	End note link	 EN	 ??
    	End link	 EL	 </link>

    	*/
    	String style = css.findOriginalName(t);
    	
    	if (t.isClosing()) return "<EL>";
    	
    	if (!t.isOpening()) return null;
    	
    	if (t.get("program") != null) return "<PL:\"" + style + "\",\"" + t.get("program") + "\">";
    	if (t.get("href") != null) return "<WW:\"" + style + "\",\"" + t.get("href") + "\">";
    	if (t.get("dataLink") != null) return "<DL:\"" + style + "\",\"" + t.get("dataLink") + "\">";
    	if (t.get("popupTitle") != null) return "<PX:\"" + style + "\",\"" + t.get("popupTitle") + "\">";
    	if (t.get("menu") != null) return "<ML:\"" + style + "\",\"" + t.get("menu") + "\">";
    	if (t.get("jumpDestination") != null) {
    		if (t.get("infobase") != null)return "<JL:\"" + style + "\",\"" + t.get("jumpdestination") + "\",\"" + t.get("infobase") + "\">";
    		else return "<JL:\"" + style + "\",\"" + t.get("jumpdestination") + "\">";
    	}
    	
    	/*            else if (t.matches("QL")) { //Query link
                st.set("query", t.get(1));
                
                int ix = 2;
                
                while(ix < t.count()){
                	String opt= t.get(ix);
                	if (opt.equalsIgnoreCase("RH")) 
                		st.set("showOnlyHitRecords", "true");
                	else if (st.get("infobase") == null) 
                		st.set("infobase", opt);
                	else
                		throw new InvalidMarkupException("Unrecognized option " + opt,t);
                	ix++;
                }
            }else if (t.matches("OL")){ //Object link
                st.set("objectName", t.get(1));
                
                int ix = 2;
                
                while(ix < t.count()){
                	String opt= t.get(ix);
                	if (opt.equalsIgnoreCase("ZM")) 
                		st.set("zoomFit", "true");
                	else if (st.get("className") == null) 
                		st.set("className", opt); //Fill class name first
                	else if (st.get("infobase") == null) 
                		st.set("infobase", opt); //Then infobase
                	else
                		throw new InvalidMarkupException("Unrecognized option " + opt,t);
                	ix++;
                }
               */
    	
    	if (t.get("query") != null) {
    		String s ="<QL:\"" + style + "\",\"" + t.get("query");
    		if ("true".equalsIgnoreCase(t.get("showOnlyHitRecords"))) s += ",RH";
    		if (t.get("infobase") != null) s+= ",\"" + t.get("infobase") + "\"";
    		return s + ">";
    	}
    	if (t.get("objectName") != null) {
    		String s ="<OL:\"" + style + "\",\"" + t.get("objectName");
    		if (t.get("className") != null) s+= ",\"" + t.get("className") + "\"";
    		if (t.get("infobase") != null) s+= ",\"" + t.get("infobase") + "\"";
    		if ("true".equalsIgnoreCase(t.get("zoomFit"))) s += ",ZM";
    		
    		return s + ">";
    	}
    	
    	
    	
    	throw new InvalidMarkupException("Failed to translate link to folio: " + t.toTokenString());
    }
    //SCHEME: ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
    protected static Pattern scheme = Pattern.compile("^[a-zA-Z][A-Za-z0-9+.-]*:\\/\\/");//\\G\\s++(\\w[\\w-:]*+)(?:\\s*+=\\s*+\"([^\"]*+)\"|\\s*+=\\s*+'([^']*+)'|\\s*+=\\s*+([^\\s=/>]*+)|(\\s*?))");
    
    
    protected static Pattern simpleDomain = Pattern.compile("^[a-zA-Z][A-Za-z0-9+.-]*:\\/\\/");//\\G\\s++(\\w[\\w-:]*+)(?:\\s*+=\\s*+\"([^\"]*+)\"|\\s*+=\\s*+'([^']*+)'|\\s*+=\\s*+([^\\s=/>]*+)|(\\s*?))");
    
    
    
    private static boolean isUrl(String s){
    	s = s.trim();//Trim whitespace
    	boolean missingScheme = false;
    	if (!scheme.matcher(s).find()){
    		//No scheme specified. 
    		//if (s.indexOf('\\') > -1) return false; //It means we have a windows path.
    		
    		//s = "http://" + s;
    		//missingScheme = true;
    		return false; //Don't try to help. If they forgot the http://, consider it a program
    	}
    	try{
    		URL u = new URL(s); 
    		//if (missingScheme && !TokenUtils.fastMatches(regex, u.getHost()))
    		return true;
    	}catch(MalformedURLException e){
    		return false;
    	}
    }
    
    
}