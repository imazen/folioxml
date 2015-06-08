package folioxml.export.html;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.xml.Node;
import folioxml.xml.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class FixHttpLinks implements NodeListProcessor {


    public FixHttpLinks(){

    }

    protected static Pattern scheme = Pattern.compile("\\A\\s*(mailto:|[a-zA-Z][A-Za-z0-9+.-]*://)",Pattern.CASE_INSENSITIVE);

    protected static Pattern missingslash = Pattern.compile("\\A\\s*(https?):/([^/])",Pattern.CASE_INSENSITIVE);//\\G\\s++(\\w[\\w-:]*+)(?:\\s*+=\\s*+\"([^\"]*+)\"|\\s*+=\\s*+'([^']*+)'|\\s*+=\\s*+([^\\s=/>]*+)|(\\s*?))");


    private boolean validUrl(String address){
        try{
            URL u = new URL(address);
            return true;
        }catch(MalformedURLException e){
            return false;
        }
    }

    public NodeList process(NodeList nodes) throws InvalidMarkupException {
        NodeList links = nodes.filterByTagName("a|link", true);
        for (Node n:links.list()){
            //Type may be any of 'folio', 'data-link', 'ole', or 'class-object', as the attributes from the object definition have been merged in
            //Limit this exclusively to web links; data links are physical paths.
            if (TokenUtils.fastMatches("folio|data-link", n.get("type"))){
                continue;
            }
            String url = n.get("href");
            String repairedUrl = repairUrl(url);
            if (!repairedUrl.equals(url)){
                n.set("href",  repairedUrl);
            }
        }
        return nodes;
    }

    public  String repairUrl(String url){
        //Backslashes indicate a physical path; do not touch.
        if (url == null || url.contains("\\")) return url;

        //First check if the scheme is missing
        if (!scheme.matcher(url).find()){
            //Try to repair http:/ -> http://
            String url2 = missingslash.matcher(url).replaceFirst("$1://$2");
            //If the scheme is still missing, add it
            if (!scheme.matcher(url2).find()){
                url2 = "http://" + url.trim();
            }
            //If that causes the URL to be correctly formed, return that repaired url
            if (validUrl(url2)){
                return url2;
            }
        }
        return url;
    }

}
