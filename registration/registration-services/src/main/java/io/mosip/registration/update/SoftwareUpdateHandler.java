package io.mosip.registration.update;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.micrometer.core.annotation.Counted;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.VersionMappings;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;

/**
 * This class will update the application based on comapring the versions of the
 * jars from the Manifest. The comparison will be done by comparing the Local
 * Manifest and the meta-inf.xml file. If there is any updation available in the
 * jar then the new jar gets downloaded and the old gets archived.
 * 
 * @author YASWANTH S
 *
 */
@Component
public class SoftwareUpdateHandler extends BaseService {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(SoftwareUpdateHandler.class);
	private static final String SLASH = "/";
	private static final String manifestFile = "MANIFEST.MF";
	/** Detached signature over ./MANIFEST.MF's bytes; must be replaced whenever the manifest is. */
	private static final String manifestSignatureFile = "MANIFEST.MF.sig";
	// Upper bound on the detached signature body. A SHA256withRSA signature is tiny (RSA-4096 ->
	// 512 bytes), so anything larger is not a signature -- typically an HTML error page or a
	// redirect body from the upgrade server. This MUST match the launcher's own MAX_SIGNATURE_BYTES:
	// a larger body accepted here would be written to ./MANIFEST.MF.sig and then rejected by the
	// launcher on the next start as Case B, which re-downloads nothing and cannot recover.
	private static final int MAX_SIGNATURE_BYTES = 1024;
	private static final String libFolder = "lib";
	private static final String dbFolder = "db";
	private static final String binFolder = "bin";
	private static final String lastUpdatedTag = "lastUpdated";
	private static final String SQL = "sql";
	private static final String exectionSqlFile = "initial_db_scripts.sql";
	private static final String rollBackSqlFile = "rollback_scripts.sql";
	private static final String versionTag = "version";
	private static final String MOSIP_SERVICES = "registration-services";
	private static final String MOSIP_CLIENT = "registration-client";
	private static final String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
	private static final String EXTERNAL_DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

	/** Guards against two concurrent upgrades now that the UI stays usable during the download. */
	private final AtomicBoolean upgradeInProgress = new AtomicBoolean(false);

	private String currentVersion;
	private String latestVersion;
	private Manifest localManifest;
	private Manifest serverManifest;
	private String latestVersionReleaseTimestamp;

	@Value("${mosip.reg.rollback.path}")
	private String backUpPath;

	@Value("${mosip.reg.client.url}")
	private String serverRegClientURL;

	@Value("${mosip.reg.xml.file.url}")
	private String serverMosipXmlFileUrl;

	@Autowired
	private GlobalParamService globalParamService;
	
	@Autowired
	private AuditManagerService auditFactory;

	// ---------------------------------------------------------------------------------------------------
	// Local DB upgrade (version-mapped sql/<dbVersion>/ scripts).
	//
	// Static and plain JDBC on purpose: it runs from DaoConfig.entityManagerFactory(), after the datasource
	// is up but BEFORE Hibernate and any JPA-backed bean exists. Beans may query the schema while the Spring
	// context is still being created (the keymanager library's PartnerCertificateManagerServiceImpl reads
	// CA_CERT_STORE.CA_CERT_TYPE in its init method), so an upgrade run once the context is up never gets
	// the chance on a database that still needs it. This bean instance cannot be used there either: through
	// BaseService it depends on JPA services, which depend on the entityManagerFactory being built.
	// ---------------------------------------------------------------------------------------------------

	private static final String UPDATE_GLOBAL_PARAM = "UPDATE REG.GLOBAL_PARAM SET VAL = ?, NAME = ?, IS_ACTIVE = TRUE, "
			+ "UPD_BY = ?, UPD_DTIMES = ? WHERE CODE = ? AND LANG_CODE = ?";
	private static final String INSERT_GLOBAL_PARAM = "INSERT INTO REG.GLOBAL_PARAM (CODE, NAME, VAL, TYP, LANG_CODE, "
			+ "IS_ACTIVE, CR_BY, CR_DTIMES, UPD_BY, UPD_DTIMES) VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?)";

	// Inputs of the run in progress; the static counterparts of the jdbcTemplate/backUpPath the instance code uses.
	private static JdbcTemplate dbJdbcTemplate;
	private static File dbAppRoot;
	private static String dbBackUpPath;

	private static boolean dbUpgradeRan;
	private static ResponseDTO dbUpgradeResult;

	/**
	 * Runs the pending DB upgrade scripts, once per process; later calls return the first outcome.
	 *
	 * @param jdbcTemplate template over the (already booted) local database
	 * @param appRoot      application root holding MANIFEST.MF, lib/, db/, bin/
	 * @param backUpPath   backup folder (mosip.reg.rollback.path), relative to appRoot
	 * @return null when nothing needed upgrading, otherwise a success or error response
	 */
	public static synchronized ResponseDTO upgradeLocalDatabase(JdbcTemplate jdbcTemplate, File appRoot, String backUpPath) {
		if (!dbUpgradeRan) {
			dbJdbcTemplate = jdbcTemplate;
			dbAppRoot = appRoot;
			dbBackUpPath = backUpPath;
			dbUpgradeResult = updateDerbyDB();
			dbUpgradeRan = true;
		}
		return dbUpgradeResult;
	}

	/** Outcome of {@link #upgradeLocalDatabase}; null when nothing needed upgrading (or it has not run). */
	public static synchronized ResponseDTO getLocalDatabaseUpgradeResult() {
		return dbUpgradeResult;
	}

	private static ResponseDTO updateDerbyDB() {
		String currentVersion = readManifestVersion();
		String version = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SERVICES_VERSION_KEY);
		LOGGER.info("Inside updateDerbyDB currentVersion: {} and {} : {}", currentVersion,
				RegistrationConstants.SERVICES_VERSION_KEY, version);

		Map<String, VersionMappings> versionMappings;
		try {
			versionMappings = getSortedVersionMappings(RegistrationConstants.VERSION_MAPPINGS_KEY);
		} catch (Exception exception) {
			LOGGER.error("Exception in parsing the version-mappings: ", exception);
			return addDbUpgradeError(new ResponseDTO(), RegistrationConstants.VERSION_MAPPINGS_ERROR);
		}

		if (version == null || version.isEmpty() || version.equals("0")) {
			version = setupPreviousVersion(version, versionMappings);
		}

		if (version != null && !version.trim().equals("0") && currentVersion != null
				&& !currentVersion.equalsIgnoreCase(version)) {
			return executeSqlFile(currentVersion, version, versionMappings);
		}
		return null;
	}

	private static String readManifestVersion() {
		File localManifestFile = dbFile(manifestFile);
		if (!localManifestFile.exists()) {
			LOGGER.error("Local manifest not found: {}", localManifestFile.getAbsolutePath());
			return null;
		}
		try (FileInputStream inputStream = new FileInputStream(localManifestFile)) {
			return new Manifest(inputStream).getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);
		} catch (IOException exception) {
			LOGGER.error("Failed to read the local manifest", exception);
			return null;
		}
	}

	private static String setupPreviousVersion(String version, Map<String, VersionMappings> versionMappings) {
		File file = dbFile(dbBackUpPath);
		LOGGER.info("Backup Path found: {}", file.exists());
		if (!file.exists()) {
			LOGGER.info("Backup folder not found, returning the version as the same: {}", version);
			return version;
		}
		Map<Integer, String> backupVersions = new TreeMap<>(Collections.reverseOrder());
		File[] backUpFolders = file.listFiles();
		for (File backUpFolder : backUpFolders == null ? new File[0] : backUpFolders) {
			File localManifestFile = new File(backUpFolder, manifestFile);
			if (localManifestFile.exists()) {
				try (FileInputStream inputStream = new FileInputStream(localManifestFile)) {
					String backupVersion = new Manifest(inputStream).getMainAttributes()
							.getValue(Attributes.Name.MANIFEST_VERSION);
					// Key the backups by releaseOrder so the highest one is the latest previous version.
					if (versionMappings.containsKey(backupVersion)) {
						backupVersions.put(versionMappings.get(backupVersion).getReleaseOrder(), backupVersion);
					}
				} catch (IOException exception) {
					LOGGER.error("Exception while reading backed up manifest file: ", exception);
				}
			}
		}
		if (!backupVersions.isEmpty()) {
			return backupVersions.entrySet().iterator().next().getValue();
		}
		return version;
	}

	private static ResponseDTO executeSqlFile(String currentVersion, String previousVersion,
			Map<String, VersionMappings> versionMappings) {
		LOGGER.info("DB-Script files execution started from previous version : {} , To Current Version : {}",
				previousVersion, currentVersion);

		// Only the versions released after the previous one need their scripts run.
		if (versionMappings.containsKey(previousVersion)) {
			Integer previousVersionReleaseOrder = versionMappings.get(previousVersion).getReleaseOrder();
			versionMappings.entrySet().removeIf(versionMapping -> versionMapping.getValue().getReleaseOrder() <= previousVersionReleaseOrder);
		}

		ResponseDTO responseDTO = new ResponseDTO();
		List<String> fullSyncEntitiesList = new ArrayList<>();

		for (Entry<String, VersionMappings> entry : versionMappings.entrySet()) {
			try {
				LOGGER.info("DB Script files execution started for the version : {}", entry.getKey());
				executeSQL(entry.getValue().getDbVersion(), previousVersion);
				// Backing up the DB with ongoing upgrade version name
				String date = new Timestamp(System.currentTimeMillis()).toString().replace(":", "-") + "Z";
				dbUpgradeBackUpSetup(new File(dbFile(dbBackUpPath), entry.getKey() + "_" + date));
				previousVersion = entry.getKey();
				saveGlobalParam(RegistrationConstants.SERVICES_VERSION_KEY, entry.getKey());
				String fullSyncEntities = entry.getValue().getFullSyncEntities();
				if (fullSyncEntities != null && !fullSyncEntities.isBlank()) {
					fullSyncEntitiesList.add(fullSyncEntities);
				}
			} catch (Throwable exception) {
				LOGGER.error("Error while executing SQL files for upgrade : ", exception);
				responseDTO = rollBack(responseDTO);
				addDbUpgradeError(responseDTO, RegistrationConstants.SQL_EXECUTION_FAILURE);
				return responseDTO;
			}
		}

		if (!fullSyncEntitiesList.isEmpty()) {
			LOGGER.info("Saving the list of fullSyncEntities mentioned in version-mappings..");
			saveGlobalParam(RegistrationConstants.UPGRADE_FULL_SYNC_ENTITIES, String.join(",", fullSyncEntitiesList));
		}
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setCode(RegistrationConstants.ALERT_INFORMATION);
		successResponseDTO.setMessage(RegistrationConstants.SQL_EXECUTION_SUCCESS);
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		LOGGER.info("DB-Script files execution completed");
		return responseDTO;
	}

	private static ResponseDTO rollBack(ResponseDTO responseDTO) {
		try {
			String backupPath = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SOFTWARE_BACKUP_FOLDER);
			if (backupPath != null) {
				dbUpgradeRollBackSetup(new File(backupPath));
			}
			addDbUpgradeError(responseDTO, RegistrationConstants.BACKUP_PREVIOUS_SUCCESS);
		} catch (Throwable exception) {
			LOGGER.error("Failed to execute db rollback scripts", exception);
		}
		return responseDTO;
	}

	private static void executeSQL(String dbVersion, String previousVersion) throws RegBaseCheckedException {
		boolean isExecutionSuccess = false;
		boolean isRollBackSuccess = false;
		try {
			LOGGER.info("Checking Started : " + dbVersion + SLASH + exectionSqlFile);
			execute(SQL + SLASH + dbVersion + SLASH + exectionSqlFile);
			isExecutionSuccess = true;
			LOGGER.info("Checking completed : " + dbVersion + SLASH + exectionSqlFile);
		} catch (RuntimeException | IOException exception) {
			LOGGER.error("Failed to execute db upgrade scripts", exception);
		}
		if (!isExecutionSuccess) {
			try {
				LOGGER.info("Rollback started : " + dbVersion + SLASH + rollBackSqlFile);
				execute(SQL + SLASH + dbVersion + SLASH + rollBackSqlFile);
				isRollBackSuccess = true;
				LOGGER.info("Rollback completed : " + dbVersion + SLASH + rollBackSqlFile);
			} catch (RuntimeException | IOException exception) {
				LOGGER.error("Failed to execute db rollback scripts", exception);
			}

			if (!isRollBackSuccess) {
				LOGGER.info("Trying to rollback DB from the backup folder as rollback scripts failed for the version: " + dbVersion);
				dbRollBackSetup(previousVersion);
			}
			throw new RegBaseCheckedException();
		}
	}

	private static void dbRollBackSetup(String previousVersion) {
		LOGGER.info("Replacing DB backup started for the version: " + previousVersion);
		File file = dbFile(dbBackUpPath);
		LOGGER.info("Backup Path found : " + file.exists());

		if (!file.exists()) {
			LOGGER.info("Backup folder not found, db backup stopped");
			return;
		}

		File[] backUpFolders = file.listFiles();
		for (File backUpFolder : backUpFolders == null ? new File[0] : backUpFolders) {
			if (backUpFolder.getName().contains(previousVersion)) {
				try {
					FileUtils.copyDirectory(new File(backUpFolder, dbFolder), dbFile(dbFolder));
					LOGGER.info("Replacing DB backup completed for the version: " + previousVersion);
				} catch (Exception exception) {
					LOGGER.error("Exception in backing up the DB folder: ", exception);
				}
				break;
			}
		}
	}

	private static void execute(String path) throws IOException {
		try (InputStream inputStream = SoftwareUpdateHandler.class.getClassLoader().getResourceAsStream(path)) {
			LOGGER.info(inputStream != null ? path + " found" : path + " Not Found");
			if (inputStream != null) {
				runSqlFile(inputStream);
			}
		}
	}

	private static void runSqlFile(InputStream inputStream) throws IOException {
		LOGGER.info("Execution started sql file");
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder sb = new StringBuilder();
			String str;
			while ((str = bufferedReader.readLine()) != null) {
				sb.append(str + "\n ");
			}
			for (String statement : sb.toString().split(";")) {
				// Derby rejects a statement that is only comments ("Syntax error: Encountered <EOF>"), which is
				// all a no-change script such as sql/1.2.0.2 holds. A comment ahead of real SQL is accepted.
				if (!isBlankOrCommentOnly(statement)) {
					LOGGER.info("Executing Statment : " + statement);
					dbJdbcTemplate.execute(statement);
				}
			}
		}
		LOGGER.info("Execution completed sql file");
	}

	private static boolean isBlankOrCommentOnly(String statement) {
		for (String line : statement.split("\n")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
				return false;
			}
		}
		return true;
	}

	/** backUpSetup() for the DB upgrade: the same backup, but the folder is recorded over JDBC. */
	private static void dbUpgradeBackUpSetup(File backUpFolder) throws io.mosip.kernel.core.exception.IOException {
		LOGGER.info("Backup of current version started {}", backUpFolder);
		File lib = new File(backUpFolder, libFolder);
		lib.mkdirs();
		File db = new File(backUpFolder, dbFolder);
		db.mkdirs();

		// Installs from reg-client.zip have no bin/, and copyDirectory throws on a missing source.
		File binSource = dbFile(binFolder);
		if (binSource.exists()) {
			FileUtils.copyDirectory(binSource, new File(backUpFolder, binFolder));
		}
		FileUtils.copyDirectory(dbFile(libFolder), lib);
		FileUtils.copyDirectory(dbFile(dbFolder), db);
		FileUtils.copyFile(dbFile(manifestFile), new File(backUpFolder, manifestFile));

		File[] backups = dbFile(dbBackUpPath).listFiles();
		for (File backUpFile : backups == null ? new File[0] : backups) {
			if (!backUpFile.getAbsolutePath().equals(backUpFolder.getAbsolutePath())) {
				FileUtils.deleteDirectory(backUpFile);
			}
		}

		saveGlobalParam(RegistrationConstants.SOFTWARE_BACKUP_FOLDER, backUpFolder.getAbsolutePath());
		LOGGER.info("Backup of current version completed at {}", backUpFolder.getAbsolutePath());
	}

	/** rollBackSetup() for the DB upgrade, resolving against the app root it was given. */
	private static void dbUpgradeRollBackSetup(File backUpFolder) throws io.mosip.kernel.core.exception.IOException {
		LOGGER.info("Replacing Backup of current version started");
		if (backUpFolder.exists()) {
			File binBackup = new File(backUpFolder, binFolder);
			if (binBackup.exists()) {
				FileUtils.copyDirectory(binBackup, dbFile(binFolder));
			}
			FileUtils.copyDirectory(new File(backUpFolder, libFolder), dbFile(libFolder));
			FileUtils.copyFile(new File(backUpFolder, manifestFile), dbFile(manifestFile));
		}
		LOGGER.info("Replacing Backup of current version completed");
	}

	/**
	 * The JDBC equivalent of GlobalParamService.update, which is JPA-backed and so unavailable here: update
	 * the row, or insert it when missing, and mirror the value into the application map.
	 */
	private static void saveGlobalParam(String code, String val) {
		if (code == null || val == null) {
			LOGGER.error("Not Update global param because of code or val is null value");
			return;
		}
		Timestamp now = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
		int updated = dbJdbcTemplate.update(UPDATE_GLOBAL_PARAM, val, code, RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM,
				now, code, RegistrationConstants.ENGLISH_LANG_CODE);
		if (updated == 0) {
			dbJdbcTemplate.update(INSERT_GLOBAL_PARAM, code, code, val, RegistrationConstants.CONFIGURATION,
					RegistrationConstants.ENGLISH_LANG_CODE, RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM, now,
					RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM, now);
		}
		ApplicationContext.setGlobalConfigValueOf(code, val);
	}

	private static ResponseDTO addDbUpgradeError(ResponseDTO response, String message) {
		List<ErrorResponseDTO> errorResponses = response.getErrorResponseDTOs() != null
				? response.getErrorResponseDTOs()
				: new LinkedList<>();
		ErrorResponseDTO errorResponse = new ErrorResponseDTO();
		errorResponse.setCode(RegistrationConstants.ERROR);
		errorResponse.setMessage(message);
		errorResponses.add(errorResponse);
		response.setErrorResponseDTOs(errorResponses);
		return response;
	}

	private static File dbFile(String path) {
		File file = new File(path);
		return file.isAbsolute() ? file : new File(dbAppRoot, path);
	}

	// --------------------------------------- end of local DB upgrade ---------------------------------------

	/**
	 * It will check whether any software updates are available or not.
	 * <p>
	 * The check will be done by comparing the Local Manifest file version with the
	 * version of the server meta-inf.xml file
	 * </p>
	 * 
	 * @return Boolean true - If there is any update available. false - If no
	 *         updates available
	 */
	@Counted(recordFailuresOnly = true)
	public boolean hasUpdate() {
		LOGGER.info("Checking for any new updates");
		try {
			return !getCurrentVersion().equals(getLatestVersion());
		} catch (Throwable exception) {
			LOGGER.error("Failed to check if update is available or not", exception);
			return false;
		}
	}

	/**
	 * 
	 * @return Returns the current version which is read from the server meta-inf
	 *         file.
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private String getLatestVersion() throws IOException, ParserConfigurationException, SAXException, RegBaseCheckedException {
		LOGGER.info("Checking for latest version started");
		// Get latest version using meta-inf.xml
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setFeature(FEATURE, true);
		documentBuilderFactory.setFeature(EXTERNAL_DTD_FEATURE, false);
		documentBuilderFactory.setXIncludeAware(false);
		documentBuilderFactory.setExpandEntityReferences(false);
		DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
		try(InputStream in = SoftwareUpdateUtil.download(getURL(serverMosipXmlFileUrl))) {
			org.w3c.dom.Document metaInfXmlDocument = db.parse(in);
			setLatestVersion(getElementValue(metaInfXmlDocument, versionTag));
			setLatestVersionReleaseTimestamp(getElementValue(metaInfXmlDocument, lastUpdatedTag));
		}
		LOGGER.info("Checking for latest version completed");
		return latestVersion;
	}

	private String getElementValue(Document metaInfXmlDocument, String tagName) {
		NodeList list = metaInfXmlDocument.getDocumentElement().getElementsByTagName(tagName);
		String val = null;
		if (list != null && list.getLength() > 0) {
			NodeList subList = list.item(0).getChildNodes();

			if (subList != null && subList.getLength() > 0) {
				// Set Latest Version
				val = subList.item(0).getNodeValue();
			}
		}
		return val;
	}

	/**
	 * Get Current version of setup
	 * 
	 * @return current version
	 */
	public String getCurrentVersion() {
		LOGGER.info("Checking for current version started...");
		// Get Local manifest file
		try {
			if (getLocalManifest() != null) {
				setCurrentVersion((String) localManifest.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION));
			}
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(exception.getMessage(), exception);
		}
		LOGGER.info("Checking for current version completed : {}", currentVersion);
		return currentVersion;
	}

	public UpgradeOutcome doSoftwareUpgrade() {
		return doSoftwareUpgrade(UpgradeProgressListener.NO_OP);
	}

	/** True while an upgrade is running, so callers can decline to start a second one. */
	public boolean isUpgradeInProgress() {
		return upgradeInProgress.get();
	}

	/**
	 * As {@link #doSoftwareUpgrade()}, reporting overall download progress so the caller can show a
	 * determinate progress bar while the operator keeps using the application. Callbacks arrive on the
	 * calling thread, which is never the JavaFX application thread.
	 *
	 * @param progressListener receives progress updates; {@link UpgradeProgressListener#NO_OP} to ignore
	 */
	public UpgradeOutcome doSoftwareUpgrade(UpgradeProgressListener progressListener) {
		// Refuse to run two upgrades at once. The UI no longer disables the pane for the whole download,
		// so a second click (or another entry point) could otherwise start a concurrent run: backUpSetup
		// deletes every backup folder except its own -- destroying the first run's rollback point -- and
		// both runs would write ./MANIFEST.MF and the same .artifacts/<name>.part files, splicing bytes
		// from two attempts into one artifact.
		if (!upgradeInProgress.compareAndSet(false, true)) {
			// Distinct from FAILED on purpose: "already running" is not an error, and reporting it as
			// one made the caller tear down the FIRST run's progress display and offer a retry.
			LOGGER.warn("A software upgrade is already in progress; ignoring this request");
			return UpgradeOutcome.ALREADY_IN_PROGRESS;
		}
		try {
			LOGGER.info("Updating latest version started");
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String date = timestamp.toString().replace(":", "-") + "Z";
			File backupFolder = new File(backUpPath + SLASH + getCurrentVersion() + "_" + date);

			try {
				// Back Current Application
				backUpSetup(backupFolder);
				update(progressListener == null ? UpgradeProgressListener.NO_OP : progressListener);
				LOGGER.info("Updating to latest version completed.");
				return UpgradeOutcome.COMPLETED;
			} catch (Throwable t) {
				LOGGER.error("Failed with software upgrade", t);
			}

			try {
				rollBackSetup(backupFolder);
			} catch (io.mosip.kernel.core.exception.IOException e) {
				LOGGER.error("Failed to rollback setup", e);
			}
			// Report the failure instead of swallowing it. The caller previously had no way to tell a
			// rolled-back failure from a success, so the operator was told the update completed either
			// way -- and, on a forced update, was restarted into the rolled-back install.
			return UpgradeOutcome.FAILED;
		} finally {
			upgradeInProgress.set(false);
		}
	}

	/**
	 * <p>
	 * Checks whteher the update is available or not
	 * </p>
	 * <p>
	 * If the Update is available:
	 * </p>
	 * <p>
	 * If the jars needs to be added/updated in the local
	 * </p>
	 * <ul>
	 * <li>Take the back-up of the current jars</li>
	 * <li>Download the jars from the server and add/update it in the local</li>
	 * </ul>
	 * <p>
	 * If the jars needs to be deleted in the local
	 * </p>
	 * <ul>
	 * <li>Take the back-up of the current jars</li>
	 * <li>Delete that particular jar from the local</li>
	 * </ul>
	 * <p>
	 * If there is any error occurs while updation then the restoration of the jars
	 * will happen by taking the back-up jars
	 * </p>
	 * 
	 * @throws Exception
	 *             - IOException
	 */
	@Counted(recordFailuresOnly = true)
	private void update(UpgradeProgressListener progressListener) throws Exception {
		// Fetch the server manifest, but do NOT adopt it yet -- see the write at the end of this method.
		setServerManifest();

		// From 1.3.0 the handler is download-and-record only: artifacts are staged into .artifacts/
		// (resumable, never into lib/) and the .artifacts/ directory is intentionally NOT cleared so an
		// interrupted download can resume from its .part file on the next run. Unzipping and applying to
		// lib/ is the launcher's / run.bat's job (T2), not the handler's.
		Map<String, Attributes> localAttributes = serverManifest.getEntries();
		// Overall progress is weighted per artifact rather than per byte: the manifest carries no sizes,
		// so the total download volume is not known up front. Within an artifact the byte-level callback
		// refines the fraction, which keeps the bar moving through the large jre21.zip entry.
		final int totalArtifacts = localAttributes.size();
		int completedArtifacts = 0;
		for (Map.Entry<String, Attributes> entry : localAttributes.entrySet()) {
			File staged = new File(SoftwareUpdateUtil.ARTIFACTS_DIRECTORY + SLASH + entry.getKey());
			String url = serverRegClientURL + latestVersion + SLASH + libFolder + SLASH + entry.getKey();
			final String artifactName = entry.getKey();
			final int alreadyDone = completedArtifacts;
			ResumableDownloader.ProgressListener byteListener = (bytesDone, totalBytes) -> {
				if (totalBytes <= 0) {
					// Server sent no usable Content-Length. Propagate "unknown" so the bar goes
					// indeterminate; reporting a determinate fraction here would freeze it at a fixed
					// percentage for the whole of a chunked ~200MB transfer.
					progressListener.onProgress(-1.0d, artifactName);
					return;
				}
				double within = Math.min(1.0d, (double) bytesDone / (double) totalBytes);
				progressListener.onProgress((alreadyDone + within) / totalArtifacts, artifactName);
			};

			// Skip anything already staged in .artifacts/ AND intact. The previous check tested
			// lib/<entry>, which is meaningless from 1.3.0: the design guarantees nothing is downloaded
			// into lib/ any more, so it was always false and EVERY artifact was re-fetched on every
			// attempt -- including the ~200MB jre21.zip on a retry that had already completed it.
			if (staged.exists() && SoftwareUpdateUtil.validateJarChecksum(staged, entry.getValue())) {
				LOGGER.info("{} already staged in .artifacts/ and intact, skipping download", entry.getKey());
				completedArtifacts++;
				progressListener.onProgress((double) completedArtifacts / totalArtifacts, artifactName);
				continue;
			}

			LOGGER.info("Downloading {}", entry.getKey());
			SoftwareUpdateUtil.downloadResumable(url, SoftwareUpdateUtil.ARTIFACTS_DIRECTORY, entry.getKey(), byteListener);
			// Verify what actually landed. The download reports success on byte count alone, and the 416
			// path finalizes a .part purely because its LENGTH matches the server total -- so a stale
			// artifact of the same size is adopted silently. Without this check update() would go on to
			// commit ./MANIFEST.MF, doSoftwareUpgrade would report COMPLETED, and the corruption would
			// surface only on the next start, where the launcher's integrity gate fails and Case B
			// re-downloads nothing: an unbootable client. Failing here instead leaves the OLD manifest in
			// place, so the client still boots and the operator can simply retry -- and the retry's skip
			// check re-hashes the bad artifact and re-fetches it.
			if (!SoftwareUpdateUtil.validateJarChecksum(staged, entry.getValue())) {
				throw new RegBaseCheckedException("REG-BUILD-007",
						"Downloaded artifact failed its manifest checksum: " + entry.getKey());
			}
			LOGGER.info("Successfully downloaded the file : {}", entry.getKey());
			completedArtifacts++;
			progressListener.onProgress((double) completedArtifacts / totalArtifacts, artifactName);
		}

		// Adopt the new manifest ONLY now that every artifact is on disk. Writing it up front (the
		// previous behaviour) meant an interrupted download left ./MANIFEST.MF claiming the new version
		// while .artifacts/ was incomplete -- on the next start the launcher saw root != lib, entered the
		// migration path, failed on the missing artifact and exited, and the client could never boot far
		// enough to resume the download. Deferring the write keeps an interrupted attempt fully
		// resumable: the version still reads as the old one, so the client starts normally and the
		// operator can retry, picking up the .part files where they left off.
		// The detached signature is fetched BEFORE anything is written and committed together with the
		// manifest. ./MANIFEST.MF.sig signs the manifest's BYTES, so a new manifest left paired with the
		// previous signature fails the launcher's signature check on the very next start -- and that is
		// Case B, which deliberately re-downloads nothing, so the client cannot recover. Every upgrade
		// after 1.3.0 would brick on a SUCCESSFUL update. (The 1.2.x -> 1.3.0 hop hides it: no
		// ./MANIFEST.MF.sig exists yet, so the launcher's Case C fetches a fresh one.)
		byte[] serverSignature = downloadRootManifestSignature();
		commitRootManifest(serverSignature);
		// Deliberately NOT reloading localManifest here. ./MANIFEST.MF on disk is now the PENDING
		// version; the jars actually executing in this JVM are still the old ones, and stay that way
		// until the operator restarts -- which they may defer indefinitely, now that the restart is a
		// prompt rather than automatic. Refreshing the in-memory manifest would make
		// getCurrentVersion() report the new version to everything that asks: packet metadata
		// (META_CLIENT_VERSION), the version sent on every sync/REST call, and the UI labels. Packets
		// would be stamped with a version that never ran. The on-disk manifest is picked up naturally
		// on the next start, which is exactly when it becomes true.

		auditFactory.audit(AuditEvent.CLIENT_UPGRADE_JARS_DOWNLOADED, Components.CLIENT_UPGRADE, 
				RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());
		
		setServerManifest(null);
		setLatestVersion(null);

		// Update global param of software update flag as false
		globalParamService.update(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE,
				RegistrationConstants.DISABLE);
		globalParamService.update(RegistrationConstants.LAST_SOFTWARE_UPDATE,
				String.valueOf(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())));
	}

	/**
	 * Fetches the detached signature for the server manifest. Read fully into memory before any local
	 * file is touched, so a network failure here aborts the upgrade with the old manifest and old
	 * signature still paired and valid.
	 */
	private byte[] downloadRootManifestSignature() throws IOException, RegBaseCheckedException {
		String url = serverRegClientURL + latestVersion + SLASH + manifestSignatureFile;
		LOGGER.info("Downloading root manifest signature from {}", url);
		try (InputStream in = SoftwareUpdateUtil.download(url);
				ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			if (in == null) {
				// Guarded exactly as setServerManifest guards the identical call. Without it a null body
				// is an NPE swallowed by doSoftwareUpgrade's catch(Throwable), so the operator gets a
				// bare stack trace instead of a message naming the URL that failed.
				throw new IOException("No response body for " + url);
			}
			byte[] chunk = new byte[8192];
			int read;
			while ((read = in.read(chunk)) != -1) {
				buffer.write(chunk, 0, read);
				if (buffer.size() > MAX_SIGNATURE_BYTES) {
					throw new IOException("Downloaded " + manifestSignatureFile + " exceeds "
							+ MAX_SIGNATURE_BYTES + " bytes; refusing to adopt it as a signature");
				}
			}
			if (buffer.size() == 0) {
				throw new IOException("Downloaded " + manifestSignatureFile + " is empty");
			}
			return buffer.toByteArray();
		}
	}

	/**
	 * Replaces ./MANIFEST.MF and ./MANIFEST.MF.sig together.
	 * <p>
	 * The signature is <b>removed first</b>, deliberately. The two files cannot be swapped in one atomic
	 * step, so some crash window is unavoidable -- the choice is which state it leaves behind. A
	 * mismatched manifest/signature pair is unrecoverable (the launcher's Case B aborts and re-downloads
	 * nothing), whereas a MISSING signature is recoverable by design: Case C fetches the signature for
	 * whichever manifest version is on disk. Deleting first means every crash window lands on the
	 * recoverable state, whether the manifest has been replaced yet or not.
	 * <p>
	 * Each file is staged to a temp name and moved into place, so a failure mid-write can never leave a
	 * truncated manifest behind.
	 */
	private void commitRootManifest(byte[] signature) throws IOException {
		File manifestTarget = new File(manifestFile);
		File signatureTarget = new File(manifestSignatureFile);
		File manifestTmp = new File(manifestFile + ".tmp");
		File signatureTmp = new File(manifestSignatureFile + ".tmp");

		try (FileOutputStream out = new FileOutputStream(manifestTmp)) {
			serverManifest.write(out);
		}
		Files.write(signatureTmp.toPath(), signature);

		Files.deleteIfExists(signatureTarget.toPath());
		Files.move(manifestTmp.toPath(), manifestTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.move(signatureTmp.toPath(), signatureTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
		LOGGER.info("Adopted the new root manifest and its signature");
	}

	private void backUpSetup(File backUpFolder) throws io.mosip.kernel.core.exception.IOException {
		LOGGER.info("Backup of current version started {}", backUpFolder);
		// bin backup folder -- created by copyDirectory only when bin/ exists, so rollBackSetup can tell
		File bin = new File(backUpFolder.getAbsolutePath() + SLASH + binFolder);

		// lib backup folder
		File lib = new File(backUpFolder.getAbsolutePath() + SLASH + libFolder);
		lib.mkdirs();

		// db backup folder
		File db = new File(backUpFolder.getAbsolutePath() + SLASH + dbFolder);
		db.mkdirs();

		// manifest backup file
		File manifest = new File(backUpFolder.getAbsolutePath() + SLASH + manifestFile);

		// reg-client.zip ships no bin/, and copyDirectory throws on a missing source, which used to
		// abort every upgrade of a fresh install before anything was downloaded.
		File binSource = new File(binFolder);
		if (binSource.exists()) {
			FileUtils.copyDirectory(binSource, bin);
		}
		FileUtils.copyDirectory(new File(libFolder), lib);
		FileUtils.copyDirectory(new File(dbFolder), db);
		FileUtils.copyFile(new File(manifestFile), manifest);

		for (File backUpFile : new File(backUpPath).listFiles()) {
			if (!backUpFile.getAbsolutePath().equals(backUpFolder.getAbsolutePath())) {
				FileUtils.deleteDirectory(backUpFile);
			}
		}

		globalParamService.update(RegistrationConstants.SOFTWARE_BACKUP_FOLDER,
				backUpFolder.getAbsolutePath());
		LOGGER.info("Backup of current version completed at {}", backUpFolder.getAbsolutePath());
	}

	private void setLocalManifest() throws RegBaseCheckedException {
		try {
			File localManifestFile = new File(manifestFile);
			if (localManifestFile.exists()) {
				// Close the stream. An unclosed FileInputStream keeps a Windows file handle on
				// ./MANIFEST.MF until GC, which blocks the atomic replace in commitRootManifest with
				// "The process cannot access the file because it is being used by another process" --
				// so the upgrade fails at the very last step, after every artifact downloaded.
				try (FileInputStream inputStream = new FileInputStream(localManifestFile)) {
					localManifest = new Manifest(inputStream);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load local manifest file", e);
			throw new RegBaseCheckedException("REG-BUILD-003", "Local Manifest not found", e);
		}
	}

	private void setServerManifest() throws RegBaseCheckedException {
		// Clear the field FIRST. It survives a failed run -- the reset at the end of update() is only
		// reached on success -- so a previous attempt's manifest would otherwise still be sitting here.
		serverManifest = null;
		String url = serverRegClientURL + latestVersion + SLASH + manifestFile;
		try (InputStream in = SoftwareUpdateUtil.download(url)) {
			if (in == null) {
				throw new IOException("No response body for " + url);
			}
			serverManifest = new Manifest(in);
		} catch (IOException e) {
			// Propagate instead of swallowing. Returning quietly here used to leave update() running
			// against whatever manifest was left over: it would download from the NEW version's URLs
			// but commit the OLD version's manifest, and doSoftwareUpgrade would still report success.
			// The boolean contract makes that silent mismatch actively misleading to the operator.
			LOGGER.error("Failed to load server manifest file", e);
			throw new RegBaseCheckedException("REG-BUILD-004",
					"Failed to download the server manifest from " + url, e);
		}
	}

	/**
	 * The latest version timestamp will be taken from the server meta-inf.xml file.
	 * This timestamp will the be parsed in this method.
	 * 
	 * @return timestamp
	 */
	public Timestamp getLatestVersionReleaseTimestamp() {

		Calendar calendar = Calendar.getInstance();

		String dateString = latestVersionReleaseTimestamp;

		int year = Integer.valueOf(dateString.charAt(0) + "" + dateString.charAt(1) + "" + dateString.charAt(2) + ""
				+ dateString.charAt(3));
		int month = Integer.valueOf(dateString.charAt(4) + "" + dateString.charAt(5));
		int date = Integer.valueOf(dateString.charAt(6) + "" + dateString.charAt(7));
		int hourOfDay = Integer.valueOf(dateString.charAt(8) + "" + dateString.charAt(9));
		int minute = Integer.valueOf(dateString.charAt(10) + "" + dateString.charAt(11));
		int second = Integer.valueOf(dateString.charAt(12) + "" + dateString.charAt(13));

		calendar.set(year, month - 1, date, hourOfDay, minute, second);

		return new Timestamp(calendar.getTime().getTime());
	}

	private void rollBackSetup(File backUpFolder) throws io.mosip.kernel.core.exception.IOException {
		LOGGER.info("Replacing Backup of current version started");
		if(backUpFolder.exists()) {
			File binBackup = new File(backUpFolder.getAbsolutePath() + SLASH + binFolder);
			if (binBackup.exists()) {
				FileUtils.copyDirectory(binBackup, new File(binFolder));
			}
			FileUtils.copyDirectory(new File(backUpFolder.getAbsolutePath() + SLASH + libFolder), new File(libFolder));
			FileUtils.copyFile(new File(backUpFolder.getAbsolutePath() + SLASH + manifestFile), new File(manifestFile));
		}
		LOGGER.info("Replacing Backup of current version completed");
	}

	public Map<String, String> getJarChecksum() {
		Map<String, String> checksumMap = new HashMap<>();
		if(localManifest != null) {
			Map<String, java.util.jar.Attributes> localEntries = localManifest.getEntries();
			List<String> keys = localEntries.keySet().stream().filter( k -> k.contains(MOSIP_CLIENT) || k.contains(MOSIP_SERVICES)).collect(Collectors.toList());
			for(String key : keys) {
				checksumMap.put(key, localEntries.get(key).getValue(Attributes.Name.CONTENT_TYPE));
			}
		}
		return checksumMap;
	}

	private String getURL(String urlPostFix) {
		String upgradeServerURL = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL);
		String url = String.format(urlPostFix, upgradeServerURL);
		url = serviceDelegateUtil.prepareURLByHostName(url);
		LOGGER.info("Upgrade server : {}", url);
		return url;
	}

	private void setServerManifest(Manifest serverManifest) {
		this.serverManifest = serverManifest;
	}

	private void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}

	private void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}

	public void setLatestVersionReleaseTimestamp(String latestVersionReleaseTimestamp) {
		this.latestVersionReleaseTimestamp = latestVersionReleaseTimestamp;
	}

	private Manifest getLocalManifest() throws RegBaseCheckedException {
		if(localManifest == null) { setLocalManifest(); }
		return localManifest;
	}
}
