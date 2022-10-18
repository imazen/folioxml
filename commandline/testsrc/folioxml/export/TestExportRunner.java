package folioxml.export;

import folioxml.config.InfobaseSet;
import folioxml.config.TestConfig;
import folioxml.config.YamlInfobaseSet;
import folioxml.core.InvalidMarkupException;
import folioxml.export.html.*;
import folioxml.export.plugins.*;
import folioxml.lucene.InfobaseSetIndexer;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TestExportRunner {

    private InfobaseSet loadPrivate(String name) {

        InputStream privateYaml =  TestConfig.class.getResourceAsStream("/private.yaml");

        String classDir = null;
        try {
            classDir = new File(TestConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String workingDir = Paths.get(classDir).getParent().getParent().getParent().getParent().toAbsolutePath().toString();

        return YamlInfobaseSet.parseYaml(workingDir, privateYaml).get(name);
    }

    @Test @Ignore
    public void IndexHelp() throws InvalidMarkupException, IOException {
        new ExportRunner(TestConfig.get("folio_help")).Index();
    }

    @Test @Ignore
    public void ExportHelp() throws InvalidMarkupException, IOException{
        new ExportRunner(TestConfig.get("folio_help")).Export();
    }

    @Test @Ignore
    public void IndexCrp() throws InvalidMarkupException, IOException {
        new ExportRunner(loadPrivate("crp")).Index();
    }

    @Test @Ignore
    public void ExportCrp() throws InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("crp")).Export();
    }


    @Test @Ignore
    public void IndexSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset")).Index();
    }


    @Test @Ignore
    public void ExportSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset")).Export();
    }

    @Test @Ignore
    public void IndexExportSet() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset")).Run();
    }

    @Test @Ignore
    public void IndexSet2() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset2")).Index();
    }


    @Test @Ignore
    public void ExportSet2() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset2")).Export();
    }

    @Test @Ignore
    public void IndexExportSet2() throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException{
        new ExportRunner(loadPrivate("testset2")).Run();
    }
}
