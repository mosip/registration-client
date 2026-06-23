package io.mosip.registration.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Build-time signer that produces the detached {@code MANIFEST.MF.sig} files introduced in 1.3.0
 * (issue #812). Signatures are {@code SHA256withRSA} over the raw manifest bytes, created with the
 * existing code-signing key from {@code keystore.p12} &mdash; the same keypair whose certificate is
 * shipped as {@code provider.pem} and used by the launcher's {@code SignatureVerifier} to verify.
 * <p>
 * Reads the private key straight out of the PKCS#12 keystore so the build never has to extract it to
 * disk. Invoked from {@code configure.sh} via {@code java -cp ... io.mosip.registration.update.ManifestSigner}.
 */
public final class ManifestSigner {

    private static final Logger logger = LoggerFactory.getLogger(ManifestSigner.class);
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEYSTORE_TYPE = "PKCS12";
    /**
     * Environment variable carrying the keystore password. Read from the environment rather than a
     * command-line argument so the secret is never exposed in process listings (ps, proc cmdline).
     * Already present in the build container as a Dockerfile {@code ENV} and inherited by this process.
     */
    private static final String STORE_PASS_ENV_VAR = "keystore_secret_env";

    private ManifestSigner() {
        // utility class
    }

    /**
     * CLI entry point. The keystore password is read from the {@code keystore_secret_env} environment
     * variable (NOT a command-line argument), so it is never exposed in process listings.
     *
     * @param args {@code <keystorePath> <alias> <dataFile> <sigFile>}
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            logger.error("Usage: ManifestSigner <keystorePath> <alias> <dataFile> <sigFile> "
                    + "(keystore password is read from the {} environment variable)", STORE_PASS_ENV_VAR);
            System.exit(2);
        }
        String storePass = System.getenv(STORE_PASS_ENV_VAR);
        if (storePass == null || storePass.isEmpty()) {
            logger.error("Keystore password not provided: set the {} environment variable", STORE_PASS_ENV_VAR);
            System.exit(2);
        }
        try {
            PrivateKey privateKey = loadPrivateKey(new File(args[0]), storePass.toCharArray(), args[1]);
            signFile(new File(args[2]), new File(args[3]), privateKey);
            logger.info("Signed {} -> {}", args[2], args[3]);
        } catch (Exception e) {
            logger.error("Failed to sign {}", args[2], e);
            System.exit(1);
        }
    }

    /**
     * Loads the private key for {@code alias} from a PKCS#12 keystore, using the store password as the
     * key password (the convention for the registration-client {@code keystore.p12}).
     */
    public static PrivateKey loadPrivateKey(File keystoreFile, char[] storePass, String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream in = new FileInputStream(keystoreFile)) {
            keyStore.load(in, storePass);
        }
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, storePass);
        if (privateKey == null) {
            throw new IllegalArgumentException("No private key found for alias: " + alias);
        }
        return privateKey;
    }

    /** Writes a detached signature of {@code dataFile} to {@code sigFile}. */
    public static void signFile(File dataFile, File sigFile, PrivateKey privateKey) throws Exception {
        byte[] signature = sign(Files.readAllBytes(dataFile.toPath()), privateKey);
        Files.write(sigFile.toPath(), signature);
    }

    /** Produces a detached {@code SHA256withRSA} signature over {@code data}. */
    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }
}
