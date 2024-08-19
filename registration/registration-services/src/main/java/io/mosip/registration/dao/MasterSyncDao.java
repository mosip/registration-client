package io.mosip.registration.dao;

import java.util.List;

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

/**
 * This class is used to store the master data details like Location,
 * gender,Registration center, Document types,category etc., in the respective
 * Databases. This class is also used to fetch any of the master data details
 * from the DB.
 *
 * @author Sreekar Chukka
 * @since 1.0.0
 */
public interface MasterSyncDao {

	/**
	 * This method is used to fetch a job detail of a paticular job id.
	 *
	 * @param synccontrol the {@link SyncControl} entity
	 * @return the master sync status
	 */
	public SyncControl syncJobDetails(String synccontrol);

	/**
	 * Find location by lang code.
	 *
	 * @param hierarchyLevel the hierarchy code
	 * @param langCode      the lang code
	 * @return the list
	 */
	List<Location> findLocationByLangCode(int hierarchyLevel, String langCode);

	/**
	 * Find location by parent loc code.
	 *
	 * @param parentLocCode the parent loc code
	 * @param langCode      the lang code
	 * @return the list
	 */
	List<Location> findLocationByParentLocCode(String parentLocCode, String langCode);

	/**
	 * Gets the all reason catogery.
	 *
	 * @param langCode the lang code
	 * @return the all reason catogery
	 */
	List<ReasonCategory> getAllReasonCatogery(String langCode);

	/**
	 * Gets the reason list.
	 *
	 * @param langCode  the lang code
	 * @param reasonCat the reason cat
	 * @return the reason list
	 */
	List<ReasonList> getReasonList(String langCode, List<String> reasonCat);

	/**
	 * Gets the block listed words.
	 * 
	 * @return the block listed words
	 */
	List<String> getBlockListedWords();

	/**
	 * Gets the Document Categories.
	 *
	 * @param docCode  the doc code
	 * @param langCode the lang code
	 * @return the document categories
	 */
	List<DocumentType> getDocumentTypes(List<String> docCode, String langCode);


	/**
	 * Get All the Active Sync JOBS
	 * 
	 * @return active sync jobs
	 */
	List<SyncJobDef> getSyncJobs();

	/**
	 * Gets the biometric type.
	 *
	 * @param biometricType the biometricType
	 * @param langCode      the lang code
	 * @return the biometric type
	 */
	List<BiometricAttribute> getBiometricType(String langCode, List<String> biometricType);

	/**
	 * Get all the active languages
	 * 
	 * @return List of {@link Language}
	 */
	List<Language> getActiveLanguages();


	/**
	 * Get all the active document category from the DB
	 * 
	 * @return List of active {@link DocumentCategory}
	 */
	List<DocumentCategory> getDocumentCategory();

	/**
	 * Get all the active locations from the DB
	 * 
	 * @return list of active {@link Location}
	 */
	List<Location> getLocationDetails();

	/**
	 * Get all the active locations from the DB
	 * 
	 * @param langCode the language code
	 * @return list of active {@link Location}
	 */
	List<Location> getLocationDetails(String langCode);

	/**
	 * Get all the hiraerchy active locations from the DB
	 * 
	 * @param hierarchyName name
	 * @param langCode  the language code
	 * @return list of active {@link Location}
	 */
	List<Location> getLocationDetails(String hierarchyName, String langCode);

	/**
	 * @param code     location code
	 * @param langCode language code
	 * @return Location
	 */
	public Location getLocation(String code, String langCode);

	public List<LocationHierarchy> getAllLocationHierarchy(String langCode);

	/**
	 * @param docCode Document code
	 * @param langCode LangCode
	 * @return DocuementType
	 */
	DocumentType getDocumentType(String docCode, String langCode);

	/**
	 * Get count of all the entries from LocationHierarchy table
	 * 
	 * @return count of {@link LocationHierarchy}
	 */
	Long getLocationHierarchyCount();
	/**
	 * Find location by parent loc code.
	 *
	 * @param parentLocCode the parent loc code
	 * @param hierarchyName
	 * @param langCode the lang code
	 * @return the list
	 */
	List<Location> findLocationByParentLocCode(String parentLocCode, String hierarchyName, String
			langCode);
}
