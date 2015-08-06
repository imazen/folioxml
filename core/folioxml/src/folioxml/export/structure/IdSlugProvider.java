package folioxml.export.structure;


import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.FileNode;
import folioxml.export.NodeInfoProvider;
import folioxml.export.StaticFileNode;
import folioxml.xml.XmlRecord;

import javax.jws.soap.SOAPBinding;
import java.util.*;


public class IdSlugProvider extends BaseFileSplitter {


    private StaticFileNode silentRoot = new StaticFileNode(null);

    public IdSlugProvider(String levelRegex, String splitOnFieldName) {
        super(levelRegex, splitOnFieldName);
    }

    public IdSlugProvider(String levelRegex, String splitOnFieldName, Integer idKind, Integer root_index, Integer start_index) {
        super(levelRegex, splitOnFieldName);
        this.idKind = idKind == null ? 0 : idKind;
        this.start_index = start_index == null ? 1 : start_index;
        this.root_index = root_index== null ? 1 :  root_index;
        this.sequentialIndex = root_index;
    }
    int idKind;
    int start_index;
    int root_index;


    public IdSlugProvider() {
    }

    public IdSlugProvider(String levelRegex) {
        super(levelRegex);
    }

    int sequentialIndex;

    @Override
    public String getRelativePathFor(FileNode fn) {
        return getRelativePathFor(fn, idKind);
        //TODO - verify 1-1 mapping between all relative paths and all file node values.
    }

    public String getRelativePathFor(FileNode fn, int kind) {
        if (kind == 0){
            return getSlugPathFor(fn);
        }else if (kind == 1){
            return Integer.toString((Integer) fn.getBag().get("global-index"));
        }else if (kind == 2 ){
            return getNestedIntegerPathFor(fn);
        }else if (kind == 3){
            return ((UUID) fn.getBag().get("guid")).toString();
        }else if (kind == 4){
            if (fn.getBag().get("folio-id") == null){
                return (fn.getBag().get("infobase-id") + "-i" +Integer.toString((Integer) fn.getBag().get("global-index"))).toLowerCase();
            }else {
                return (fn.getBag().get("infobase-id") + "-" + (String) fn.getBag().get("folio-id")).toLowerCase();
            }
        }else if (kind > 4){
            //Try to use split field value as ID, then fall back to kind - 5

            Object otext = fn.getBag().get("split-field-text");
            if (otext == null)
                return getRelativePathFor(fn, kind - 5);
            else
                return (String)otext;
        }
        return getSlugPathFor(fn);
    }


    public String getNestedIntegerPathFor(FileNode fn) {

        StringBuilder sb = new StringBuilder();
        Deque<StaticFileNode> list = ((StaticFileNode)fn).getAncestors(true);
        StaticFileNode n = null;
        while (!list.isEmpty()){
            sb.append((String) list.removeLast().getBag().get("local-index"));
            if (!list.isEmpty()) sb.append('.');
        }
        return sb.toString();
    }

    public String getSlugPathFor(FileNode fn) {

        StringBuilder sb = new StringBuilder();
        Deque<StaticFileNode> list = ((StaticFileNode)fn).getAncestors(true);
        StaticFileNode n = null;
        while (!list.isEmpty()){
            sb.append((String) list.removeLast().getBag().get("slug"));
            if (!list.isEmpty()) sb.append('/');
        }
        return sb.toString();
    }



    public void PopulateNodeInfo(XmlRecord r, FileNode f) throws InvalidMarkupException {
        //Infobase ID comes in handy when generating the slug
        XmlRecord root = r.getRoot();

        if (root != null && root.get("infobaseId") != null){
            f.getBag().put("infobase-id", root.get("infobaseId"));
        }

        String[] levels = root.get("levelDefOrder").split(",");
        String level = r.getLevelType();
        if (level != null){
            f.getBag().put("folio-level", level);

            for (int i =0; i < levels.length; i++){
                if (levels[i].equalsIgnoreCase(level)){
                    f.getBag().put("folio-level-index", i + 1);
                    f.getAttributes().put("level-index", Integer.toString(i + 1));
                    break;
                }
            }

        }


        f.getBag().put("slug", getSlug(r, f));
        f.getBag().put("guid", UUID.randomUUID());
        FileNode parentScope = f.getParent() == null ? silentRoot : f.getParent();
        f.getBag().put("local-index", incrementChildCount(parentScope));
        f.getBag().put("global-index", sequentialIndex);
        sequentialIndex++;
        f.getBag().put("folio-id", r.get("folioId")); //TODO: verify uniqueness
        if (r.get("heading") != null && r.get("heading").length() > 0) {
            f.getAttributes().put("heading", r.get("heading"));
        }

        String splitText = getSplitFieldText(r);
        if (splitText != null && splitText.length() > 0){
            //ONLY ADD IF unique across infobase

            Integer totalCount = incrementValueCount(splitText, "splitTexts", silentRoot);
            if (totalCount > 1){
                System.out.append("Skipping use of split-text-field for ID - value '" + splitText + "' has been used before and is not unique.\n");
            }else {
                f.getBag().put("split-field-text", splitText);
                f.getAttributes().put(splitOnFieldName.toLowerCase() + "-text", splitText);
            }
        }


        populateHeadings(f, f);

    }

    private void populateHeadings(FileNode from, FileNode to){
        Object headingIndex = from.getBag().get("folio-level-index");
        if (headingIndex != null) {
            String heading = from.getAttributes().get("heading");
            if (heading != null && heading.length() > 0) {
                to.getAttributes().put("heading" + headingIndex, heading);
            }
        }
        if (from.getParent() != null)
            populateHeadings(from.getParent(), to);
    }

    private String getSlug(XmlRecord r, FileNode f) throws InvalidMarkupException {

        String heading =  r.get("heading");

        String slug =  heading == null ? null : slugify(heading, 100);

        if (r.isRootRecord() && (slug == null || slug.isEmpty())) {
            Object name = f.getBag().get("infobase-id");
            if (name == null) name = "index";
            slug = (String)name;
        }
        if (slug == null) slug = "untitled";
        FileNode parentScope = f.getParent() == null ? silentRoot : f.getParent();
        Integer suffix = incrementSlug(slug, parentScope);
        if (suffix > 1) return slug + "-" + suffix;
        else return slug;
    }



    protected String slugify(String text, int maxLength){
        String slug = text.toLowerCase(Locale.ENGLISH).replaceAll("[^a-zA-Z0-9-_~$]", " ").trim();
        slug = slug.replaceAll("[ \t\r\n]+", "-").toLowerCase(Locale.ENGLISH);

        if (slug.length() > maxLength) slug = slug.substring(0,maxLength);
        return slug;
    }


    protected Integer incrementSlug(String slug, FileNode scope){
        return incrementValueCount(slug, "childSlugs", scope);
    }

    protected Integer incrementValueCount(String value, String  dictName, FileNode scope){
        //Access sibling values to ensure uniqueness.
        Object oslugs = scope.getBag().get(dictName);
        if (oslugs == null) {
            oslugs = new HashMap<String, Integer>();
            scope.getBag().put(dictName, oslugs);
        }
        Map<String, Integer> allValues = (Map<String, Integer>)oslugs;

        String normalizedValue = value.trim().toLowerCase();

        if (allValues.get(normalizedValue) == null){
            allValues.put(normalizedValue, 0);
        }
        //Increment
        allValues.put(normalizedValue, allValues.get(normalizedValue) + 1);

        return allValues.get(normalizedValue);
    }


    protected Integer incrementChildCount(FileNode scope){
        //Access sibling slugs to ensure uniqueness.
        Object count = scope.getBag().get("childCount");
        if (count == null) {
            Integer index_at = ((scope == silentRoot) ? root_index : start_index);
            scope.getBag().put("childCount", index_at - 1);
        }
        Integer child_count = (Integer)scope.getBag().get("childCount");
        child_count++;
        scope.getBag().put("childCount",child_count);
        return child_count;
    }



}
