/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
    public void fromProperties_validServer_buildsAllVersionedUrls() {
        LauncherConfig config = LauncherConfig.fromProperties(props("https://dev.mosip.net"));
        String base = "https://dev.mosip.net/registration-client/1.4.0/";

        assertEquals(base + "MANIFEST.MF.sig", config.rootManifestSigUrl("1.4.0"));
        assertEquals(base + "lib/MANIFEST.MF", config.libManifestUrl("1.4.0"));
        assertEquals(base + "lib/MANIFEST.MF.sig", config.libManifestSigUrl("1.4.0"));
        assertEquals(base + "lib.zip", config.libZipUrl("1.4.0"));
    }

    @Test
    public void fromProperties_serverTrailingSlash_isNormalised() {
        LauncherConfig config = LauncherConfig.fromProperties(props("https://dev.mosip.net/"));
        assertEquals("https://dev.mosip.net/registration-client/1.4.0/lib.zip", config.libZipUrl("1.4.0"));
    }

    @Test
    public void fromProperties_regClientTemplateAbsent_usesDefaultTemplate() {
        Properties p = new Properties();
        p.setProperty("mosip.client.upgrade.server.url", "https://example.org");
        LauncherConfig config = LauncherConfig.fromProperties(p);
        assertEquals("https://example.org/registration-client/2.0.0/lib.zip", config.libZipUrl("2.0.0"));
    }

    @Test(expected = NullPointerException.class)
    public void fromProperties_missingUpgradeServer_throws() {
        LauncherConfig.fromProperties(new Properties());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromProperties_plaintextHttpServer_rejected() {
        LauncherConfig.fromProperties(props("http://dev.mosip.net"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromProperties_templateCrossHost_rejected() {
        // "%s@evil.example/..." passes a startsWith("https://") check but parses to host=evil.example.
        Properties p = new Properties();
        p.setProperty("mosip.client.upgrade.server.url", "https://good.host");
        p.setProperty("mosip.reg.client.url", "%s@evil.example/registration-client/");
        LauncherConfig.fromProperties(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromProperties_templateWithQuery_rejected() {
        // A query would be corrupted once versionBase appends "<version>/" as plain path text.
        Properties p = new Properties();
        p.setProperty("mosip.client.upgrade.server.url", "https://dev.mosip.net");
        p.setProperty("mosip.reg.client.url", "%s/registration-client?channel=beta/");
        LauncherConfig.fromProperties(p);
    }

    @Test
    public void load_propertiesFile_buildsConfig() throws Exception {
        File file = folder.newFile("mosip-application.properties");
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            props("https://dev.mosip.net").store(out, null);
        }
        LauncherConfig config = LauncherConfig.load(file);
        assertEquals("https://dev.mosip.net/registration-client/1.3.0/lib/MANIFEST.MF",
                config.libManifestUrl("1.3.0"));
    }
}
