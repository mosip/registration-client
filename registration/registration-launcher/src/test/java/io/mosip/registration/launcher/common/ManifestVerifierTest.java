package io.mosip.registration.launcher.common;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManifestVerifierTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File libDir;
    private byte[] jarContent;

    @Before
    public void setUp() throws Exception {
        libDir = folder.newFolder("lib");
        jarContent = "some-jar-bytes".getBytes();
        Files.write(new File(libDir, "foo.jar").toPath(), jarContent);
    }

    private File writeManifest(String version, String entryName, String entryHash) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        if (entryName != null) {
            Attributes attrs = new Attributes();
            attrs.put(Attributes.Name.CONTENT_TYPE, entryHash);
            manifest.getEntries().put(entryName, attrs);
        }
        File file = folder.newFile("MANIFEST-" + version + "-" + System.nanoTime() + ".MF");
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            manifest.write(out);
        }
        return file;
    }

    @Test
    public void getVersion_returnsManifestVersion() throws Exception {
        File manifest = writeManifest("1.3.0", null, null);
        assertEquals("1.3.0", ManifestVerifier.getVersion(manifest));
    }

    @Test
    public void findMismatchedFiles_allValid_returnsEmpty() throws Exception {
        File manifest = writeManifest("1.3.0", "foo.jar", HashUtil.sha256Hex(jarContent));
        assertTrue(ManifestVerifier.findMismatchedFiles(ManifestVerifier.parse(Files.readAllBytes(manifest.toPath())), libDir).isEmpty());
    }

    @Test
    public void findMismatchedFiles_tamperedFile_reportsEntry() throws Exception {
        File manifest = writeManifest("1.3.0", "foo.jar", HashUtil.sha256Hex("different".getBytes()));
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(ManifestVerifier.parse(Files.readAllBytes(manifest.toPath())), libDir);
        assertEquals(1, mismatched.size());
        assertTrue(mismatched.contains("foo.jar"));
    }

    @Test
    public void findMismatchedFiles_missingFile_reportsEntry() throws Exception {
        File manifest = writeManifest("1.3.0", "missing.jar", HashUtil.sha256Hex(jarContent));
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(ManifestVerifier.parse(Files.readAllBytes(manifest.toPath())), libDir);
        assertTrue(mismatched.contains("missing.jar"));
    }

    @Test
    public void findMismatchedFiles_relativeTraversalEntry_rejectedEvenIfHashMatches() throws Exception {
        // A real file OUTSIDE libDir whose hash the "../" entry would match: the containment guard must
        // still reject it, proving the check is on the resolved path — not merely on existence/hash.
        byte[] outside = "outside-bytes".getBytes();
        Files.write(new File(libDir.getParentFile(), "outside.jar").toPath(), outside);
        Manifest manifest = singleEntryManifest("../outside.jar", HashUtil.sha256Hex(outside));
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(manifest, libDir);
        assertTrue(mismatched.contains("../outside.jar"));
    }

    @Test
    public void findMismatchedFiles_absoluteEntry_rejectedEvenIfHashMatches() throws Exception {
        // An absolute manifest entry pointing outside the update root must be rejected, never hashed.
        byte[] outside = "abs-bytes".getBytes();
        File abs = folder.newFile("abs-target.jar");
        Files.write(abs.toPath(), outside);
        Manifest manifest = singleEntryManifest(abs.getAbsolutePath(), HashUtil.sha256Hex(outside));
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(manifest, libDir);
        assertTrue(mismatched.contains(abs.getAbsolutePath()));
    }

    private static Manifest singleEntryManifest(String entryName, String entryHash) {
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.3.0");
        Attributes attrs = new Attributes();
        attrs.put(Attributes.Name.CONTENT_TYPE, entryHash);
        m.getEntries().put(entryName, attrs);
        return m;
    }

    @Test
    public void findUnexpectedFiles_onlyListedAndIgnored_returnsEmpty() throws Exception {
        Manifest manifest = manifest("1.3.0", "foo.jar");
        // foo.jar is listed; MANIFEST.MF is an ignored control file.
        Files.write(new File(libDir, "MANIFEST.MF").toPath(), "x".getBytes());
        List<String> unexpected = ManifestVerifier.findUnexpectedFiles(
                manifest, libDir, Collections.singleton("MANIFEST.MF"));
        assertTrue(unexpected.isEmpty());
    }

    @Test
    public void findUnexpectedFiles_topLevelExtraFile_reported() throws Exception {
        Manifest manifest = manifest("1.3.0", "foo.jar");
        Files.write(new File(libDir, "extra.jar").toPath(), "x".getBytes());
        List<String> unexpected = ManifestVerifier.findUnexpectedFiles(
                manifest, libDir, Collections.emptySet());
        assertEquals(1, unexpected.size());
        assertTrue(unexpected.contains("extra.jar"));
    }

    @Test
    public void findUnexpectedFiles_fileSmuggledInSubdirectory_reported() throws Exception {
        // A tampered archive plants an extra file under a nested directory; the flat manifest lists
        // none of it, so the recursive allowlist must report it (relative path with '/').
        Manifest manifest = manifest("1.3.0", "foo.jar");
        File sub = new File(libDir, "evil");
        Files.createDirectories(sub.toPath());
        Files.write(new File(sub, "payload.jar").toPath(), "x".getBytes());
        List<String> unexpected = ManifestVerifier.findUnexpectedFiles(
                manifest, libDir, Collections.emptySet());
        assertTrue(unexpected.contains("evil/payload.jar"));
    }

    private static Manifest manifest(String version, String entryName) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        Attributes attrs = new Attributes();
        attrs.put(Attributes.Name.CONTENT_TYPE, "hash");
        manifest.getEntries().put(entryName, attrs);
        return manifest;
    }
}
