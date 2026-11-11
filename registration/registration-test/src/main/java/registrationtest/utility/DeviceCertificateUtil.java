package registrationtest.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Key pair / certificate / PKCS12 helpers for the device (SBI) certificate chain, following the DSL
 * authentication-demo-service KeyMgrUtil: CA -> INTER -> PARTNER -> DSK (device specific key), all RSA 2048
 * with SHA256withRSA, CA basic constraints and digitalSignature|keyCertSign key usage.
 */
public final class DeviceCertificateUtil {

    public static final String KEY_ALIAS = "keyalias";

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final SecureRandom RANDOM = new SecureRandom();

    private DeviceCertificateUtil() {
    }

    /** Subject/issuer name in the same layout the DSL uses; O must equal the PMS partner organization name. */
    public static X500Principal name(String commonName, String organization) throws IOException {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.C, "IN");
        builder.addRDN(BCStyle.ST, "KA");
        builder.addRDN(BCStyle.O, organization);
        builder.addRDN(BCStyle.OU, "REGCLIENT-TEST-ORG-UNIT");
        builder.addRDN(BCStyle.CN, commonName);
        X500Name name = builder.build();
        return new X500Principal(name.getEncoded());
    }

    public static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE, RANDOM);
        return generator.generateKeyPair();
    }

    /**
     * Issues a CA-capable certificate. notBefore is back-dated by a day because PMS rejects certificates
     * that are not yet valid on the server clock.
     */
    public static X509Certificate issue(PrivateKey signingKey, X500Principal issuer, X500Principal subject,
            PublicKey publicKey, Duration validity) throws GeneralSecurityException, IOException {
        try {
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer,
                    new BigInteger(64, RANDOM).abs(), Date.from(now.minus(Duration.ofDays(1))),
                    Date.from(now.plus(validity)), subject, publicKey);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.subjectKeyIdentifier, false,
                    new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign));
            ContentSigner signer = new JcaContentSignerBuilder(SIGN_ALGORITHM).build(signingKey);
            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        } catch (org.bouncycastle.operator.OperatorCreationException e) {
            throw new GeneralSecurityException("Unable to create certificate signer", e);
        }
    }

    public static void saveP12(Path file, PrivateKey privateKey, X509Certificate[] chain, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);
        keyStore.setEntry(KEY_ALIAS, new PrivateKeyEntry(privateKey, chain), new PasswordProtection(password));
        Files.createDirectories(file.toAbsolutePath().getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            keyStore.store(out, password);
        }
    }

    /** @return the key entry, or null when the file does not exist */
    public static PrivateKeyEntry loadP12(Path file, char[] password) throws GeneralSecurityException, IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(file)) {
            keyStore.load(in, password);
        }
        return (PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, new PasswordProtection(password));
    }

    public static String toPem(Certificate certificate) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate);
        }
        return writer.toString();
    }
}
