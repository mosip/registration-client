package io.mosip.registration.service.sync;

import java.util.List;

import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.mastersync.*;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * It makes call to the external 'MASTER Sync' services to download the master
 * data which are relevant to center specific by passing the center id or mac
 * address or machine id. Once download the data, it stores the information into
 * the DB for further processing. If center remapping found from the sync
 * response object, it invokes this 'CenterMachineReMapService' object to
 * initiate the center remapping related activities. During the process, the
 * required informations are updated into the audit table for further tracking.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 *
 */
public interface MasterSyncService {

	/**
	 * It invokes the Master Sync service to download the required information from
	 * external services if the system is online. Once download, the data would be
	 * updated into the DB for further process.
	 *
	 * @param masterSyncDetails the master sync details
	 * @param triggerPoint      from where the call has been initiated [Either :
	 *                          user or system]
	 * @return success or failure status as Response DTO.
	 */
	ResponseDTO getMasterSync(String masterSyncDetails, String triggerPoint) throws RegBaseCheckedException;

	/**
	 * Find location or region by hierarchy code.
	 *
	 * @param hierarchyLevel the hierarchy code
	 * @param langCode       the lang code
	 * @return the list holds the Location data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> findLocationByHierarchyCode(int hierarchyLevel, String langCode) throws RegBaseCheckedException;

	/**
	 * Find proviance by hierarchy code.
	 *
	 * @param code     the code
	 * @param langCode the lang code
	 * @return the list holds the Province data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> findLocationByParentHierarchyCode(String code, String langCode) throws RegBaseCheckedException;

	/**
	 * Gets all the reasons for rejection that to be selected during EOD approval
	 * process.
	 *
	 * @param langCode the lang code
	 * @return the all reasons
	 * @throws RegBaseCheckedException
	 */
	List<ReasonListDto> getAllReasonsList(String langCode) throws RegBaseCheckedException;

	/**
	 * Gets all the block listed words that shouldn't be allowed while capturing
	 * demographic information from user.
	 *
	 * @return the all block listed words
	 * @throws RegBaseCheckedException
	 */
	List<String> getAllBlockListedWords();

	/**
	 * Gets all the document categories from db that to be displayed in the UI
	 * dropdown.
	 *
	 * @param docCode  the doc code
	 * @param langCode the lang code
	 * @return all the document categories
	 * @throws RegBaseCheckedException
	 */
	//List<DocumentCategoryDto> getDocumentCategories(String docCode, String langCode);
	
	/**
	 * Get the document Type from db that to be displayed in the UI
	 * dropdown.
	 *
	 * @param docCode  the doc code
	 * @param langCode the lang code
	 * @return all the document categories
	 * @throws RegBaseCheckedException
	 */
	DocumentType getDocumentType(String docCode, String langCode);

	/**
	 *
	 * @param fieldName
	 * @param langCode
	 * @return
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> getDynamicField(String fieldName, String langCode) throws RegBaseCheckedException;

	/**
	 *
	 * @param fieldName
	 * @param langCode
	 * @return
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> getFieldValues(String fieldName, String langCode, boolean isHierarchical);

	/**
	 * @param code     location code
	 * @param langCode language code
	 * @return Location
	 */
	public Location getLocation(String code, String langCode);

	/**
	 *
	 * @return
	 */
	public List<SyncJobDef> getSyncJobs();

	/**
	 * Find proviance by hierarchy code.
	 *
	 * @param code the code
	 * @param langCode the lang code
	 * @param hierarchyName
	 * @return the list holds the Province data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> findLocationByParentHierarchyCode(String code, String hierarchyName, String
			langCode) throws RegBaseCheckedException;
	/**
	 *
	 * @param fieldName
	 * @param hierarchyLevelName
	 * @param langCode
	 * @return
	 * @throws RegBaseCheckedException
	 */
	List<GenericDto> getFieldValues(String fieldName, String hierarchyLevelName, String langCode,
									boolean isHierarchical);

}
