package io.mosip.registration.test.update;

import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.update.SoftwareUpdateUtil;
import org.junit.*;
import org.mockito.InjectMocks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.Assert.*;

public class SoftwareUpdateUtilTest extends SoftwareUpdateUtil {

    private static final File LIB_DIR = new File("lib");
    private static final File TEMP_DIR = new File(".TEMP");

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
    public void deleteUnknownJars_unknownFilePresent() throws Exception {
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
    public void validateJarChecksum_nullAttributes() {
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
    public void download_stream_invalidUrl() throws Exception {
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
}

