package io.mosip.registration.test.update;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.update.SoftwareUpdateUtil;
import io.mosip.registration.update.ResumableDownloader;
import io.mosip.registration.update.UpgradeOutcome;
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

	/**
	 * update() calls SoftwareUpdateUtil.download(String) twice - once for the server manifest and once
	 * for its detached signature - but PowerMock's stub(...).toReturn() can only hand back a single
	 * instance, which the first read exhausts. ByteArrayInputStream.close() is already a no-op, so
	 * re-arming on close gives both calls the full bytes without changing what is stubbed.
	 */
	private static InputStream rereadable(byte[] bytes) {
		return new ByteArrayInputStream(bytes) {
			@Override
			public void close() {
				// Rewind explicitly rather than via reset(): Manifest's parser calls mark() while
				// reading, so reset() would return to wherever the parser last marked - near EOF -
				// and the second caller would see an empty stream.
				this.pos = 0;
				this.mark = 0;
			}
		};
	}

	/**
	 * Reads a file without java.nio.file.Files, which throws InaccessibleObjectException under the
	 * PowerMock classloader used by this class.
	 */
	private static byte[] readAllBytes(File file) throws IOException {
		try (FileInputStream in = new FileInputStream(file);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] chunk = new byte[4096];
			int read;
			while ((read = in.read(chunk)) != -1) {
				out.write(chunk, 0, read);
			}
			return out.toByteArray();
		}
	}

	@After
	public void cleanup() {
		// MANIFEST.MF at the project root is written by the production update() method;
		// tempFolder handles cleanup of all test-created directory trees automatically.
		new File("MANIFEST.MF").delete();
		// update() now commits the detached signature alongside the manifest - clean it up too, along
		// with the staging files, so a mid-write failure cannot leave them behind for the next test.
		new File("MANIFEST.MF.sig").delete();
		new File("MANIFEST.MF.tmp").delete();
		new File("MANIFEST.MF.sig.tmp").delete();
		// update() stages into .artifacts/ relative to the module dir; remove what the tests created.
		File artifactsDir = new File(".artifacts");
		File[] staged = artifactsDir.listFiles();
		if (staged != null) {
			for (File f : staged) {
				f.delete();
			}
		}
		artifactsDir.delete();
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
		// update() re-checks each artifact's hash after downloading it; under mockStatic the real
		// validateJarChecksum would return a default false and fail these happy-path runs. The skip
		// branch still does not fire, because nothing creates the .artifacts/ file.
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(true);

		// Build a manifest with one entry so the update() loop body executes
		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes entryAttrs = new Attributes();
		entryAttrs.put(Attributes.Name.CONTENT_TYPE, "fakechecksum");
		serverManifestContent.getEntries().put("test-lib.jar", entryAttrs);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));

		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		softwareUpdateHandler.doSoftwareUpgrade();
		// update() calls globalParamService.update(IS_SOFTWARE_UPDATE_AVAILABLE, DISABLE) at the very
		// end — verifying this confirms the entire update path ran (manifest fetched, files processed)
		Mockito.verify(globalParamService).update(
				Mockito.eq(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE),
				Mockito.eq(RegistrationConstants.DISABLE));
	}

	@Test
	public void doSoftwareUpgrade_withProgressListener_reportsEveryArtifactAndEndsAtFullProgress()
			throws Exception {
		// The in-app upgrade leaves the operator working, so the bar must report real progress rather
		// than spin: every artifact advances it and the last report is 100%.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		// Back up into a TemporaryFolder, not src/test/resources/sql: backUpSetup mkdirs() a
		// <version>_<timestamp> tree and relies on FileUtils.deleteDirectory to prune old ones, but
		// FileUtils is statically mocked here so that prune is a no-op -- pointing at the resources
		// tree leaks a directory per run into the repo.
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-progress").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);
		// update() re-checks each artifact's hash after downloading it; under mockStatic the real
		// validateJarChecksum would return a default false and fail these happy-path runs. The skip
		// branch still does not fire, because nothing creates the .artifacts/ file.
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(true);

		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes first = new Attributes();
		first.put(Attributes.Name.CONTENT_TYPE, "checksum-a");
		Attributes second = new Attributes();
		second.put(Attributes.Name.CONTENT_TYPE, "checksum-b");
		serverManifestContent.getEntries().put("artifact-a.jar", first);
		serverManifestContent.getEntries().put("artifact-b.jar", second);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		List<Double> fractions = new ArrayList<>();
		Set<String> artifacts = new HashSet<>();
		softwareUpdateHandler.doSoftwareUpgrade((fraction, artifact) -> {
			fractions.add(fraction);
			artifacts.add(artifact);
		});

		Assert.assertFalse("expected progress reports", fractions.isEmpty());
		Assert.assertEquals("progress must finish at 100%", 1.0d, fractions.get(fractions.size() - 1), 0.0001d);
		double previous = -1d;
		for (Double fraction : fractions) {
			Assert.assertTrue("progress must never go backwards", fraction >= previous);
			Assert.assertTrue("progress must stay within [0,1]", fraction >= 0d && fraction <= 1d);
			previous = fraction;
		}
		// Manifest entry order is not guaranteed, so assert coverage rather than sequence.
		Assert.assertTrue("both artifacts must be reported", artifacts.contains("artifact-a.jar")
				&& artifacts.contains("artifact-b.jar"));
		// The detached signature must be adopted together with the manifest. Leaving the previous
		// signature in place would fail the launcher's Case B check on the next start, with no recovery.
		Assert.assertTrue("MANIFEST.MF must be written on success", new File("MANIFEST.MF").exists());
		Assert.assertTrue("MANIFEST.MF.sig must be written alongside it",
				new File("MANIFEST.MF.sig").exists());
		Assert.assertFalse("no staging files may be left behind", new File("MANIFEST.MF.tmp").exists());
	}

	@Test
	public void doSoftwareUpgrade_afterSuccess_stillReportsTheRunningVersionUntilRestart() throws Exception {
		// The restart is now a prompt the operator can defer, so the downloaded version can sit on disk
		// for the rest of the session while the OLD jars keep executing. getCurrentVersion() feeds packet
		// metadata (META_CLIENT_VERSION), the version sent on every sync/REST call and the UI labels --
		// all of which must keep describing what is actually running, not what is staged.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-version").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "9.9.9-NEW");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);
		// update() re-checks each artifact's hash after downloading it; under mockStatic the real
		// validateJarChecksum would return a default false and fail these happy-path runs. The skip
		// branch still does not fire, because nothing creates the .artifacts/ file.
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(true);

		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "9.9.9-NEW");
		Attributes entryAttrs = new Attributes();
		entryAttrs.put(Attributes.Name.CONTENT_TYPE, "checksum");
		serverManifestContent.getEntries().put("artifact-a.jar", entryAttrs);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals("upgrade should succeed", UpgradeOutcome.COMPLETED,
				softwareUpdateHandler.doSoftwareUpgrade());

		Assert.assertTrue("the new manifest must be on disk", new File("MANIFEST.MF").exists());
		Assert.assertEquals("getCurrentVersion() must still report the RUNNING version until restart",
				"1.2.0-SNAPSHOT", softwareUpdateHandler.getCurrentVersion());
	}

	@Test
	public void doSoftwareUpgrade_artifactAlreadyStagedAndIntact_isNotDownloadedAgain() throws Exception {
		// The skip test used to look at lib/<entry>, which the design guarantees is never populated from
		// 1.3.0 onwards -- so it was always false and every artifact was re-fetched on every attempt,
		// including the ~200MB jre21.zip on a retry that had already finished it.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-staged").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);

		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes entryAttrs = new Attributes();
		entryAttrs.put(Attributes.Name.CONTENT_TYPE, "checksum");
		serverManifestContent.getEntries().put("artifact-a.jar", entryAttrs);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));

		// The artifact is already staged and passes its checksum.
		File artifactsDir = new File(".artifacts");
		Assert.assertTrue(artifactsDir.exists() || artifactsDir.mkdirs());
		// Plain stream, not java.nio.file.Files: PowerMock's instrumentation cannot reflect into
		// java.nio.file under JPMS ("module java.base does not opens java.nio.file").
		try (FileOutputStream staged = new FileOutputStream(new File(artifactsDir, "artifact-a.jar"))) {
			staged.write("already-downloaded".getBytes());
		}
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(true);
		// Any download attempt fails the test outright: reaching it means the skip did not work.
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "downloadResumable",
				String.class, String.class, String.class, ResumableDownloader.ProgressListener.class))
				.toThrow(new IllegalStateException("a staged, intact artifact must not be re-downloaded"));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(UpgradeOutcome.COMPLETED, softwareUpdateHandler.doSoftwareUpgrade());
	}

	@Test
	public void doSoftwareUpgrade_whileAnotherIsRunning_reportsAlreadyInProgressNotFailure() throws Exception {
		// The operator can reach the update menu during a download now that the pane is released. A
		// rejected duplicate must be distinguishable from a real failure: reporting FAILED made the
		// caller show "unable to update" and tear down the RUNNING upgrade's progress display.
		ReflectionTestUtils.setField(softwareUpdateHandler, "upgradeInProgress",
				new AtomicBoolean(true));

		UpgradeOutcome outcome = softwareUpdateHandler.doSoftwareUpgrade();

		Assert.assertEquals(UpgradeOutcome.ALREADY_IN_PROGRESS, outcome);
		Assert.assertTrue("the in-progress flag must be reported to callers",
				softwareUpdateHandler.isUpgradeInProgress());
		// The rejected request must not have touched anything.
		Mockito.verifyNoInteractions(globalParamService);
	}

	@Test
	public void doSoftwareUpgrade_noArgOverload_stillWorksWithoutAProgressListener() throws Exception {
		// The pre-existing no-arg entry point must keep working for every caller that wants no progress.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		// See the note on the progress test above: temp folder, so nothing leaks into the repo.
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-noarg").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);
		// update() re-checks each artifact's hash after downloading it; under mockStatic the real
		// validateJarChecksum would return a default false and fail these happy-path runs. The skip
		// branch still does not fire, because nothing creates the .artifacts/ file.
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(true);

		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes entryAttrs = new Attributes();
		entryAttrs.put(Attributes.Name.CONTENT_TYPE, "checksum");
		serverManifestContent.getEntries().put("artifact-a.jar", entryAttrs);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		softwareUpdateHandler.doSoftwareUpgrade();

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
	public void doSoftwareUpgrade_artifactFailsItsChecksum_failsWithoutCommittingTheManifest() throws Exception {
		// The download layer reports success on byte count alone, and its 416 path finalizes a .part
		// purely because the LENGTH matches the server total -- so a stale artifact of the same size is
		// adopted silently. If update() did not re-check the hash it would commit ./MANIFEST.MF and
		// report COMPLETED, and the corruption would surface only on the next start, where the launcher
		// aborts as Case B and re-downloads nothing: an unbootable client.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-bad-checksum").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);

		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Attributes only = new Attributes();
		only.put(Attributes.Name.CONTENT_TYPE, "expected-checksum");
		serverManifestContent.getEntries().put("artifact-a.jar", only);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));
		// downloadResumable is a no-op under the static mock, so nothing is written; validateJarChecksum
		// returning false is what a same-length stale artifact looks like to update().
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "validateJarChecksum",
				File.class, Attributes.class)).toReturn(false);
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		File rootManifest = new File("MANIFEST.MF");
		boolean existedBefore = rootManifest.exists();
		byte[] before = existedBefore ? readAllBytes(rootManifest) : null;

		UpgradeOutcome outcome = softwareUpdateHandler.doSoftwareUpgrade();

		Assert.assertEquals("a checksum mismatch must fail the upgrade", UpgradeOutcome.FAILED, outcome);
		Assert.assertEquals("the manifest must not be created or removed on a checksum failure",
				existedBefore, rootManifest.exists());
		if (existedBefore) {
			Assert.assertArrayEquals("the OLD manifest must survive so the client still boots",
					before, readAllBytes(rootManifest));
		}
		Assert.assertFalse("no signature may be committed on a checksum failure",
				new File("MANIFEST.MF.sig").exists());
	}

	@Test
	public void doSoftwareUpgrade_oversizedSignatureBody_isRejectedAndNothingIsCommitted() throws Exception {
		// The upgrade server answers the MANIFEST.MF.sig request with something far larger than a
		// signature -- an HTML error page or a redirect body is the realistic case. Adopting it as
		// ./MANIFEST.MF.sig would pair a valid manifest with a bogus signature, and the launcher treats
		// that as Case B: it aborts with "signature invalid" and re-downloads NOTHING, so the client
		// cannot boot again without manual repair. The download must be refused instead.
		Attributes mainAttrs = new Attributes();
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(mainAttrs);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath",
				tempFolder.newFolder("backup-oversized-sig").getAbsolutePath());
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);

		// One stubbed body serves both fetches. It is a VALID manifest, so setServerManifest succeeds and
		// the run reaches the signature download -- where the same bytes are well over the 1024-byte cap.
		Manifest serverManifestContent = new Manifest();
		serverManifestContent.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		for (int i = 0; i < 40; i++) {
			Attributes entry = new Attributes();
			entry.put(Attributes.Name.CONTENT_TYPE, "checksum-" + i);
			serverManifestContent.getEntries().put("artifact-" + i + ".jar", entry);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serverManifestContent.write(baos);
		Assert.assertTrue("the fixture must exceed the signature cap to exercise it",
				baos.toByteArray().length > 1024);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(rereadable(baos.toByteArray()));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		// Snapshot rather than assert non-existence: ./MANIFEST.MF may or may not be on disk depending on
		// which tests ran before. What matters is that this run leaves it exactly as it found it.
		File rootManifest = new File("MANIFEST.MF");
		boolean manifestExistedBefore = rootManifest.exists();
		byte[] manifestBefore = manifestExistedBefore ? readAllBytes(rootManifest) : null;

		UpgradeOutcome outcome = softwareUpdateHandler.doSoftwareUpgrade();

		Assert.assertEquals("an oversized signature body must fail the upgrade", UpgradeOutcome.FAILED, outcome);
		Assert.assertEquals("the manifest must not be created or removed when its signature was refused",
				manifestExistedBefore, rootManifest.exists());
		if (manifestExistedBefore) {
			Assert.assertArrayEquals("the manifest must not be replaced when its signature was refused",
					manifestBefore, readAllBytes(rootManifest));
		}
		Assert.assertFalse("no signature may be adopted from an oversized body",
				new File("MANIFEST.MF.sig").exists());
		Assert.assertFalse("no staging files may be left behind", new File("MANIFEST.MF.tmp").exists());
		Assert.assertFalse("no staging files may be left behind", new File("MANIFEST.MF.sig.tmp").exists());
	}

	@Test
	public void doSoftwareUpgrade_staleServerManifestAndFailedFetch_reportsFailure() throws Exception {
		Attributes attributes = new Attributes();
		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.2.0-SNAPSHOT");
		Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
		ReflectionTestUtils.setField(softwareUpdateHandler, "backUpPath", "src/test/resources/sql");
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverRegClientURL", "https://dev.mosip.net/registration-client/");
		ReflectionTestUtils.setField(softwareUpdateHandler, "latestVersion", "1.2.0-SNAPSHOT");
		// Suppress copyFile(File,File) explicitly to allow backUpSetup to complete
		PowerMockito.suppress(PowerMockito.method(FileUtils.class, "copyFile", File.class, File.class));
		// A leftover manifest from an earlier attempt is planted here: the field is only reset at the end
		// of a SUCCESSFUL update(), so this is a state a real client reaches after any failed upgrade.
		ReflectionTestUtils.setField(softwareUpdateHandler, "serverManifest", manifest);
		// Arm the static mock so setServerManifest() gets a null body -- the manifest fetch fails. Without
		// this the REAL download(String) runs and opens a connection to the configured serverRegClientURL,
		// making the test depend on network reachability (and on that host not serving a valid manifest).
		PowerMockito.mockStatic(SoftwareUpdateUtil.class);
		PowerMockito.stub(PowerMockito.method(SoftwareUpdateUtil.class, "download", String.class))
				.toReturn(null);
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());

		UpgradeOutcome outcome = softwareUpdateHandler.doSoftwareUpgrade();

		// It must report failure rather than quietly proceeding on the stale manifest -- which would
		// download from the NEW version's URLs while committing the OLD version's manifest.
		Assert.assertEquals("a failed manifest fetch must report failure", UpgradeOutcome.FAILED, outcome);
		Assert.assertNull("the stale manifest must not survive the failed fetch",
				ReflectionTestUtils.getField(softwareUpdateHandler, "serverManifest"));
		// backUpSetup still ran, so the backup-folder param was recorded before the failure.
		Mockito.verify(globalParamService, Mockito.atLeastOnce())
				.update(Mockito.anyString(), Mockito.anyString());
	}

}
