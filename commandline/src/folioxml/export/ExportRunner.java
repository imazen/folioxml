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

    private Boolean IsIndexRequired(){
        Boolean resolve_jump_links= set.getBool("resolve_jump_links");
        if (resolve_jump_links == null) resolve_jump_links = true;
        Boolean resolve_query_links= set.getBool("resolve_query_links");
        if (resolve_query_links == null) resolve_query_links = true;
        return resolve_jump_links || resolve_query_links;
    }

    public void Run() throws IOException, InvalidMarkupException {
        if (IsIndexRequired()) Index();
        Export();
    }
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

        //Fix links and pull elements first
        plugins.add(new ApplyProcessor(new MultiRunner(new FixHttpLinks(), new LinkMapper(this.set), new PullElements(this.set)))); //FixHttpLinks must be the first thing to touch links - it cannot come after ResolveHyperlinks or RenameFiles

        //Exports assets
        plugins.add(new RenameFiles());

        //Resolve hyperlinks via Lucene if configured
        plugins.add(new ResolveHyperlinks());

        //Export inventory report
        if (!Boolean.FALSE.equals(set.getBool("export_inventory"))) {
            plugins.add(new ExportInventory());
        }

        //HTML transform Notes and Popups for highslide use if we're using it
        if (!Boolean.FALSE.equals(set.getBool("use_highslide"))) {
            plugins.add(new ApplyProcessor(new MultiRunner(new Notes(), new Popups())));
        }


        plugins.add(new ApplyProcessor(new FauxTabs(this.set)));

        //Universal cleanup, underline refactoring
        plugins.add(new ApplyProcessor(new MultiRunner(new CleanupSlxStuff(EnumSet.of(
                CleanupSlxStuff.CleanupOptions.DropTypeAttr,
                CleanupSlxStuff.CleanupOptions.RenameBookmarks,
                CleanupSlxStuff.CleanupOptions.RenameRecordToDiv)),
                new ReplaceUnderline(),
                new SplitSelfClosingTags(),
                new HtmlTidy())));


        //Always export a CSS file
        plugins.add(new ExportCssFile());


        if (!Boolean.FALSE.equals(set.getBool("export_hidden_text"))) {
            plugins.add(new ExportHiddenText());
        }
        if (!Boolean.FALSE.equals(set.getBool("export_xml"))) {
            plugins.add(new ExportXmlFile());
        }
        if (!Boolean.FALSE.equals(set.getBool("export_html"))) {
            plugins.add(new ExportHtmlFiles());
            plugins.add(new CreateIndexHtmlFile());
        }

        InfobaseSetVisitor visitor = new InfobaseSetVisitor(set, plugins);
        //Run with all plugins
        visitor.complete();
    }



}
