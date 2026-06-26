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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 0);

            assertEquals(LibUpdateResult.READY_RESTART, result);
            assertTrue(new File(temp, ENTRY).exists());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void update_invalidSignature_abortsBeforeZip() throws Exception {
        byte[] manifest = manifestBytes("1.4.0", ENTRY, HashUtil.sha256Hex(JAR));
        byte[] sig = sign(manifest, wrongKeyPair.getPrivate()); // signed by untrusted key
        byte[] zip = zipBytes(ENTRY, JAR);

        HttpServer server = serve(routes(manifest, sig, zip));
        File temp = folder.newFolder(".TEMP");
        try {
            LibUpdateResult result = LibUpdater.update(
                    url(server, "/v/lib/MANIFEST.MF"), url(server, "/v/lib/MANIFEST.MF.sig"),
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 0);

            assertEquals(LibUpdateResult.ABORT_INVALID_SIGNATURE, result);
            assertFalse("lib.zip must not be downloaded after sig failure",
                    new File(temp, "lib.zip").exists());
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
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 0);

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
                    url(server, "/v/lib.zip"), temp, keyPair.getPublic(), 50000, 0);
            fail("expected IOException for a malformed downloaded manifest");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("network/server error"));
        } finally {
            server.stop(0);
        }
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
