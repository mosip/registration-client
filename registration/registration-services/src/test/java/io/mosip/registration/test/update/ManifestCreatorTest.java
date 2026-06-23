package io.mosip.registration.test.update;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.update.ClientIntegrityValidator;
import io.mosip.registration.update.ClientSetupValidator;
import io.mosip.registration.update.ManifestCreator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


@RunWith(MockitoJUnitRunner.class)
public class ManifestCreatorTest extends ManifestCreator {

    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";

    private File listModeTempDir;

    @After
    public void cleanUpListMode() throws IOException {
        if (listModeTempDir != null && listModeTempDir.exists()) {
            for (File f : listModeTempDir.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
            Files.deleteIfExists(listModeTempDir.toPath());
        }
    }

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
    public void listModeCreatesManifestFromExplicitFiles() throws Exception {
        listModeTempDir = Files.createTempDirectory("manifest-list-mode").toFile();
        File jre = new File(listModeTempDir, "jre21.zip");
        File runBat = new File(listModeTempDir, "run.bat");
        Files.write(jre.toPath(), "jre-bytes".getBytes(StandardCharsets.UTF_8));
        Files.write(runBat.toPath(), "run-bytes".getBytes(StandardCharsets.UTF_8));

        String version = "1.3.0";
        main(new String[]{"--list", version, listModeTempDir.getPath(), jre.getPath(), runBat.getPath()});

        File manifestFile = new File(listModeTempDir, MANIFEST_FILE_NAME);
        Assert.assertTrue(manifestFile.exists());
        Manifest manifest;
        try (FileInputStream in = new FileInputStream(manifestFile)) {
            manifest = new Manifest(in);
        }
        Assert.assertEquals(version, manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));
        Assert.assertEquals(2, manifest.getEntries().size());
        Assert.assertTrue(manifest.getEntries().containsKey("jre21.zip"));
        Assert.assertTrue(manifest.getEntries().containsKey("run.bat"));
        Assert.assertNotNull(manifest.getEntries().get("jre21.zip").getValue(Attributes.Name.CONTENT_TYPE));
    }

    @Test
    public void listModeSkipsMissingFiles() throws Exception {
        listModeTempDir = Files.createTempDirectory("manifest-list-mode-missing").toFile();
        File present = new File(listModeTempDir, "run.bat");
        Files.write(present.toPath(), "run-bytes".getBytes(StandardCharsets.UTF_8));
        File missing = new File(listModeTempDir, "_launcher.jar"); // never created (produced later by T2)

        String version = "1.3.0";
        main(new String[]{"--list", version, listModeTempDir.getPath(), present.getPath(), missing.getPath()});

        File manifestFile = new File(listModeTempDir, MANIFEST_FILE_NAME);
        Assert.assertTrue(manifestFile.exists());
        Manifest manifest;
        try (FileInputStream in = new FileInputStream(manifestFile)) {
            manifest = new Manifest(in);
        }
        Assert.assertEquals(1, manifest.getEntries().size());
        Assert.assertTrue(manifest.getEntries().containsKey("run.bat"));
        Assert.assertFalse(manifest.getEntries().containsKey("_launcher.jar"));
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
