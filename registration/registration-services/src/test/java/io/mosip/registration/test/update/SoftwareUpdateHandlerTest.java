package io.mosip.registration.test.update;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
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

import com.itextpdf.io.IOException;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.VersionMappings;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({Manifest.class, ApplicationContext.class, FileUtils.class})
public class SoftwareUpdateHandlerTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private SoftwareUpdateHandler softwareUpdateHandler;

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
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
	}

	@Test
	public void executeSQLTest() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Map<String, VersionMappings> versionsMap = new LinkedHashMap<>();
		versionsMap.put("0.11.0", new VersionMappings("0.11.0", 1, ""));
		Assert.assertSame(RegistrationConstants.SQL_EXECUTION_SUCCESS,
				softwareUpdateHandler.executeSqlFile("0.11.0", versionsMap).getSuccessResponseDTO().getMessage());
	}


	@Ignore
	@Test
	public void executeSQLTestRollBack() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(),
		Mockito.anyString());

		System.setProperty("user.dir", "src/test/resources/");
		Mockito.doThrow(RuntimeException.class).when(jdbcTemplate).execute(Mockito.anyString());

		SoftwareUpdateHandler softwareUpdateHandle = new SoftwareUpdateHandler();
		Assert.assertNull(softwareUpdateHandle.executeSqlFile("0.11.0", new LinkedHashMap<>()).getErrorResponseDTOs());
	}

	@Test
	public void setTimestapTest() {
		softwareUpdateHandler.setLatestVersionReleaseTimestamp("20190520091122");
		Assert.assertNotNull(softwareUpdateHandler.getLatestVersionReleaseTimestamp());
	}
	
	@Test
	public void hasUpdateTest() {
		Assert.assertFalse(softwareUpdateHandler.hasUpdate());
	}
	
	@Test
	public void hasUpdateSuccessTest() {
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverMosipXmlFileUrl", "https://dev.mosip.net/registration-client/maven-metadata.xml");
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL)).thenReturn("https://dev.mosip.net");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/registration-client/maven-metadata.xml");
		Assert.assertFalse(softwareUpdateHandler.hasUpdate());
	}

	@Test
	public void updateDerbyDBTest() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Assert.assertNotNull(softwareUpdateHandler.updateDerbyDB());
	}
	
	@Test
	public void updateDerbyDBRollBackTest() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SOFTWARE_BACKUP_FOLDER)).thenReturn("src/test/resources/sql");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doThrow(IOException.class).when(jdbcTemplate).execute("----- create new tables\n" + 
				" \n" + 
				" CREATE TABLE \"REG\".\"LOC_HIERARCHY_LIST\"(\"HIERARCHY_LEVEL\" INTEGER NOT NULL, \"HIERARCHY_LEVEL_NAME\" VARCHAR(36) NOT NULL, \"LANG_CODE\" VARCHAR(3) NOT NULL, \"IS_ACTIVE\" BOOLEAN NOT NULL, \"CR_BY\" VARCHAR(256) NOT NULL, \"CR_DTIMES\" TIMESTAMP NOT NULL, \"UPD_BY\" VARCHAR(256), \"UPD_DTIMES\" TIMESTAMP)");
		PowerMockito.doNothing().when(FileUtils.class, "copyDirectory", Mockito.any(File.class), Mockito.any(File.class));
		File backUpFile = new File("src/test/resources/sql");
		PowerMockito.doNothing().when(FileUtils.class, "copyFile", new File(backUpFile.getAbsolutePath() + "/MANIFEST.MF"), new File("MANIFEST.MF"));
		Assert.assertNotNull(softwareUpdateHandler.updateDerbyDB());
	}
	
	@Test
	public void updateDerbyDBNullResponseTest() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Assert.assertNull(softwareUpdateHandler.updateDerbyDB());
	}
	
	@Test
	public void doSoftwareUpgradeTest() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "");
		PowerMockito.doNothing().when(FileUtils.class, "copyDirectory", Mockito.any(File.class), Mockito.any(File.class));
		PowerMockito.doNothing().when(FileUtils.class, "deleteDirectory", Mockito.any(File.class));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		softwareUpdateHandler.doSoftwareUpgrade();
	}
	
	@Test
	public void getJarChecksumTest() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.CONTENT_TYPE, "test");
		Map<String, Attributes> entries = new HashMap<>();
		entries.put("registration-client", attributes);
		entries.put("registration-services", attributes);
		Mockito.when(manifest.getEntries()).thenReturn(entries);
		assertNotNull(softwareUpdateHandler.getJarChecksum());
	}

}
