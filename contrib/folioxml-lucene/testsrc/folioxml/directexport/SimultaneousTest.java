package folioxml.directexport;

import folioxml.config.TestConfig;
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
import java.io.UnsupportedEncodingException;
import java.util.*;

public class SimultaneousTest {

    @Test @Ignore
    public void IndexHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("")));
        plugins.add(new InfobaseSetIndexer());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("folio_help"),plugins);
        visitor.complete();
    }

    @Test @Ignore
    public void InventoryHelp() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("")));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("folio_help"),plugins);
        visitor.complete();
    }

    @Test @Ignore
    public void IndexSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("")));
        plugins.add(new InfobaseSetIndexer());
        //plugins.add(new ExportMappingsFiles());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("testset"),plugins);

        visitor.complete();
    }


    @Test @Ignore
    public void InventorySet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(CleanupSlxStuff.CleanupOptions.PullProgramLinks, CleanupSlxStuff.CleanupOptions.PullMenuLinks, CleanupSlxStuff.CleanupOptions.DropTypeAttr));
        MultiRunner xhtml = new MultiRunner(cleanup, new Images(), new Notes(), new Popups(), new SplitSelfClosingTags());
        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("")));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("testset"),plugins);

        visitor.complete();

    }

    @Test @Ignore
    public void ExportSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{

        //Newlines in headings converts to "" instead of "  "
        CleanupSlxStuff cleanup = new CleanupSlxStuff(EnumSet.of(
                CleanupSlxStuff.CleanupOptions.PullProgramLinks,
                CleanupSlxStuff.CleanupOptions.PullMenuLinks,
                CleanupSlxStuff.CleanupOptions.DropTypeAttr,
                CleanupSlxStuff.CleanupOptions.RenameLinkToA,
                CleanupSlxStuff.CleanupOptions.RenameBookmarks,
                CleanupSlxStuff.CleanupOptions.RenameRecordToDiv));
        MultiRunner xhtml = new MultiRunner(cleanup, new Images(), new Notes(), new Popups(), new SplitSelfClosingTags());

        List<InfobaseSetPlugin> plugins = new ArrayList<InfobaseSetPlugin>();
        plugins.add(new ExportStructure(new SlugProvider("")));
        plugins.add(new RenameFiles());
        plugins.add(new ApplyProcessor(new FixHttpLinks()));
        plugins.add(new ResolveHyperlinks());
        plugins.add(new ExportInventory());

        plugins.add(new ApplyProcessor(xhtml));
        plugins.add(new ExportCssFile());
        plugins.add(new ExportXmlFile(true));
        //plugins.add(new ExportHtmlFiles());
        InfobaseSetVisitor visitor = new InfobaseSetVisitor(TestConfig.get("testset"),plugins);

        visitor.complete();

    }


}


