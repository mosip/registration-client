/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import com.sun.net.httpserver.HttpServer;
import io.mosip.registration.launcher.common.HashUtil;
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
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LibUpdaterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static KeyPair keyPair;
    private static KeyPair wrongKeyPair;

    private static final byte[] JAR = "fake-jar-content".getBytes(StandardCharsets.UTF_8);
    private static final String ENTRY = "app.jar";

    @BeforeClass
    public static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        wrongKeyPair = generator.generateKeyPair();
    }

    @Test
    public void update_validManifestAndContent_readyRestart() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        byte[] zip = zipBytes(ENTRY, JAR);

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertTrue(new File(temp, ENTRY).exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_reportsLibZipDownloadProgress_toListener() throws Exception {
        // The progress listener passed to update() must be forwarded to the lib.zip download (only —
        // the manifest/sig are tiny). Guards against the wiring silently dropping the listener.
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        byte[] zip = zipBytes(ENTRY, JAR);

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            List<long[]> events = new ArrayList<>();
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000,
                    (done, total) -> events.add(new long[]{done, total}));

            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertFalse("progress listener must be invoked for the lib.zip download", events.isEmpty());
            long[] last = events.get(events.size() - 1);
            assertEquals("total should be the lib.zip content length", zip.length, last[1]);
            assertEquals("final done should equal the lib.zip size", zip.length, last[0]);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_reportsEachStepToTheOperator_withDownloadSizeInBetween() throws Exception {
        // The lib-only update path shows the same window as the JRE migration; its status line must name
        // the step now running, and the download line must carry the size so a slow link visibly moves.
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        byte[] zip = zipBytes(ENTRY, JAR);

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            List<String> lines = new ArrayList<>();
            List<long[]> events = new ArrayList<>();
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000,
                    (done, total) -> events.add(new long[]{done, total}), lines::add);

            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertFalse("the caller's byte-progress listener must still be fed", events.isEmpty());
            assertEquals("Downloading the update…", lines.get(0));
            assertTrue("a size line must follow the download step",
                    lines.get(1).startsWith("Downloading the update (") && lines.get(1).endsWith(" MB)…"));
            assertEquals("Unpacking the update…", lines.get(lines.size() - 2));
            assertEquals("Verifying the update files…", lines.get(lines.size() - 1));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void megabytes_roundsDownToWholeMegabytes() {
        assertEquals(0L, LibUpdater.megabytes(0L));
        assertEquals(0L, LibUpdater.megabytes(1024L * 1024L - 1L));
        assertEquals(1L, LibUpdater.megabytes(1024L * 1024L));
        // the 1.3.0 lib.zip served on dev1: 382,219,410 bytes, which Windows Explorer shows as 364 MB
        assertEquals(364L, LibUpdater.megabytes(382_219_410L));
    }

    @Test
    public void update_invalidSignature_abortsBeforeZip() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, wrongKeyPair.getPrivate()); // signed by untrusted key
        byte[] zip = zipBytes(ENTRY, JAR);

        List<String> requested = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = serve(routes(manifest, sig, zip), requested);
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.ABORT_INVALID_SIGNATURE, result);
            assertFalse("lib.zip must not be requested after sig failure",
                    requested.contains("/v/lib.zip"));
            assertFalse("no lib.zip may be staged after sig failure",
                    new File(temp.getParentFile(), temp.getName() + ".zipstage/lib.zip").exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_contentHashMismatch_verifyFailed() throws Exception {
        // manifest records a hash for a DIFFERENT content than what lib.zip ships
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex("other".getBytes()));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        byte[] zip = zipBytes(ENTRY, JAR);

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.VERIFY_FAILED, result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_malformedManifestBody_failsAsIoErrorNotSecurityAlert() throws Exception {
        // server returns an HTML error page instead of a real manifest -> network/server problem,
        // which must surface as an IOException (handled as "Update failed"), NOT a signature alert.
        byte[] htmlManifest = "<html><body>500 Internal Server Error</body></html>".getBytes(StandardCharsets.UTF_8);
        byte[] sig = sign(htmlManifest, keyPair.getPrivate());

        HttpServer server = serve(routes(htmlManifest, sig, zipBytes(ENTRY, JAR)));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for a malformed downloaded manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("network/server error"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_oversizeManifest_failsAsIoErrorNotSecurityAlert() throws Exception {
        // A hostile/misconfigured server returns an oversized manifest body; it must be rejected on
        // size BEFORE being buffered into memory and verified, surfacing as a network/server
        // IOException (not a signature alert, and never an unbounded allocation).
        byte[] huge = new byte[1024 * 1024 + 1];             // just over MAX_MANIFEST_BYTES (1 MiB)
        byte[] sig = sign(huge, keyPair.getPrivate());        // never reached (manifest cap trips first)

        HttpServer server = serve(routes(huge, sig, zipBytes(ENTRY, JAR)));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for an oversized downloaded manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("network/server error"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_stalePayloadInTemp_clearedBeforeExtract() throws Exception {
        // A reused .TEMP/ with a stale jar from a prior failed attempt must not trip the allowlist:
        // the stale file is cleared before extraction so a valid update still succeeds.
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        HttpServer server = serve(routes(manifest, sig, zipBytes(ENTRY, JAR)));
        File temp = folder.newFolder(".TEMP");
        Files.write(new File(temp, "stale-old.jar").toPath(), "stale".getBytes(StandardCharsets.UTF_8));
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);
            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertFalse("stale jar must be cleared before extraction", new File(temp, "stale-old.jar").exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_zipShipsControlFileEntry_verifyFailed() throws Exception {
        // A crafted lib.zip that smuggles an extra entry named "lib.zip" must NOT overwrite the archive
        // mid-extract (it is staged outside .TEMP/) and must be caught by the allowlist: once unpacked
        // into .TEMP/, "lib.zip" is no longer a control file there, so it is an unexpected file and the
        // update fails closed rather than being silently accepted.
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        byte[] zip = zipBytes(ENTRY, JAR, "lib.zip", "rogue".getBytes(StandardCharsets.UTF_8));

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);
            assertEquals(LibUpdateResult.VERIFY_FAILED, result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_success_publishesVerifiedManifestAndSignatureInTemp_andLeavesNoStaging() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        HttpServer server = serve(routes(manifest, sig, zipBytes(ENTRY, JAR)));
        File temp = new File(folder.getRoot(), ".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertArrayEquals(manifest, Files.readAllBytes(new File(temp, "MANIFEST.MF").toPath()));
            assertArrayEquals(sig, Files.readAllBytes(new File(temp, "MANIFEST.MF.sig").toPath()));
            assertFalse(new File(folder.getRoot(), ".TEMP.zipstage").exists());
            assertFalse(new File(folder.getRoot(), ".TEMP.extract").exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_libZipDownloadCutOff_leavesNoTemp_andKeepsThePartForResume() throws Exception {
        // dev1, Java 21 lib-only update: lib.zip was cut off mid-download after the new lib manifest and
        // signature had been staged in .TEMP/. run.bat then copied them into lib/, the versions matched,
        // and the launcher started normally on the old jars instead of resuming. Nothing may reach .TEMP/
        // before the whole update is in, so the next start still sees different versions and resumes.
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        Map<String, byte[]> routes = routes(manifest, sig, zipBytes(ENTRY, JAR));
        HttpServer server = serve(routes, Collections.synchronizedList(new ArrayList<>()), "/v/lib.zip");
        File temp = new File(folder.getRoot(), ".TEMP");
        try {
            LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);
            fail("expected an IOException for the cut-off lib.zip download");
        } catch (IOException expected) {
            assertFalse("a cut-off update must not leave .TEMP/ for run.bat to apply", temp.exists());
            assertTrue("the partial lib.zip must be kept so the retry resumes",
                    new File(folder.getRoot(), ".TEMP.zipstage/lib.zip.part").exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_verifyFailed_leavesNoTempAndNoUnpackedTree() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex("other".getBytes()));
        byte[] sig = sign(manifest, keyPair.getPrivate());
        HttpServer server = serve(routes(manifest, sig, zipBytes(ENTRY, JAR)));
        File temp = new File(folder.getRoot(), ".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.VERIFY_FAILED, result);
            assertFalse("a failed update must not leave .TEMP/ for run.bat to apply", temp.exists());
            assertFalse(new File(folder.getRoot(), ".TEMP.extract").exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_invalidSignature_leavesNoTemp() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, wrongKeyPair.getPrivate());
        HttpServer server = serve(routes(manifest, sig, zipBytes(ENTRY, JAR)));
        File temp = new File(folder.getRoot(), ".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 30000);

            assertEquals(LibUpdateResult.ABORT_INVALID_SIGNATURE, result);
            assertFalse("the rejected manifest and signature must not reach .TEMP/", temp.exists());
        } finally {
            server.stop(0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void update_nonPositiveReadTimeout_rejected() throws Exception {
        // Invalid timeout is rejected at this entry point (fail fast) before any download is attempted.
        File temp = folder.newFolder("temp-validate");
        LibUpdater.update("https://localhost/m", "https://localhost/s", "https://localhost/z",
                temp, keyPair.getPublic(), 50000, 0);
    }

    // ---- helpers ----

    private static Map<String, byte[]> routes(byte[] manifest, byte[] sig, byte[] zip) {
        Map<String, byte[]> routes = new HashMap<>();
        routes.put("/v/lib/MANIFEST.MF", manifest);
        routes.put("/v/lib/MANIFEST.MF.sig", sig);
        routes.put("/v/lib.zip", zip);
        return routes;
    }

    private static byte[] manifestBytes(String version, String entryName, String hash) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        Attributes attrs = new Attributes();
        attrs.put(Attributes.Name.CONTENT_TYPE, hash);
        manifest.getEntries().put(entryName, attrs);
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

    private static byte[] zipBytes(String entryName, byte[] content) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private static byte[] zipBytes(String name1, byte[] content1, String name2, byte[] content2) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(name1));
            zos.write(content1);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(name2));
            zos.write(content2);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private static HttpServer serve(Map<String, byte[]> routes) throws IOException {
        return serve(routes, Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * As {@link #serve(Map)}, additionally recording every requested path into {@code requested} so a
     * test can assert that something was never fetched. Asserting on the absence of a downloaded FILE is
     * not equivalent: LibUpdater stages lib.zip in a {@code .zipstage} sibling of the temp dir, so
     * checking for {@code temp/lib.zip} passes whether or not the download happened.
     */
    private static HttpServer serve(Map<String, byte[]> routes, List<String> requested) throws IOException {
        return serve(routes, requested, null);
    }

    /**
     * As {@link #serve(Map, List)}; the body of {@code cutOffPath} (if any) is announced at its full length
     * but only half of it is sent before the connection closes, like a network drop mid-download.
     */
    private static HttpServer serve(Map<String, byte[]> routes, List<String> requested, String cutOffPath)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requested.add(path);
            byte[] body = routes.get(path);
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(200, body.length);
            if (path.equals(cutOffPath)) {
                OutputStream os = exchange.getResponseBody();
                os.write(body, 0, body.length / 2);
                os.flush();
                exchange.close();
                return;
            }
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
