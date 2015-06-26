package folioxml.export;

import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.xml.XmlRecord;

import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


public class SlugProvider implements NodeInfoProvider{

    public SlugProvider(){

    }
    public SlugProvider(String levelRegex){
        this.levelRegex = levelRegex;
    }
    String levelRegex;
    private StaticFileNode silentRoot = new StaticFileNode(null);

    @Override
    public boolean separateInfobases(InfobaseConfig ic, InfobaseSet set) {
        return (set.getInfobases().size() > 1);
    }

    @Override
    public boolean startNewFile(XmlRecord r) throws InvalidMarkupException {
        if (levelRegex == null)
            return r.isLevelRecord();
        else
            return r.isLevelRecord() && TokenUtils.fastMatches(levelRegex, r.getLevelType());
    }

    public void PopulateNodeInfo(XmlRecord r, FileNode f) throws InvalidMarkupException {
        //Infobase ID comes in handy when generating the slug
        XmlRecord root = r.getRoot();
        if (root != null && root.get("infobaseId") != null){
            f.getBag().put("infobase-id", root.get("infobaseId"));
        }

        f.getBag().put("slug", getSlug(r, f));
        f.getBag().put("folio-id", r.get("folioId"));
        f.getBag().put("folio-level", r.getLevelType());
        f.getAttributes().put("heading", r.get("heading"));

    }

    public String getSlug(XmlRecord r, FileNode f) throws InvalidMarkupException {

        String heading = r.get("heading");
        if (heading == null) heading = "UNTITLED";

        String slug = heading.trim().toLowerCase(Locale.ENGLISH);

        if ("Issue".equalsIgnoreCase(r.getLevelType())){
            //then apply regex to months
            slug = slug.replaceFirst("\\A\\d\\d?/\\d\\d\\s+", "").trim(); //Drop the d/m prefix
            slug = slug.replaceAll("(?i)\\A.*(January|February|March|April|May|June|July|August|September|October|November|December).*\\Z", "$1"); //If present, just use the month.
        }
        // strip out all characters except letters, digits, dashes, underscores, tildes (~) and dollar signs,
        slug = slug.replaceAll("[^a-zA-Z0-9-_~$]", " ").trim();
        slug = slug.replaceAll("[ \t\r\n]+", "-").toLowerCase(Locale.ENGLISH);

        if (r.isRootRecord() && (slug == null || slug.isEmpty())) {
            Object name = f.getBag().get("infobase-id");
            if (name == null) name = "index";
            slug = (String)name;
        }


        //Then truncate if longer than 100 chars
        if (slug.length() > 100) slug = slug.substring(0,100);

        FileNode parentScope = f.getParent() == null ? silentRoot : f.getParent();
        Integer suffix = incrementSlug(slug, parentScope);
        if (suffix > 1) return slug + "-" + suffix;
        else return slug;
    }



    private Integer incrementSlug(String slug, FileNode scope){
        //Access sibling slugs to ensure uniqueness.
        Object oslugs = scope.getBag().get("childSlugs");
        if (oslugs == null) {
            oslugs = new HashMap<String, Integer>();
            scope.getBag().put("childSlugs", oslugs);
        }
        Map<String, Integer> siblingSlugs = (Map<String, Integer>)oslugs;

        if (siblingSlugs.get(slug) == null){
            siblingSlugs.put(slug, 0);
        }
        //Increment
        siblingSlugs.put(slug, siblingSlugs.get(slug) + 1);

        return siblingSlugs.get(slug);
    }


    @Override
    public String getRelativePathFor(FileNode fn) {

        StringBuilder sb = new StringBuilder();
        Deque<StaticFileNode> list = ((StaticFileNode)fn).getAncestors(true);
        StaticFileNode n = null;
        while (!list.isEmpty()){
            sb.append((String) list.removeLast().getBag().get("slug"));
            if (!list.isEmpty()) sb.append('/');
        }
        return sb.toString();
    }
}
