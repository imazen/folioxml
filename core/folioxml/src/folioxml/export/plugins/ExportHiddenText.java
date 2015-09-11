package folioxml.export.plugins;

import folioxml.config.ExportLocations;
import folioxml.config.InfobaseConfig;
import folioxml.config.InfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.css.EffectiveStyle;
import folioxml.export.FileNode;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.InventoryNodes;
import folioxml.export.LogStreamProvider;
import folioxml.slx.ISlxTokenReader;
import folioxml.slx.SlxContextStack;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;
import folioxml.xml.NodeList;
import folioxml.xml.XmlRecord;

import java.io.IOException;
import java.util.List;


public class ExportHiddenText implements InfobaseSetPlugin {

    public ExportHiddenText(){}

    LogStreamProvider logs = null;

    @Override
    public void beginInfobaseSet(InfobaseSet set, ExportLocations export, LogStreamProvider logs) throws IOException, InvalidMarkupException {
        this.logs = logs;
    }

    EffectiveStyle eDisplay = null;
    EffectiveStyle eForeground = null;
    EffectiveStyle eBackground = null;

    @Override
    public void beginInfobase(InfobaseConfig infobase) throws IOException {
        eDisplay = new EffectiveStyle("display");
        eForeground = new EffectiveStyle("color");
        eBackground = new EffectiveStyle("background-color");
    }

    @Override
    public ISlxTokenReader wrapSlxReader(ISlxTokenReader reader) {
        return reader;
    }

    @Override
    public void onSlxRecordParsed(SlxRecord clean_slx) throws InvalidMarkupException, IOException {
        if (clean_slx.isRootRecord()){
            eDisplay.addStylesheet(clean_slx);
            eForeground.addStylesheet(clean_slx);
            eBackground.addStylesheet(clean_slx);
            return;
        }

        StringBuilder hiddenText = new StringBuilder();
        SlxContextStack stack = new SlxContextStack(false,false);
        stack.process(clean_slx);
        String spacing = TokenUtils.entityDecodeString(" &#x00A0; ");
        boolean recordUsesColorInvisibility = false;
        for (SlxToken t : clean_slx.getTokens()) {
            stack.process(t);// call this on each token.

            boolean causesNewline = t.matches("p|br|td|th|note") && !t.isOpening();

            //We only care about tokens that add text.
            if (!t.isTextOrEntity() && !causesNewline) continue;

            //Get all open tags within the current context.
            List<SlxToken> tags = stack.getOpenTags(null,false,false);

            boolean hidden = false;
            //Get the effective CSS values
            String display = eDisplay.getEffectiveValue(t, tags);
            String fc = eForeground.getEffectiveValue(t,tags);
            String bg = eBackground.getEffectiveValue(t,tags);

            if (fc != null && fc.equalsIgnoreCase(bg)){
                recordUsesColorInvisibility = true;
                hidden = true;
            }
            if ("none".equalsIgnoreCase(display)){
                hidden = true;
            }

            if (hidden && t.isTextOrEntity()) {
                hiddenText.append(t.isEntity() ? TokenUtils.entityDecodeString(t.markup) : t.markup);
            }
            if (causesNewline && hiddenText.length() > 0) {
                hiddenText.append(spacing);
            }
        }

        //Write hidden text to file
        if (hiddenText.length() > 0) {
            logs.getNamedStream("hidden_text").append(recordUsesColorInvisibility ? "Text hidden by coloring" : "Hidden text").append(" in record ").append(clean_slx.get("folioId")).append("\n").append(hiddenText).append("\n");
        }
    }


    @Override
    public void onRecordTransformed(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {

    }

    @Override
    public FileNode assignFileNode(XmlRecord xr, SlxRecord dirty_slx) throws InvalidMarkupException, IOException {
        return null;
    }

    @Override
    public void onRecordComplete(XmlRecord xr, FileNode file) throws InvalidMarkupException, IOException {

    }


    @Override
    public void endInfobase(InfobaseConfig infobase) throws IOException, InvalidMarkupException {

    }

    @Override
    public void endInfobaseSet(InfobaseSet set) throws IOException {

    }
}
