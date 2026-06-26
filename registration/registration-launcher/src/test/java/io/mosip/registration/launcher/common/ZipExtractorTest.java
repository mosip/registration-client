package io.mosip.registration.launcher.common;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ZipExtractorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File writeZip(String name, String[] entryNames, byte[][] contents) throws Exception {
        File zip = folder.newFile(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (int i = 0; i < entryNames.length; i++) {
                zos.putNextEntry(new ZipEntry(entryNames[i]));
                if (contents[i] != null) {
                    zos.write(contents[i]);
                }
                zos.closeEntry();
            }
        }
        return zip;
    }

    @Test
    public void extract_writesAllEntries() throws Exception {
        byte[] jar = "jar-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] nested = "nested".getBytes(StandardCharsets.UTF_8);
        File zip = writeZip("lib.zip",
                new String[] {"foo.jar", "sub/bar.txt"},
                new byte[][] {jar, nested});

        File out = folder.newFolder("out");
        ZipExtractor.extract(zip, out);

        assertTrue(new File(out, "foo.jar").exists());
        assertArrayEquals(jar, Files.readAllBytes(new File(out, "foo.jar").toPath()));
        assertArrayEquals(nested, Files.readAllBytes(new File(out, "sub/bar.txt").toPath()));
    }

    @Test
    public void extract_createsTargetDirIfMissing() throws Exception {
        File zip = writeZip("a.zip", new String[] {"x.txt"}, new byte[][] {"x".getBytes()});
        File out = new File(folder.getRoot(), "does-not-exist-yet");
        assertFalse(out.exists());

        ZipExtractor.extract(zip, out);

        assertTrue(new File(out, "x.txt").exists());
    }

    @Test
    public void extract_zipSlipEntry_isBlocked() throws Exception {
        File zip = writeZip("evil.zip",
                new String[] {"../escaped.txt"},
                new byte[][] {"pwned".getBytes()});
        File out = folder.newFolder("safe");

        try {
            ZipExtractor.extract(zip, out);
            fail("Expected zip-slip entry to be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("outside target directory"));
        }
        assertFalse(new File(folder.getRoot(), "escaped.txt").exists());
    }
}
