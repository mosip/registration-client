package io.mosip.registration.test.update;

import io.mosip.registration.update.ManifestSigner;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.UUID;

/**
 * Verifies that {@link ManifestSigner} produces detached signatures that the launcher's
 * {@code SHA256withRSA}/{@code provider.pem} verification accepts.
 */
public class ManifestSignerTest {

    private static final String ALIAS = "CodeSigning";
    // Generated per test run so no credential literal is committed. The keystore it protects is a
    // throwaway PKCS12 created in a temp dir (@BeforeClass) and deleted in @AfterClass.
    private static final String STORE_PASS = UUID.randomUUID().toString();

    private static File tempDir;
    private static File keystore;

    @BeforeClass
    public static void setUp() throws Exception {
        tempDir = Files.createTempDirectory("manifest-signer-test").toFile();
        keystore = new File(tempDir, "keystore.p12");
        generateKeystore(keystore);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (tempDir != null && tempDir.exists()) {
            for (File f : tempDir.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
            Files.deleteIfExists(tempDir.toPath());
        }
    }

    @Test
    public void sign_roundTrip_verifies() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        byte[] data = "MANIFEST contents".getBytes(StandardCharsets.UTF_8);

        byte[] signature = ManifestSigner.sign(data, keyPair.getPrivate());

        Assert.assertTrue(verify(data, signature, keyPair.getPublic()));
    }

    @Test
    public void sign_tamperedData_failsVerification() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        byte[] signature = ManifestSigner.sign("original".getBytes(StandardCharsets.UTF_8), keyPair.getPrivate());

        Assert.assertFalse(verify("tampered".getBytes(StandardCharsets.UTF_8), signature, keyPair.getPublic()));
    }

    @Test
    public void signFile_writesDetachedSignatureThatVerifies() throws Exception {
        File data = new File(tempDir, "MANIFEST.MF");
        File sig = new File(tempDir, "MANIFEST.MF.sig");
        Files.write(data.toPath(), "lib manifest body".getBytes(StandardCharsets.UTF_8));

        PrivateKey privateKey = ManifestSigner.loadPrivateKey(keystore, STORE_PASS.toCharArray(), ALIAS);
        ManifestSigner.signFile(data, sig, privateKey);

        Assert.assertTrue(sig.exists());
        Assert.assertTrue(Files.size(sig.toPath()) > 0);
        Assert.assertTrue(verify(Files.readAllBytes(data.toPath()), Files.readAllBytes(sig.toPath()), publicKeyFromKeystore()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadPrivateKey_unknownAlias_throws() throws Exception {
        ManifestSigner.loadPrivateKey(keystore, STORE_PASS.toCharArray(), "no-such-alias");
    }

    private static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }

    private static PublicKey publicKeyFromKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (java.io.FileInputStream in = new java.io.FileInputStream(keystore)) {
            ks.load(in, STORE_PASS.toCharArray());
        }
        return ks.getCertificate(ALIAS).getPublicKey();
    }

    private static void generateKeystore(File target) throws Exception {
        String keytool = new File(System.getProperty("java.home"), "bin/keytool").getPath();
        Process process = new ProcessBuilder(
                keytool,
                "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-dname", "CN=Test, OU=Registration, O=MOSIP",
                "-validity", "365",
                "-storetype", "PKCS12",
                "-keystore", target.getPath(),
                "-storepass", STORE_PASS,
                "-keypass", STORE_PASS)
                .redirectErrorStream(true)
                .start();
        int exit = process.waitFor();
        Assert.assertEquals("keytool failed to generate test keystore", 0, exit);
    }
}