/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import com.sun.net.httpserver.HttpServer;
import io.mosip.registration.launcher.common.HashUtil;
import io.mosip.registration.launcher.common.ManifestVerifier;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JreMigrationStagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static KeyPair keyPair;

    private static final byte[] APP_JAR = "jar-bytes".getBytes(StandardCharsets.UTF_8);
    private static final String LIB_ENTRY = "app.jar";

    @BeforeClass
    public static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    /**
     * Sets up a root with run.bat, a signed lib (served), and a root manifest covering jre21.zip plus
     * the migration.exe / rollback.exe the launcher now requires and the staged .artifacts/run.bat that
     * migration.exe installs into the app root (all staged in .artifacts/, integrity listed).
     * {@code _launcher.jar} is deliberately NOT staged/listed so tests can use it as the
     * "present but unverifiable" artifact.
     */
    private TestServer baseSetup(File root) throws Exception {
        return baseSetup(root, true);
    }

    /**
     * @param stageLauncher when true, {@code _launcher.jar} is staged in {@code .artifacts/} and listed
     *                      in the root manifest, which the happy paths need now that staging refuses to
     *                      start the swap without it. Pass false to model it as present-but-unlisted.
     */
    private TestServer baseSetup(File root, boolean stageLauncher) throws Exception {
        return baseSetup(root, stageLauncher, Collections.<String, byte[]>emptyMap());
    }

    /**
     * @param routeOverrides replaces what the server returns for the given paths, so a test can model an
     *                       upgrade server whose own copy of an artifact disagrees with the signed root
     *                       manifest.
     */
    private TestServer baseSetup(File root, boolean stageLauncher, Map<String, byte[]> routeOverrides)
            throws Exception {
        write(new File(root, "run.bat"), "current-run-bat");

        // jre21.zip + the two native exes on disk in .artifacts, hashes recorded in the (verified)
        // root manifest so they pass the integrity gate and copyRequired can stage them.
        File jre21Zip = new File(root, ".artifacts/jre21.zip");
        writeZipFile(jre21Zip, entry("release", "JAVA_VERSION=\"21.0.3\"\n"));
        File migrationExe = new File(root, ".artifacts/migration.exe");
        File rollbackExe = new File(root, ".artifacts/rollback.exe");
        write(migrationExe, "migration-exe-bytes");
        write(rollbackExe, "rollback-exe-bytes");
        // The Java 21 run.bat is delivered through .artifacts/ as well (migration.exe copies it into the
        // app root after the swap), so it is integrity-listed and verified like the rest.
        File runBatArtifact = new File(root, ".artifacts/run.bat");
        write(runBatArtifact, "jre21-run-bat");
        // migration.exe copies _launcher.jar into lib/ after emptying it, so staging requires it too.
        File launcherArtifact = new File(root, ".artifacts/_launcher.jar");
        if (stageLauncher) {
            write(launcherArtifact, "launcher-jar-bytes");
        }

        Map<String, String> rootEntries = new LinkedHashMap<>();
        rootEntries.put("jre21.zip", HashUtil.sha256Hex(jre21Zip));
        rootEntries.put("migration.exe", HashUtil.sha256Hex(migrationExe));
        rootEntries.put("rollback.exe", HashUtil.sha256Hex(rollbackExe));
        rootEntries.put("run.bat", HashUtil.sha256Hex(runBatArtifact));
        if (stageLauncher) {
            rootEntries.put("_launcher.jar", HashUtil.sha256Hex(launcherArtifact));
        }
        byte[] rootManifest = manifestBytes("1.4.0", rootEntries);
        File rootManifestFile = new File(root, "MANIFEST.MF");
        write(rootManifestFile, new String(rootManifest, StandardCharsets.UTF_8));

        // signed lib manifest + lib.zip served over HTTP
        byte[] libManifest = manifestBytes("1.4.0", LIB_ENTRY, HashUtil.sha256Hex(APP_JAR));
        byte[] libSig = sign(libManifest, keyPair.getPrivate());
        byte[] libZip = zipBytes(LIB_ENTRY, APP_JAR);

        Map<String, byte[]> routes = new HashMap<>();
        routes.put("/v/lib/MANIFEST.MF", libManifest);
        routes.put("/v/lib/MANIFEST.MF.sig", libSig);
        routes.put("/v/lib.zip", libZip);
        // The root artifacts are served from <version>/lib/ as well (the layout softwareUpdateHandler
        // downloads them from), which is what makes the Case A / Case D restore possible.
        routes.put("/v/lib/jre21.zip", Files.readAllBytes(jre21Zip.toPath()));
        routes.put("/v/lib/migration.exe", Files.readAllBytes(migrationExe.toPath()));
        routes.put("/v/lib/rollback.exe", Files.readAllBytes(rollbackExe.toPath()));
        routes.put("/v/lib/run.bat", Files.readAllBytes(runBatArtifact.toPath()));
        if (stageLauncher) {
            routes.put("/v/lib/_launcher.jar", Files.readAllBytes(launcherArtifact.toPath()));
        }
        routes.putAll(routeOverrides);
        // stage() receives the signature-verified parsed manifest (as Initialization passes it from
        // StartupEvaluator), not a File it re-reads from disk.
        return new TestServer(serve(routes), ManifestVerifier.parse(rootManifest));
    }

    @Test
    public void stage_verifiedHappyPath() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertTrue(new File(root, ".TEMP/app.jar").exists());
            assertTrue(new File(root, "jre21_temp/release").exists());
            assertTrue(new File(root, "run.bat_jre11").exists());
            assertTrue(new File(root, "migration.exe").exists());
            assertTrue(new File(root, "rollback.exe").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_partialJre21Temp_isDiscardedAndReExtracted() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // A power loss / force-kill during a previous unzip: jre21_temp.partial/ holds a half-written
        // tree and jre21_temp/ was never created, because the rename is the last step.
        File partial = new File(root, "jre21_temp.partial");
        write(new File(partial, "release"), "TRUNCATED");
        write(new File(partial, "leftover-from-interrupted-unzip"), "junk");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertFalse("the partial tree must not survive the retry", partial.exists());
            assertTrue(new File(root, "jre21_temp/release").exists());
            assertTrue("the JRE must be re-extracted, not resumed from the partial tree",
                    read(new File(root, "jre21_temp/release")).contains("21.0.3"));
            assertFalse("stale files from the interrupted unzip must be gone",
                    new File(root, "jre21_temp/leftover-from-interrupted-unzip").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_completeJre21Temp_skipsReExtraction() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // migration.exe was force-killed after a COMPLETE extraction (N6). jre21_temp/ exists because the
        // rename succeeded, so re-extracting ~200MB would be wasted work -- the retry must skip step 4.
        // A marker file that a real extraction would never produce proves the directory was left alone.
        write(new File(root, "jre21_temp/already-extracted-marker"), "kept");
        // An EARLIER run was interrupted mid-unzip and a later one completed, so a stale partial tree is
        // also on disk. Skipping extraction must not mean skipping its reclamation: rollback.exe removes
        // only jre21_temp/, and MigrationCleaner runs solely on normal startup -- unreachable while the
        // version mismatch driving this migration persists. Nothing else would ever free those ~200MB.
        File stalePartial = new File(root, "jre21_temp.partial");
        write(new File(stalePartial, "release"), "TRUNCATED");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertTrue("a complete jre21_temp/ must be reused, not re-extracted",
                    new File(root, "jre21_temp/already-extracted-marker").exists());
            assertFalse("a stale partial tree must be reclaimed even when extraction is skipped",
                    stalePartial.exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_tamperedJre21Zip_isRestoredFromServer() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // tamper jre21.zip AFTER its hash was recorded in the root manifest. The root manifest's
        // signature is still valid, so this is Case D -- a bad local file under a trustworthy manifest,
        // which the design repairs from the server instead of aborting.
        write(new File(root, ".artifacts/jre21.zip"), "tampered-bytes");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IntegrityRestoredException for a tampered jre21.zip");
        } catch (IntegrityRestoredException expected) {
            assertEquals(Collections.singletonList("jre21.zip"), expected.getRestored());
            assertTrue(ManifestVerifier.fileMatches(ts.rootManifest, "jre21.zip",
                    new File(root, ".artifacts/jre21.zip")));
            // The run stops at the restore: the operator is asked to restart, and nothing downstream may
            // have run on artifacts that changed underneath it.
            assertFalse(new File(root, "jre21_temp").exists());
            assertFalse(new File(root, "migration.exe").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_staleAppRootExes_overwrittenFromVerifiedArtifacts() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // An earlier (interrupted, possibly different-version) attempt left exes at the app root. Those
        // are the binaries MigrationLauncher executes, so staging must replace them with the copies just
        // verified against the signed root manifest -- never leave a stale/unverified binary in place.
        write(new File(root, "migration.exe"), "stale-migration-exe-from-older-version");
        write(new File(root, "rollback.exe"), "stale-rollback-exe-from-older-version");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertEquals("migration-exe-bytes", read(new File(root, "migration.exe")));
            assertEquals("rollback-exe-bytes", read(new File(root, "rollback.exe")));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_appRootExeAlreadyMatchesVerifiedArtifact_isNotRecopied() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // An app-root exe whose bytes already equal the just-verified .artifacts/ copy must be left
        // alone. Re-copying it unconditionally is what breaks a resumable retry on Windows, where a
        // still-running (or AV-held) exe locks its own image and REPLACE_EXISTING throws
        // AccessDeniedException -- turning a retry that used to succeed into a permanent abort.
        File appRootExe = new File(root, "migration.exe");
        write(appRootExe, "migration-exe-bytes");
        long untouched = System.currentTimeMillis() - 600000L;
        assertTrue("test setup: could not age the file", appRootExe.setLastModified(untouched));
        long before = appRootExe.lastModified();
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertEquals("identical bytes must not be rewritten", before, appRootExe.lastModified());
            assertEquals("migration-exe-bytes", read(appRootExe));
            // The differing sibling still gets staged, so the skip is content-driven, not a blanket skip.
            assertEquals("rollback-exe-bytes", read(new File(root, "rollback.exe")));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_doesNotTouchTheApplicationRootRunBat() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // run.bat is now hash-verified in .artifacts/, but staging must never install it at the app
        // root -- migration.exe does that after the JRE swap. The running JRE 11 launcher script has to
        // survive staging intact, or a failed migration would leave an unbootable client.
        File appRootRunBat = new File(root, "run.bat");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);

            assertEquals("app-root run.bat must be left as the JRE 11 script", "current-run-bat",
                    read(appRootRunBat));
            // ... and the backup taken for rollback must be that same script.
            assertEquals("current-run-bat", read(new File(root, "run.bat_jre11")));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_tamperedRunBatInArtifacts_isRestoredFromServer() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // migration.exe copies .artifacts/run.bat into the app root, where it becomes the script that
        // launches the client. Design scenario N3: a failed hash under a valid ./MANIFEST.MF.sig is
        // restored from the server and the operator asked to restart -- never installed unverified, and
        // never a dead end the operator cannot get out of.
        write(new File(root, ".artifacts/run.bat"), "tampered-run-bat");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IntegrityRestoredException for a tampered run.bat");
        } catch (IntegrityRestoredException expected) {
            assertEquals(Collections.singletonList("run.bat"), expected.getRestored());
            assertEquals("jre21-run-bat", read(new File(root, ".artifacts/run.bat")));
            // the app-root run.bat is the JRE 11 script this very JVM was started by: untouched
            assertEquals("current-run-bat", read(new File(root, "run.bat")));
            // .TEMP/ is left clean: step 3 extracts lib.zip there and rejects anything the signed
            // lib/MANIFEST.MF does not list, so a leftover copy would break the next lib update. The
            // download never goes there in the first place, and its scratch dir is dropped once complete
            // -- run.bat copies .TEMP/* into lib/, so a stray .part there would reach the classpath.
            assertFalse(new File(root, ".TEMP/run.bat").exists());
            assertFalse(new File(root, ".TEMP/run.bat.part").exists());
            assertFalse(new File(root, ".TEMP.restore").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_restore_alertsTheOperatorWithTheDesignWording() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        write(new File(root, ".artifacts/run.bat"), "tampered-run-bat");
        // The design does not just require the repair, it requires the operator be told it is happening,
        // in these words -- otherwise the client appears to stall on an unexplained download.
        List<String> alerts = new ArrayList<>();
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000, null, alerts::add);
            fail("expected IntegrityRestoredException for a tampered run.bat");
        } catch (IntegrityRestoredException expected) {
            assertEquals(Collections.singletonList("run.bat integrity check failed. Restoring from server..."),
                    alerts);
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_restoredCopyAlsoFailsHash_failsClosed() throws Exception {
        File root = folder.getRoot();
        // The server's own run.bat disagrees with the signed root manifest. Re-downloading cannot fix
        // that, and the bytes are unverifiable, so staging must fail closed instead of restoring.
        Map<String, byte[]> serverCopyIsWrong = new HashMap<>();
        serverCopyIsWrong.put("/v/lib/run.bat", "server-copy-does-not-match".getBytes(StandardCharsets.UTF_8));
        TestServer ts = baseSetup(root, true, serverCopyIsWrong);
        write(new File(root, ".artifacts/run.bat"), "tampered-run-bat");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException when the restored copy still fails its hash");
        } catch (IntegrityRestoredException restored) {
            fail("a server copy that fails the manifest hash must never count as a restore");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("still fails its root-manifest hash"));
            // nothing unverifiable may be promoted into .artifacts/, and .TEMP/ is left clean
            assertEquals("tampered-run-bat", read(new File(root, ".artifacts/run.bat")));
            assertFalse(new File(root, ".TEMP/run.bat").exists());
            assertFalse(new File(root, ".TEMP.restore").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_runBatPresentButUnlistedInRootManifest_failsClosed() throws Exception {
        File root = folder.getRoot();
        write(new File(root, "run.bat"), "current");
        File jre21Zip = new File(root, ".artifacts/jre21.zip");
        writeZipFile(jre21Zip, entry("release", "JAVA_VERSION=\"21.0.3\"\n"));
        // .artifacts/run.bat staged but the root manifest lists jre21.zip ONLY -> nothing to verify it
        // against, so staging must abort instead of letting migration.exe install unverifiable bytes.
        write(new File(root, ".artifacts/run.bat"), "unverifiable-run-bat");
        byte[] rootManifest = manifestBytes("1.4.0", "jre21.zip", HashUtil.sha256Hex(jre21Zip));
        write(new File(root, "MANIFEST.MF"), new String(rootManifest, StandardCharsets.UTF_8));

        byte[] libManifest = manifestBytes("1.4.0", LIB_ENTRY, HashUtil.sha256Hex(APP_JAR));
        Map<String, byte[]> routes = new HashMap<>();
        routes.put("/v/lib/MANIFEST.MF", libManifest);
        routes.put("/v/lib/MANIFEST.MF.sig", sign(libManifest, keyPair.getPrivate()));
        routes.put("/v/lib.zip", zipBytes(LIB_ENTRY, APP_JAR));
        HttpServer server = serve(routes);
        try {
            JreMigrationStager.stage(root, ManifestVerifier.parse(rootManifest),
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"), url(server, "/v/lib.zip"),
                    url(server, "/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for a run.bat missing from the root manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("run.bat"));
            assertTrue(expected.getMessage().contains("no integrity entry"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void stage_launcherJarMissingFromArtifacts_failsBeforeStartingTheSwap() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root, false);   // _launcher.jar neither staged nor listed
        // migration.exe empties lib/ and then copies _launcher.jar in from .artifacts/. If it is not
        // there, that copy fails AFTER the JRE swap, where migration.exe's rollback trigger is off --
        // leaving an empty lib/, a Java 21 jre/ and nothing to boot. Staging must refuse first.
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException when _launcher.jar is absent from .artifacts/");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("_launcher.jar"));
            assertTrue(expected.getMessage().contains("rollback is no longer possible"));
            // and nothing destructive may have happened
            assertFalse("migration.exe must not have been staged at the app root",
                    new File(root, "migration-launched.marker").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_runBatMissingFromArtifacts_failsBeforeStartingTheSwap() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // Same point-of-no-return argument for run.bat: migration.exe restores it to the app root after
        // the swap, and hard-fails without it, so the client is left on Java 21 with the JRE 11 script.
        assertTrue(new File(root, ".artifacts/run.bat").delete());
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException when run.bat is absent from .artifacts/");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("run.bat"));
            assertTrue(expected.getMessage().contains("rollback is no longer possible"));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_artifactPresentButUnlistedInRootManifest_failsClosed() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root, false);
        // _launcher.jar is present in .artifacts/ but the root manifest has NO entry for it (baseSetup
        // lists jre21.zip/migration.exe/rollback.exe/run.bat, never _launcher.jar), so it cannot be integrity-checked ->
        // staging must abort rather than use an unverifiable artifact.
        write(new File(root, ".artifacts/_launcher.jar"), "unverifiable-binary");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for an artifact missing from the root manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("_launcher.jar"));
            assertTrue(expected.getMessage().contains("no integrity entry"));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_missingMigrationExe_failsClosed() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // migration.exe is listed in the root manifest but NOT present in .artifacts/ -> copyRequired
        // must abort rather than silently skip (the launcher would otherwise have no exe to run).
        Files.delete(new File(root, ".artifacts/migration.exe").toPath());
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    ts.url("/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for a missing migration.exe");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("migration.exe"));
            assertTrue(expected.getMessage().contains("required"));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_invalidLibSignature_aborts() throws Exception {
        File root = folder.getRoot();
        write(new File(root, "run.bat"), "current");
        File jre21Zip = new File(root, ".artifacts/jre21.zip");
        writeZipFile(jre21Zip, entry("release", "JAVA_VERSION=\"21.0.3\"\n"));
        byte[] rootManifest = manifestBytes("1.4.0", "jre21.zip", HashUtil.sha256Hex(jre21Zip));
        File rootManifestFile = new File(root, "MANIFEST.MF");
        write(rootManifestFile, new String(rootManifest, StandardCharsets.UTF_8));

        // lib manifest signed by a DIFFERENT key -> verification must fail
        KeyPair wrong = KeyPairGenerator.getInstance("RSA").genKeyPair();
        byte[] libManifest = manifestBytes("1.4.0", LIB_ENTRY, HashUtil.sha256Hex(APP_JAR));
        Map<String, byte[]> routes = new HashMap<>();
        routes.put("/v/lib/MANIFEST.MF", libManifest);
        routes.put("/v/lib/MANIFEST.MF.sig", sign(libManifest, wrong.getPrivate()));
        routes.put("/v/lib.zip", zipBytes(LIB_ENTRY, APP_JAR));
        HttpServer server = serve(routes);
        try {
            JreMigrationStager.stage(root, ManifestVerifier.parse(rootManifest),
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"), url(server, "/v/lib.zip"),
                    url(server, "/v/lib/"), keyPair.getPublic(), 50000, 30000);
            fail("expected SecurityException when lib signature is invalid");
        } catch (SecurityException expected) {
            // Case B: the invalid lib signature must surface as a security distinction, not a generic
            // IOException, so the operator sees a tamper alert rather than a retry-able failure.
            assertTrue(expected.getMessage().contains("signature is invalid"));
        } finally {
            server.stop(0);
        }
    }

    // ---- helpers ----

    private static final class TestServer {
        final HttpServer server;
        final Manifest rootManifest;
        TestServer(HttpServer server, Manifest rootManifest) {
            this.server = server;
            this.rootManifest = rootManifest;
        }
        String url(String path) { return JreMigrationStagerTest.url(server, path); }
        void stop() { server.stop(0); }
    }

    private static Map<String, String> entry(String name, String content) {
        Map<String, String> m = new HashMap<>();
        m.put(name, content);
        return m;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void write(File file, String content) throws IOException {
        Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeZipFile(File file, Map<String, String> entries) throws IOException {
        Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    private static byte[] manifestBytes(String version, String entryName, String hash) throws IOException {
        Map<String, String> single = new LinkedHashMap<>();
        single.put(entryName, hash);
        return manifestBytes(version, single);
    }

    private static byte[] manifestBytes(String version, Map<String, String> entries) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        for (Map.Entry<String, String> e : entries.entrySet()) {
            Attributes attrs = new Attributes();
            attrs.put(Attributes.Name.CONTENT_TYPE, e.getValue());
            manifest.getEntries().put(e.getKey(), attrs);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        manifest.write(bos);
        return bos.toByteArray();
    }

    private static byte[] sign(byte[] data, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(data);
        return signer.sign();
    }

    private static byte[] zipBytes(String name, byte[] content) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(name));
            zos.write(content);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private static HttpServer serve(Map<String, byte[]> routes) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = routes.get(exchange.getRequestURI().getPath());
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server, String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }
}
