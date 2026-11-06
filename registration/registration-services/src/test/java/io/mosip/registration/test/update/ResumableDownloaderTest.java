/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.test.update;

import com.sun.net.httpserver.HttpServer;
import io.mosip.registration.update.ResumableDownloader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Direct unit tests for {@link ResumableDownloader}. These exercise the class through its own
 * {@code download(...)} entry point (not via {@code SoftwareUpdateUtil}) so the suite travels with the
 * class when it is later moved into the launcher's shared module. The server is the JDK's built-in
 * {@link HttpServer}, matching the style already used in {@code SoftwareUpdateUtilTest}.
 */
public class ResumableDownloaderTest {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;
    private static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    private static final String ETAG = "\"v1-test-etag\"";
    private static final String LAST_MODIFIED = "Wed, 21 Oct 2026 07:28:00 GMT";

    // ---------------------------------------------------------------------
    // Core happy paths (kept so the class is self-contained / portable)
    // ---------------------------------------------------------------------

    @Test
    public void download_fullResponse_succeedsAndCleansSidecars() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File dir = tempDir();
        try {
            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(new File(dir, "artifact.bin.part").exists());
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            stop(server, dir);
        }
    }

    @Test
    public void download_partialPresent_resumesByAppending() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File dir = tempDir();
        try {
            Files.write(new File(dir, "artifact.bin.part").toPath(), Arrays.copyOf(payload, 800));
            writeMeta(dir, ETAG);

            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G1: 206 whose Content-Range start != expected offset -> discard + restart
    // ---------------------------------------------------------------------

    @Test
    public void download_206WrongStartOffset_discardsAndRestarts() throws Exception {
        byte[] payload = randomPayload(2000);
        // Serves 206 but always claims it started at byte 0, regardless of the requested offset.
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.getResponseHeaders().add("ETag", ETAG);
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null) {
                exchange.getResponseHeaders().add("Content-Range", "bytes 0-9/" + payload.length);
                exchange.sendResponseHeaders(206, 10);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload, 0, 10);
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
        File dir = tempDir();
        try {
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[800]);
            writeMeta(dir, ETAG);

            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            // bad 206 discarded, second attempt restarts clean from a 200 -> correct payload
            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G2: 416 where the local part size != server total -> discard + restart fresh
    // ---------------------------------------------------------------------

    @Test
    public void download_416PartLargerThanTotal_restartsFresh() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File dir = tempDir();
        try {
            // an over-sized (corrupt) part: Range start (2500) >= server size (2000) -> 416
            Files.write(new File(dir, "artifact.bin.part").toPath(), new byte[2500]);
            writeMeta(dir, ETAG);

            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G2b: 416 where the local part size == server total -> finalize the local part (no re-download)
    // ---------------------------------------------------------------------

    @Test
    public void download_416PartMatchesTotal_finalizesLocalPart() throws Exception {
        byte[] payload = randomPayload(2000);
        // A complete .part exactly the server's size, but with distinguishable content so the
        // assertion proves the local part is finalized as-is rather than re-downloaded. (finalize is
        // completeness-based - length == server total; content integrity is verified downstream per
        // the class contract.) The resume Range then starts at EOF (2000) -> the server answers 416,
        // and because part length == server total handleRangeNotSatisfiable finalizes the part.
        byte[] completePart = payload.clone();
        completePart[0] ^= 0xFF;
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File dir = tempDir();
        try {
            Files.write(new File(dir, "artifact.bin.part").toPath(), completePart);
            writeMeta(dir, ETAG);

            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(completePart, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
            assertFalse(new File(dir, "artifact.bin.part").exists());       // part consumed by finalize
            assertFalse(new File(dir, "artifact.bin.part.meta").exists());  // validator sidecar removed
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G3 / G4: validator selection - weak ETag ignored, Last-Modified used
    // ---------------------------------------------------------------------

    @Test
    public void download_weakEtag_usesLastModifiedValidator() throws Exception {
        byte[] payload = randomPayload(4000);
        // A weak ETag (W/...) is invalid for byte-range validation -> Last-Modified must be chosen.
        HttpServer server = truncatingServer(payload, "W/\"weak\"", LAST_MODIFIED);
        File dir = tempDir();
        try {
            expectIOException(server, dir);
            // .part retained, and the persisted validator is the Last-Modified value (not the weak ETag)
            assertEquals(LAST_MODIFIED, readMeta(dir));
        } finally {
            stop(server, dir);
        }
    }

    @Test
    public void download_noEtag_usesLastModifiedValidator() throws Exception {
        byte[] payload = randomPayload(4000);
        HttpServer server = truncatingServer(payload, null, LAST_MODIFIED);
        File dir = tempDir();
        try {
            expectIOException(server, dir);
            assertEquals(LAST_MODIFIED, readMeta(dir));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G5: no validator headers at all -> no .meta written (cannot resume next time)
    // ---------------------------------------------------------------------

    @Test
    public void download_noValidatorHeaders_writesNoMeta() throws Exception {
        byte[] payload = randomPayload(4000);
        HttpServer server = truncatingServer(payload, null, null);
        File dir = tempDir();
        try {
            expectIOException(server, dir);
            assertTrue("partial retained", new File(dir, "artifact.bin.part").exists());
            assertFalse("no validator -> no meta", new File(dir, "artifact.bin.part.meta").exists());
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G6: unknown Content-Length (chunked) -> completeness check skipped, download still succeeds
    // ---------------------------------------------------------------------

    @Test
    public void download_chunkedUnknownLength_completes() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(200, 0);   // 0 => chunked, length unknown to the client
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
            exchange.close();
        });
        server.start();
        File dir = tempDir();
        try {
            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G7: insufficient disk space (public ensureSpace guard)
    // ---------------------------------------------------------------------

    @Test(expected = IOException.class)
    public void ensureSpace_insufficient_throws() throws Exception {
        File dir = tempDir();
        try {
            ResumableDownloader.ensureSpace(dir, Long.MAX_VALUE);
        } finally {
            deleteDir(dir);
        }
    }

    @Test
    public void ensureSpace_sufficient_doesNotThrow() throws Exception {
        File dir = tempDir();
        try {
            assertTrue("precondition: temp dir has free space", dir.getUsableSpace() > 1L);
            // a real directory with far more than 1 byte free must pass without throwing
            ResumableDownloader.ensureSpace(dir, 1L);
        } finally {
            deleteDir(dir);
        }
    }

    @Test
    public void ensureSpace_nullOrMissingDir_fallsBackToCwd() throws Exception {
        // null and a non-existent directory both fall back to new File(".") (the working dir),
        // so a modest requirement is satisfiable and must not throw.
        assertTrue("precondition: working dir has free space", new File(".").getUsableSpace() > 1L);
        ResumableDownloader.ensureSpace(null, 1L);
        ResumableDownloader.ensureSpace(new File("no-such-dir-" + System.nanoTime()), 1L);
    }

    // ---------------------------------------------------------------------
    // G8: target directory is created when missing
    // ---------------------------------------------------------------------

    @Test
    public void download_missingTargetDir_createsDirectory() throws Exception {
        byte[] payload = randomPayload(1000);
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File base = tempDir();
        File nested = new File(base, "a/b/c");
        try {
            assertFalse(nested.exists());

            ResumableDownloader.download(urlFor(server), nested.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(nested, "artifact.bin").toPath()));
        } finally {
            stop(server, base);
        }
    }

    // ---------------------------------------------------------------------
    // G9: finalize overwrites an already-present target file
    // ---------------------------------------------------------------------

    @Test
    public void download_existingTarget_overwrites() throws Exception {
        byte[] payload = randomPayload(2000);
        HttpServer server = rangeServer(payload, ETAG, null, true);
        File dir = tempDir();
        try {
            Files.write(new File(dir, "artifact.bin").toPath(), "OLD-CONTENT".getBytes(StandardCharsets.UTF_8));

            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);

            assertArrayEquals(payload, Files.readAllBytes(new File(dir, "artifact.bin").toPath()));
        } finally {
            stop(server, dir);
        }
    }

    // ---------------------------------------------------------------------
    // G10 + name guards + transport errors (direct on the class)
    // ---------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void download_blankFileName_throwsIllegalArgument() throws Exception {
        ResumableDownloader.download("http://localhost/file", tempDir().getAbsolutePath(), "",
                CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void download_pathTraversalName_throwsIllegalArgument() throws Exception {
        ResumableDownloader.download("http://localhost/file", tempDir().getAbsolutePath(), "..\\evil.bin",
                CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Test(expected = IOException.class)
    public void download_unexpectedStatus_throws() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        File dir = tempDir();
        try {
            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);
        } finally {
            stop(server, dir);
        }
    }

    // ---- helpers ----

    private static byte[] randomPayload(int size) {
        byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    private static File tempDir() throws IOException {
        return Files.createTempDirectory("rdl").toFile();
    }

    private static String urlFor(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort() + "/file";
    }

    private static void writeMeta(File dir, String validator) throws IOException {
        Files.write(new File(dir, "artifact.bin.part.meta").toPath(), validator.getBytes(StandardCharsets.UTF_8));
    }

    private static String readMeta(File dir) throws IOException {
        return new String(Files.readAllBytes(new File(dir, "artifact.bin.part.meta").toPath()),
                StandardCharsets.UTF_8).trim();
    }

    /** Runs a download that is expected to fail (truncated), asserting an IOException is raised. */
    private static void expectIOException(HttpServer server, File dir) throws Exception {
        try {
            ResumableDownloader.download(urlFor(server), dir.getAbsolutePath(), "artifact.bin",
                    CONNECT_TIMEOUT, READ_TIMEOUT);
            org.junit.Assert.fail("expected IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    /** Serves the payload with the given validators, honouring Range (with If-Range) when {@code honorRange}. */
    private static HttpServer rangeServer(byte[] payload, String etag, String lastModified, boolean honorRange)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            if (etag != null) {
                exchange.getResponseHeaders().add("ETag", etag);
            }
            if (lastModified != null) {
                exchange.getResponseHeaders().add("Last-Modified", lastModified);
            }
            String range = exchange.getRequestHeaders().getFirst("Range");
            String ifRange = exchange.getRequestHeaders().getFirst("If-Range");
            boolean serveRange = honorRange && range != null && range.startsWith("bytes=")
                    && (ifRange == null || etag == null || etag.equals(ifRange));
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

    /** Declares the full Content-Length but sends only half the body then closes (a truncated transfer). */
    private static HttpServer truncatingServer(byte[] payload, String etag, String lastModified)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/file", exchange -> {
            if (etag != null) {
                exchange.getResponseHeaders().add("ETag", etag);
            }
            if (lastModified != null) {
                exchange.getResponseHeaders().add("Last-Modified", lastModified);
            }
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload, 0, payload.length / 2);
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    private static void stop(HttpServer server, File dir) throws Exception {
        server.stop(0);
        deleteDir(dir);
    }

    private static void deleteDir(File dir) throws Exception {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDir(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}