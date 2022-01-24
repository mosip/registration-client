package io.mosip.registration.test.update;

import io.mosip.registration.update.ManifestCreator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


@RunWith(MockitoJUnitRunner.class)
public class ManifestCreatorTest extends ManifestCreator {

    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";

    @Test
    public void mainTest() throws IOException {
        String version = "0.1v";
        String libraryFolderPath = Path.of(".","src", "test", "resources", "manifesttest", "lib").toString();
        String targetPath = Path.of(".","src", "test", "resources", "manifesttest").toString();
        main(new String[]{version, libraryFolderPath, targetPath});

        File manifestFile = Path.of(".","src", "test", "resources", "manifesttest", MANIFEST_FILE_NAME).toFile();
        Assert.assertTrue(manifestFile.exists());
        Manifest manifest = new Manifest(new FileInputStream(manifestFile));
        Assert.assertEquals(version, manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));
        Assert.assertEquals(2, manifest.getEntries().size());
        Assert.assertTrue(manifest.getEntries().containsKey("logback.xml"));
        Assert.assertTrue(manifest.getEntries().containsKey("mosip-application.properties"));
    }

}
