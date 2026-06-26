package io.mosip.registration.launcher.common;

import com.sun.net.httpserver.HttpServer;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Tests the resumable/byte-range download behaviour. Ported from the original coverage in
 * registration-services' SoftwareUpdateUtilTest when ResumableDownloader moved into
 * registration-launcher-common (T2 #810); failures here surface as plain {@link IOException}
 * (the {@code RegBaseCheckedException} wrapping lived in the services layer, not in this class).
 */
public class ResumableDownloaderTest {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;

    private static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    private static final String SERVER_ETAG = "\"v1-test-etag\"";

    private static File tempDir;

    @BeforeClass
    public static void setup() throws Exception {
        tempDir = Files.createTempDirectory("rd-shared").toFile();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        deleteDir(tempDir);
    }

    /** Thin wrapper so the test bodies read like the original downloadResumable(...) call sites. */
    private static void download(String url, String dir, String name) throws IOException {
        ResumableDownloader.download(url, dir, name, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Test
    public void download_fullDownload_success() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

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
    public void download_resumesFromPartial() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // pre-stage the first 800 bytes + the matching validator, as an interrupted attempt would
            Files.write(new File(dir, "artifact.bin.part").toPath(), Arrays.copyOf(payload, 800));
            writeMeta(dir, SERVER_ETAG);

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_staleValidator_restartsFull_noSplice() throws Exception {
        // A .part left from a PREVIOUS resource version must not be spliced onto the new one.
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // partial of a different (old) version + an out-of-date validator
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[800]);
            writeMeta(dir, "\"old-stale-etag\"");

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            // If-Range mismatch -> server sends 200 full -> file is the new payload, NOT old+new spliced
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_noValidator_restartsFull() throws Exception {
        // A partial with no stored validator cannot be safely resumed -> discarded and re-downloaded.
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[1500]);
            // no .part.meta written

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_serverIgnoresRange_restartsFromZero() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, false); // always 200, ignores Range
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // stale partial; server ignores the Range and sends a full 200 -> must overwrite, not append
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[1500]);
            writeMeta(dir, SERVER_ETAG);

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_rangeNotSatisfiable_finalizesExistingPart() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = startServer(payload, true);
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            // part is already complete -> Range start == length -> server returns 416
            File part = new File(dir, "artifact.bin.part");
            Files.write(part.toPath(), payload);
            writeMeta(dir, SERVER_ETAG);

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(part.exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_rangeNotSatisfiable_validatorChanged_restartsFresh() throws Exception {
        // Non-conformant server: returns 416 even though the resource changed (If-Range ignored) and the
        // new resource has the SAME length as the stale local part. A length-only finalize would pass the
        // stale bytes off as complete; the validator mismatch must force a fresh re-download instead.
        byte[] fresh = randomPayload(2000);
        byte[] stale = fresh.clone();
        stale[0] ^= 0xFF;                       // same length, different content
        String freshEtag = "\"etag-v2\"";       // differs from SERVER_ETAG stored in the part's .meta
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", freshEtag);
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null && range.startsWith("bytes=")) {
                long start = Long.parseLong(range.substring("bytes=".length()).split("-")[0]);
                if (start >= fresh.length) {     // non-conformant: 416 regardless of If-Range
                    exchange.getResponseHeaders().add("Content-Range", "bytes */" + fresh.length);
                    exchange.sendResponseHeaders(HTTP_RANGE_NOT_SATISFIABLE, -1);
                    exchange.close();
                    return;
                }
            }
            exchange.sendResponseHeaders(200, fresh.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fresh);
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            File part = new File(dir, "artifact.bin.part");
            Files.write(part.toPath(), stale);  // full-length but STALE content
            writeMeta(dir, SERVER_ETAG);         // old validator

            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");

            assertArrayEquals(fresh, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(part.exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_truncatedResponse_throwsAndRetainsPart() throws Exception {
        // A body shorter than the declared Content-Length must not be finalized as complete.
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
                download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
                fail("expected IOException for a truncated download");
            } catch (IOException expected) {
                // expected
            }
            assertFalse("truncated file must not be finalized", new File(dir, "artifact.bin").exists());
            assertTrue("partial must be retained for resume", new File(dir, "artifact.bin.part").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test(expected = IOException.class)
    public void download_invalidHost_throws() throws Exception {
        download("http://invalid.invalid/file", tempDir.getAbsolutePath(), "x.bin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_pathTraversalName_rejected() throws Exception {
        // A malicious manifest entry must not be able to write outside the target directory.
        download("http://localhost/file", tempDir.getAbsolutePath(), "..\\..\\evil.bin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_separatorInName_rejected() throws Exception {
        download("http://localhost/file", tempDir.getAbsolutePath(), "sub/evil.bin");
    }

    @Test(expected = IOException.class)
    public void download_unexpectedStatus_throws() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "x.bin");
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_targetDirIsAnExistingFile_throws() throws Exception {
        // targetDir points at a regular file: mkdirs() cannot create it and it is not a directory,
        // so the download must fail fast rather than write somewhere unexpected.
        File asFile = File.createTempFile("not-a-dir", ".tmp");
        try {
            download("http://localhost/file", new File(asFile, "sub").getAbsolutePath(), "x.bin");
            fail("expected IOException when the target directory cannot be created");
        } catch (IOException expected) {
            // expected
        } finally {
            asFile.delete();
        }
    }

    @Test
    public void download_chunkedNoContentLength_success() throws Exception {
        // sendResponseHeaders(200, 0) makes HttpServer use chunked encoding -> getContentLengthLong()
        // returns -1, exercising the "length unknown" branches in ensureSpaceForWrite / verifyComplete.
        byte[] payload = randomPayload(3000);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", SERVER_ETAG);
            exchange.sendResponseHeaders(200, 0);          // 0 -> chunked, no Content-Length
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_lastModifiedValidatorOnly_success() throws Exception {
        // No ETag, only Last-Modified -> extractValidator() falls back to the Last-Modified header.
        byte[] payload = randomPayload(1500);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("Last-Modified", "Wed, 21 Oct 2020 07:28:00 GMT");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_weakEtagIgnored_fallsBackToLastModified() throws Exception {
        // A weak ETag (W/...) is not valid for byte-range validation -> ignored; Last-Modified is used.
        byte[] payload = randomPayload(1500);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", "W/\"weak-etag\"");
            exchange.getResponseHeaders().add("Last-Modified", "Wed, 21 Oct 2020 07:28:00 GMT");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void download_noValidatorOnResponse_success() throws Exception {
        // Server provides neither ETag nor Last-Modified -> extractValidator() returns null and
        // writeValidator() removes any stale sidecar; the download still completes.
        byte[] payload = randomPayload(1500);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
            exchange.close();
        });
        server.start();
        File dir = Files.createTempDirectory("dl").toFile();
        try {
            download(urlFor(server), dir.getAbsolutePath(), "artifact.bin");
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            server.stop(0);
            deleteDir(dir);
        }
    }

    @Test
    public void ensureSpace_sufficient_noException() throws Exception {
        ResumableDownloader.ensureSpace(tempDir, 1L);
    }

    @Test(expected = IOException.class)
    public void ensureSpace_insufficient_throws() throws Exception {
        ResumableDownloader.ensureSpace(tempDir, Long.MAX_VALUE);
    }

    @Test
    public void ensureSpace_nullDir_usesCurrentDir_noException() throws Exception {
        ResumableDownloader.ensureSpace(null, 1L);
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
                    exchange.sendResponseHeaders(HTTP_RANGE_NOT_SATISFIABLE, -1);
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

    private static void deleteDir(File dir) {
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
}