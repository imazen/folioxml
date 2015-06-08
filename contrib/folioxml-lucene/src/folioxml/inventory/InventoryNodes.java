package folioxml.inventory;

import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.slx.SlxToken;
import folioxml.xml.Node;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.Not;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import java.util.regex.*;

public class InventoryNodes implements NodeListProcessor {

    //HashSet<String> bookmarkNames = new HashSet<String>();



    HashMap<String, Integer> stats = new HashMap<String, Integer>(30);

     HashMap<String, HashSet<String>> uniques = new HashMap<String, HashSet<String>>(30);


    private void increment(String statName){
        incrementBy(statName, 1);
    }

    public void PrintStats(){
        for(Map.Entry<String, Integer> e:stats.entrySet()){
            System.out.println(e.getKey() + ": " + e.getValue().toString());
        }


    }

    public void PrintExternalInfobases(InfobaseSet internalInfobases){
        HashSet<String> dests = uniques.get("destination infobases");
        if (dests == null) return;

        System.out.println("External infobases: ");
        for (String val: dests){
            if (internalInfobases.byName(val) == null){
                System.out.println(val);
            }
        }
        System.out.println();
    }

    public void PrintUniques(){
        for(Map.Entry<String, HashSet<String>> e:uniques.entrySet()){
            System.out.println();
            for (int i =0; i < 60; i++)
              System.out.print('-');
            System.out.println();
            System.out.println("Unique " + e.getKey() + " (" + + e.getValue().size() +"): ");
            for (String val: e.getValue()){
                System.out.println(val);
            }
        }

    }
    private void incrementBy(String statName, int offset){
        Integer i = stats.get(statName);
        if (i == null) i = 0;
        i+= offset;
        stats.put(statName, i);
    }


    private Pattern tabsInTheMiddle = Pattern.compile("\\A\\s*[a-zA-Z0-9_-]+\\t+");

    private boolean paragraph_uses_tab_alignment(Node p){
        List<StringBuilder> lines = new ArrayList<StringBuilder>();
        add_lines(p, lines);

        for(StringBuilder l:lines){
            Matcher m = tabsInTheMiddle.matcher(l);
            if (m.find()) return true;
        }

        return false;
    }

    private boolean phrasing_content_only(NodeList nodes){
        //https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Content_categories#Phrasing_content
        for (Node every: nodes.flattenRecursive().list()){
            if (every.isTag()){
                if (!every.matches("abbr|audio|b|bdo|br|button|canvas|cite|code|command|datalist|dfn|em|embded|i|iframe|img|input|kbd|keygen|label|mark|math|meter|noscript|object|output|progress|q|ruby|samp|script|select|small|span|strong|sub|sup|svg|textarea|time|var|video|wbr|")
                        &&
                    !every.matches("a|area|del|ins|map|link|meta|bookmark")){
                    return false;
                }
            }
        }
        return true;
    }



    private void add_lines(Node n, List<StringBuilder> lines){
        if (n.matches("p|br|table|td|th|note|div|record")) line_new(lines);

        if (n.isTag() && n.children != null){
            for(Node c:n.children.list()){
                add_lines(c,lines);
            }
        }else if (n.isTextOrEntity()){
            String s = n.markup;
            if (n.isEntity()) s = TokenUtils.entityDecodeString(s);
            last_line(lines).append(s);
        }

        if (n.matches("p|br|table|td|th|note|div|record")) line_new(lines);

    }

    private StringBuilder last_line(List<StringBuilder> lines){
        if (lines.size() == 0){
            lines.add(new StringBuilder());
        }
        return lines.get(lines.size() - 1);
    }

    private void line_new(List<StringBuilder> lines){
        if (lines.size() == 0 || lines.get(lines.size() - 1).length() > 0){
            lines.add(new StringBuilder());
        }
    }


    public NodeList process(NodeList nodes) throws InvalidMarkupException {
        for (Node n:nodes.search(new NodeFilter("bookmark")).list())
            increment("bookmark definitions");

        //Report unique groups and levels
        for (Node n:nodes.search(new NodeFilter("record")).list()){
            if (n.get("groups") != null){
                for(String group:n.get("groups").split(",")){
                    logUnique(group, "groups");
                }
            }
            if (n.get("level") != null){
                logUnique(n.get("level"), "levels");
                increment("level records");
            }

            //TODO report unique combination of levels

            
        }

        for (Node every: nodes.flattenRecursive().list()){
            if (every.isTag()){
                logUnique(every.getTagName(), "XML element names");
            }
        }


        //TODO: generate diagnostics ff->SLX mapping report (TranslationTableDump)

        //TODO: report on use of tabs inside paragraphs
       for (Node para:nodes.search(new NodeFilter("p")).list()){
           //I guess recurse the tree and reset each time a breaking element opens or closes.
           boolean tabAlign = paragraph_uses_tab_alignment(para);
           boolean canPre = para.children == null || phrasing_content_only(para.children);
           if (tabAlign){
               System.out.println("Tab-aligned paragraph:");
               increment("tab-aligned paragraphs");
               System.out.println(para.toXmlString(true));
               if (!canPre){
                   logAndPullNode(para,"tab-aligned paragraphs that contain block elements", "Tab-aligned paragraph with block elements:");
               }

           }
       }

        //TODO: report on use of underlining for non-links


        //TODO: export hidden text.





        for (Node link:nodes.search(new NodeFilter("link", "infobase", null)).list())
            logUnique(link.get("infobase"), "destination infobases");

        NodeList objects = nodes.filterByTagName("object", true);
        for (Node t:objects.list()){
            String handler = t.get("handler");
            if (TokenUtils.fastMatches("bitmap|metafile|picture",handler)){ //Convert these three types to "img" tags immediately.
                increment("images");
            }else{
                logAndPullNode(t, "unsupported objects", "Unsupported object:");
            }
        }

        logAndPullNodes(nodes.filterByTagName("note", true),"notes", "Note: ");


        return processLinks(nodes);
    }

    private void logAndPullNodes(NodeList list, String counter, String intro) throws InvalidMarkupException{
        for (Node n:list.list()){
            logAndPullNode(n,counter,intro);
        }
    }
    private void logAndPullNode(Node n, String counter, String intro) throws InvalidMarkupException{

        //Unless type = popup, skip the internals for distinction.
        String data = null;

        if (n.matches("link")){
            if(n.get("type") == null || !n.get("type").equalsIgnoreCase("popup")){
                data = n.toTokenString();
            }
        }
        if (data == null) data = n.toXmlString(true);

        logUnique(data, counter);
        System.out.println(intro);
        System.out.println(n.toXmlString(true));
        increment(counter);
        n.pull();
    }

    private void logUnique(String data, String counter) throws InvalidMarkupException{
        HashSet<String> set = uniques.get(counter);
        if (set == null){
            set = new HashSet<String>();
            uniques.put(counter, set);
        }
        if (!set.contains(data)){
            set.add(data);
            increment("unique " + counter);
        }
    }

    private boolean validUrl(String s){
        try{
            URL u = new URL(s);
            return true;
        }catch(MalformedURLException e){
            return false;
        }
    }

    public NodeList processLinks(NodeList nodes) throws InvalidMarkupException {

         if (nodes.filterByTagName("a", true).count() > 0){
            throw new InvalidMarkupException("Only raw XML can be inventoried.");
        }

        //Program, menu, data links are always local
        logAndPullNodes(nodes.search(new NodeFilter("link","program",null)), "program links", "Program link:");
        logAndPullNodes(nodes.search(new NodeFilter("link","dataLink",null)), "data links", "Data link:");
        logAndPullNodes(nodes.search(new NodeFilter("link","menu",null)), "menu links", "Menu link:");

        //Add number of href URL links.
        NodeList urlLinks = nodes.search(new NodeFilter("link", "href", null));

        incrementBy("URL links", urlLinks.count());
        for (Node n:urlLinks.list()){
            logUnique(n.get("href"),"URL links");
        }

        for (Node n:urlLinks.list()){
            String url = n.get("href");
            if (!validUrl(url)){
                logUnique(url,"invalid URL links");
            }
        }






        urlLinks.pull(); //Pull so we don't run into them later


        //jump and cross-infobase jump links
        NodeList jumpLinks = nodes.search(new NodeFilter("link","jumpDestination",null));
        for (Node n:jumpLinks.list()){
            if (n.get("infobase") != null){
                logAndPullNode(n, "cross-infobase bookmark links", "Cross-infobase jump link:");
            }else{
                increment("bookmark links");
                n.pull();
            }
        }

        //object and cross-infobase object links
        NodeList objectLinks = nodes.search(new NodeFilter("link","objectName",null));
        for (Node n:objectLinks.list()){
            if (n.get("infobase") != null){
                logAndPullNode(n, "cross-infobase object links", "Cross-infobase object link:");
            }else{
                logAndPullNode(n, "object links", "Object link:");
            }
        }

        //Inline popups (not originally links)
        logAndPullNodes(nodes.search(new NodeFilter("link","type","popup")), "inline popups", "Inline popup link:");

        //Named popup links
        logAndPullNodes(nodes.search(new NodeFilter("link","popupTitle",null)), "named popup links", "Link to named popup:");


        NodeList queryLinks = nodes.search(new NodeFilter("link","query", null));
        for (Node n:queryLinks.list()){
            if (n.get("infobase") != null){
                logAndPullNode(n, "cross-infobase query links", "Cross-infobase query link:");
            }else if (n.get("title") != null){
                logAndPullNode(n, "query popup links", "Query popup link:");
            } else{
                logAndPullNode(n, "query links", "Query link:");
            }
        }

        logAndPullNodes(nodes.search(new NodeFilter("link")), "unrecognized links", "Unrecognized link:");

        return nodes;
    }
}
