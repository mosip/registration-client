/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
    public void evaluate_signatureMissing_returnsSignatureMissing() throws Exception {
        // rootSignature file not created
        assertEquals(StartupAction.SIGNATURE_MISSING,
                StartupEvaluator.evaluate(rootManifest, rootSignature, null, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_invalidSignature_returnsAbortInvalidSignature() throws Exception {
        sign(rootManifest, rootSignature, wrongKeyPair.getPrivate()); // signed by untrusted key
        assertEquals(StartupAction.ABORT_INVALID_SIGNATURE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, null, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_versionsMatchAndLibSignatureValid_returnsNormalStartup() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File libSignature = new File(folder.getRoot(), "lib-MANIFEST.MF.sig");
        sign(libManifest, libSignature, keyPair.getPrivate());
        assertEquals(StartupAction.NORMAL_STARTUP,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, libSignature, trusted(), 21).action());
    }

    @Test
    public void evaluate_versionsMatchButLibSignatureAbsent_returnsLibSignatureMissing() throws Exception {
        // A 1.3.0+ bundle always ships lib/MANIFEST.MF.sig (configure.sh signs it into lib/ before
        // lib.zip is built), so an absent one on this branch means it was removed — repairable from the
        // server (Case C), which the entry point does before re-evaluating.
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File absentLibSig = new File(folder.getRoot(), "no-such-lib-MANIFEST.MF.sig");
        assertEquals(StartupAction.LIB_SIGNATURE_MISSING,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, absentLibSig, trusted(), 21).action());
    }

    @Test
    public void evaluate_versionsMatchButLibSignatureUntrusted_returnsAbortInvalidLibSignature() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File libSignature = new File(folder.getRoot(), "lib-MANIFEST.MF.sig");
        sign(libManifest, libSignature, wrongKeyPair.getPrivate()); // signed by an untrusted key
        assertEquals(StartupAction.ABORT_INVALID_LIB_SIGNATURE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, libSignature, trusted(), 21).action());
    }

    @Test
    public void evaluate_libManifestRewrittenAfterSigning_returnsAbortInvalidLibSignature() throws Exception {
        // The attack this gate exists for: something able to write lib/ rewrites a jar AND its hash entry
        // in lib/MANIFEST.MF, which the per-file check alone would then report as valid. The signature
        // covers the manifest bytes, so the edit is caught on the next launch.
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File libSignature = new File(folder.getRoot(), "lib-MANIFEST.MF.sig");
        sign(libManifest, libSignature, keyPair.getPrivate());
        Files.write(libManifest.toPath(),
                "Manifest-Version: 1.3.0\r\n\r\nName: evil.jar\r\nContent-Type: deadbeef\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
        assertEquals(StartupAction.ABORT_INVALID_LIB_SIGNATURE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, libSignature, trusted(), 21).action());
    }

    @Test
    public void evaluate_versionsDifferWithNoLibSignature_stillMigrates() throws Exception {
        // The pre-1.3.0 population: lib/MANIFEST.MF is the old UNSIGNED one and no .sig exists at all.
        // The lib signature gate must stay on the versions-match branch only — gating the comparison
        // itself would abort exactly the machines this migration is for.
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("legacy-lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.MIGRATE_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_versionsDifferJre11_returnsMigrateJre() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.MIGRATE_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_versionsDifferJre21_returnsUpdateLib() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.3.0");
        File newerRoot = writeManifest("root2-MANIFEST.MF", "1.4.0");
        File newerSig = new File(folder.getRoot(), "root2-MANIFEST.MF.sig");
        sign(newerRoot, newerSig, keyPair.getPrivate());
        assertEquals(StartupAction.UPDATE_LIB,
                StartupEvaluator.evaluate(newerRoot, newerSig, libManifest, null, trusted(), 21).action());
    }

    @Test
    public void evaluate_versionsDifferJre8_returnsAbortUnsupportedJre() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.ABORT_UNSUPPORTED_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, null, trusted(), 8).action());
    }

    @Test
    public void evaluate_versionsDifferJre25_returnsUpdateLib() throws Exception {
        // any JRE >= 21 takes the lib-only update path
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        libManifest = writeManifest("lib-MANIFEST.MF", "1.2.0.1");
        assertEquals(StartupAction.UPDATE_LIB,
                StartupEvaluator.evaluate(rootManifest, rootSignature, libManifest, null, trusted(), 25).action());
    }

    @Test
    public void evaluate_libManifestMissing_returnsMigrateJre() throws Exception {
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        File missingLib = new File(folder.getRoot(), "no-such-lib-MANIFEST.MF");
        assertEquals(StartupAction.MIGRATE_JRE,
                StartupEvaluator.evaluate(rootManifest, rootSignature, missingLib, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_libManifestNoVersion_returnsAbortCorruptLibManifest() throws Exception {
        // A present-but-version-less lib/MANIFEST.MF (corrupt/truncated) must NOT trigger a migration —
        // it aborts so a possibly-current machine is not needlessly re-migrated.
        sign(rootManifest, rootSignature, keyPair.getPrivate());
        File corruptLib = folder.newFile("corrupt-lib-MANIFEST.MF"); // empty/truncated — parses but has no version
        assertEquals(StartupAction.ABORT_CORRUPT_LIB_MANIFEST,
                StartupEvaluator.evaluate(rootManifest, rootSignature, corruptLib, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_rootManifestNoVersion_returnsAbortCorruptRootManifest() throws Exception {
        // A signature-valid root MANIFEST.MF with no Manifest-Version (corrupt/mispackaged) must hard-stop
        // rather than fall through to a migration/update decision.
        File noVersionRoot = folder.newFile("noversion-MANIFEST.MF");
        Files.write(noVersionRoot.toPath(), "Created-By: test\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        File sig = new File(folder.getRoot(), "noversion-MANIFEST.MF.sig");
        sign(noVersionRoot, sig, keyPair.getPrivate());
        assertEquals(StartupAction.ABORT_CORRUPT_ROOT_MANIFEST,
                StartupEvaluator.evaluate(noVersionRoot, sig, null, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_rootManifestAbsent_returnsAbortMissingRootManifest() throws Exception {
        // Both ./MANIFEST.MF and its .sig are gone (a broken install). This must NOT be reported as
        // SIGNATURE_MISSING: Case C would try to download a signature for a manifest that isn't there
        // and fail with a message pointing the operator at the signature instead of the manifest.
        File absentRoot = new File(folder.getRoot(), "absent-MANIFEST.MF");
        File absentSig = new File(folder.getRoot(), "absent-MANIFEST.MF.sig");
        assertEquals(StartupAction.ABORT_MISSING_ROOT_MANIFEST,
                StartupEvaluator.evaluate(absentRoot, absentSig, null, null, trusted(), 11).action());
    }

    @Test
    public void evaluate_rootManifestAbsentButSignaturePresent_returnsAbortMissingRootManifest() throws Exception {
        // A leftover .sig with no manifest is still a missing manifest, not a signature problem: the
        // manifest check runs first, so the signature gate never reads the nonexistent file.
        File absentRoot = new File(folder.getRoot(), "absent2-MANIFEST.MF");
        File strandedSig = folder.newFile("absent2-MANIFEST.MF.sig");
        Files.write(strandedSig.toPath(), "not-a-real-signature".getBytes(StandardCharsets.UTF_8));
        assertEquals(StartupAction.ABORT_MISSING_ROOT_MANIFEST,
                StartupEvaluator.evaluate(absentRoot, strandedSig, null, null, trusted(), 11).action());
    }
}
