package io.mosip.registration.launcher.common;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class HashUtilTest {

    // Known SHA-256("abc") in upper-case hex — matches kernel HMACUtils2.digestAsPlainText format.
    private static final String SHA256_ABC =
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";

    @Test
    public void sha256Hex_bytes_matchesKnownVectorUpperCase() {
        assertEquals(SHA256_ABC, HashUtil.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void sha256Hex_file_matchesByteVariant() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        file.deleteOnExit();
        byte[] content = "abc".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), content);

        assertEquals(HashUtil.sha256Hex(content), HashUtil.sha256Hex(file));
        assertEquals(SHA256_ABC, HashUtil.sha256Hex(file));
    }

    @Test
    public void sha256Hex_isAlways64UpperCaseHexChars() {
        String hash = HashUtil.sha256Hex(new byte[] {1, 2, 3, 4, 5});
        assertEquals(64, hash.length());
        assertEquals(hash.toUpperCase(), hash);
    }
}
