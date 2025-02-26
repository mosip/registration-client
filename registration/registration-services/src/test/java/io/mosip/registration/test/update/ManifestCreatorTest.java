package io.mosip.registration.test.update;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.test.service.PreRegZipHandlingServiceTest;
import io.mosip.registration.update.ClientIntegrityValidator;
import io.mosip.registration.update.ClientSetupValidator;
import io.mosip.registration.update.ManifestCreator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


@RunWith(MockitoJUnitRunner.class)
public class ManifestCreatorTest extends ManifestCreator {

    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";

    @Test
    public void mainTest() throws Exception {
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

        FileUtils.copyDirectory(Path.of(".","src", "test", "resources", "manifesttest", "lib").toFile(),
                Path.of("lib").toFile());
        FileUtils.copyFile(Path.of(".","src", "test", "resources", "manifesttest", MANIFEST_FILE_NAME).toFile(),
                Path.of(MANIFEST_FILE_NAME).toFile());

        ClientSetupValidator clientSetupValidator = new ClientSetupValidator();
        clientSetupValidator.validateBuildSetup();
        boolean failed = clientSetupValidator.isValidationFailed();
        Assert.assertFalse(failed);
    }


    @Test
    public void integrityCheckTest() throws IOException {
        URL url = ManifestCreatorTest.class.getResource("/setup/registration-api-1.3.0-SNAPSHOT.jar");
        X509Certificate certificate =  ClientIntegrityValidator.getCertificate();
        JarFile jarFile = new JarFile(url.getFile());
        ClientIntegrityValidator.verifyIntegrity(certificate, jarFile);
    }

    @Test(expected = SecurityException.class)
    public void integrityCheckTest2() throws IOException {
        URL url = ManifestCreatorTest.class.getResource("/setup/registration-api-1.2.0-SNAPSHOT.jar");
        JarFile jarFile = new JarFile(url.getFile());
        ClientIntegrityValidator.verifyIntegrity(null, jarFile);
    }

}
