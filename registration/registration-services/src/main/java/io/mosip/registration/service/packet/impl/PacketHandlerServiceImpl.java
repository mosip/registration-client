package io.mosip.registration.service.packet.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_HANLDER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.commons.packet.dto.Document;
import io.mosip.commons.packet.dto.packet.DeviceMetaInfo;
import io.mosip.commons.packet.dto.packet.DigitalId;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.idgenerator.rid.constant.RidGeneratorPropertyConstant;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.dto.packetmanager.metadata.BiometricsMetaInfoDto;
import io.mosip.registration.dto.packetmanager.metadata.DocumentMetaInfoDTO;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.kernel.biometrics.constant.OtherKey;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * The implementation class of {@link PacketHandlerService} to handle the
 * registration data to create packet out of it and save the encrypted packet
 * data in the configured local system
 * 
 * @author Balaji Sridharanha
 * @since 1.0.0
 *
 */
@Service
public class PacketHandlerServiceImpl extends BaseService implements PacketHandlerService {

	private static final Logger LOGGER = AppConfig.getLogger(PacketHandlerServiceImpl.class);
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	@Autowired
	private Environment environment;

	@Autowired
	private AuditManagerService auditFactory;

	@Autowired
	private RegistrationDAO registrationDAO;

	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private PacketWriter packetWriter;

	@Autowired
	private BIRBuilder birBuilder;

	@Autowired
	private AuditDAO auditDAO;

	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;

	/** The machine mapping DAO. */
	@Autowired
	private MachineMappingDAO machineMappingDAO;

	@Autowired
	private BioService bioService;

	@Autowired
	private RidGenerator<String> ridGenerator;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private MasterSyncService masterSyncService;

	@Value("${objectstore.packet.source:REGISTRATION_CLIENT}")
	private String source;

	@Value("${packet.manager.account.name}")
	private String packetManagerAccount;

	@Value("${object.store.base.location}")
	private String baseLocation;

	@Value("${objectstore.packet.officer_biometrics_file_name}")
	private String officerBiometricsFileName;

	@Value("${objectstore.packet.supervisor_biometrics_file_name}")
	private String supervisorBiometricsFileName;

	private ObjectMapper objectMapper = new ObjectMapper();
	private static String SLASH = "/";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.PacketHandlerService#handle(io.mosip.
	 * registration.dto.RegistrationDTO)
	 */
	@Counted
	@Timed
	@Override
	public ResponseDTO handle(RegistrationDTO registrationDTO) {
		LOGGER.info("Registration Handler had been called");
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setErrorResponseDTOs(new ArrayList<>());
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();

		if (registrationDTO == null || registrationDTO.getRegistrationId() == null) {
			errorResponseDTO.setCode(REG_PACKET_CREATION_ERROR_CODE.getErrorCode());
			errorResponseDTO.setMessage(REG_PACKET_CREATION_ERROR_CODE.getErrorMessage());
			responseDTO.getErrorResponseDTOs().add(errorResponseDTO);
			return responseDTO;
		}

		if (registrationDTO.getAdditionalInfoReqId() != null) {
			registrationDTO.setAppId(registrationDTO.getAdditionalInfoReqId().split("-")[0]);
		}
		
		registrationDTO.setRegistrationId(registrationDTO.getAppId());
		
		Map<String, String> metaInfoMap = new LinkedHashMap<>();
		try {
			SchemaDto schema = identitySchemaService.getIdentitySchema(registrationDTO.getIdSchemaVersion());
			setDemographics(registrationDTO);
			setDocuments(registrationDTO, metaInfoMap);
			setBiometrics(registrationDTO, metaInfoMap);

			setOperatorBiometrics(registrationDTO.getRegistrationId(), registrationDTO.getProcessId().toUpperCase(),
					registrationDTO.getOfficerBiometrics(), officerBiometricsFileName);
			setOperatorBiometrics(registrationDTO.getRegistrationId(), registrationDTO.getProcessId().toUpperCase(),
					registrationDTO.getSupervisorBiometrics(), supervisorBiometricsFileName);

			setAudits(registrationDTO);

			setMetaInfo(registrationDTO, metaInfoMap);
			LOGGER.debug("Adding Meta info to packet manager");
			packetWriter.addMetaInfo(registrationDTO.getRegistrationId(), metaInfoMap, source.toUpperCase(),
					registrationDTO.getProcessId().toUpperCase());

			String refId = String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID))
					.concat(RegistrationConstants.UNDER_SCORE)
					.concat(String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID)));
			
			LOGGER.debug("Requesting packet manager to persist packet");
			List<PacketInfo> packetInfo = packetWriter.persistPacket(registrationDTO.getRegistrationId(),
					String.valueOf(registrationDTO.getIdSchemaVersion()), schema.getSchemaJson(), source.toUpperCase(),
					registrationDTO.getProcessId().toUpperCase(),
					registrationDTO.getAppId(),
					refId, true);
			
			if (!CollectionUtils.isEmpty(packetInfo)) {
				registrationDTO.setPacketId(packetInfo.get(0).getId());
			}

			LOGGER.info("Saving registration info in DB and on disk.");
			registrationDAO.save(baseLocation + SLASH + packetManagerAccount + SLASH + registrationDTO.getPacketId(), registrationDTO);

			globalParamService.update(RegistrationConstants.AUDIT_TIMESTAMP, DateUtils.getUTCCurrentDateTime().toString());

			auditFactory.audit(AuditEvent.PACKET_CREATION_SUCCESS, Components.PACKET_HANDLER,
					registrationDTO.getRegistrationId(), AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId());

			SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
			successResponseDTO.setCode("0000");
			successResponseDTO.setMessage("Success");
			responseDTO.setSuccessResponseDTO(successResponseDTO);

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("Exception while creating packet ",regBaseCheckedException);

			auditFactory.audit(AuditEvent.PACKET_INTERNAL_ERROR, Components.PACKET_HANDLER,
					registrationDTO.getRegistrationId(), AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId());

			errorResponseDTO.setCode(regBaseCheckedException.getErrorCode());
			errorResponseDTO.setMessage(regBaseCheckedException.getErrorText());
			responseDTO.getErrorResponseDTOs().add(errorResponseDTO);
		} catch (Exception exception) {
			LOGGER.error("Exception while creating packet ", exception);

			auditFactory.audit(AuditEvent.PACKET_INTERNAL_ERROR, Components.PACKET_HANDLER,
					registrationDTO.getRegistrationId(), AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId());

			errorResponseDTO.setCode(exception.getMessage());
			errorResponseDTO.setMessage(exception.getMessage());
			responseDTO.getErrorResponseDTOs().add(errorResponseDTO);
		} finally {
			LOGGER.info("Finally clearing all the captured data from registration DTO");
			registrationDTO.clearRegistrationDto();
		}
		LOGGER.info(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Registration Handler had been ended");
		return responseDTO;
	}

	private void setOperatorBiometrics(String registrationId, String registrationCategory,
			List<BiometricsDto> operatorBiometrics, String fileName) {
		/** Operator/officer/supervisor Biometrics */
		if (!operatorBiometrics.isEmpty()) {
			LOGGER.debug("Adding operator biometrics : {}", fileName);
			BiometricRecord biometricRecord = new BiometricRecord();
			for (BiometricsDto biometricsDto : operatorBiometrics) {
				BIR bir = birBuilder.buildBIR(biometricsDto);
				biometricRecord.getSegments().add(bir);
			}
			packetWriter.setBiometric(registrationId, fileName, biometricRecord, source.toUpperCase(),
					registrationCategory.toUpperCase());
		}
	}

	private void setMetaInfo(RegistrationDTO registrationDTO, Map<String, String> metaInfoMap)
			throws RegBaseCheckedException {

		LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Adding registered devices to meta info");
		addRegisteredDevices(metaInfoMap);

		LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Adding operations data to meta info");
		setOperationsData(metaInfoMap, registrationDTO);

		LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Adding other info to meta info");
		setOthersMetaInfo(metaInfoMap, registrationDTO);

		setMetaData(metaInfoMap, registrationDTO);

	}

	private void setMetaData(Map<String, String> metaInfoMap, RegistrationDTO registrationDTO)
			throws RegBaseCheckedException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(PacketManagerConstants.REGISTRATIONID, registrationDTO.getRegistrationId());
		metaData.put(RegistrationConstants.PACKET_APPLICATION_ID, registrationDTO.getAppId());
		metaData.put(PacketManagerConstants.META_CREATION_DATE, LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter
				.ofPattern(RidGeneratorPropertyConstant.TIMESTAMP_FORMAT.getProperty())));
		metaData.put(PacketManagerConstants.META_CLIENT_VERSION, softwareUpdateHandler.getCurrentVersion());
		metaData.put(PacketManagerConstants.META_REGISTRATION_TYPE,
				registrationDTO.getFlowType().getCategory().toUpperCase());
		metaData.put(PacketManagerConstants.META_PRE_REGISTRATION_ID, registrationDTO.getPreRegistrationId());

		MachineMaster machineMaster = machineMappingDAO.getMachine();
		if (machineMaster == null || machineMaster.getRegCenterId() == null) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_PKT_INVALID_MACHINE_ID_EXCEPTION);
		} else {
			metaData.put(PacketManagerConstants.META_MACHINE_ID, machineMaster.getId());
			metaData.put(PacketManagerConstants.META_CENTER_ID, machineMaster.getRegCenterId());
			metaData.put(PacketManagerConstants.META_DONGLE_ID, machineMaster.getSerialNum());
			metaData.put(PacketManagerConstants.META_KEYINDEX, machineMaster.getKeyIndex());
		}

		metaData.put("langCodes", String.join(RegistrationConstants.COMMA, registrationDTO.getSelectedLanguagesByApplicant()));
		metaData.put(PacketManagerConstants.META_APPLICANT_CONSENT,
				registrationDTO.getRegistrationMetaDataDTO().getConsentOfApplicant());

		metaInfoMap.put("metaData", getJsonString(getLabelValueDTOListString(metaData)));
		metaInfoMap.put("blockListedWords", getJsonString(registrationDTO.BLOCKLISTED_CHECK));
	}

	private void setOperationsData(Map<String, String> metaInfoMap, RegistrationDTO registrationDTO)
			throws RegBaseCheckedException {

		Map<String, String> operationsDataMap = new LinkedHashMap<>();
		operationsDataMap.put(PacketManagerConstants.META_OFFICER_ID, registrationDTO.getOsiDataDTO().getOperatorID());
		operationsDataMap.put(PacketManagerConstants.META_OFFICER_BIOMETRIC_FILE,
				registrationDTO.getOfficerBiometrics().isEmpty() ? null : officerBiometricsFileName);
		operationsDataMap.put(PacketManagerConstants.META_SUPERVISOR_ID,
				registrationDTO.getOsiDataDTO().getSupervisorID());
		operationsDataMap.put(PacketManagerConstants.META_SUPERVISOR_BIOMETRIC_FILE,
				registrationDTO.getSupervisorBiometrics().isEmpty() ? null : supervisorBiometricsFileName);
		operationsDataMap.put(PacketManagerConstants.META_SUPERVISOR_PWD,
				String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPassword()));
		operationsDataMap.put(PacketManagerConstants.META_OFFICER_PWD,
				String.valueOf(registrationDTO.getOsiDataDTO().isOperatorAuthenticatedByPassword()));
		operationsDataMap.put(PacketManagerConstants.META_SUPERVISOR_PIN, null);
		operationsDataMap.put(PacketManagerConstants.META_OFFICER_PIN, null);
		operationsDataMap.put(PacketManagerConstants.META_SUPERVISOR_OTP,
				String.valueOf(registrationDTO.getOsiDataDTO().isSuperviorAuthenticatedByPIN()));
		operationsDataMap.put(PacketManagerConstants.META_OFFICER_OTP,
				String.valueOf(registrationDTO.getOsiDataDTO().isOperatorAuthenticatedByPIN()));

		metaInfoMap.put(PacketManagerConstants.META_INFO_OPERATIONS_DATA,
				getJsonString(getLabelValueDTOListString(operationsDataMap)));

	}

	private List<Map<String, String>> getLabelValueDTOListString(Map<String, String> operationsDataMap) {

		List<Map<String, String>> labelValueMap = new LinkedList<>();

		for (Entry<String, String> fieldName : operationsDataMap.entrySet()) {

			Map<String, String> map = new LinkedHashMap<>();

			map.put("label", fieldName.getKey());
			map.put("value", fieldName.getValue());

			labelValueMap.add(map);
		}

		return labelValueMap;
	}

	private void setOthersMetaInfo(Map<String, String> metaInfoMap, RegistrationDTO registrationDTO)
			throws RegBaseCheckedException {

		RegistrationCenterDetailDTO registrationCenter = SessionContext.userContext().getRegistrationCenterDetailDTO();
		if (RegistrationConstants.ENABLE
				.equalsIgnoreCase(environment.getProperty(RegistrationConstants.GPS_DEVICE_DISABLE_FLAG))) {
			metaInfoMap.put(PacketManagerConstants.META_LATITUDE, registrationCenter.getRegistrationCenterLatitude());
			metaInfoMap.put(PacketManagerConstants.META_LONGITUDE, registrationCenter.getRegistrationCenterLongitude());
		}

		Map<String, String> checkSumMap = softwareUpdateHandler.getJarChecksum();
		metaInfoMap.put("checkSum", getJsonString(checkSumMap));
		metaInfoMap.put(PacketManagerConstants.REGISTRATIONID, registrationDTO.getRegistrationId());
	}

	private void setDemographics(RegistrationDTO registrationDTO) throws RegBaseCheckedException {
		LOGGER.debug(LOG_PKT_HANLDER, APPLICATION_NAME, APPLICATION_ID, "Adding demographics to packet manager");
		Map<String, Object> demographics = registrationDTO.getDemographics();

		for (String fieldName : demographics.keySet()) {
			LOGGER.info("Adding demographics for field : {}", fieldName);
			switch (registrationDTO.getFlowType()) {
				case UPDATE:
					if (demographics.get(fieldName) != null && (registrationDTO.getUpdatableFields().contains(fieldName) ||
							fieldName.equals("UIN")))
						setField(registrationDTO.getRegistrationId(), fieldName, demographics.get(fieldName),
								registrationDTO.getProcessId().toUpperCase(), source);
					break;
				case CORRECTION:
				case LOST:
				case NEW:
					if (demographics.get(fieldName) != null)
						setField(registrationDTO.getRegistrationId(), fieldName, demographics.get(fieldName),
								registrationDTO.getProcessId().toUpperCase(), source);
					break;
				}
		}
	}

	private void setDocuments(RegistrationDTO registrationDTO, Map<String, String> metaInfoMap)
			throws RegBaseCheckedException {
		LOGGER.debug("Adding Documents to packet manager");

		List<DocumentMetaInfoDTO> documentMetaInfoDTOs = new LinkedList<>();
		for (String fieldName : registrationDTO.getDocuments().keySet()) {
			DocumentDto document = registrationDTO.getDocuments().get(fieldName);
			DocumentMetaInfoDTO documentMetaInfoDTO = new DocumentMetaInfoDTO();
			documentMetaInfoDTO.setDocumentCategory(document.getCategory());
			documentMetaInfoDTO.setDocumentName(document.getValue());
			documentMetaInfoDTO.setDocumentOwner(document.getOwner());
			documentMetaInfoDTO.setDocumentType(document.getType());
			documentMetaInfoDTO.setRefNumber(document.getRefNumber());

			documentMetaInfoDTOs.add(documentMetaInfoDTO);

			packetWriter.setDocument(registrationDTO.getRegistrationId(), fieldName, getDocument(document),
					source.toUpperCase(), registrationDTO.getProcessId().toUpperCase());
		}

		metaInfoMap.put("documents", getJsonString(documentMetaInfoDTOs));
	}

	private void setBiometrics(RegistrationDTO registrationDTO, Map<String, String> metaInfoMap) throws RegBaseCheckedException {
		LOGGER.debug("Adding Biometrics to packet manager started..");
		Map<String, List<BIR>> capturedBiometrics = new HashMap<>();
		Map<String, Map<String, Object>> capturedMetaInfo = new LinkedHashMap<>();
		Map<String, Map<String, Object>> exceptionMetaInfo = new LinkedHashMap<>();

		for(String key : registrationDTO.getBiometrics().keySet()) {
			String fieldId = key.split("_")[0];
			String bioAttribute = key.split("_")[1];
			BIR bir = birBuilder.buildBIR(registrationDTO.getBiometrics().get(key));
			if (!capturedBiometrics.containsKey(fieldId)) {
				capturedBiometrics.put(fieldId, new ArrayList<>());
			}
			capturedBiometrics.get(fieldId).add(bir);
			if (!capturedMetaInfo.containsKey(fieldId)) {
				capturedMetaInfo.put(fieldId, new HashMap<>());
			}
			capturedMetaInfo.get(fieldId).put(bioAttribute, new BiometricsMetaInfoDto(
					registrationDTO.getBiometrics().get(key).getNumOfRetries(),
					registrationDTO.getBiometrics().get(key).isForceCaptured(),
					bir.getBdbInfo().getIndex()));
		}

		for(String key : registrationDTO.getBiometricExceptions().keySet()) {
			String fieldId = key.split("_")[0];
			String bioAttribute = key.split("_")[1];
			BIR bir = birBuilder.buildBIR(new BiometricsDto(bioAttribute, null, 0));
			capturedBiometrics.getOrDefault(fieldId, new ArrayList<>()).add(bir);
			exceptionMetaInfo.computeIfAbsent(fieldId, field -> new HashMap<>()).put(bioAttribute,
					registrationDTO.getBiometricExceptions().get(key));
		}

		capturedBiometrics.keySet().forEach(fieldId -> {
			BiometricRecord biometricRecord = new BiometricRecord();
			biometricRecord.setOthers(new HashMap<>());
			biometricRecord.getOthers().put(OtherKey.CONFIGURED, String.join(",",
					registrationDTO.CONFIGURED_BIOATTRIBUTES.getOrDefault(fieldId, Collections.EMPTY_LIST)));
			biometricRecord.setSegments(capturedBiometrics.get(fieldId));
			LOGGER.debug("Adding biometric to packet manager for field : {}", fieldId);
			packetWriter.setBiometric(registrationDTO.getRegistrationId(), fieldId, biometricRecord,
					source.toUpperCase(), registrationDTO.getProcessId().toUpperCase());
		});

		metaInfoMap.put("biometrics", getJsonString(capturedMetaInfo));
		metaInfoMap.put("exceptionBiometrics", getJsonString(exceptionMetaInfo));
	}

	private void setAudits(RegistrationDTO registrationDTO) {
		String auditTimestamp = getGlobalConfigValueOf(RegistrationConstants.AUDIT_TIMESTAMP);
		List<Audit> audits = auditDAO.getAudits(registrationDTO.getRegistrationId(), auditTimestamp);

		List<Map<String, String>> auditList = new LinkedList<>();

		for (Audit audit : audits) {
			Map<String, String> auditMap = new LinkedHashMap<>();
			auditMap.put("uuid", audit.getUuid());
			auditMap.put("createdAt", audit.getCreatedAt().format(formatter));
			auditMap.put("eventId", audit.getEventId());
			auditMap.put("eventName", audit.getEventName());
			auditMap.put("eventType", audit.getEventType());
			auditMap.put("hostName", audit.getHostName());
			auditMap.put("hostIp", audit.getHostIp());
			auditMap.put("applicationId", audit.getApplicationId());
			auditMap.put("applicationName", audit.getApplicationName());
			auditMap.put("sessionUserId", audit.getSessionUserId());
			auditMap.put("sessionUserName", audit.getSessionUserName());
			auditMap.put("id", audit.getId());
			auditMap.put("idType", audit.getIdType());
			auditMap.put("createdBy", audit.getCreatedBy());
			auditMap.put("moduleName", audit.getModuleName());
			auditMap.put("moduleId", audit.getModuleId());
			auditMap.put("description", audit.getDescription());
			auditMap.put("actionTimeStamp", audit.getActionTimeStamp().format(formatter));
			auditList.add(auditMap);
		}
		Assert.notEmpty(auditList, "Audit list is empty for the current registration");
		packetWriter.addAudits(registrationDTO.getRegistrationId(), auditList, source.toUpperCase(),
				registrationDTO.getProcessId());
	}

	private void addRegisteredDevices(Map<String, String> metaInfoMap) throws RegBaseCheckedException {
		List<DeviceMetaInfo> capturedRegisteredDevices = new ArrayList<DeviceMetaInfo>();
		MosipDeviceSpecificationFactory.getDeviceRegistryInfo().forEach((deviceName, device) -> {
			DeviceMetaInfo registerdDevice = new DeviceMetaInfo();
			registerdDevice.setDeviceServiceVersion(device.getSerialVersion());
			registerdDevice.setDeviceCode(device.getDeviceCode());
			DigitalId digitalId = new DigitalId();
			digitalId.setDateTime(device.getTimestamp());
			digitalId.setDeviceProvider(device.getDeviceProviderName());
			digitalId.setDeviceProviderId(device.getDeviceProviderId());
			digitalId.setMake(device.getDeviceMake());
			digitalId.setModel(device.getDeviceModel());
			digitalId.setSerialNo(device.getSerialNumber());
			digitalId.setDeviceSubType(device.getDeviceSubType());
			digitalId.setType(device.getDeviceType());
			registerdDevice.setDigitalId(digitalId);
			capturedRegisteredDevices.add(registerdDevice);
		});

		metaInfoMap.put("capturedRegisteredDevices", getJsonString(capturedRegisteredDevices));
	}


	private void setField(String registrationId, String fieldName, Object value, String process, String source)
			throws RegBaseCheckedException {
		LOGGER.debug("Adding demographics to packet manager for field : {}", fieldName);
		packetWriter.setField(registrationId, fieldName, getValueAsString(value), source.toUpperCase(),
				process.toUpperCase());
	}

	private String getValueAsString(Object value) throws RegBaseCheckedException {
		if (value instanceof String) {
			return (String) value;
		} else {
			return getJsonString(value);
		}

	}

	private String getJsonString(Object object) throws RegBaseCheckedException {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (IOException ioException) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_JSON_PROCESSING_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_JSON_PROCESSING_EXCEPTION.getErrorMessage());
		}
	}

	private Document getDocument(DocumentDto documentDto) {
		Document document = new Document();

		document.setDocument(documentDto.getDocument());
		document.setFormat(documentDto.getFormat());
		document.setType(documentDto.getType());
		document.setValue(documentDto.getValue());
		document.setRefNumber(documentDto.getRefNumber());
		return document;
	}
	
	@Override
	public List<Registration> getAllRegistrations() {
		return registrationDAO.getAllRegistrations();
	}
	
	@Override
	public List<PacketStatusDTO> getAllPackets() {
		LOGGER.info("Fetching all the packets that are registered");
		List<PacketStatusDTO> packets = new ArrayList<>();
		List<Registration> registeredPackets = registrationDAO.getAllRegistrations();
		for (Registration registeredPacket : registeredPackets) {
			if (!registeredPacket.getClientStatusCode().equalsIgnoreCase(RegistrationClientStatusCode.CREATED.getCode())) {
				packets.add(preparePacketStatusDto(registeredPacket));
			}
		}
		return packets;
	}

	@Counted
	@Timed
	@Override
	public RegistrationDTO startRegistration(String id, @NonNull String processId) throws RegBaseCheckedException {
		//Pre-check conditions, throws exception if preconditions are not met
		proceedWithRegistration();

		RegistrationDTO registrationDTO = new RegistrationDTO();
		// set id-schema version to be followed for this registration
		registrationDTO.setIdSchemaVersion(identitySchemaService.getLatestEffectiveSchemaVersion());
		ProcessSpecDto processSpecDto = identitySchemaService.getProcessSpecDto(processId, registrationDTO.getIdSchemaVersion());
		registrationDTO.setProcessId(processSpecDto.getId());
		registrationDTO.setFlowType(FlowType.valueOf(processSpecDto.getFlow()));

		// Create object for OSIData DTO
		registrationDTO.setOsiDataDTO(new OSIDataDTO());
		//by default setting the maker ID
		registrationDTO.getOsiDataDTO().setOperatorID(SessionContext.userId());

		// Create RegistrationMetaData DTO & set default values in it
		RegistrationMetaDataDTO registrationMetaDataDTO = new RegistrationMetaDataDTO();
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);

		//set application id
		registrationDTO.setAppId(ridGenerator.generateId(
				(String) ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID),
				(String) ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID)));
		registrationDTO.setRegistrationId(registrationDTO.getAppId());

		LOGGER.info("Registration Started for ApplicationId  : {}", registrationDTO.getAppId());

		List<String> defaultFieldGroups = new ArrayList<>();
		if(processSpecDto.getAutoSelectedGroups() != null)
				defaultFieldGroups.addAll(processSpecDto.getAutoSelectedGroups());

		registrationDTO.setDefaultUpdatableFieldGroups(defaultFieldGroups);
		registrationDTO.setConfiguredBlockListedWords(masterSyncService.getAllBlockListedWords());
		return registrationDTO;
	}

	@Override
	public void createAcknowledgmentReceipt(@NonNull String packetId, byte[] content, String format)
			throws io.mosip.kernel.core.exception.IOException {
		LOGGER.debug("Starting to create Registration ack receipt : {}", packetId);
		byte[] signature = clientCryptoFacade.getClientSecurity().signData(content);
		byte[] key = clientCryptoFacade.getClientSecurity().getEncryptionPublicPart();
		FileUtils.copyToFile(new ByteArrayInputStream(clientCryptoFacade.encrypt(key, content)),
				Paths.get(baseLocation, packetManagerAccount, packetId.concat("_Ack.").concat(format)).toFile());
		registrationDAO.updateAckReceiptSignature(packetId, CryptoUtil.encodeToURLSafeBase64(signature));
	}


	public String getAcknowledgmentReceipt(@NonNull String packetId, @NonNull String filepath)
			throws RegBaseCheckedException, io.mosip.kernel.core.exception.IOException {
		Registration registration = registrationDAO.getRegistrationByPacketId(packetId);

		//handling backward compatibility for existing pre-LTS packets receipt
		if(registration.getAckSignature() == null && registration.getPacketId().equals(registration.getId())) {
			try {
				LOGGER.info("As signature is empty, attempting to sign and encrypt ack receipt : {}", packetId);
				createAcknowledgmentReceipt(packetId,
						FileUtils.readFileToByteArray(new File(filepath)),
						RegistrationConstants.ACKNOWLEDGEMENT_FORMAT);
				registration = registrationDAO.getRegistrationByPacketId(packetId);
			} catch (io.mosip.kernel.core.exception.IOException  ex) {
				LOGGER.error("Failed to sign and encrypt existing ack receipt : {}", packetId, ex);
			}
		}

		byte[] decryptedContent = clientCryptoFacade.decrypt(FileUtils.readFileToByteArray(new File(filepath)));
		boolean isSignatureValid = clientCryptoFacade.getClientSecurity()
				.validateSignature(ClientCryptoUtils.decodeBase64Data(registration.getAckSignature()), decryptedContent);
		if(isSignatureValid)
			return new String(decryptedContent);

		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_ACK_RECEIPT_READ_ERROR.getErrorCode(),
				RegistrationExceptionConstants.REG_ACK_RECEIPT_READ_ERROR.getErrorMessage());
	}
}
