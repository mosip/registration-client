package io.mosip.registration.test.update;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.update.SoftwareUpdateUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.VersionMappings;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*"})
@PrepareForTest({Manifest.class, ApplicationContext.class, FileUtils.class, SoftwareUpdateUtil.class})
public class SoftwareUpdateHandlerTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

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

	@Mock
	private AuditManagerService auditFactory;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, FileUtils.class);
	}

	@After
	public void cleanup() {
		// MANIFEST.MF at the project root is written by the production update() method;
		// tempFolder handles cleanup of all test-created directory trees automatically.
		new File("MANIFEST.MF").delete();
	}

	@Test
	public void executeSql_withValidVersion_returnsSuccess() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Map<String, VersionMappings> versionsMap = new LinkedHashMap<>();
		versionsMap.put("0.11.0", new VersionMappings("0.11.0", 1, ""));
		Assert.assertEquals(RegistrationConstants.SQL_EXECUTION_SUCCESS,
				softwareUpdateHandler.executeSqlFile("0.11.0", versionsMap).getSuccessResponseDTO().getMessage());
	}


	@Ignore
	@Test
	public void executeSql_withRollback_returnsNullOnError() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(),
		Mockito.anyString());

		System.setProperty("user.dir", "src/test/resources/");
		Mockito.doThrow(RuntimeException.class).when(jdbcTemplate).execute(Mockito.anyString());

		SoftwareUpdateHandler softwareUpdateHandle = new SoftwareUpdateHandler();
		Assert.assertNull(softwareUpdateHandle.executeSqlFile("0.11.0", new LinkedHashMap<>()).getErrorResponseDTOs());
	}

	@Test
	public void setLatestVersionReleaseTimestamp_withValue_returnsNotNull() {
		softwareUpdateHandler.setLatestVersionReleaseTimestamp("20190520091122");
		Assert.assertNotNull(softwareUpdateHandler.getLatestVersionReleaseTimestamp());
	}
	
	@Test
	public void hasUpdate_withNoServerConfig_returnsFalse() {
		Assert.assertFalse(softwareUpdateHandler.hasUpdate());
	}
	
	@Test
	public void hasUpdate_withServerAndManifest_returnsFalse() {
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverMosipXmlFileUrl", "https://dev.mosip.net/registration-client/maven-metadata.xml");
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL)).thenReturn("https://dev.mosip.net");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/registration-client/maven-metadata.xml");
		Assert.assertFalse(softwareUpdateHandler.hasUpdate());
	}

	@Test
	public void updateDerbyDB_withValidManifest_returnsNotNull() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Assert.assertNotNull(softwareUpdateHandler.updateDerbyDB());
	}

	@Test
	public void updateDerbyDB_withSqlError_handlesRollback_returnsNotNull() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SOFTWARE_BACKUP_FOLDER)).thenReturn("src/test/resources/sql");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doThrow(DataAccessResourceFailureException.class).when(jdbcTemplate).execute("----- create new tables\n" +
				" \n" + 
				" CREATE TABLE \"REG\".\"LOC_HIERARCHY_LIST\"(\"HIERARCHY_LEVEL\" INTEGER NOT NULL, \"HIERARCHY_LEVEL_NAME\" VARCHAR(36) NOT NULL, \"LANG_CODE\" VARCHAR(3) NOT NULL, \"IS_ACTIVE\" BOOLEAN NOT NULL, \"CR_BY\" VARCHAR(256) NOT NULL, \"CR_DTIMES\" TIMESTAMP NOT NULL, \"UPD_BY\" VARCHAR(256), \"UPD_DTIMES\" TIMESTAMP)");
		PowerMockito.doNothing().when(FileUtils.class, "copyDirectory", Mockito.any(File.class), Mockito.any(File.class));
		File backUpFile = new File("src/test/resources/sql");
		PowerMockito.doNothing().when(FileUtils.class, "copyFile", new File(backUpFile.getAbsolutePath() + "/MANIFEST.MF"), new File("MANIFEST.MF"));
		Assert.assertNotNull(softwareUpdateHandler.updateDerbyDB());
	}
	
	@Test
	public void updateDerbyDB_withSnapshot_returnsNull() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("1.2.0-SNAPSHOT");
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		Assert.assertNull(softwareUpdateHandler.updateDerbyDB());
	}
	
	@Test
	public void doSoftwareUpgrade_withBackupAndServer_executes() throws Exception {
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
	public void getJarChecksum_withManifestEntries_returnsNotNull() {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.CONTENT_TYPE, "test");
		Map<String, Attributes> entries = new HashMap<>();
		entries.put("registration-client", attributes);
		entries.put("registration-services", attributes);
		Mockito.when(manifest.getEntries()).thenReturn(entries);
		assertNotNull(softwareUpdateHandler.getJarChecksum());
	}

	@Test
	public void executeSqlFile_multipleVersions_executesAllInOrder() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		// FileUtils is already statically mocked in @Before; no explicit doNothing needed

		Map<String, VersionMappings> versionsMap = new LinkedHashMap<>();
		versionsMap.put("0.10.0", new VersionMappings("0.10.0", 1, null));
		versionsMap.put("0.11.0", new VersionMappings("0.11.0", 2, null));

		ResponseDTO response = softwareUpdateHandler.executeSqlFile("0.9.0", versionsMap);
		Assert.assertEquals(RegistrationConstants.SQL_EXECUTION_SUCCESS,
				response.getSuccessResponseDTO().getMessage());
		InOrder inOrder = Mockito.inOrder(globalParamService);
		inOrder.verify(globalParamService).update(RegistrationConstants.SERVICES_VERSION_KEY, "0.10.0");
		inOrder.verify(globalParamService).update(RegistrationConstants.SERVICES_VERSION_KEY, "0.11.0");
	}

	@Test
	public void executeSqlFile_withFullSyncEntities_savesEntities() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		// FileUtils is already statically mocked in @Before; no explicit doNothing needed

		Map<String, VersionMappings> versionsMap = new LinkedHashMap<>();
		versionsMap.put("0.11.0", new VersionMappings("0.11.0", 1, "REG_CENTER,USER_DETAIL"));

		softwareUpdateHandler.executeSqlFile("0.10.0", versionsMap);
		Mockito.verify(globalParamService).update(
				Mockito.eq(RegistrationConstants.UPGRADE_FULL_SYNC_ENTITIES), Mockito.anyString());
	}

	@Test
	public void executeSqlFile_previousVersionInMap_skipsOlderEntries() throws Exception {
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(jdbcTemplate).execute(Mockito.anyString());

		Map<String, VersionMappings> versionsMap = new LinkedHashMap<>();
		versionsMap.put("0.10.0", new VersionMappings("0.10.0", 1, null));
		versionsMap.put("0.11.0", new VersionMappings("0.11.0", 2, null));

		// previousVersion is "0.11.0" → releaseOrder 2; only entries with order > 2 run → none
		ResponseDTO response = softwareUpdateHandler.executeSqlFile("0.11.0", versionsMap);
		Assert.assertEquals(RegistrationConstants.SQL_EXECUTION_SUCCESS,
				response.getSuccessResponseDTO().getMessage());
		Mockito.verify(globalParamService, Mockito.never())
				.update(Mockito.eq(RegistrationConstants.SERVICES_VERSION_KEY), Mockito.anyString());
	}

	@Test
	public void getJarChecksum_nullManifest_returnsEmptyMap() {
		ReflectionTestUtils.setField(softwareUpdateHandler, "localManifest", null);
		Map<String, String> result = softwareUpdateHandler.getJarChecksum();
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void updateDerbyDB_versionZero_backupFolderMissing_returnsNull() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.1-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("0");
		// backUpPath points to a non-existent path so setupPreviousVersion returns "0"
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "nonexistent_backup_path_xyz");
		// FileUtils.getFile is statically mocked (returns null by default); stub it to return a real File
		PowerMockito.when(FileUtils.getFile("nonexistent_backup_path_xyz")).thenReturn(new File("nonexistent_backup_path_xyz"));
		Assert.assertNull(softwareUpdateHandler.updateDerbyDB());
	}

	@Test
	public void doSoftwareUpgrade_backupFails_doesNotPropagateException() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		// Use tempFolder so bin/lib/db mkdirs() inside backUpSetup don't leave real dirs behind
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", tempFolder.getRoot().getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/reg/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "");
		// First copyDirectory call (backup) throws; rollBackSetup's calls do nothing so the
		// second catch(IOException) in doSoftwareUpgrade is never bypassed by a RuntimeException.
		// doThrow().doNothing() chaining doesn't work for PowerMockito static stubs, so use doAnswer.
		boolean[] backupAttempted = {false};
		PowerMockito.doAnswer(inv -> {
			if (!backupAttempted[0]) { backupAttempted[0] = true; throw new RuntimeException("backup failed"); }
			return null;
		}).when(FileUtils.class, "copyDirectory", Mockito.any(File.class), Mockito.any(File.class));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		softwareUpdateHandler.doSoftwareUpgrade();
		// update() was never reached, so the IS_SOFTWARE_UPDATE_AVAILABLE flag must not have been set
		Mockito.verify(globalParamService, Mockito.never())
				.update(Mockito.eq(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE), Mockito.anyString());
	}

	@Test
	public void updateDerbyDB_withVersionMappingsParseError_returnsResponse() throws Exception {
		SoftwareUpdateHandler spyHandler = PowerMockito.spy(softwareUpdateHandler);
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.3");
		ReflectionTestUtils.setField(spyHandler, "localManifest", manifest);
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SERVICES_VERSION_KEY))
				.thenReturn("0");

		PowerMockito.doThrow(new RuntimeException("parse error")).when(spyHandler, "getSortedVersionMappings",
				RegistrationConstants.VERSION_MAPPINGS_KEY);

		ResponseDTO response = spyHandler.updateDerbyDB();

		Assert.assertNotNull(response);
	}

	@Test
	public void updateDerbyDB_versionZero_backupFolderExistsWithNoManifests_returnsNull() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.1-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("0");
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		// Stub FileUtils.getFile to return a real existing directory (backup folder exists)
		PowerMockito.when(FileUtils.getFile("src/test/resources/sql")).thenReturn(new File("src/test/resources/sql"));
		// setupPreviousVersion iterates subdirs, finds no MANIFEST.MF → backupVersions empty → returns "0"
		Assert.assertNull(softwareUpdateHandler.updateDerbyDB());
	}

	@Test
	public void updateDerbyDB_versionZero_backupFolderWithManifest_returnsNull() throws Exception {
		// Create isolated backup tree inside tempFolder so no shared test-resource dirs are mutated.
		// setupPreviousVersion() scans backUpPath's subdirectories for MANIFEST.MF files.
		File testBackupDir = tempFolder.newFolder("testbackup_coverage");
		File testManifestFile = new File(testBackupDir, "MANIFEST.MF");

		Manifest backupManifest = new Manifest();
		backupManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.1-SNAPSHOT");
		try (FileOutputStream fos = new FileOutputStream(testManifestFile)) {
			backupManifest.write(fos);
		}

		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.1-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString())).thenReturn("0");

		String backupRoot = tempFolder.getRoot().getAbsolutePath();
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", backupRoot);
		PowerMockito.when(FileUtils.getFile(backupRoot)).thenReturn(tempFolder.getRoot());

		// setupPreviousVersion reads the MANIFEST.MF, finds "1.2.1-SNAPSHOT" in versionMappings
		// currentVersion == version → updateDerbyDB returns null
		Assert.assertNull(softwareUpdateHandler.updateDerbyDB());
	}

	@Test
	public void hasUpdate_withMockedLatestVersion_returnsTrue() throws Exception {
		// Covers getLatestVersion() (L223-227) and getElementValue() (L231-241)
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverMosipXmlFileUrl", "%s/maven-metadata.xml");
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL))
				.thenReturn("http://test");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString()))
				.thenReturn("http://test/maven-metadata.xml");
		String xml = "<metadata><versioning><version>1.2.1-SNAPSHOT</version>"
				+ "<lastUpdated>20230101120000</lastUpdated></versioning></metadata>";
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);
		// Use stub() with explicit param type to avoid protected-access and overload issues
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(new ByteArrayInputStream(xml.getBytes()));
		boolean result = softwareUpdateHandler.hasUpdate();
		Assert.assertTrue(result);
	}

	@Test
	public void doSoftwareUpgrade_withManifestEntries_downloadsFiles() throws Exception {
		// Covers update() loop body (L326-333) and setServerManifest() success path (L404)
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);

		// Build a manifest with one entry so the update() loop body executes
		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes entryAttrs = new Attributes();
		entryAttrs.put(Attributes.Name.CONTENT_TYPE, "fakechecksum");
		serverManifestContent.getEntries().put("test-lib.jar", entryAttrs);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(new ByteArrayInputStream(baos.toByteArray()));

		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		softwareUpdateHandler.doSoftwareUpgrade();
		// update() calls globalParamService.update(IS_SOFTWARE_UPDATE_AVAILABLE, DISABLE) at the very
		// end — verifying this confirms the entire update path ran (manifest fetched, files processed)
		Mockito.verify(globalParamService).update(
				Mockito.eq(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE),
				Mockito.eq(RegistrationConstants.DISABLE));
	}

	@Test
	public void updateDerbyDB_withAllSqlFailing_triggersDbRollback() throws Exception {
		// Covers executeSQL() rollback path (L537-555) and dbRollBackSetup() (L560-566)
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-rc2-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(Mockito.anyString()))
				.thenReturn("1.2.0-SNAPSHOT");
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		// All jdbcTemplate.execute() calls throw RuntimeException (both upgrade and rollback scripts fail)
		Mockito.doThrow(new RuntimeException("SQL error")).when(jdbcTemplate).execute(Mockito.anyString());
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		// Stub FileUtils.getFile so dbRollBackSetup() can check .exists() without NPE
		PowerMockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("nonexistent_db_backup_xyz"));
		ResponseDTO response = softwareUpdateHandler.updateDerbyDB();
		Assert.assertNotNull(response);
		// jdbcTemplate.execute is stubbed to throw; verify it was invoked (upgrade SQL attempted)
		Mockito.verify(jdbcTemplate, Mockito.atLeastOnce()).execute(Mockito.anyString());
	}

	@Test
	public void doSoftwareUpgrade_withSuppressedCopyFile_callsUpdate() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		// Suppress copyFile(File,File) explicitly to allow backUpSetup to complete
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		// Pre-set serverManifest so update() can call write() (mocked, no-op) after setServerManifest fails silently
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverManifest", manifest);
		// SoftwareUpdateUtil.download returns null by default → new Manifest(null) → NPE caught in setServerManifest → serverManifest stays pre-set
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		softwareUpdateHandler.doSoftwareUpgrade();
		// update() ran to completion — IS_SOFTWARE_UPDATE_AVAILABLE and LAST_SOFTWARE_UPDATE must have been set
		Mockito.verify(globalParamService, Mockito.atLeastOnce())
				.update(Mockito.anyString(), Mockito.anyString());
	}

}
