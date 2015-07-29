package folioxml.export.structure;


import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.export.NodeInfoProvider;
import folioxml.export.StaticFileNode;
import folioxml.xml.NodeFilter;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;

public abstract class BaseFileSplitter implements NodeInfoProvider {

    public BaseFileSplitter(){}
    public BaseFileSplitter(String levelRegex){
        this(levelRegex, null);
    }
    public BaseFileSplitter(String levelRegex, String splitOnFieldName){
        if ("null".equalsIgnoreCase(levelRegex)) levelRegex = null;
        if ("null".equalsIgnoreCase(splitOnFieldName)) splitOnFieldName = null;

        this.levelRegex = levelRegex;
        this.splitOnFieldName = splitOnFieldName;
    }


    String levelRegex;
    String splitOnFieldName;

    InfobaseSet iset;

    protected InfobaseSet getSet(){
        return iset;
    }
    @Override
    public boolean separateInfobases(InfobaseConfig ic, InfobaseSet set) {
        iset = set;
        return (set.getInfobases().size() > 1);
    }

    private enum SplitReason{
        None,
        Level,
        Field
    }
    SplitReason lastRecord = SplitReason.None;

    @Override
    public boolean startNewFile(XmlRecord r) throws InvalidMarkupException {
        SplitReason thisReason = SplitReason.None;


        if (r.isLevelRecord() && (levelRegex == null || TokenUtils.fastMatches(levelRegex, r.getLevelType()))){
            thisReason = SplitReason.Level;
        }


        String splitText = getSplitFieldText(r);
        if (splitText != null && splitText.length() > 0){
            thisReason = SplitReason.Field;
        }


        //Do we actually care about the reason for the previous record's split? We may not need such logic.
        lastRecord = thisReason;

        return (thisReason != SplitReason.None);

    }


    protected String getSplitFieldText(XmlRecord r) throws InvalidMarkupException {
        if (splitOnFieldName != null){
            NodeList matches;
            matches = new NodeList(r).searchOuter(new NodeFilter("span", "type", splitOnFieldName));
            if (matches.count() > 1){
                //TODO: log irregularity
                System.out.append("Irregular use of splitting field ");
                System.out.append(splitOnFieldName);
                System.out.append(" - " + matches.count() + " instances found in record " + r.get("folioId"));
            }
            if (matches.count() > 0){
                String text = matches.getTextContents().trim();
                return text;
            }
        }
        return null;
    }



}
