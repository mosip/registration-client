package io.mosip.registration.test.update;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.update.ClientIntegrityValidator;

/**
 * 
 * @author Rama Devi
 *
 */

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ Manifest.class, ApplicationContext.class, FileUtils.class, HMACUtils2.class })
public class ClientIntegrityValidatorTest {

	private static final String certPath = "provider.pem";

	@InjectMocks
	private ClientIntegrityValidator clientIntegrityValidator;

	// Backup of a pre-existing working-dir lib/MANIFEST.MF (e.g. left by another test in the
	// same fork), restored in teardown so these tests don't depend on external filesystem state.
	private File libManifestBackup;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
		PowerMockito.mockStatic(HMACUtils2.class);
		// Start every test from a known-absent lib/MANIFEST.MF; back up any pre-existing one.
		File libManifest = new File("lib", "MANIFEST.MF");
		if (libManifest.exists()) {
			libManifestBackup = new File("lib", "MANIFEST.MF.testbak");
			libManifestBackup.delete();
			libManifest.renameTo(libManifestBackup);
		} else {
			libManifestBackup = null;
		}
	}

	@After
	public void restoreLibManifest() {
		File libDir = new File("lib");
		new File(libDir, "MANIFEST.MF").delete();
		if (libManifestBackup != null && libManifestBackup.exists()) {
			libManifestBackup.renameTo(new File(libDir, "MANIFEST.MF"));
		}
		libDir.delete(); // removes lib/ only if empty (i.e. created by a test)
	}

	// In a non-LOCAL environment (test props use environment=TEST) with no lib/MANIFEST.MF on
	// disk, integrity verification must fail closed with a SecurityException rather than skip.
	@Test(expected = SecurityException.class)
	public void verifyClientIntegrityTest() throws RegBaseCheckedException {
		ClientIntegrityValidator.verifyClientIntegrity();
	}

	// Counterpart to verifyClientIntegrityTest: with lib/MANIFEST.MF present (and no
	// registration-* entries to verify), integrity verification proceeds without throwing.
	// Uses java.io (not java.nio) on purpose: under PowerMock + Java 21 the module system
	// blocks the reflective access nio Path operations need (sun.nio.fs not opened).
	// Setup/teardown (@Before/@After) own the lib/MANIFEST.MF backup + cleanup.
	@Test
	public void verifyClientIntegrityManifestPresentTest() throws Exception {
		File libDir = new File("lib");
		libDir.mkdirs();
		try (FileOutputStream fos = new FileOutputStream(new File(libDir, "MANIFEST.MF"))) {
			fos.write("Manifest-Version: 1.0\r\n\r\nName: logback.xml\r\nContent-Type: testhash\r\n\r\n"
					.getBytes(StandardCharsets.UTF_8));
		}
		// Non-registration entry -> per-jar verification loop is a no-op -> returns normally.
		ClientIntegrityValidator.verifyClientIntegrity();
	}

	@Test
	public void integrityCheckTest() throws IOException {
		URL url = ManifestCreatorTest.class.getResource("/setup/registration-api-1.3.0-SNAPSHOT.jar");
		X509Certificate certificate = ClientIntegrityValidator.getCertificate();
		JarFile jarFile = new JarFile(url.getFile());
		ClientIntegrityValidator.verifyIntegrity(certificate, jarFile);

		
		/*
		 * boolean manfound = true; Manifest man = jarFile.getManifest(); if (man !=
		 * null) { manfound = true; } else { manfound = false; } assertFalse(manfound);
		 */
	}

	@SuppressWarnings("null")
	@Test
	public void verifyCertificateTest() throws SecurityException{
		
		X509Certificate trustedCertificate = Mockito.mock(X509Certificate.class);
		if (trustedCertificate != null) {
			System.out.println("trustedCertificate is not null");
		} else {
			try {
 
				when(trustedCertificate.getClass()).thenThrow(new NullPointerException());
			} catch (SecurityException e) {
				throw new SecurityException("Failed to read jar");
			}
		}
		
		
	}
	@Test
	public void getAChainTest() {
		
	}

}
