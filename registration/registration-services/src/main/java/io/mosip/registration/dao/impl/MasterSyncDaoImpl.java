package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.entity.BiometricAttribute;
import io.mosip.registration.entity.DocumentCategory;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BlocklistedWordsRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationHierarchyRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.SyncJobControlRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;

/**
 * The implementation class of {@link MasterSyncDao}
 * 
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@Repository
@Transactional
public class MasterSyncDaoImpl implements MasterSyncDao {

	/** Object for Sync Status Repository. */
	@Autowired
	private SyncJobControlRepository syncStatusRepository;

	/** Object for Sync Biometric Attribute Repository. */
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;

	/** Object for Sync Blocklisted Words Repository. */
	@Autowired
	private BlocklistedWordsRepository blocklistedWordsRepository;

	/** Object for Sync Document Category Repository. */
	@Autowired
	private DocumentCategoryRepository documentCategoryRepository;

	/** Object for Sync Document Type Repository. */
	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	/** Object for Sync Location Repository. */
	@Autowired
	private LocationRepository locationRepository;

	/** Object for Sync Reason Category Repository. */
	@Autowired
	private ReasonCategoryRepository reasonCategoryRepository;

	/** Object for Sync Reason List Repository. */
	@Autowired
	private ReasonListRepository reasonListRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private LanguageRepository languageRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private SyncJobDefRepository syncJobDefRepository;

	@Autowired
	private LocationHierarchyRepository locationHierarchyRepository;

	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(MasterSyncDaoImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getMasterSyncStatus()
	 */
	@Override
	public SyncControl syncJobDetails(String synccontrol) {

		SyncControl syncControlResonse = null;

		LOGGER.info(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
				"DAO findByID method started");

		try {
			// find the user
			syncControlResonse = syncStatusRepository.findBySyncJobId(synccontrol);

		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_JOD_DETAILS,
					runtimeException.getMessage());
		}

		LOGGER.info(RegistrationConstants.MASTER_SYNC_JOD_DETAILS, APPLICATION_NAME, APPLICATION_ID,
				"DAO findByID method ended");

		return syncControlResonse;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#findLocationByLangCode(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public List<Location> findLocationByLangCode(int hierarchyLevel, String langCode) {
		return locationRepository.findByIsActiveTrueAndHierarchyLevelAndLangCode(hierarchyLevel, langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#findLocationByParentLocCode(java.lang
	 * .String)
	 */
	@Override
	public List<Location> findLocationByParentLocCode(String parentLocCode, String langCode) {
		return locationRepository.findByIsActiveTrueAndParentLocCodeAndLangCode(parentLocCode, langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getAllReasonCatogery()
	 */
	@Override
	public List<ReasonCategory> getAllReasonCatogery(String langCode) {
		return reasonCategoryRepository.findByIsActiveTrueAndLangCode(langCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getReasonList(java.util.List)
	 */
	@Override
	public List<ReasonList> getReasonList(String langCode, List<String> reasonCat) {
		return reasonListRepository.findByIsActiveTrueAndLangCodeAndReasonCategoryCodeIn(langCode, reasonCat);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MasterSyncDao#getBlockListedWords(java.lang.String)
	 */
	@Override
	public List<String> getBlockListedWords() {
		return blocklistedWordsRepository.findAllActiveBlockListedWords();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getDocumentCategories(java.lang.
	 * String)
	 */
	@Override
	public List<DocumentType> getDocumentTypes(List<String> docCode, String langCode) {
		return documentTypeRepository.findByIsActiveTrueAndLangCodeAndCodeIn(langCode, docCode);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getDocumentCategories(java.lang.
	 * String)
	 */
	@Override
	public DocumentType getDocumentType(String docCode, String langCode) {
		return documentTypeRepository.findByIsActiveTrueAndLangCodeAndCode(langCode, docCode);
	}


	public List<SyncJobDef> getSyncJobs() {
		return syncJobDefRepository.findAllByIsActiveTrue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.MasterSyncDao#getBiometricType(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public List<BiometricAttribute> getBiometricType(String langCode, List<String> biometricType) {
		return biometricAttributeRepository.findByLangCodeAndBiometricTypeCodeIn(langCode, biometricType);
	}

	public List<Language> getActiveLanguages() {

		return languageRepository.findAllByIsActiveTrue();
	}

	public List<DocumentCategory> getDocumentCategory() {
		return documentCategoryRepository.findAllByIsActiveTrue();
	}

	public List<Location> getLocationDetails() {
		return locationRepository.findAllByIsActiveTrue();
	}

	public List<Location> getLocationDetails(String langCode) {
		return locationRepository.findByIsActiveTrueAndLangCode(langCode);
	}

	public List<Location> getLocationDetails(String hierarchyName, String langCode) {
		return locationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(hierarchyName, langCode);
	}

	public Location getLocation(String code, String langCode) {
		return locationRepository.findByCodeAndLangCode(code, langCode);
	}

	@Override
	public List<LocationHierarchy> getAllLocationHierarchy(String langCode) {
		return locationHierarchyRepository.findAllByIsActiveTrueAndLangCode(langCode);
	}
	
	@Override
	public Long getLocationHierarchyCount() {
		return locationHierarchyRepository.count();
	}

	@Override
	public List<Location> findLocationByParentLocCode(String parentLocCode, String hierarchyName,
													  String langCode) {
		return
				locationRepository.findByIsActiveTrueAndParentLocCodeAndHierarchyNameAndLangCode(parentLocCode,
						hierarchyName, langCode);
	}
}
