package folioxml.export;

import folioxml.core.InvalidMarkupException;
import folioxml.xml.XmlRecord;

import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


public class SlugProvider implements NodeInfoProvider{

    public SlugProvider(String pathSuffix){
        this.pathSuffix = pathSuffix;

    }
    String pathSuffix;
    private StaticFileNode silentRoot = new StaticFileNode(null);

    @Override
    public boolean startNewFile(XmlRecord r) throws InvalidMarkupException {
        return r.isLevelRecord();
    }

    public void PopulateNodeInfo(XmlRecord r, FileNode f) throws InvalidMarkupException {
        f.getBag().put("slug", getSlug(r,f));
        f.getAttributes().put("heading", r.get("heading"));
    }

    public String getSlug(XmlRecord r, FileNode f) throws InvalidMarkupException {

        String heading = r.get("heading");
        if (heading == null) heading = "UNTITLED";

        String slug = heading.trim().toLowerCase(Locale.ENGLISH);

        if ("Issue".equalsIgnoreCase(r.getLevelType())){
            //then apply regex to months
            slug = slug.replaceFirst("\\A\\d\\d?/\\d\\d\\s+", "").trim();
        }
        // strip out all characters except letters, digits, dashes, underscores, tildes (~) and dollar signs,
        slug = slug.replaceAll("[^a-zA-Z0-9-_~$]", " ");
        slug = slug.replaceAll("[ \t\r\n]+", "-");

        FileNode parentScope = f.getParent() == null ? silentRoot : f.getParent();

        //Access sibling slugs to ensure uniqueness.
        Object oslugs = parentScope.getBag().get("childSlugs");
        if (oslugs == null) {
            oslugs = new HashMap<String, Integer>();
            parentScope.getBag().put("childSlugs", oslugs);
        }
        Map<String, Integer> siblingSlugs = (Map<String, Integer>)oslugs;

        if (siblingSlugs.get(slug) == null){
            siblingSlugs.put(slug, 0);
        }
        //Increment
        siblingSlugs.put(slug, siblingSlugs.get(slug) + 1);

        Integer suffix = siblingSlugs.get(slug);
        if (suffix > 1) return slug + "-" + suffix;
        else return slug;
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
        sb.append(pathSuffix);
        return sb.toString();
    }
}
