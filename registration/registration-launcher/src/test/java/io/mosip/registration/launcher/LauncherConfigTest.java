package io.mosip.registration.launcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class LauncherConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static Properties props(String upgradeServer) {
        Properties p = new Properties();
        p.setProperty("mosip.client.upgrade.server.url", upgradeServer);
        p.setProperty("mosip.reg.client.url", "%s/registration-client/");
        return p;
    }

    @Test
    public void buildsAllUrlsForVersion() {
        LauncherConfig config = LauncherConfig.fromProperties(props("https://dev.mosip.net"));
        String base = "https://dev.mosip.net/registration-client/1.4.0/";

        assertEquals(base + "MANIFEST.MF.sig", config.rootManifestSigUrl("1.4.0"));
        assertEquals(base + "lib/MANIFEST.MF", config.libManifestUrl("1.4.0"));
        assertEquals(base + "lib/MANIFEST.MF.sig", config.libManifestSigUrl("1.4.0"));
        assertEquals(base + "lib.zip", config.libZipUrl("1.4.0"));
    }

    @Test
    public void serverTrailingSlash_isNormalised() {
        LauncherConfig config = LauncherConfig.fromProperties(props("https://dev.mosip.net/"));
        assertEquals("https://dev.mosip.net/registration-client/1.4.0/lib.zip", config.libZipUrl("1.4.0"));
    }

    @Test
    public void usesDefaultTemplateWhenAbsent() {
        Properties p = new Properties();
        p.setProperty("mosip.client.upgrade.server.url", "https://example.org");
        LauncherConfig config = LauncherConfig.fromProperties(p);
        assertEquals("https://example.org/registration-client/2.0.0/lib.zip", config.libZipUrl("2.0.0"));
    }

    @Test(expected = NullPointerException.class)
    public void missingUpgradeServer_throws() {
        LauncherConfig.fromProperties(new Properties());
    }

    @Test(expected = IllegalArgumentException.class)
    public void plaintextHttpUpgradeServer_rejected() {
        LauncherConfig.fromProperties(props("http://dev.mosip.net"));
    }

    @Test
    public void loadsFromFile() throws Exception {
        File file = folder.newFile("mosip-application.properties");
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            props("https://dev.mosip.net").store(out, null);
        }
        LauncherConfig config = LauncherConfig.load(file);
        assertEquals("https://dev.mosip.net/registration-client/1.3.0/lib/MANIFEST.MF",
                config.libManifestUrl("1.3.0"));
    }
}
