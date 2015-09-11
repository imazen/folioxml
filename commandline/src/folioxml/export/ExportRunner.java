package folioxml.export;

import folioxml.config.*;
import folioxml.core.InvalidMarkupException;
import folioxml.export.html.ReplaceUnderline;
import folioxml.export.html.*;
import folioxml.export.plugins.*;
import folioxml.lucene.InfobaseSetIndexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class ExportRunner {

    InfobaseSet set;
    public ExportRunner(InfobaseSet set) {
        this.set = set;


    }

    private NodeInfoProvider createProvider() throws InvalidMarkupException {
        String providerName = set.getString("structure_class");
        if (providerName == null) providerName = "folioxml.export.structure.SlugProvider";

        Object oparams = set.getObject("structure_class_params");

        if (oparams instanceof java.util.ArrayList){

        }

        ArrayList params = oparams == null ? new ArrayList() : (ArrayList)oparams;
        Class[] paramClasses = new Class[params.size()];
        for(int i =0; i < params.size(); i++)
            paramClasses[i] = params.get(i).getClass();
        try {
            Class<?> clazz = Class.forName(providerName);
            Constructor<?> ctor = clazz.getConstructor(paramClasses);
            Object object = ctor.newInstance(params.toArray(new Object[params.size()]));

            return (NodeInfoProvider) object;
        }catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex){
            throw new InvalidMarkupException("Failed to create the specified structure_class (" + providerName + "). \n" + ex.toString());
        }
    }

    //NodeInfoProvider
    //How to deal with query links
    //Which levels to

    public void Index() throws IOException, InvalidMarkupException {
        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(createProvider()));
        plugins.add(new InfobaseSetIndexer());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(set, plugins);
        visitor.complete();
    }

    public void Export() throws IOException, InvalidMarkupException {
        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(createProvider()));

        plugins.add(new ApplyProcessor(new MultiRunner(new FixHttpLinks(), new LinkMapper(this.set), new PullElements(this.set)))); //FixHttpLinks must be the first thing to touch links - it cannot come after ResolveHyperlinks or RenameFiles


        //TODO: Do we export assets?
        plugins.add(new RenameFiles());



        //TODO: Do we resolve query links? Do we log unresolved links
        plugins.add(new ResolveHyperlinks());


        //TODO: Do we export inventory?
        plugins.add(new ExportInventory());


        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(
                CleanupSlxStuff.CleanupOptions.PullProgramLinks,
                CleanupSlxStuff.CleanupOptions.PullMenuLinks,
                CleanupSlxStuff.CleanupOptions.DropTypeAttr,
                CleanupSlxStuff.CleanupOptions.RenameBookmarks,
                CleanupSlxStuff.CleanupOptions.RenameRecordToDiv));

        //TODO: faux tabs y/n, widths? other tuning?



        MultiRunner xhtml = new MultiRunner(new Notes(), new Popups(), cleanup,new FauxTabs(80,120), new ReplaceUnderline(), new SplitSelfClosingTags(), new HtmlTidy());

        plugins.add(new ApplyProcessor(xhtml));

        plugins.add(new ExportCssFile());
        if (!Boolean.FALSE.equals(set.getBool("export_xml"))) {
            plugins.add(new ExportXmlFile());
        }
        if (!Boolean.FALSE.equals(set.getBool("export_html"))) {
            plugins.add(new ExportHtmlFiles());
        }
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(set, plugins);

        visitor.complete();
    }



}
