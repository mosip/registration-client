package io.mosip.registration.test.update;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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

	@InjectMocks
	private ClientIntegrityValidator clientIntegrityValidator;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
		PowerMockito.mockStatic(HMACUtils2.class);
	}

	@Test
	public void verifyClientIntegrityTest() throws RegBaseCheckedException {
		clientIntegrityValidator.verifyClientIntegrity();
	}

	@Test
	public void integrityCheckTest() throws IOException {
		URL url = ManifestCreatorTest.class.getResource("/setup/registration-api-1.2.0-SNAPSHOT.jar");
		X509Certificate certificate = ClientIntegrityValidator.getCertificate();
		JarFile jarFile = new JarFile(url.getFile());
		ClientIntegrityValidator.verifyIntegrity(certificate, jarFile);
	}

}
