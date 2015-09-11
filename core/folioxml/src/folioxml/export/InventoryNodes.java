package folioxml.export;

import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeListProcessor;
import folioxml.slx.SlxToken;
import folioxml.text.TextLinesBuilder;
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

    LogStreamProvider logs;

    public InventoryNodes(LogStreamProvider logs){
        this.logs = logs;
    }

    HashMap<String, Integer> stats = new HashMap<String, Integer>(30);

     HashMap<String, HashSet<String>> uniques = new HashMap<String, HashSet<String>>(30);


    private void increment(String statName){
        incrementBy(statName, 1);
    }

    public void PrintStats(Appendable a) throws IOException {
        for(Map.Entry<String, Integer> e:stats.entrySet()){
            a.append(e.getKey() + ": " + e.getValue().toString() + "\n");
        }


    }

    public void PrintExternalInfobases(InfobaseSet internalInfobases, Appendable a) throws IOException {
        HashSet<String> dests = uniques.get("destination infobases");
        if (dests == null) return;

        a.append("External infobases: \n");
        for (String val: dests){
            if (internalInfobases.byName(val) == null){
                a.append(val);
                a.append("\n");
            }
        }
        a.append("\n");
    }

    public void PrintUniques(Appendable a) throws IOException {
        for(Map.Entry<String, HashSet<String>> e:uniques.entrySet()){
            a.append("\n");
            for (int i =0; i < 60; i++)
                a.append('-');

            a.append("\nUnique " + e.getKey() + " (" + + e.getValue().size() +"): \n");
            for (String val: e.getValue()){
                a.append(val);
                a.append("\n");
            }
        }

    }
    private void incrementBy(String statName, int offset){
        Integer i = stats.get(statName);
        if (i == null) i = 0;
        i+= offset;
        stats.put(statName, i);
    }





    public NodeList process(NodeList nodes) throws InvalidMarkupException {
        //Deep copy so we don't affect anything.
        nodes = nodes.deepCopy();


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


        //Report on use of tabs inside paragraphs
       for (Node para:nodes.search(new NodeFilter("p")).list()){
           //I guess recurse the tree and reset each time a breaking element opens or closes.
           TextLinesBuilder.TabUsage tabs = new TextLinesBuilder().analyzeTabUsage(new NodeList(para));
           boolean canPre = para.children == null || para.children.phrasingContentOnly();
           if (tabs != TextLinesBuilder.TabUsage.None){
               //System.out.println("Tab-aligned paragraph:");
               increment("tab-aligned paragraphs");
               //System.out.println(para.toXmlString(true));
               if (!canPre){
                   logAndPullNode(para,"tab-aligned paragraphs that contain block elements", "Tab-aligned paragraph with block elements:");
               }

           }
       }

        //TODO: report on use of underlining for non-links


        //TODO: export hidden text. (better if we use SLX?)


        NodeList images = nodes.filterByTagName("img|object|link|a", true);
        for (Node n:images.list()){
            if ("true".equalsIgnoreCase(n.get("resolved"))) {
                continue; //It's resolved.
            }
            if (n.get("href") != null && validUrl(n.get("href"))){
                continue; //It's a valid URI
            }
            if (n.get("id") != null && n.get("href") == null && "a".equalsIgnoreCase(n.getTagName())){
                //It's an anchor, skip
                continue;
            }
            if (!"true".equalsIgnoreCase(n.get("resolved")) && !"popup".equalsIgnoreCase(n.get("type"))){
                logUnique(n.toXmlString(false), "unresolved references");
            }
        }


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

       /*  if (nodes.filterByTagName("a", true).count() > 0){
            throw new InvalidMarkupException("Only raw XML can be inventoried.");
        }*/

        //Program, menu, data links are always local
        logAndPullNodes(nodes.search(new NodeFilter("link|a","program",null)), "program links", "Program link:");
        logAndPullNodes(nodes.search(new NodeFilter("link|a","dataLink",null)), "data links", "Data link:");
        logAndPullNodes(nodes.search(new NodeFilter("link|a","menu",null)), "menu links", "Menu link:");

        //Add number of href URL links.
        NodeList urlLinks = nodes.search(new NodeFilter("link|a", "href", null));

        incrementBy("URL links", urlLinks.count());
        for (Node n:urlLinks.list()){
            if (validUrl(n.get("href"))) {
                logUnique(n.get("href"), "URL links");
            }
        }

        for (Node n:urlLinks.list()){
            String url = n.get("href");
            if (!"true".equalsIgnoreCase(n.get("resolved")) && !validUrl(url)){
                logUnique(url,"invalid URL links");
            }
        }






        urlLinks.pull(); //Pull so we don't run into them later


        //jump and cross-infobase jump links
        NodeList jumpLinks = nodes.search(new NodeFilter("link|a","jumpDestination",null));
        for (Node n:jumpLinks.list()){
            if (n.get("infobase") != null){
                //logAndPullNode(n, "cross-infobase bookmark links", "Cross-infobase jump link:");
                n.pull();
            }else{
                increment("bookmark links");
                n.pull();
            }
        }

        //object and cross-infobase object links
        NodeList objectLinks = nodes.search(new NodeFilter("link|a","objectName",null));
        for (Node n:objectLinks.list()){
            if (n.get("infobase") != null){
                logAndPullNode(n, "cross-infobase object links", "Cross-infobase object link:");
            }else{
                logAndPullNode(n, "object links", "Object link:");
            }
        }

        //Inline popups (not originally links)
        logAndPullNodes(nodes.search(new NodeFilter("link|a","type","popup")), "inline popups", "Inline popup link:");

        //Named popup links
        logAndPullNodes(nodes.search(new NodeFilter("link|a","popupTitle",null)), "named popup links", "Link to named popup:");


        NodeList queryLinks = nodes.search(new NodeFilter("link|a","query", null));
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
