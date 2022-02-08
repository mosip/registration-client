package io.mosip.registration.util.mastersync;


import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.mastersync.DynamicFieldDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.dto.response.SyncDataBaseDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.DynamicField;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.AppAuthenticationRepository;
import io.mosip.registration.repositories.AppRolePriorityRepository;
import io.mosip.registration.repositories.ApplicantValidDocumentRepository;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BiometricTypeRepository;
import io.mosip.registration.repositories.BlocklistedWordsRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.DynamicFieldRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationHierarchyRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.MachineSpecificationRepository;
import io.mosip.registration.repositories.MachineTypeRepository;
import io.mosip.registration.repositories.PermittedLocalConfigRepository;
import io.mosip.registration.repositories.ProcessListRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.repositories.RegistrationCenterTypeRepository;
import io.mosip.registration.repositories.ScreenAuthorizationRepository;
import io.mosip.registration.repositories.ScreenDetailRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.repositories.TemplateRepository;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import lombok.NonNull;

@Component
public class ClientSettingSyncHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientSettingSyncHelper.class);
	
	private static final String ENTITY_PACKAGE_NAME = "io.mosip.registration.entity.";
	private static final String FIELD_TYPE_DYNAMIC_URL = "dynamic-url";
	private static final String FIELD_TYPE_DYNAMIC = "dynamic";
	private static final String FIELD_TYPE_SCRIPT = "script";
		
	/** Object for Sync Biometric Attribute Repository. */
	@Autowired
	private BiometricAttributeRepository biometricAttributeRepository;

	/** Object for Sync Biometric Type Repository. */
	@Autowired
	private BiometricTypeRepository biometricTypeRepository;

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

	/** Object for Sync Machine Repository. */
	@Autowired
	private MachineMasterRepository machineRepository;

	/** Object for Sync Machine Specification Repository. */
	@Autowired
	private MachineSpecificationRepository machineSpecificationRepository;

	/** Object for Sync Machine Type Repository. */
	@Autowired
	private MachineTypeRepository machineTypeRepository;

	/** Object for Sync Reason Category Repository. */
	@Autowired
	private ReasonCategoryRepository reasonCategoryRepository;

	/** Object for Sync Reason List Repository. */
	@Autowired
	private ReasonListRepository reasonListRepository;

	/** Object for Sync Template Repository. */
	@Autowired
	private TemplateRepository templateRepository;

	/** Object for Sync Applicant Valid Document Repository. */
	@Autowired
	private ApplicantValidDocumentRepository applicantValidDocumentRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private LanguageRepository languageRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterRepository registrationCenterRepository;

	/** Object for Sync language Repository. */
	@Autowired
	private RegistrationCenterTypeRepository registrationCenterTypeRepository;

	/** Object for Sync app authentication Repository. */
	@Autowired
	private AppAuthenticationRepository appAuthenticationRepository;

	/** Object for Sync app role Repository. */
	@Autowired
	private AppRolePriorityRepository appRolePriorityRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private ScreenAuthorizationRepository screenAuthorizationRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private ProcessListRepository processListRepository;

	/** Object for screen detail Repository. */
	@Autowired
	private ScreenDetailRepository screenDetailRepository;

	/** Object for Sync screen auth Repository. */
	@Autowired
	private SyncJobDefRepository syncJobDefRepository;

	@Autowired
	private DynamicFieldRepository dynamicFieldRepository;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private ServiceDelegateUtil serviceDelegateUtil;

	@Autowired
	private IdentitySchemaDao identitySchemaDao;

	@Autowired
	private LocationHierarchyRepository locationHierarchyRepository;
	
	@Autowired
	private PermittedLocalConfigRepository permittedLocalConfigRepository;

	@Autowired
	private RegistrationAppHealthCheckUtil registrationAppHealthCheckUtil;
		
	private static final Map<String, String> ENTITY_CLASS_NAMES = new HashMap<String, String>();
	
	
	static {
		ENTITY_CLASS_NAMES.put("MachineSpecification", ENTITY_PACKAGE_NAME + "RegMachineSpec");
		ENTITY_CLASS_NAMES.put("Machine", ENTITY_PACKAGE_NAME + "MachineMaster");
	}
	

	/**
	 * Save the SyncDataResponseDto 
	 * 
	 * @param syncDataResponseDto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String saveClientSettings(@NonNull SyncDataResponseDto syncDataResponseDto) throws RegBaseUncheckedException {
		long start = System.currentTimeMillis();
		try {
			List<CompletableFuture> futures = new ArrayList<CompletableFuture>();
			futures.add(handleMachineSync(syncDataResponseDto));
			futures.add(handleRegistrationCenterSync(syncDataResponseDto));
			futures.add(handleAppDetailSync(syncDataResponseDto));
			futures.add(handleTemplateSync(syncDataResponseDto));
			futures.add(handleDocumentSync(syncDataResponseDto));
			futures.add(handleIdSchemaPossibleValuesSync(syncDataResponseDto));
			futures.add(handleMisellaneousSync1(syncDataResponseDto));
			futures.add(handleMisellaneousSync2(syncDataResponseDto));
			futures.add(handleDynamicFieldSync(syncDataResponseDto));
			futures.add(handleDynamicURLFieldSync(syncDataResponseDto));
			futures.add(handlePermittedConfigSync(syncDataResponseDto));
			futures.add(syncSchema("System"));
			futures.add(handleScripts(syncDataResponseDto));

			CompletableFuture array [] = new CompletableFuture[futures.size()];
			CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(array));

			try {
				future.join();
			} catch (CompletionException e) {
				throw e.getCause();
			}

			LOGGER.info("======================== Complete master sync completed in {} ms ========================",
					(System.currentTimeMillis() - start));
			return RegistrationConstants.SUCCESS;
		} catch (Throwable e) {
			LOGGER.error("saveClientSettings failed", e);
		}
		throw new RegBaseUncheckedException(RegistrationConstants.MASTER_SYNC_EXCEPTION, RegistrationConstants.FAILURE);
	}

	/**
	 * creating meta data for building the entities from SyncDataBaseDto
	 * 
	 * @param syncDataBaseDto
	 * @return
	 * @throws Exception
	 */
	private List buildEntities(SyncDataBaseDto syncDataBaseDto) throws SyncFailedException {
		try {		
			List<Object> entities = new ArrayList<Object>();
			if(syncDataBaseDto == null || syncDataBaseDto.getData() == null || syncDataBaseDto.getData().isEmpty())
				return entities;

			LOGGER.info("Building entity of type : {} : {}", syncDataBaseDto.getEntityName(), syncDataBaseDto.getEntityType());
			JSONArray jsonArray = null;
			byte[] data = clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(syncDataBaseDto.getData()));
			switch (syncDataBaseDto.getEntityType()) {
				case "structured-url":
					Path path = Paths.get(System.getProperty("user.dir"), syncDataBaseDto.getEntityName());
					JSONObject jsonObject = new JSONObject(new String(data));
					downloadUrlData(path, jsonObject);
					jsonArray = new JSONArray(jsonObject.getBoolean("encrypted") ?
							new String(clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(FileUtils.readFileToString(path.toFile(),
									StandardCharsets.UTF_8)))) :
							FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8));
					if (path.toFile().delete()) 
						LOGGER.info("Entity file deleted");
					break;
				case "structured":
					jsonArray = new JSONArray(new String(data));
					break;
			}

			if(jsonArray == null)
				return entities;

			for(int i =0; i < jsonArray.length(); i++) {
				Object entity = MetaDataUtils.setCreateJSONObjectToMetaData(jsonArray.getJSONObject(i),
						getEntityClass(syncDataBaseDto.getEntityName()));
				entities.add(entity);
			}

			return entities;
		} catch (Throwable e) {
			LOGGER.error("Building entities is failed", e);
			throw new SyncFailedException("Building entities is failed..." + e.getMessage());
		}
	}

	private void downloadUrlData(Path path, JSONObject jsonObject) throws SyncFailedException {
		Map<String, String> requestParamMap = new HashMap<String, String>();
		requestParamMap.put(RegistrationConstants.KEY_INDEX.toLowerCase(),
				CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null));

		try {
			serviceDelegateUtil.download(jsonObject.getString("url"),
					requestParamMap,
					jsonObject.getString("headers"),
					jsonObject.getBoolean("auth-required"),
					jsonObject.getString("auth-token"),
					"System",
					path,
					jsonObject.getBoolean("encrypted"));
		} catch (Exception e) {
			LOGGER.error("Building entities is failed {}", path, e);
			throw new SyncFailedException("Failed to download entity file" + path.toString());
		}
	}
	
	private SyncDataBaseDto getSyncDataBaseDto(SyncDataResponseDto syncDataResponseDto, String entityName) {
		SyncDataBaseDto syncDataBaseDto = syncDataResponseDto.getDataToSync().stream()
				.filter(obj -> entityName.equals(obj.getEntityName()) && !obj.getEntityType().equalsIgnoreCase(FIELD_TYPE_DYNAMIC))
				.findAny()
				.orElse(null);
		 
		return syncDataBaseDto;
	}
	
	private Class getEntityClass(String entityName) throws ClassNotFoundException {
		try {
			
			return ENTITY_CLASS_NAMES.containsKey(entityName) ? Class.forName(ENTITY_CLASS_NAMES.get(entityName)) : 
				Class.forName(ENTITY_PACKAGE_NAME + entityName);
			
		} catch(ClassNotFoundException ex) {
			return Class.forName(ENTITY_PACKAGE_NAME + "Reg" + entityName);
		}
	}

	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMachineSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			machineTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineType")));
			machineSpecificationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "MachineSpecification")));
			machineRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Machine")));
		}  catch (Exception e) {
			LOGGER.error("Machine Data sync failed", e);
			throw new SyncFailedException("Machine data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleRegistrationCenterSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			registrationCenterTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenterType")));
			registrationCenterRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "RegistrationCenter")));			
		} catch (Exception e ) {
			LOGGER.error("RegistrationCenter Data sync failed", e);
			throw new SyncFailedException("RegistrationCenter data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleAppDetailSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			appRolePriorityRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppRolePriority")));
			appAuthenticationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "AppAuthenticationMethod")));
		} catch (Exception e) {
			LOGGER.error("AppDetail Data sync failed", e);
			throw new SyncFailedException("AppDetail data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleTemplateSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException{
		try {
			templateRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Template")));
		} catch (Exception e) {
			LOGGER.error("Template Data sync failed", e);
			throw new SyncFailedException("Template data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleDocumentSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			documentTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentType")));
			documentCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "DocumentCategory")));
			applicantValidDocumentRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ApplicantValidDocument")));
		} catch (Exception e) {
			LOGGER.error("Document Data sync failed", e);
			throw new SyncFailedException("Document data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleIdSchemaPossibleValuesSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			biometricTypeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricType")));
			biometricAttributeRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BiometricAttribute")));
			locationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Location")));
			locationHierarchyRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "LocationHierarchy")));
		} catch (Exception e) {
			LOGGER.error("Data sync failed", e);
			throw new SyncFailedException("IdSchema data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMisellaneousSync1(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			blocklistedWordsRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "BlocklistedWords")));
			processListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ProcessList")));
			screenDetailRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenDetail")));
			screenAuthorizationRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ScreenAuthorization")));
		} catch (Exception e) {
			LOGGER.error("Miscellaneous data sync failed", e);
			throw new SyncFailedException("Miscellaneous data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	/**
	 * save the entities data in respective repository
	 * @param syncDataResponseDto
	 * @throws SyncFailedException
	 */
	@Async
	private CompletableFuture handleMisellaneousSync2(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			languageRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "Language")));
			reasonCategoryRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ReasonCategory")));
			reasonListRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "ReasonList")));
			syncJobDefRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "SyncJobDef")));
		} catch (Exception e) {
			LOGGER.error("Miscellaneous data sync failed", e);
			throw new SyncFailedException("Miscellaneous data sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	@Async
	private CompletableFuture handleDynamicURLFieldSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException  {
		try {
			Iterator<SyncDataBaseDto> iterator = syncDataResponseDto.getDataToSync().stream()
					.filter(obj -> FIELD_TYPE_DYNAMIC_URL.equalsIgnoreCase(obj.getEntityType()))
					.iterator();

			while(iterator.hasNext()) {
				SyncDataBaseDto syncDataBaseDto = iterator.next();
				byte[] data = clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(syncDataBaseDto.getData()));
				Path path = Paths.get(System.getProperty("user.dir"), syncDataBaseDto.getEntityName());
				JSONObject jsonObject = new JSONObject(new String(data));
				downloadUrlData(path, jsonObject);

				String downloadedData = jsonObject.getBoolean("encrypted") ?
						new String(clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(FileUtils.readFileToString(path.toFile(),
								StandardCharsets.UTF_8)))) :
						FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);

				if (path.toFile().delete()) 
					LOGGER.info("Entity file deleted");

				if(syncDataBaseDto.getEntityName().equalsIgnoreCase("DynamicFieldDto")) {
					List<SyncDataBaseDto> list = MapperUtils.convertJSONStringToDto(downloadedData,
							new TypeReference<List<SyncDataBaseDto>>() {});
					for(SyncDataBaseDto dto : list)	{
						saveDynamicFieldData(dto.getData());
					}
				}
				else {
					saveDynamicFieldData(downloadedData);
				}
			}

		} catch (Throwable t) {
			LOGGER.error("Dynamic field URL based sync failed", t);
			throw new SyncFailedException("Dynamic field URL based sync failed due to " +  t.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}
	
	/**
	 * save dynamic fields with value json
	 * @param syncDataResponseDto
	 */
	@Async
	private CompletableFuture handleDynamicFieldSync(SyncDataResponseDto syncDataResponseDto) throws SyncFailedException {
		try {
			Iterator<SyncDataBaseDto> iterator = syncDataResponseDto.getDataToSync().stream()
					.filter(obj -> FIELD_TYPE_DYNAMIC.equalsIgnoreCase(obj.getEntityType()))
					.iterator();

			while(iterator.hasNext()) {
				SyncDataBaseDto syncDataBaseDto = iterator.next();
				if(syncDataBaseDto != null && syncDataBaseDto.getData() != null && !syncDataBaseDto.getData().isEmpty()) {
					byte[] data = clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(syncDataBaseDto.getData()));
					saveDynamicFieldData(new String(data));
				}
			}
				
		} catch(IOException e) {
			LOGGER.error("Dynamic Field sync failed", e);
			throw new SyncFailedException("Dynamic field sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	private void saveDynamicFieldData(String data) throws IOException {
		if(data == null || data.trim().isEmpty())
			return;

		JSONArray jsonArray = new JSONArray(data);
		Map<String, DynamicField> fields = new HashMap<>();
		for(int i=0; i< jsonArray.length(); i++) {
			DynamicFieldDto dynamicFieldDto = MapperUtils.convertJSONStringToDto(jsonArray.getJSONObject(i).toString(),
					new TypeReference<DynamicFieldDto>() {});
			DynamicField dynamicField = new DynamicField();
			dynamicField.setId(dynamicFieldDto.getId());
			dynamicField.setDataType(dynamicFieldDto.getDataType());
			dynamicField.setName(dynamicFieldDto.getName());
			dynamicField.setLangCode(dynamicFieldDto.getLangCode());
			dynamicField.setValueJson(dynamicFieldDto.getFieldVal() == null ?
					"[]" : MapperUtils.convertObjectToJsonString(dynamicFieldDto.getFieldVal()));
			dynamicField.setActive(dynamicFieldDto.isActive());
			fields.put(dynamicFieldDto.getName()+dynamicFieldDto.getLangCode(), dynamicField);
		}

		if (!fields.isEmpty()) {
			dynamicFieldRepository.saveAll(fields.values());
		}
	}


	@Async
	public CompletableFuture syncSchema(String triggerPoint) throws SyncFailedException {
		LOGGER.info("ID Schema sync started .....");

		if (!serviceDelegateUtil.isNetworkAvailable()) {
			throw new SyncFailedException(RegistrationConstants.NO_INTERNET);
		}

		try {
			LinkedHashMap<String, Object> syncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(
					RegistrationConstants.ID_SCHEMA_SYNC_SERVICE, new HashMap<String, String>(), true,
					triggerPoint);

			if (null != syncResponse.get(RegistrationConstants.RESPONSE)) {
				LOGGER.info("ID Schema sync fetched from server.");

				String jsonString = MapperUtils
						.convertObjectToJsonString(syncResponse.get(RegistrationConstants.RESPONSE));
				SchemaDto schemaDto = MapperUtils.convertJSONStringToDto(jsonString,
						new TypeReference<SchemaDto>() {});

				identitySchemaDao.createIdentitySchema(schemaDto);
				saveProcessSpec(schemaDto, jsonString);
				return CompletableFuture.completedFuture(true);
			}

		} catch (Exception e) {
			LOGGER.error("Schema sync failed", e);
		}
		throw new SyncFailedException("Schema sync failed");
	}

	//saves processes and subprocess as separate schema
	private void saveProcessSpec(SchemaDto schemaDto, String jsonString) {
		JSONObject jsonObject = new JSONObject(jsonString);
		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if(key.toLowerCase().endsWith("process")) {
				try {
					ProcessSpecDto processSpecDto = MapperUtils.convertJSONStringToDto(jsonObject.get(key).toString(),
							new TypeReference<ProcessSpecDto>() {});
					identitySchemaDao.createProcessSpec(key, schemaDto.getIdVersion(), processSpecDto);
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}
	
	@Async
	private CompletableFuture handlePermittedConfigSync(@NonNull SyncDataResponseDto syncDataResponseDto) throws SyncFailedException{
		try {
			permittedLocalConfigRepository.saveAll(buildEntities(getSyncDataBaseDto(syncDataResponseDto, "PermittedLocalConfig")));
		} catch (Exception e) {
			LOGGER.error("Permitted Config sync failed", e);
			throw new SyncFailedException("Permitted Config sync failed due to " +  e.getMessage());
		}
		return CompletableFuture.completedFuture(true);
	}

	@Async
	private CompletableFuture handleScripts(@NonNull SyncDataResponseDto syncDataResponseDto) throws SyncFailedException{
		try {
			List<SyncDataBaseDto> scriptToDownload = syncDataResponseDto.getDataToSync().stream()
					.filter(obj -> obj.getEntityType().equalsIgnoreCase(FIELD_TYPE_SCRIPT))
					.collect(Collectors.toList());

				for (SyncDataBaseDto syncDataBaseDto : scriptToDownload) {
					byte[] data = clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(syncDataBaseDto.getData()));
					downloadUrlData(Paths.get(System.getProperty("user.dir"), syncDataBaseDto.getEntityName()), new JSONObject(new String(data)));
				}
			} catch (Exception e) {
				LOGGER.error("Scripts sync failed", e);
				throw new SyncFailedException("Scripts sync failed due to " +  e.getMessage());
			}
		return CompletableFuture.completedFuture(true);
	}
}
