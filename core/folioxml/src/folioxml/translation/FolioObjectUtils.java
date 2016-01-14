package folioxml.translation;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.folio.FolioToken;
import folioxml.slx.SlxToken;

import java.io.StringWriter;
import java.util.List;

public class FolioObjectUtils {


    public static SlxToken translateObject(FolioToken ft) throws InvalidMarkupException {
        if (!ft.matches("OB")) return null;

    	/*
    	 * <obj name="My Photo Name" type="folio|ole" style="width:x;height:y;border:opts;background-color:black;" />
    	 */
        if (ft.isClosing())
            throw new InvalidMarkupException("Object (OB) tags are all self-closing. Closing </OB> tags should not exist", ft);
        SlxToken t = new SlxToken("<object />");
        if (ft.count() < 2) throw new InvalidMarkupException("Object (OB) tags must have 2 or more arguments.", ft);

        List<String> opts = ft.getOptionsArray();
        String type = opts.get(0);
        t.set("name", opts.get(1));

        if (TokenUtils.fastMatches("FO", type)) t.set("type", "folio");
        else if (TokenUtils.fastMatches("OL", type)) t.set("type", "ole");
        else throw new InvalidMarkupException("Invalid Object Type: " + type);

        //Parse Borders, background color, width, and height
        StringWriter css = new StringWriter(50);
        int ix = 2;
        boolean hasWidth = false;
        boolean hasHeight = false;
        while (ix < opts.size()) {
            int i = ix;
            String s = opts.get(ix);
            if (FolioCssUtils.isNumber(s)) {
                double val = Double.parseDouble(s);
                if (!hasWidth) {
                    css.append("width:" + ((val != 0) ? FolioCssUtils.fixUnits(s) : "auto") + ";");
                    hasWidth = true;
                } else if (hasWidth) {
                    css.append("height:" + ((val != 0) ? FolioCssUtils.fixUnits(s) : "auto") + ";"); //Assuming 0 = auto... I hope?
                    hasHeight = true;
                } else if (hasHeight)
                    throw new InvalidMarkupException("Only Width and Height may be specified on OB tags. Unexpected number");
                i++;
            }

            if (i == ix) i = FolioCssUtils.tryParseBorder(i, opts, css, false);
            if (i == ix) i = FolioCssUtils.tryParseParagraphBackgroundColor(i, opts, css);
            if (i == ix) throw new InvalidMarkupException("Unrecognized option encountered in Object (OB) tag");
            else ix = i;
        }
        t.set("style", css.toString());
        return t;
    }

    /**
     * RP (replace definition) must already be removed from ft.
     * object-def type="folio|data-link|ole|class-object" name="id" src="path"
     * [handler="Bitmap|Metafile|HyperGraphic|Picture" | mime="mime-type" | version="2.0" [linked="true"] [iconOnly="true] | className="class name"] />
     *
     * @param ft
     * @return
     * @throws InvalidMarkupException
     */

    public static SlxToken translateObjectDefinition(FolioToken ft) throws InvalidMarkupException {
        SlxToken t = new SlxToken("<object-def />");
        if (ft.count() < 4)
            throw new InvalidMarkupException("Object Definition (OD) tags must have 4 or more arguments.", ft);

        List<String> opts = ft.getOptionsArray();
        String type = opts.get(0);
        t.set("name", opts.get(1));
        t.set("src", opts.get(3)); //File name is always #4
        String thirdName = null;

        if (TokenUtils.fastMatches("FO", type)) {
            thirdName = "handler";
            t.set("type", "folio");
        } else if (TokenUtils.fastMatches("DL", type)) {
            thirdName = "mime";
            t.set("type", "data-link");
        } else if (TokenUtils.fastMatches("OL", type)) {
            thirdName = "version";
            t.set("type", "ole");
            int i = 4;
            while (i < opts.size()) {
                String s = opts.get(i);
                if (TokenUtils.fastMatches("LI", s)) t.set("linked", "true");
                else if (TokenUtils.fastMatches("IC", s)) t.set("iconOnly", "true");
                else throw new InvalidMarkupException("Invalid option found on OLE Object definition: " + s);
                i++;
            }

        } else if (TokenUtils.fastMatches("CL", type)) {
            thirdName = "className";
            t.set("type", "class-object");
        } else throw new InvalidMarkupException("Invalid Object Type: " + type);

        //The primary difference between the types is the meaning of the third argument.
        t.set(thirdName, opts.get(2));

        //All types except OLE should have exactly 4 arguments (RP is removed before this is called).
        if (!TokenUtils.fastMatches("OL", type) && opts.size() > 4)
            throw new InvalidMarkupException("Object Definition (OD) tags OL, CL, and FO can only have 4 arguments.", ft);


        return t;
    }
}