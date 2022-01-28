package io.mosip.registration.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ClientIntegrityValidator {

    private static final Logger logger = LoggerFactory.getLogger(ClientIntegrityValidator.class);
    private static final String PROPERTIES_FILE = "props/mosip-application.properties";
    private static final String libFolder = "lib";
    private static final String certPath = "provider.pem";
    private static final String manifestFile = "MANIFEST.MF";


    public static void verifyClientIntegrity() {
        try(InputStream keyStream = ClientSetupValidator.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {

            Properties properties = new Properties();
            properties.load(keyStream);
            logger.info("Loading {} completed", PROPERTIES_FILE);

            if("LOCAL".equals(properties.getProperty("environment"))) {
                logger.warn("NOTE :: IGNORING LOCAL REGISTRATION CLIENT INTEGRITY CHECK AS ITS LOCAL ENVIRONMENT");
                return;
            }

            X509Certificate trustedCertificate = getCertificate();
            Manifest localManifest = getLocalManifest();

            if (localManifest != null) {
            	Map<String, Attributes> localAttributes = localManifest.getEntries();
                for (Map.Entry<String, Attributes> entry : localAttributes.entrySet()) {
                    if(entry.getKey().toLowerCase().startsWith("registration-services") ||
                            entry.getKey().toLowerCase().startsWith("registration-client") ) {
                        File file = new File(libFolder + File.separator + entry.getKey());
                        if(trustedCertificate != null) {
                            verifyIntegrity(trustedCertificate, new JarFile(file));
                            logger.info("Integrity check passed -> {}", entry.getKey());
                        }
                        else {
                            logger.info("As provider.cer is not found, invoking verify with JarFile class : {}", entry.getKey());
                            try(JarFile jarFile = new JarFile(file, true)){
                                logger.info("Integrity check passed -> {}", entry.getKey());
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new SecurityException("Client integrity check failed",t);
        }
    }

    public static void verifyIntegrity(X509Certificate trustedCertificate, JarFile jarFile) throws IOException {
        logger.info("Integrity check started -> {}", jarFile.getName());
        verifyCertificate(trustedCertificate);

        if(jarFile == null)
            throw new SecurityException("Failed to read jar");

        try {

            Vector entriesVec = new Vector();

            // Ensure the jar file is signed.
            Manifest man = jarFile.getManifest();
            if (man == null) {
                throw new SecurityException("The provider is not signed");
            }

            // Ensure all the entries' signatures verify correctly
            byte[] buffer = new byte[8192];
            Enumeration entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry je = (JarEntry) entries.nextElement();

                // Skip directories.
                if (je.isDirectory()) continue;
                entriesVec.addElement(je);
                InputStream is = jarFile.getInputStream(je);

                // Read in each jar entry. A security exception will
                // be thrown if a signature/digest check fails.
                int n;
                while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                    // Don't care
                }
                is.close();
            }

            // Get the list of signer certificates
            Enumeration e = entriesVec.elements();

            while (e.hasMoreElements()) {
                JarEntry je = (JarEntry) e.nextElement();

                // Every file must be signed except files in META-INF.
                Certificate[] certs = je.getCertificates();
                if ((certs == null) || (certs.length == 0)) {
                    if (!je.getName().startsWith("META-INF"))
                        throw new SecurityException("The provider has unsigned class files.");
                } else {
                    // Check whether the file is signed by the expected
                    // signer. The jar may be signed by multiple signers.
                    // See if one of the signers is 'targetCert'.
                    int startIndex = 0;
                    X509Certificate[] certChain;
                    boolean signedAsExpected = false;

                    while ((certChain = getAChain(certs, startIndex)) != null) {
                        if (certChain[0].equals(trustedCertificate)) {
                            // Stop since one trusted signer is found.
                            signedAsExpected = true;
                            break;
                        }
                        // Proceed to the next chain.
                        startIndex += certChain.length;
                    }

                    if (!signedAsExpected) {
                        throw new SecurityException("The provider is not signed by a trusted signer");
                    }
                }
            }

        } catch (Throwable t) {
            logger.error("Failed to get jar manifest", t);
            throw new SecurityException(t);
        } finally {
            if(jarFile != null) { jarFile.close(); }
        }
    }

    /**
     * Extracts ONE certificate chain from the specified certificate array
     * which may contain multiple certificate chains, starting from index
     * 'startIndex'.
     */
    private static X509Certificate[] getAChain(Certificate[] certs,
                                               int startIndex) {
        if (startIndex > certs.length - 1)
            return null;

        int i;
        // Keep going until the next certificate is not the
        // issuer of this certificate.
        for (i = startIndex; i < certs.length - 1; i++) {
            if (!((X509Certificate)certs[i + 1]).getSubjectDN().
                    equals(((X509Certificate)certs[i]).getIssuerDN())) {
                break;
            }
        }
        // Construct and return the found certificate chain.
        int certChainSize = (i-startIndex) + 1;
        X509Certificate[] ret = new X509Certificate[certChainSize];
        for (int j = 0; j < certChainSize; j++ ) {
            ret[j] = (X509Certificate) certs[startIndex + j];
        }
        return ret;
    }

    private static void verifyCertificate(X509Certificate trustedCertificate) {
        if(trustedCertificate == null)
            throw new SecurityException("Trusted certificate is null");

        Date now = Calendar.getInstance().getTime();
        if(trustedCertificate.getNotAfter().before(now))
            throw new SecurityException("Trusted certificate is expired");
    }

    public static X509Certificate getCertificate() {
        try(InputStream inStream = ClientIntegrityValidator.class.getClassLoader().getResourceAsStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            return cert;
        } catch (IOException | CertificateException e) {
            logger.error("Failed to get certificate", e);
        }
        return null;
    }

    private static Manifest getLocalManifest() {
        try {
            File localManifestFile = new File(manifestFile);
            if (localManifestFile.exists()) {
                return new Manifest(new FileInputStream(localManifestFile));
            }
        } catch (IOException e) {
            logger.error("Failed to load local manifest file", e);
        }
        return null;
    }
}
