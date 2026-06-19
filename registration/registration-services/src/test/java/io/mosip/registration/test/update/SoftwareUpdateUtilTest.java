package io.mosip.registration.test.update;

import com.sun.net.httpserver.HttpServer;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.update.SoftwareUpdateUtil;
import org.junit.*;
import org.mockito.InjectMocks;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.Assert.*;

public class SoftwareUpdateUtilTest extends SoftwareUpdateUtil {

    private static final File LIB_DIR = new File("lib");
    private static final File TEMP_DIR = new File(".TEMP");

    private static final int HTTP_RANGE_NOT_SATISFIABLE_TEST = 416;
    private static final String SERVER_ETAG = "\"v1-test-etag\"";

    @InjectMocks
    private SoftwareUpdateUtil softwareUpdateUtil;

    @BeforeClass
    public static void setup() {
        LIB_DIR.mkdirs();
        TEMP_DIR.mkdirs();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        deleteDir(LIB_DIR);
        deleteDir(TEMP_DIR);
        new File(".UNKNOWN_JARS").delete();
    }

    private static void deleteDir(File dir) throws Exception {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void deleteUnknownJars_unknownFilePresent_returnsTrueAndCreatesMarker() throws Exception {
        File unknownJar = new File(LIB_DIR, "unknown.jar");
        Files.write(unknownJar.toPath(), "test".getBytes());

        Manifest manifest = new Manifest();

        boolean result = softwareUpdateUtil.deleteUnknownJars(manifest);

        assertTrue(result);
        assertFalse(unknownJar.exists());
        assertTrue(new File(".UNKNOWN_JARS").exists());
    }

    @Test
    public void validateJarChecksum_success() throws Exception {
        File jar = File.createTempFile("test", ".jar");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        Files.write(jar.toPath(), content);

        Attributes attributes = new Attributes();
        attributes.put(Attributes.Name.CONTENT_TYPE,
                io.mosip.kernel.core.util.HMACUtils2.digestAsPlainText(content));

        assertTrue(softwareUpdateUtil.validateJarChecksum(jar, attributes));
    }

    @Test
    public void validateJarChecksum_failure() throws Exception {
        File jar = File.createTempFile("test", ".jar");
        Files.write(jar.toPath(), "data".getBytes());

        Attributes attributes = new Attributes();
        attributes.put(Attributes.Name.CONTENT_TYPE, "wrong");

        assertFalse(softwareUpdateUtil.validateJarChecksum(jar, attributes));
    }

    @Test
    public void validateJarChecksum_nullAttributes_returnsFalse() {
        assertFalse(softwareUpdateUtil.validateJarChecksum(new File("dummy"), null));
    }

    @Test
    public void download_fileUrl_success() throws Exception {
        File source = File.createTempFile("source", ".txt");
        Files.write(source.toPath(), "data".getBytes());

        softwareUpdateUtil.download(
                source.toURI().toURL().toString(),
                "downloaded.txt"
        );

        assertTrue(new File(".TEMP/downloaded.txt").exists());
    }

    @Test(expected = RegBaseCheckedException.class)
    public void download_invalidUrl_throwsException() throws Exception {
        softwareUpdateUtil.download("http://invalid.invalid/file", "x.jar");
    }

    @Test(expected = RegBaseCheckedException.class)
    public void download_stream_invalidUrl_throwsException() throws Exception {
        softwareUpdateUtil.download("http://invalid.invalid/file");
    }

    @Test
    public void deleteFile_success() throws Exception {
        File file = File.createTempFile("delete", ".txt");
        assertTrue(softwareUpdateUtil.deleteFile(file.getAbsolutePath()));
    }

    @Test
    public void deleteFile_failure() {
        assertFalse(softwareUpdateUtil.deleteFile("non-existing-file.txt"));
    }

    @Test
    public void deleteFileOnExit_noException() {
        softwareUpdateUtil.deleteFileOnExit("dummy.txt");
    }

    @Test
    public void clearTempDirectory_success() throws Exception {
        File temp = new File(".TEMP/test.txt");
        Files.write(temp.toPath(), "data".getBytes());

        softwareUpdateUtil.clearTempDirectory();

        assertFalse(temp.exists());
    }

    // ---------------------------------------------------------------------
    // downloadResumable(...) + ensureSpace(...) tests
    // ---------------------------------------------------------------------

    @Test
    public void downloadResumable_fullDownload_success() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            File out = new File(dir, "artifact.bin");
            assertTrue(out.exists());
            assertArrayEquals(payload, Files.readAllBytes(out.toPath()));
            assertFalse(new File(dir, "artifact.bin.part").exists());
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_resumesFromPartial() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // pre-stage the first 800 bytes + the matching validator, as an interrupted attempt would
            Files.write(new File(dir, "artifact.bin.part").toPath(), Arrays.copyOf(payload, 800));
            writeMeta(dir, SERVER_ETAG);

            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_staleValidator_restartsFull_noSplice() throws Exception {
        // Finding #1: a .part left from a PREVIOUS resource version must not be spliced onto the new one.
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // partial of a different (old) version + an out-of-date validator
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[800]);
            writeMeta(dir, "\"old-stale-etag\"");

            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            // If-Range mismatch -> server sends 200 full -> file is the new payload, NOT old+new spliced
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_noValidator_restartsFull() throws Exception {
        // A partial with no stored validator cannot be safely resumed -> discarded and re-downloaded.
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[1500]);
            // no .part.meta written

            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_serverIgnoresRange_restartsFromZero() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, false); // always 200, ignores Range
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // stale partial; server ignores the Range and sends a full 200 -> must overwrite, not append
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[1500]);
            writeMeta(dir, SERVER_ETAG);

            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_rangeNotSatisfiable_finalizesExistingPart() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // part is already complete -> Range start == length -> server returns 416
            File part = new File(dir, "artifact.bin.part");
            Files.write(part.toPath(), payload);
            writeMeta(dir, SERVER_ETAG);

            downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(part.exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void downloadResumable_truncatedResponse_throwsAndRetainsPart() throws Exception {
        // Finding #2: a body shorter than the declared Content-Length must not be finalized as complete.
        byte[] payload = randomPayload(4000);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, payload.length);   // declares full length...
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload, 0, payload.length / 2);        // ...but only sends half
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            try {
                downloadResumable(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
                fail("expected RegBaseCheckedException for a truncated download");
            } catch (RegBaseCheckedException expected) {
                // expected
            }
            assertFalse("truncated file must not be finalized", new File(dir, "artifact.bin").exists());
            assertTrue("partial must be retained for resume", new File(dir, "artifact.bin.part").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test(expected = RegBaseCheckedException.class)
    public void downloadResumable_invalidHost_throws() throws Exception {
        downloadResumable("http://invalid.invalid/file", TEMP_DIR.getAbsolutePath(), "x.bin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void downloadResumable_pathTraversalName_rejected() throws Exception {
        // A malicious manifest entry must not be able to write outside the target directory.
        downloadResumable("http://localhost/file", TEMP_DIR.getAbsolutePath(), "..\\..\\evil.bin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void downloadResumable_separatorInName_rejected() throws Exception {
        downloadResumable("http://localhost/file", TEMP_DIR.getAbsolutePath(), "sub/evil.bin");
    }

    @Test(expected = RegBaseCheckedException.class)
    public void downloadResumable_unexpectedStatus_throws() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            downloadResumable(urlFor(server), dir.getAbsolutePath(), "x.bin");
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void ensureSpace_sufficient_noException() throws Exception {
        ensureSpace(TEMP_DIR, 1L);
    }

    @Test(expected = RegBaseCheckedException.class)
    public void ensureSpace_insufficient_throws() throws Exception {
        ensureSpace(TEMP_DIR, Long.MAX_VALUE);
    }

    @Test
    public void ensureSpace_nullDir_usesCurrentDir_noException() throws Exception {
        ensureSpace(null, 1L);
    }

    // ---- helpers ----

    private static byte[] randomPayload(int size) {
        byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    private static void writeMeta(File dir, String validator) throws IOException {
        Files.write(new File(dir, "artifact.bin.part.meta").toPath(), validator.getBytes(StandardCharsets.UTF_8));
    }

    /** Serves the payload with an ETag validator, honouring Range only when If-Range matches that ETag. */
    private static HttpServer startServer(byte[] payload, boolean honorRange) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", SERVER_ETAG);
            String range = exchange.getRequestHeaders().getFirst("Range");
            String ifRange = exchange.getRequestHeaders().getFirst("If-Range");
            boolean serveRange = honorRange && range != null && range.startsWith("bytes=")
                    && (ifRange == null || SERVER_ETAG.equals(ifRange));
            if (serveRange) {
                long start = Long.parseLong(range.substring("bytes=".length()).split("-")[0]);
                if (start >= payload.length) {
                    exchange.getResponseHeaders().add("Content-Range", "bytes */" + payload.length);
                    exchange.sendResponseHeaders(HTTP_RANGE_NOT_SATISFIABLE_TEST, -1);
                    exchange.close();
                    return;
                }
                int len = (int) (payload.length - start);
                exchange.getResponseHeaders().add("Content-Range",
                        "bytes " + start + "-" + (payload.length - 1) + "/" + payload.length);
                exchange.sendResponseHeaders(206, len);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload, (int) start, len);
                }
            } else {
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload);
                }
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String urlFor(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort() + "/file";
    }

}
