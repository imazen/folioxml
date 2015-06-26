package folioxml.directexport;

import folioxml.config.InfobaseSet;
import folioxml.config.TestConfig;
import folioxml.config.YamlInfobaseSet;
import folioxml.export.html.ReplaceUnderline;
import folioxml.export.DateCollapsingSlugProvider;
import folioxml.export.InfobaseSetPlugin;
import folioxml.export.InfobaseSetVisitor;
import folioxml.export.SlugProvider;
import folioxml.export.html.*;

import folioxml.export.plugins.*;
import folioxml.lucene.InfobaseSetIndexer;
import org.junit.Ignore;
import org.junit.Test;

import folioxml.core.InvalidMarkupException;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.*;

public class SimultaneousTest {


    private InfobaseSet loadPrivate(String name){

        InputStream privateYaml =  TestConfig.class.getResourceAsStream("../../private.yaml");

        String classDir = TestConfig.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String workingDir = Paths.get(classDir).getParent().getParent().getParent().getParent().toAbsolutePath().toString();

        return YamlInfobaseSet.parseYaml(workingDir, privateYaml).get(name);
    }

    @Test @Ignore
    public void IndexHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("Book|Section")));
        plugins.add(new InfobaseSetIndexer());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("folio_help"),plugins);
        visitor.complete();
    }

    @Test @Ignore
    public void InventoryHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider()));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("folio_help"),plugins);
        visitor.complete();
    }

    @Test @Ignore
    public void ExportHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        //inventory and

        //NodeInfoProvider class name

        //FixHttpLinks (MUST occur before ResolveHyperlinks or any other processors)
        //ResolveHyperlinks

        //Log unresolved hyperlinks

        //export XHTML or XML? Indent XML? HEadings only?

        //Notes/Popups: Pull and log, or enable via highslide?
        //FauxTabs?
        //
        //Drop program/menu links?


        //Newlines in headings converts to "" instead of "  "
        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(
                CleanupSlxStuff.CleanupOptions.PullProgramLinks,
                CleanupSlxStuff.CleanupOptions.PullMenuLinks,
                CleanupSlxStuff.CleanupOptions.DropTypeAttr,
                CleanupSlxStuff.CleanupOptions.RenameLinkToA,
                CleanupSlxStuff.CleanupOptions.RenameBookmarks,
                CleanupSlxStuff.CleanupOptions.RenameRecordToDiv));
        MultiRunner xhtml = new MultiRunner( new Images(), new Notes(), new Popups(), cleanup,new FauxTabs(80,120), new ReplaceUnderline(), new SplitSelfClosingTags());

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("Book|Section")));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());

        plugins.add(new ApplyProcessor(xhtml));
        plugins.add(new ExportCssFile());
        plugins.add(new ApplyProcessor(new HtmlTidy()));
        plugins.add(new ExportXmlFile());
        plugins.add(new ExportHtmlFiles(true,true));
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("folio_help"), plugins);

        visitor.complete();

    }


    @Test @Ignore
    public void IndexSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new DateCollapsingSlugProvider()));
        plugins.add(new InfobaseSetIndexer());
        //plugins.add(new ExportMappingsFiles());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(loadPrivate("testset"),plugins);

        visitor.complete();
    }


    @Test @Ignore
    public void InventorySet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(CleanupSlxStuff.CleanupOptions.PullProgramLinks, CleanupSlxStuff.CleanupOptions.PullMenuLinks, CleanupSlxStuff.CleanupOptions.DropTypeAttr));
        MultiRunner xhtml = new MultiRunner(cleanup, new Images(), new Notes(), new Popups(), new SplitSelfClosingTags());
        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider()));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(loadPrivate("testset"), plugins);

        visitor.complete();

    }

    @Test @Ignore
    public void ExportSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        //inventory and

        //NodeInfoProvider class name

        //FixHttpLinks (MUST occur before ResolveHyperlinks or any other processors)
        //ResolveHyperlinks

        //Log unresolved hyperlinks

        //export XHTML or XML? Indent XML? HEadings only?

        //Notes/Popups: Pull and log, or enable via highslide?
        //FauxTabs?
        //
        //Drop program/menu links?


        //Newlines in headings converts to "" instead of "  "
        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(
                CleanupSlxStuff.CleanupOptions.PullProgramLinks,
                CleanupSlxStuff.CleanupOptions.PullMenuLinks,
                CleanupSlxStuff.CleanupOptions.DropTypeAttr,
                CleanupSlxStuff.CleanupOptions.RenameLinkToA,
                CleanupSlxStuff.CleanupOptions.RenameBookmarks,
                CleanupSlxStuff.CleanupOptions.RenameRecordToDiv));
        MultiRunner xhtml = new MultiRunner( new Images(), new Notes(), new Popups(), cleanup,new FauxTabs(80,120), new ReplaceUnderline(), new SplitSelfClosingTags());

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new DateCollapsingSlugProvider()));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());

        plugins.add(new ApplyProcessor(xhtml));
        plugins.add(new ExportCssFile());
        plugins.add(new ExportXmlFile(false));
        plugins.add(new ExportHtmlFiles(true,true));
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(loadPrivate("testset"), plugins);

        visitor.complete();

    }


}


