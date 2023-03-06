package io.mosip.registration.test.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.itextpdf.text.pdf.hyphenation.TernaryTree.Iterator;

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

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
		PowerMockito.mockStatic(HMACUtils2.class);
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	@Test
	public void verifyClientIntegrityTest() throws RegBaseCheckedException {
		ClientIntegrityValidator.verifyClientIntegrity();
		
		Manifest localManifest = Mockito.mock(Manifest.class);
//		when(localManifest.getEntries()).thenReturn(null, null);
		
		Map <String, Attributes> localAttributes = localManifest.getEntries();
		
		localAttributes.entrySet();
		 
		java.util.Iterator<Entry<String, Attributes>> it = localAttributes.entrySet().iterator();
		while (it.hasNext()) {
		    Map.Entry entry1 = (Map.Entry)it.next();
		
		    final String libFolder = "lib";
		    @SuppressWarnings("unused")
			File file = new File(libFolder + File.separator + entry1.getKey());
    
		    @SuppressWarnings("static-access")
			X509Certificate trustedCertificate = clientIntegrityValidator.getCertificate();

		    
		}
		
	
	}

	@Test
	public void integrityCheckTest() throws IOException {
		URL url = ManifestCreatorTest.class.getResource("/setup/registration-api-1.2.0-SNAPSHOT.jar");
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
