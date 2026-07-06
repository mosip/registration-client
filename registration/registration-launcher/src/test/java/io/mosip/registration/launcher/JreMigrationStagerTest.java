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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    /** Sets up a root with run.bat, a signed lib (served), and a root manifest covering jre21.zip. */
    private TestServer baseSetup(File root) throws Exception {
        write(new File(root, "run.bat"), "current-run-bat");

        // jre21.zip on disk in .artifacts, with its hash recorded in the (verified) root manifest
        File jre21Zip = new File(root, ".artifacts/jre21.zip");
        writeZipFile(jre21Zip, entry("release", "JAVA_VERSION=\"21.0.3\"\n"));
        byte[] rootManifest = manifestBytes("1.4.0", "jre21.zip", HashUtil.sha256Hex(jre21Zip));
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
                    keyPair.getPublic(), 50000, 30000);

            assertTrue(new File(root, ".TEMP/app.jar").exists());
            assertTrue(new File(root, "jre21_temp/release").exists());
            assertTrue(new File(root, "run.bat_jre11").exists());
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_tamperedJre21Zip_failsRootManifestCheck() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // tamper jre21.zip AFTER its hash was recorded in the root manifest
        write(new File(root, ".artifacts/jre21.zip"), "tampered-bytes");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for tampered jre21.zip");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("jre21.zip"));
        } finally {
            ts.stop();
        }
    }

    @Test
    public void stage_artifactPresentButUnlistedInRootManifest_failsClosed() throws Exception {
        File root = folder.getRoot();
        TestServer ts = baseSetup(root);
        // migration.exe is present in .artifacts/ but the root manifest has NO entry for it, so it
        // cannot be integrity-checked -> staging must abort rather than use an unverifiable artifact.
        write(new File(root, ".artifacts/migration.exe"), "unverifiable-binary");
        try {
            JreMigrationStager.stage(root, ts.rootManifest,
                    ts.url("/v/lib/MANIFEST.MF"), ts.url("/v/lib/MANIFEST.MF.sig"), ts.url("/v/lib.zip"),
                    keyPair.getPublic(), 50000, 30000);
            fail("expected IOException for an artifact missing from the root manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("migration.exe"));
            assertTrue(expected.getMessage().contains("no integrity entry"));
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
                    keyPair.getPublic(), 50000, 30000);
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
