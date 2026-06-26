package io.mosip.registration.launcher.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 hashing helper that produces output byte-for-byte compatible with the per-file checksums
 * written into {@code MANIFEST.MF} by the build (kernel {@code HMACUtils2.digestAsPlainText}):
 * <b>SHA-256, upper-case hex, big-endian</b>.
 * <p>
 * Pure JDK so it can run under the Java 11 JRE in {@code _launcher.jar}; do not replace this with a
 * kernel dependency.
 */
public final class HashUtil {

    private static final String SHA_256 = "SHA-256";
    private static final char[] HEX_UPPER = "0123456789ABCDEF".toCharArray();
    private static final int BUFFER_SIZE = 8192;

    private HashUtil() {
        // utility class
    }

    /**
     * @return the SHA-256 of {@code bytes} as an upper-case hex string.
     */
    public static String sha256Hex(byte[] bytes) {
        return toUpperHex(digest().digest(bytes));
    }

    /**
     * Streams {@code file} through SHA-256 (memory-friendly for large artifacts such as
     * {@code jre21.zip} / {@code lib.zip}). Produces the same value as {@link #sha256Hex(byte[])}
     * over the file's full content.
     *
     * @return the SHA-256 of the file content as an upper-case hex string
     * @throws IOException if the file cannot be read
     */
    public static String sha256Hex(File file) throws IOException {
        MessageDigest md = digest();
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        return toUpperHex(md.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK spec; absence is unrecoverable.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String toUpperHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i << 1] = HEX_UPPER[(bytes[i] >> 4) & 0xF];
            out[(i << 1) + 1] = HEX_UPPER[bytes[i] & 0xF];
        }
        return new String(out);
    }
}
