package io.mosip.registration.launcher;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;

public class StartupEvaluatorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static KeyPair keyPair;
    private static KeyPair wrongKeyPair;

    private File rootManifest;
    private File rootSignature;
    private File libManifest;

    @BeforeClass
    public static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        wrongKeyPair = generator.generateKeyPair();
    }

    @Before
    public void setUp() throws Exception {
        rootManifest = writeManifest("root-MANIFEST.MF", "1.3.0");
        rootSignature = new File(folder.getRoot(), "MANIFEST.MF.sig");
    }

    private File writeManifest(String name, String version) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        File file = folder.newFile(name);
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            manifest.write(out);
        }
        return file;
    }

    private void sign(File dataFile, File sigFile, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(Files.readAllBytes(dataFile.toPath()));
        Files.write(sigFile.toPath(), signer.sign());
    }

    private PublicKey trusted() {
        return keyPair.getPublic();
    }

    @Test
    public void signatureMissing() throws Exception {
        // rootSignature file not created
        assertEquals(StartupAction.SIGNATURE_MISSING,
                StartupEvaluator.evaluate(rootManifest, rootSignature, null, trusted(), 11).action());
    }

    @Test
    public void signatureInvalid_abort() throws Exception {
        sign(rootManifest, rootSignature, wrongKeyPair.getPrivate()); // signed by untrusted key
        assertEquals(StartupAction.ABORT_INVALID_SIGNATURE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, null, trusted(), 11).action());
    }

    @Test
    public void validSignature_versionsMatch_normalStartup() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        assertEquals(StartupAction.NORMAL_STARTUP,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, trusted(), 21).action());
    }

    @Test
    public void validSignature_versionsDiffer_jre11_migrateJre() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.MIGRATE_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, trusted(), 11).action());
    }

    @Test
    public void validSignature_versionsDiffer_jre21_updateLib() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File newerRoot = writeManifest("root2-MANIFEST.MF", "1.4.0");
        File newerSig = new File(folder.getRoot(), "root2-MANIFEST.MF.sig");
        sign(newerRoot, newerSig, keyPair.getPrivate());
        assertEquals(StartupAction.UPDATE_LIB,
                StartupEvaluator.evaluate(newerRoot, newerSig, libManifest, trusted(), 21).action());
    }

    @Test
    public void validSignature_versionsDiffer_unsupportedJre_aborts() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.ABORT_UNSUPPORTED_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, trusted(), 8).action());
    }

    @Test
    public void validSignature_versionsDiffer_jre25_updateLib() throws Exception {
        // any JRE >= 21 takes the lib-only update path
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.UPDATE_LIB,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, trusted(), 25).action());
    }

    @Test
    public void validSignature_libManifestMissing_treatedAsDiffer() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        File missingLib = new File(folder.getRoot(), "no-such-lib-MANIFEST.MF");
        assertEquals(StartupAction.MIGRATE_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, missingLib, trusted(), 11).action());
    }

    @Test
    public void validSignature_libManifestPresentButNoVersion_abortsCorrupt() throws Exception {
        // A present-but-version-less lib/MANIFEST.MF (corrupt/truncated) must NOT trigger a migration —
        // it aborts so a possibly-current machine is not needlessly re-migrated.
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        File corruptLib = folder.newFile("corrupt-lib-MANIFEST.MF"); // empty/truncated — parses but has no version
        assertEquals(StartupAction.ABORT_CORRUPT_LIB_MANIFEST,
                StartupEvaluator.evaluate(rootManifest, rootSignature, corruptLib, trusted(), 11).action());
    }

    @Test
    public void validSignature_rootManifestNoVersion_abortsCorrupt() throws Exception {
        // A signature-valid root MANIFEST.MF with no Manifest-Version (corrupt/mispackaged) must hard-stop
        // rather than fall through to a migration/update decision.
        File noVersionRoot = folder.newFile("noversion-MANIFEST.MF");
        Files.write(noVersionRoot.toPath(), "Created-By: test\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        File sig = new File(folder.getRoot(), "noversion-MANIFEST.MF.sig");
        sign(noVersionRoot, sig, keyPair.getPrivate());
        assertEquals(StartupAction.ABORT_CORRUPT_ROOT_MANIFEST,
                StartupEvaluator.evaluate(noVersionRoot, sig, null, trusted(), 11).action());
    }
}
