package io.mosip.registration.service.sync.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.DynamicFieldDAO;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.mastersync.DynamicFieldValueDto;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.mastersync.ReasonListDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;
import io.mosip.registration.util.mastersync.MapperUtils;

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
@Service
public class MasterSyncServiceImpl extends BaseService implements MasterSyncService {

	/**
	 * The SncTransactionManagerImpl, which Have the functionalities to get the job
	 * and to create sync transaction
	 */
	@Autowired
	protected SyncManager syncManager;

	/** Object for masterSyncDao class. */
	@Autowired
	private MasterSyncDao masterSyncDao;

	/** The global param service. */
	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private DynamicFieldDAO dynamicFieldDAO;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private ClientSettingSyncHelper clientSettingSyncHelper;

	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(MasterSyncServiceImpl.class);

	/**
	 * It invokes the Master Sync service to download the required information from
	 * external services if the system is online. Once download, the data would be
	 * updated into the DB for further process.
	 *
	 * @param masterSyncDtls the master sync details
	 * @param triggerPoint   from where the call has been initiated [Either : user
	 *                       or system]
	 * @return success or failure status as Response DTO.
	 * @throws RegBaseCheckedException
	 */
	@Timed
	@Override
	public ResponseDTO getMasterSync(String masterSyncDtls, String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info("Initiating the Master Sync");

		if (masterSyncFieldsValidate(masterSyncDtls, triggerPoint)) {
			GlobalParam upgradeFullSyncEntities = globalParamService.getGlobalParam(RegistrationConstants.UPGRADE_FULL_SYNC_ENTITIES);
			
			Map<String, String> requestParamMap = getRequestParamsForClientSettingsSync(masterSyncDtls, upgradeFullSyncEntities);
			ResponseDTO responseDto = syncClientSettings(masterSyncDtls, triggerPoint,
					requestParamMap);

			//Perform sync once again only during initial sync to pull all the latest changes.
			if(responseDto.getSuccessResponseDTO() != null && isInitialSync()) {
				// getting Last Sync date from Data from sync table
				SyncControl masterSyncDetails = masterSyncDao.syncJobDetails(masterSyncDtls);
				if (masterSyncDetails != null) {
					requestParamMap.put(RegistrationConstants.MASTER_DATA_LASTUPDTAE,
							DateUtils.formatToISOString(masterSyncDetails.getLastSyncDtimes().toLocalDateTime()));
				}
				responseDto = syncClientSettings(masterSyncDtls, triggerPoint, requestParamMap);
			}
			if (responseDto.getSuccessResponseDTO() != null && upgradeFullSyncEntities != null) {
				LOGGER.info("Deleting the list of fullSyncEntities saved during DB upgrade..");
				globalParamService.deleteGlobalParam(RegistrationConstants.UPGRADE_FULL_SYNC_ENTITIES);
			}
			return responseDto;
		} else {
			LOGGER.info("masterSyncDtls/triggerPoint is mandatory...");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorMessage());
		}

	}

	/**
	 * Find location or region by hierarchy code.
	 *
	 * @param hierarchyLevel the hierarchy code
	 * @param langCode       the lang code
	 * @return the list holds the Location data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> findLocationByHierarchyCode(int hierarchyLevel, String langCode)
			throws RegBaseCheckedException {

		List<GenericDto> locationDto = new ArrayList<>();
		List<Location> masterLocation = masterSyncDao.findLocationByLangCode(hierarchyLevel, langCode);

		for (Location masLocation : masterLocation) {
			GenericDto location = new GenericDto();
			location.setCode(masLocation.getCode());
			location.setName(masLocation.getName());
			location.setLangCode(masLocation.getLangCode());
			locationDto.add(location);
		}
		return locationDto;
	}

	/**
	 * Find proviance by hierarchy code.
	 *
	 * @param code     the code
	 * @param langCode the lang code
	 * @return the list holds the Province data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> findLocationByParentHierarchyCode(String code, String langCode) throws RegBaseCheckedException {

		List<GenericDto> locationDto = new ArrayList<>();
		if (codeAndlangCodeNullCheck(code, langCode)) {
			List<Location> masterLocation = masterSyncDao.findLocationByParentLocCode(code, langCode);

			for (Location masLocation : masterLocation) {
				GenericDto location = new GenericDto();
				location.setCode(masLocation.getCode());
				location.setName(masLocation.getName());
				location.setLangCode(masLocation.getLangCode());
				locationDto.add(location);
			}
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.CODE_AND_LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorMessage());
		}
		return locationDto;
	}

	/**
	 * Gets all the reasons for rejection that to be selected during EOD approval
	 * process.
	 *
	 * @param langCode the lang code
	 * @return the all reasons
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<ReasonListDto> getAllReasonsList(String langCode) throws RegBaseCheckedException {

		List<ReasonListDto> reasonListResponse = new ArrayList<>();
		if (langCodeNullCheck(langCode)) {
			List<String> resonCantCode = new ArrayList<>();
			// Fetting Reason Category
			List<ReasonCategory> masterReasonCatogery = masterSyncDao.getAllReasonCatogery(langCode);
			if (masterReasonCatogery != null && !masterReasonCatogery.isEmpty()) {
				masterReasonCatogery.forEach(reason -> {
					resonCantCode.add(reason.getCode());
				});
			}
			// Fetching reason list based on lang_Code and rsncat_code
			List<ReasonList> masterReasonList = masterSyncDao.getReasonList(langCode, resonCantCode);
			masterReasonList.forEach(reasonList -> {
				ReasonListDto reasonListDto = new ReasonListDto();
				reasonListDto.setCode(reasonList.getCode());
				reasonListDto.setName(reasonList.getName());
				reasonListDto.setRsnCatCode(reasonList.getRsnCatCode());
				reasonListDto.setLangCode(reasonList.getLangCode());
				reasonListResponse.add(reasonListDto);
			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorMessage());
		}
		return reasonListResponse;

	}

	/**
	 * Gets all the block listed words that shouldn't be allowed while capturing
	 * demographic information from user.
	 *
	 * @return the all block listed words
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<String> getAllBlockListedWords() {
		return masterSyncDao.getBlockListedWords();
	}


	@Override
	public List<GenericDto> getDynamicField(String fieldName, String langCode) throws RegBaseCheckedException {
		List<GenericDto> fieldValues = new LinkedList<>();
		List<DynamicFieldValueDto> syncedValues = dynamicFieldDAO.getDynamicFieldValues(fieldName, langCode);

		if (syncedValues != null) {
			for (DynamicFieldValueDto valueDto : syncedValues) {
				//if (valueDto.isActive()) {
					GenericDto genericDto = new GenericDto();
					genericDto.setName(valueDto.getValue());
					genericDto.setCode(valueDto.getCode());
					genericDto.setLangCode(langCode);
					fieldValues.add(genericDto);
				//}
			}
		}
		return fieldValues;
	}

	@Override
	public List<GenericDto> getFieldValues(String fieldName, String langCode, boolean isHierarchical) {
		try {
			if(isHierarchical) {
				return findLocationByParentHierarchyCode(fieldName, langCode);
			}
			return getDynamicField(fieldName, langCode);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Failed to fetch values for field : " + fieldName, exception);
		}
		return Collections.EMPTY_LIST;
	}

	/**
	 * Error msg.
	 *
	 * @param responseMap
	 * @return the string
	 */
	@SuppressWarnings("unchecked")
	private String errorMsg(LinkedHashMap<String, Object> responseMap) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Logging error message....");
		String errorMsg = RegistrationConstants.MASTER_SYNC + "-" + RegistrationConstants.MASTER_SYNC_FAILURE_MSG;
		if (null != responseMap && responseMap.size() > 0) {
			List<LinkedHashMap<String, Object>> errorMap = (List<LinkedHashMap<String, Object>>) responseMap
					.get(RegistrationConstants.ERRORS);
			if (null != errorMap.get(0).get(RegistrationConstants.ERROR_MSG)) {
				errorMsg = (String) errorMap.get(0).get(RegistrationConstants.ERROR_MSG);
			}
		}
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, errorMsg);
		return errorMsg;

	}

	/**
	 * Master sync fields validate.
	 *
	 * @param masterSyncDtls the master sync dtls
	 * @param triggerPoint   the trigger point
	 * @return true, if successful
	 */
	private boolean masterSyncFieldsValidate(String masterSyncDtls, String triggerPoint) {

		if (StringUtils.isEmpty(masterSyncDtls)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"masterSyncDtls is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"triggerPoint is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Lang code null check.
	 *
	 * @param langCode the language code
	 * @return true, if successful
	 */
	private boolean langCodeNullCheck(String langCode) {
		if (StringUtils.isEmpty(langCode)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"language code is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	private boolean codeAndlangCodeNullCheck(String code, String langCode) {

		if (StringUtils.isEmpty(code)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"code is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(langCode)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"language code is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	/**
	 * collects request params required for client settings sync.
	 * 
	 * @param masterSyncDtls
	 * @param upgradeFullSyncEntities 
	 * @return
	 * @throws RegBaseCheckedException
	 */
	private Map<String, String> getRequestParamsForClientSettingsSync(String masterSyncDtls, GlobalParam upgradeFullSyncEntities) {
		Map<String, String> requestParamMap = new HashMap<String, String>();
		requestParamMap.put(RegistrationConstants.KEY_INDEX.toLowerCase(),
				CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null));
		requestParamMap.put("version", softwareUpdateHandler.getCurrentVersion());
		
		String fullSyncEntities = RegistrationConstants.EMPTY;
			
		if (upgradeFullSyncEntities != null) {
			fullSyncEntities = upgradeFullSyncEntities.getVal();
		}

		if (!isInitialSync()) {
			// getting Last Sync date from Data from sync table
			SyncControl masterSyncDetails = masterSyncDao.syncJobDetails(masterSyncDtls);
			if (masterSyncDetails != null) {
				requestParamMap.put(RegistrationConstants.MASTER_DATA_LASTUPDTAE,
						DateUtils.formatToISOString(masterSyncDetails.getLastSyncDtimes().toLocalDateTime()));
			}

			String registrationCenterId = getCenterId();
			if (registrationCenterId != null)
				requestParamMap.put(RegistrationConstants.MASTER_CENTER_PARAM, registrationCenterId);
		}
		if (masterSyncDao.getLocationHierarchyCount() <= 0) {
			fullSyncEntities = fullSyncEntities.isBlank() ? LocationHierarchy.class.getSimpleName() : fullSyncEntities + "," + LocationHierarchy.class.getSimpleName();
		}
		
		requestParamMap.put(RegistrationConstants.MASTER_FULLSYNC_ENTITIES, fullSyncEntities);
		
		return requestParamMap;
	}

	/**
	 * Method gets all the client settings from syncdata-service and saves data in
	 * local DB. Also updates last sync time as per response in syncControl
	 * 
	 * @param masterSyncDtls
	 * @param triggerPoint
	 * @param requestParam
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ResponseDTO syncClientSettings(String masterSyncDtls, String triggerPoint,
			Map<String, String> requestParam) {
		LOGGER.info("Client settings sync invoked.....");
		ResponseDTO responseDTO = new ResponseDTO();
		LinkedHashMap<String, Object> masterSyncResponse = null;

		try {
			// Precondition check, proceed only if met, otherwise throws exception
			proceedWithMasterAndKeySync(masterSyncDtls);

			LOGGER.info("Client settings sync started with this params : {}", requestParam);

			masterSyncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
					.get(RegistrationConstants.MASTER_VALIDATOR_SERVICE_NAME, requestParam, true, triggerPoint);

			String errorCode = getErrorCode(getErrorList(masterSyncResponse));
			if (RegistrationConstants.MACHINE_REMAP_CODE.equalsIgnoreCase(errorCode)) {
				// Machine is remapped, exit from sync and mark the remap process to start
				globalParamService.update(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, RegistrationConstants.TRUE);
				LOGGER.info("Client settings sync - Found that machine is remapped : {}", errorCode);
				return responseDTO;
			}

			if (null != masterSyncResponse.get(RegistrationConstants.RESPONSE)) {
				saveClientSettings(masterSyncDtls, triggerPoint, masterSyncResponse, responseDTO);
				return responseDTO;
			}

			setErrorResponse(responseDTO, errorMsg(masterSyncResponse), null);

		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			setErrorResponse(responseDTO, RegistrationConstants.MASTER_SYNC_FAILURE_MSG, null);
		}

		LOGGER.info("Client settings sync completed :: {}", responseDTO);
		return responseDTO;
	}

	private void saveClientSettings(String masterSyncDtls, String triggerPoint,
			LinkedHashMap<String, Object> masterSyncResponse, ResponseDTO responseDTO) throws Exception {
		LOGGER.info("save Client Settings started...");
		String jsonString = MapperUtils
				.convertObjectToJsonString(masterSyncResponse.get(RegistrationConstants.RESPONSE));
		SyncDataResponseDto syncDataResponseDto = MapperUtils.convertJSONStringToDto(jsonString,
				new TypeReference<SyncDataResponseDto>() {
				});

		String response = null;
		try {
			response = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
		} catch (Throwable runtimeException) {
			LOGGER.error("", runtimeException);
			throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_EXCEPTION, runtimeException.getMessage());
		}

		if (RegistrationConstants.SUCCESS.equals(response)) {
			setSuccessResponse(responseDTO, RegistrationConstants.MASTER_SYNC_SUCCESS_MESSAGE, null);
			SyncTransaction syncTransaction = syncManager.createSyncTransaction(
					RegistrationConstants.JOB_EXECUTION_SUCCESS, RegistrationConstants.JOB_EXECUTION_SUCCESS,
					triggerPoint, masterSyncDtls);
			syncManager.updateClientSettingLastSyncTime(syncTransaction,
					getTimestamp(syncDataResponseDto.getLastSyncTime()));
			LOGGER.info("Save Client Settings completed successfully : {}", syncDataResponseDto.getLastSyncTime());
		} else
			setErrorResponse(responseDTO, RegistrationConstants.MASTER_SYNC_ERROR_MESSAGE, null);
	}


	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getErrorList(LinkedHashMap<String, Object> syncReponse) {

		return syncReponse.get(RegistrationConstants.ERRORS) != null
				? (List<Map<String, Object>>) syncReponse.get(RegistrationConstants.ERRORS)
				: null;

	}

	private String getErrorCode(List<Map<String, Object>> errorList) {

		return errorList != null && errorList.get(0) != null
				? (String) errorList.get(0).get(RegistrationConstants.ERROR_CODE)
				: null;

	}

	@Override
	public List<SyncJobDef> getSyncJobs() {
		return masterSyncDao.getSyncJobs();
	}

	public Location getLocation(String code, String langCode) {
		return masterSyncDao.getLocation(code, langCode);
	}

	@Override
	public DocumentType getDocumentType(String docCode, String langCode) {
		return  masterSyncDao.getDocumentType(docCode, langCode);
	}

	@Override
	public List<GenericDto> findLocationByParentHierarchyCode(String code, String hierarchyName,
															  String langCode) throws RegBaseCheckedException {
		List<GenericDto> locationDto = new ArrayList<>();
		if (codeAndlangCodeNullCheck(code, langCode)) {
			List<Location> masterLocation = masterSyncDao.findLocationByParentLocCode(code,
					hierarchyName, langCode);
			for (Location masLocation : masterLocation) {
				GenericDto location = new GenericDto();
				location.setCode(masLocation.getCode());
				location.setName(masLocation.getName());
				location.setLangCode(masLocation.getLangCode());
				locationDto.add(location);
			}
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.CODE_AND_LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorMessage());
		}
		return locationDto;
	}
	@Override
	public List<GenericDto> getFieldValues(String fieldName, String hierarchyName, String langCode,
										   boolean isHierarchical) {
		try {
			if(isHierarchical) {
				return findLocationByParentHierarchyCode(fieldName, hierarchyName,
						langCode);
			}
			return getDynamicField(fieldName, langCode);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Failed to fetch values for field : " + fieldName, exception);
		}
		return Collections.EMPTY_LIST;
	}

}
