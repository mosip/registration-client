package registrationtest.runapplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import javax.security.auth.x500.X500Principal;

import registrationtest.api.PartnerManagerClient;
import registrationtest.utility.DeviceCertificateUtil;

/**
 * Creates a server-trusted device certificate for Mock MDS and installs it, following the DSL device partner
 * flow: CA and intermediate certificates are uploaded to the DEVICE domain, the partner certificate is
 * uploaded and re-signed by PMS, and a device specific key (DSK) signed by the partner key is what Mock MDS
 * signs its responses with. The Registration Client validates that signature against the DEVICE trust chain
 * it syncs from the server, so the default keys shipped with Mock MDS are rejected.
 */
public class MockMdsSetup {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MockMdsSetup.class);

    public static final String DSK_FILE_NAME = "device-dsk-partner.p12";

    private static final String DOMAIN = "DEVICE";
    private static final Duration CA_VALIDITY = Duration.ofDays(3 * 365);
    private static final Duration DSK_VALIDITY = Duration.ofDays(180);
    private static final Duration MIN_REMAINING_VALIDITY = Duration.ofDays(7);

    private final Path certsDir;
    private final char[] password;

    public MockMdsSetup(Path certsDir, String password) {
        this.certsDir = certsDir;
        this.password = password.toCharArray();
    }

    /**
     * Reuses the device key from a previous run while it is still valid; otherwise registers the device
     * provider and creates a new trusted chain.
     *
     * @return the DSK keystore Mock MDS has to use
     */
    public Path prepareDeviceKeystore(String partnerId, Supplier<PartnerManagerClient> pmsLogin)
            throws GeneralSecurityException, IOException {
        Path dskFile = certsDir.resolve(DSK_FILE_NAME);
        PrivateKeyEntry existing = DeviceCertificateUtil.loadP12(dskFile, password);
        if (existing != null && isValidFor((X509Certificate) existing.getCertificate(), MIN_REMAINING_VALIDITY)) {
            logger.info("Reusing device key {} valid until {}", dskFile,
                    ((X509Certificate) existing.getCertificate()).getNotAfter());
            return dskFile;
        }

        PartnerManagerClient pms = pmsLogin.get();
        pms.registerDeviceProvider(partnerId, "regclient." + partnerId.toLowerCase() + "@mosip.test");

        PrivateKeyEntry ca = loadOrCreateCa("device-ca.p12", "CA-DEVICE-" + partnerId, null, partnerId);
        PrivateKeyEntry inter = loadOrCreateCa("device-inter.p12", "INTER-DEVICE-" + partnerId, ca, partnerId);
        pms.uploadCaCertificate(DeviceCertificateUtil.toPem(ca.getCertificate()), DOMAIN);
        pms.uploadCaCertificate(DeviceCertificateUtil.toPem(inter.getCertificate()), DOMAIN);

        X509Certificate interCert = (X509Certificate) inter.getCertificate();
        KeyPair partnerKeys = DeviceCertificateUtil.newKeyPair();
        X509Certificate partnerCert = DeviceCertificateUtil.issue(inter.getPrivateKey(),
                interCert.getSubjectX500Principal(), DeviceCertificateUtil.name("PARTNER-DEVICE-" + partnerId, partnerId),
                partnerKeys.getPublic(), CA_VALIDITY.minusDays(1));
        List<X509Certificate> signedChain = pms.uploadPartnerCertificate(
                DeviceCertificateUtil.toPem(partnerCert), DOMAIN, partnerId);
        X509Certificate resignedPartnerCert = signedChain.stream()
                .filter(cert -> Arrays.equals(cert.getPublicKey().getEncoded(), partnerKeys.getPublic().getEncoded()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PMS did not return the re-signed partner certificate"));
        DeviceCertificateUtil.saveP12(certsDir.resolve("device-partner.p12"), partnerKeys.getPrivate(),
                chainStartingWith(resignedPartnerCert, signedChain), password);

        // The DSK issuer must be the subject PMS put in the re-signed certificate, which can differ from the
        // subject that was uploaded, otherwise the client cannot build the trust path.
        KeyPair dskKeys = DeviceCertificateUtil.newKeyPair();
        Duration dskValidity = Duration.between(Instant.now(), resignedPartnerCert.getNotAfter().toInstant());
        X509Certificate dskCert = DeviceCertificateUtil.issue(partnerKeys.getPrivate(),
                resignedPartnerCert.getSubjectX500Principal(), DeviceCertificateUtil.name("DSK-DEVICE-" + partnerId, partnerId),
                dskKeys.getPublic(), dskValidity.compareTo(DSK_VALIDITY) < 0 ? dskValidity : DSK_VALIDITY);
        List<X509Certificate> dskChain = new ArrayList<>();
        dskChain.add(dskCert);
        dskChain.addAll(Arrays.asList(chainStartingWith(resignedPartnerCert, signedChain)));
        DeviceCertificateUtil.saveP12(dskFile, dskKeys.getPrivate(), dskChain.toArray(new X509Certificate[0]), password);
        logger.info("Created device key {} valid until {}", dskFile, dskCert.getNotAfter());
        return dskFile;
    }

    /**
     * Copies the device keystore into the Mock MDS folder and points every Registration (non-FTM) keystore entry
     * of its application.properties at it.
     */
    public void installIntoMockMds(Path dskFile, Path mockMdsDir) throws IOException {
        Path properties = mockMdsDir.resolve("application.properties");
        if (!Files.isRegularFile(properties)) {
            throw new IllegalStateException("Mock MDS application.properties not found in " + mockMdsDir);
        }
        Files.copy(dskFile, mockMdsDir.resolve(DSK_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);

        String content = new String(Files.readAllBytes(properties), StandardCharsets.UTF_8);
        String prefix = "(?m)^(mosip\\.mock\\.sbi\\.file\\.[\\w.]+\\.keys\\.";
        String updated = content
                .replaceAll(prefix + "keystorefilename=).*$", "$1" + Matcher.quoteReplacement("/" + DSK_FILE_NAME))
                .replaceAll(prefix + "keyalias=).*$", "$1" + DeviceCertificateUtil.KEY_ALIAS)
                .replaceAll(prefix + "keystorepwd=).*$", "$1" + Matcher.quoteReplacement(new String(password)));
        if (!updated.equals(content)) {
            Files.write(properties, updated.getBytes(StandardCharsets.UTF_8));
        }
        logger.info("Mock MDS in {} configured to sign with {}", mockMdsDir, DSK_FILE_NAME);
    }

    private PrivateKeyEntry loadOrCreateCa(String fileName, String commonName, PrivateKeyEntry issuer,
            String organization) throws GeneralSecurityException, IOException {
        Path file = certsDir.resolve(fileName);
        PrivateKeyEntry entry = DeviceCertificateUtil.loadP12(file, password);
        if (entry != null && isValidFor((X509Certificate) entry.getCertificate(), Duration.ofDays(365))) {
            return entry;
        }
        KeyPair keys = DeviceCertificateUtil.newKeyPair();
        X500Principal subject = DeviceCertificateUtil.name(commonName, organization);
        X509Certificate cert = issuer == null
                ? DeviceCertificateUtil.issue(keys.getPrivate(), subject, subject, keys.getPublic(), CA_VALIDITY)
                : DeviceCertificateUtil.issue(issuer.getPrivateKey(),
                        ((X509Certificate) issuer.getCertificate()).getSubjectX500Principal(), subject,
                        keys.getPublic(), CA_VALIDITY);
        DeviceCertificateUtil.saveP12(file, keys.getPrivate(), new X509Certificate[] { cert }, password);
        return new PrivateKeyEntry(keys.getPrivate(), new X509Certificate[] { cert });
    }

    private static X509Certificate[] chainStartingWith(X509Certificate leaf, List<X509Certificate> chain) {
        List<X509Certificate> ordered = new ArrayList<>();
        ordered.add(leaf);
        for (X509Certificate cert : chain) {
            if (cert != leaf) {
                ordered.add(cert);
            }
        }
        return ordered.toArray(new X509Certificate[0]);
    }

    private static boolean isValidFor(X509Certificate cert, Duration remaining) {
        return cert.getNotAfter().toInstant().isAfter(Instant.now().plus(remaining));
    }
}
