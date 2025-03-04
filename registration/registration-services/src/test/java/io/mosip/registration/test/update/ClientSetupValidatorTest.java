package io.mosip.registration.test.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.update.ClientSetupValidator;
import io.mosip.registration.update.SoftwareUpdateUtil;

/**
 * 
 * @author Rama Devi
 *
 */

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ Manifest.class, ApplicationContext.class, FileUtils.class, HMACUtils2.class })
public class ClientSetupValidatorTest {

	private static final String CONNECTION_TIMEOUT = "mosip.registration.sw.file.download.connection.timeout";
	private static final String READ_TIMEOUT = "mosip.registration.sw.file.download.read.timeout";
	private static final String manifestFile = "MANIFEST.MF";

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private SoftwareUpdateUtil softwareUpdateUtil;

	@Mock
	private File mockFile;

	@Mock
	private FileInputStream mockFileStream;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private Manifest manifest;

	@InjectMocks
	private ClientSetupValidator clientSetupValidator;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
		PowerMockito.mockStatic(HMACUtils2.class);
	}
	

	@Test
	public void clientSetupValidatorTest() throws RegBaseCheckedException {
		ClientSetupValidator clntSetupValidator = new ClientSetupValidator();
	}

	@Test
	public void validateBuildSetupExceptionTest() throws RegBaseCheckedException {
		clientSetupValidator.validateBuildSetup();
	}

	@Test
	public void validateBuildSetupTest() throws RegBaseCheckedException {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		clientSetupValidator.validateBuildSetup();
	}

	@Test
	public void validateBuildSetupEnvironmentTest() throws RegBaseCheckedException {
		ClientSetupValidator clntSetUpValidator = new ClientSetupValidator();
		ReflectionTestUtils.setField(clntSetUpValidator, "environment", "LOCAL");
		clntSetUpValidator.validateBuildSetup();
	}

	@Test
	public void validateBuildSetupsetServerManifestTest() throws RegBaseCheckedException {
		ClientSetupValidator clntSetUpValidator = new ClientSetupValidator();
		ReflectionTestUtils.setField(clntSetUpValidator, "serverRegClientURL",
				"https://dev.mosip.net/registration-client");
		ReflectionTestUtils.setField(clntSetUpValidator, "latestVersion", "");
		clntSetUpValidator.validateBuildSetup();
	}

	@Test
	public void isPatch_downloadedTest() throws RegBaseCheckedException {
		clientSetupValidator.isPatch_downloaded();
		assertEquals(Boolean.FALSE, clientSetupValidator.isPatch_downloaded());
	}

	@Test
	public void isUnknown_jars_found() throws RegBaseCheckedException {
		clientSetupValidator.isUnknown_jars_found();
		assertEquals(Boolean.FALSE, clientSetupValidator.isUnknown_jars_found());
	}

	@Test
	public void validateBuildSetupFileExceptionTest() throws RegBaseCheckedException {
		clientSetupValidator.validateBuildSetup();
	}

	@After
	public void tear() throws Exception {
		ClientSetupValidator clntSetUpValidator = null;
	}

	@Test
	public void deleteUnknownJarsTest() throws RegBaseCheckedException, IOException, Exception {
		Manifest manifest = getManifest();
		ReflectionTestUtils.setField(clientSetupValidator, "serverManifest", manifest);
		ReflectionTestUtils.setField(clientSetupValidator, "localManifest", manifest);
		Mockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("testing");
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(CONNECTION_TIMEOUT)).thenReturn(null);
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(READ_TIMEOUT)).thenReturn(null);
		clientSetupValidator.validateBuildSetup();
	}

	@Test
	public void deleteUnknownJarsExceptionTest() throws RegBaseCheckedException, IOException {
		Manifest manifest = new Manifest();
		Attributes attributes = new Attributes();
		Map<String, Attributes> entries = new HashMap<>();
		entries.put("logback.xml", attributes);
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0.1-SNAPSHOT");
		ReflectionTestUtils.setField(manifest, "attr", attributes);
		ReflectionTestUtils.setField(manifest, "entries", entries);
		ReflectionTestUtils.setField(clientSetupValidator, "serverManifest", manifest);
		ReflectionTestUtils.setField(clientSetupValidator, "localManifest", manifest);
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(CONNECTION_TIMEOUT)).thenReturn(null);
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(READ_TIMEOUT)).thenReturn(null);
		clientSetupValidator.validateBuildSetup();
	}

	@Test
	public void deleteUnknownJarsValidateCheckSumFalseTest() throws RegBaseCheckedException, IOException, Exception {
		Manifest manifest = new Manifest();
		Attributes attributes = new Attributes();
		Map<String, Attributes> entries = new HashMap<>();
		entries.put("", attributes);
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0.1-SNAPSHOT");
		ReflectionTestUtils.setField(manifest, "attr", attributes);
		ReflectionTestUtils.setField(manifest, "entries", entries);
		ReflectionTestUtils.setField(clientSetupValidator, "serverManifest", manifest);
		ReflectionTestUtils.setField(clientSetupValidator, "localManifest", manifest);
		Mockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("testing");
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(CONNECTION_TIMEOUT)).thenReturn(null);
		Mockito.when(ApplicationContext.getIntValueFromApplicationMap(READ_TIMEOUT)).thenReturn(null);
		clientSetupValidator.validateBuildSetup();
	}

	@Test
	public void hasUpdateSuccessTest() throws RegBaseCheckedException {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0.1-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(
				ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL))
				.thenReturn("https://dev.mosip.net");
		clientSetupValidator.validateBuildSetup();
	}

	private Manifest getManifest() throws Exception {
		File localManifestFile = new File(manifestFile);
		Manifest manifest = new Manifest();
		if (localManifestFile.exists()) {
			return new Manifest(new FileInputStream(localManifestFile));
		}
		return manifest;
	}
}
