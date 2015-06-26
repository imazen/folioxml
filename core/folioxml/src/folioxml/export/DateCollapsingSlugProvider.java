package folioxml.export;

import folioxml.core.InvalidMarkupException;
import folioxml.xml.XmlRecord;

/**
 * Created by nathanael on 6/26/15.
 */
public class DateCollapsingSlugProvider extends SlugProvider {

    @Override
    protected String getHeading(XmlRecord r, FileNode f) throws InvalidMarkupException {
        String heading = r.get("heading");
        if (heading == null) heading = "UNTITLED";

        if ("Issue".equalsIgnoreCase(r.getLevelType())){
            //then apply regex to months
            heading = heading.replaceFirst("\\A\\d\\d?/\\d\\d\\s+", "").trim(); //Drop the d/m prefix
            heading = heading.replaceAll("(?i)\\A.*(January|February|March|April|May|June|July|August|September|October|November|December).*\\Z", "$1"); //If present, just use the month.
        }

        return heading;
    }
}
