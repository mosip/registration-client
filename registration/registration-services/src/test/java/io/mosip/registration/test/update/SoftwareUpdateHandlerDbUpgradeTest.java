/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.test.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.VersionMappings;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.update.SoftwareUpdateHandler;

/**
 * Runs SoftwareUpdateHandler's local DB upgrade against a real in-memory Derby database shaped like a 1.2.0.2 install (no
 * CA_CERT_STORE.CA_CERT_TYPE), with the real spring.properties version-mappings and sql/ scripts.
 */
public class SoftwareUpdateHandlerDbUpgradeTest {

	private static final AtomicInteger DB_COUNTER = new AtomicInteger();
	private static final String[] CONTEXT_KEYS = { RegistrationConstants.SERVICES_VERSION_KEY,
			RegistrationConstants.SOFTWARE_BACKUP_FOLDER, RegistrationConstants.UPGRADE_FULL_SYNC_ENTITIES };

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private String dbUrl;
	private JdbcTemplate jdbcTemplate;
	private File appRoot;

	@Before
	public void setUp() throws IOException {
		ApplicationContext.getInstance(); // creates the map; DaoConfig's static init does this at startup
		resetUpgradeState();
		for (String key : CONTEXT_KEYS) {
			ApplicationContext.map().remove(key);
		}
		dbUrl = "jdbc:derby:memory:upgrader" + DB_COUNTER.incrementAndGet();
		jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(dbUrl + ";create=true"));
		jdbcTemplate.execute("CREATE SCHEMA REG");
		// DDL as in initial.sql, except that CA_CERT_STORE predates CA_CERT_TYPE
		jdbcTemplate.execute("CREATE TABLE \"REG\".\"GLOBAL_PARAM\" (\"CODE\" VARCHAR(128) NOT NULL, \"NAME\" VARCHAR(128) NOT NULL, "
				+ "\"VAL\" VARCHAR(512), \"TYP\" VARCHAR(128) NOT NULL, \"LANG_CODE\" VARCHAR(3) NOT NULL, \"IS_ACTIVE\" BOOLEAN NOT NULL, "
				+ "\"CR_BY\" VARCHAR(32) NOT NULL, \"CR_DTIMES\" TIMESTAMP NOT NULL, \"UPD_BY\" VARCHAR(32), \"UPD_DTIMES\" TIMESTAMP, "
				+ "\"IS_DELETED\" BOOLEAN, \"DEL_DTIMES\" TIMESTAMP)");
		jdbcTemplate.execute("ALTER TABLE \"REG\".\"GLOBAL_PARAM\" ADD CONSTRAINT \"PK_GLBPARM_CODE\" PRIMARY KEY (\"CODE\", \"LANG_CODE\")");
		jdbcTemplate.execute("CREATE TABLE \"REG\".\"CA_CERT_STORE\"(\"CERT_ID\" VARCHAR(36) NOT NULL, \"CERT_SUBJECT\" VARCHAR(500) NOT NULL, "
				+ "\"CERT_ISSUER\" VARCHAR(500) NOT NULL, \"ISSUER_ID\" VARCHAR(36) NOT NULL, \"CERT_NOT_BEFORE\" TIMESTAMP, "
				+ "\"CERT_NOT_AFTER\" TIMESTAMP, \"CRL_URI\" VARCHAR(120), \"CERT_DATA\" VARCHAR(3000), \"CERT_THUMBPRINT\" VARCHAR(100), "
				+ "\"CERT_SERIAL_NO\" VARCHAR(50), \"PARTNER_DOMAIN\" VARCHAR(36), \"CR_BY\" VARCHAR(256) NOT NULL, "
				+ "\"CR_DTIMES\" TIMESTAMP NOT NULL, \"UPD_BY\" VARCHAR(256), \"UPD_DTIMES\" TIMESTAMP, \"IS_DELETED\" BOOLEAN, "
				+ "\"DEL_DTIMES\" TIMESTAMP)");

		appRoot = tempFolder.newFolder("app");
		new File(appRoot, "lib").mkdirs();
		Files.write(new File(appRoot, "lib/registration-client.jar").toPath(), new byte[] { 1 });
		new File(appRoot, "db").mkdirs();
		Files.write(new File(appRoot, "db/service.properties").toPath(), new byte[] { 2 });
	}

	@After
	public void tearDown() {
		resetUpgradeState();
		for (String key : CONTEXT_KEYS) {
			ApplicationContext.map().remove(key);
		}
		try {
			DriverManager.getConnection(dbUrl + ";drop=true");
		} catch (SQLException expected) {
			// Derby reports a successful drop as an SQLException
		}
	}

	@Test
	public void upgradeFrom1202_addsCaCertTypeAndRecordsTheVersion() throws Exception {
		installedVersion("1.2.0.2");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");

		ResponseDTO response = upgrade();

		assertNotNull(response);
		assertNull(response.getErrorResponseDTOs());
		assertEquals(RegistrationConstants.SQL_EXECUTION_SUCCESS, response.getSuccessResponseDTO().getMessage());
		assertTrue("the 1.3.0 script must add CA_CERT_STORE.CA_CERT_TYPE", hasCaCertTypeColumn());
		assertEquals("the 1.3.0 script's MERGE must have run", "1",
				globalParam("mosip.kernel.partner.cacertificate.upload.minimumvalidity.month"));
		assertEquals("1.3.0-SNAPSHOT", globalParam(RegistrationConstants.SERVICES_VERSION_KEY));
		assertEquals("1.3.0-SNAPSHOT",
				ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SERVICES_VERSION_KEY));
	}

	@Test
	public void upgradeBacksUpLibDbAndManifestUnderTheNewVersion() throws Exception {
		installedVersion("1.2.0.2");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");

		upgrade();

		File[] backups = new File(appRoot, "BackUp").listFiles();
		assertNotNull(backups);
		assertEquals(1, backups.length);
		File backup = backups[0];
		assertTrue(backup.getName(), backup.getName().startsWith("1.3.0-SNAPSHOT_"));
		assertTrue(new File(backup, "lib/registration-client.jar").isFile());
		assertTrue(new File(backup, "db/service.properties").isFile());
		assertTrue(new File(backup, "MANIFEST.MF").isFile());
		assertEquals(backup.getAbsolutePath(), globalParam(RegistrationConstants.SOFTWARE_BACKUP_FOLDER));
	}

	@Test
	public void upgradeFrom1201_skipsTheCommentOnlyScriptAndStillAddsTheColumn() throws Exception {
		// Runs sql/1.2.0.2 (only a comment, which Derby rejects as a statement) before sql/1.3.0.
		installedVersion("1.2.0.1-SNAPSHOT");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");

		ResponseDTO response = upgrade();

		assertNull(response.getErrorResponseDTOs());
		assertTrue(hasCaCertTypeColumn());
		assertEquals("1.3.0-SNAPSHOT", globalParam(RegistrationConstants.SERVICES_VERSION_KEY));
	}

	@Test
	public void sameVersion_runsNothing() throws Exception {
		installedVersion("1.3.0-SNAPSHOT");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");

		assertNull(upgrade());
		assertFalse(hasCaCertTypeColumn());
	}

	@Test
	public void freshVersionZeroWithoutBackups_runsNothing() throws Exception {
		installedVersion("0");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");

		assertNull(upgrade());
		assertFalse(hasCaCertTypeColumn());
	}

	@Test
	public void versionZero_takesThePreviousVersionFromTheLatestBackup() throws Exception {
		installedVersion("0");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");
		File oldBackup = new File(appRoot, "BackUp/1.2.0.2_2026-09-24 10-00-00.000Z");
		oldBackup.mkdirs();
		writeManifest(oldBackup, "1.2.0.2");

		ResponseDTO response = upgrade();

		assertNull(response.getErrorResponseDTOs());
		assertTrue(hasCaCertTypeColumn());
		assertEquals("1.3.0-SNAPSHOT", globalParam(RegistrationConstants.SERVICES_VERSION_KEY));
	}

	@Test
	public void failingScript_runsItsRollbackAndReportsFailure() throws Exception {
		installedVersion("1.2.0.2");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");
		// Make the 1.3.0 ALTER fail: the column is already there.
		jdbcTemplate.execute("ALTER TABLE \"REG\".\"CA_CERT_STORE\" ADD COLUMN \"CA_CERT_TYPE\" VARCHAR(25)");

		ResponseDTO response = upgrade();

		assertNull(response.getSuccessResponseDTO());
		List<String> messages = new ArrayList<>();
		for (ErrorResponseDTO error : response.getErrorResponseDTOs()) {
			messages.add(error.getMessage());
		}
		// ClientApplication reports the first error; BACKUP_PREVIOUS_SUCCESS is the "failed and replaced" case.
		assertEquals(List.of(RegistrationConstants.BACKUP_PREVIOUS_SUCCESS, RegistrationConstants.SQL_EXECUTION_FAILURE), messages);
		assertFalse("the 1.3.0 rollback script drops the column", hasCaCertTypeColumn());
		assertEquals("the version must not advance on failure", "1.2.0.2", globalParam(RegistrationConstants.SERVICES_VERSION_KEY));
	}

	@Test
	public void upgrade_runsOnlyOnce() throws Exception {
		installedVersion("1.2.0.2");
		writeManifest(appRoot, "1.3.0-SNAPSHOT");
		assertNull("no outcome before it has run", SoftwareUpdateHandler.getLocalDatabaseUpgradeResult());

		ResponseDTO first = upgrade();
		// A second run would re-execute the ALTER, fail on the existing column and report an error.
		ResponseDTO second = upgrade();

		assertSame(first, second);
		assertSame(first, SoftwareUpdateHandler.getLocalDatabaseUpgradeResult());
		assertNull(second.getErrorResponseDTOs());
	}

	@Test
	public void missingManifest_runsNothing() throws Exception {
		installedVersion("1.2.0.2");

		assertNull(upgrade());
		assertFalse(hasCaCertTypeColumn());
	}

	@Test
	public void versionMappings_areSortedByReleaseOrder() throws Exception {
		Map<String, VersionMappings> mappings = ReflectionTestUtils.invokeMethod(BaseService.class,
				"getSortedVersionMappings", RegistrationConstants.VERSION_MAPPINGS_KEY);

		int previous = Integer.MIN_VALUE;
		for (VersionMappings mapping : mappings.values()) {
			assertTrue(mapping.getReleaseOrder() > previous);
			previous = mapping.getReleaseOrder();
		}
		assertEquals("1.3.0", mappings.get("1.3.0-SNAPSHOT").getDbVersion());
	}

	private ResponseDTO upgrade() {
		return SoftwareUpdateHandler.upgradeLocalDatabase(jdbcTemplate, appRoot, "BackUp");
	}

	/** The upgrade runs once per process; each test is a fresh "process". */
	private static void resetUpgradeState() {
		ReflectionTestUtils.setField(SoftwareUpdateHandler.class, "dbUpgradeRan", false);
		ReflectionTestUtils.setField(SoftwareUpdateHandler.class, "dbUpgradeResult", null);
	}

	/** What DaoConfig does at startup: the row exists and the value is mirrored into the application map. */
	private void installedVersion(String version) {
		jdbcTemplate.update("INSERT INTO REG.GLOBAL_PARAM (CODE, NAME, VAL, TYP, LANG_CODE, IS_ACTIVE, CR_BY, CR_DTIMES) "
				+ "VALUES (?, ?, ?, 'CONFIGURATION', 'eng', TRUE, 'SYSTEM', CURRENT_TIMESTAMP)",
				RegistrationConstants.SERVICES_VERSION_KEY, RegistrationConstants.SERVICES_VERSION_KEY, version);
		ApplicationContext.setGlobalConfigValueOf(RegistrationConstants.SERVICES_VERSION_KEY, version);
	}

	private String globalParam(String code) {
		List<String> values = jdbcTemplate.queryForList("SELECT VAL FROM REG.GLOBAL_PARAM WHERE CODE = ?", String.class, code);
		return values.isEmpty() ? null : values.get(0);
	}

	private boolean hasCaCertTypeColumn() throws SQLException {
		try (java.sql.Connection connection = DriverManager.getConnection(dbUrl);
				ResultSet columns = connection.getMetaData().getColumns(null, "REG", "CA_CERT_STORE", "CA_CERT_TYPE")) {
			return columns.next();
		}
	}

	private static void writeManifest(File dir, String version) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
		try (FileOutputStream out = new FileOutputStream(new File(dir, "MANIFEST.MF"))) {
			manifest.write(out);
		}
	}
}
