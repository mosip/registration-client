package io.mosip.registration.launcher.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Verifies the detached {@code MANIFEST.MF.sig} signatures introduced in 1.3.0 (design Decision #2 /
 * security Cases A&ndash;D). Signatures are {@code SHA256withRSA} over the raw manifest bytes,
 * produced at build time with the existing signing key; verification uses the public key from the
 * embedded {@code provider.pem} certificate.
 * <p>
 * Pure JDK + slf4j; safe to run under the Java 11 JRE in {@code _launcher.jar}.
 */
public final class SignatureVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureVerifier.class);
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String X509 = "X.509";

    private SignatureVerifier() {
        // utility class
    }

    /**
     * Verifies a detached signature over {@code data}.
     *
     * @return {@code true} only if {@code signature} is a valid {@code SHA256withRSA} signature of
     *         {@code data} under {@code publicKey}
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            LOGGER.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Loads the public key from an X.509 certificate (PEM or DER).
     *
     * @throws GeneralSecurityException if the stream is not a valid X.509 certificate
     */
    public static PublicKey loadPublicKeyFromCertificate(InputStream certificateStream) throws GeneralSecurityException {
        CertificateFactory cf = CertificateFactory.getInstance(X509);
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(certificateStream);
        return certificate.getPublicKey();
    }
}
