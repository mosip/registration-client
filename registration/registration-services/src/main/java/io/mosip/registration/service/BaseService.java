package io.mosip.registration.service;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.entities.SingleAnySubtypeType;
import io.mosip.kernel.biometrics.entities.VersionType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.PreConditionChecks;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

import javax.imageio.ImageIO;

/**
 * This is a base class for service package. The common functionality across the
 * 'services' classes are implemented in this class to inherit this property at
 * the required extended classes.
 * 
 */
@Service
public class BaseService {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BaseService.class);

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);

	/**
	 * serviceDelegateUtil which processes the HTTPRequestDTO requests
	 */
	@Autowired
	protected ServiceDelegateUtil serviceDelegateUtil;

	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private UserDetailService userDetailService;

	@Autowired
	private CenterMachineReMapService centerMachineReMapService;

	@Autowired
	private RegistrationCenterDAO registrationCenterDAO;

	@Autowired
	private MachineMasterRepository machineMasterRepository;

	@Autowired
	private LocalConfigService localConfigService;

	@Autowired
	private PolicySyncService policySyncService;

	@Autowired
	private RegistrationCenterRepository registrationCenterRepository;
	
	@Value("#{'${mosip.mandatory-languages:}'.split('[,]')}")
	private List<String> mandatoryLanguages;

	@Value("#{'${mosip.optional-languages:}'.split('[,]')}")
	private List<String> optionalLanguages;
	
	@Value("${mosip.min-languages.count:0}")
	private int minLanguagesCount;

	@Value("${mosip.max-languages.count:0}")
	private int maxLanguagesCount;

	public List<String> getMandatoryLanguages() {
		return mandatoryLanguages.stream()
				.filter(item-> !item.isBlank())
				.map(String::strip)
				.map(String::toLowerCase)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<String> getOptionalLanguages() throws PreConditionCheckException {
		List<String> mandatoryLang = getMandatoryLanguages();
		List<String> optionalLang = optionalLanguages.stream()
				.filter(item-> !item.isBlank())
				.map(String::strip)
				.map(String::toLowerCase)
				.distinct()
				.collect(Collectors.toList());

		if(mandatoryLang.isEmpty() && optionalLang.isEmpty()) {
			LOGGER.error("BOTH MANDATORY AND OPTIONAL LANGUAGES ARE EMPTY");
			throw new PreConditionCheckException(PreConditionChecks.INVALID_LANG_CONFIG.name(),
					"BOTH MANDATORY AND OPTIONAL LANGUAGES ARE EMPTY");
		}

		return ListUtils.subtract(optionalLang, mandatoryLang);
	}

	public int getMinLanguagesCount() {
		List<String> mandatoryLang = getMandatoryLanguages();

		return  ( minLanguagesCount <=0 ) ?
						//min-count is 0 / less than 0, then set to mandatory list size
							( mandatoryLang.size() > 0 ?  mandatoryLang.size()  : 1 )
						//if min-count is greater than 0,
							// check if its greater than mandatory list size then set it back to mandatory list size
							: (minLanguagesCount < mandatoryLang.size() ? mandatoryLang.size() : minLanguagesCount);
	}

	public int getMaxLanguagesCount() throws PreConditionCheckException {
		List<String> mandatoryLang = getMandatoryLanguages();
		List<String> optionalLang = getOptionalLanguages();
		int minCount = getMinLanguagesCount();
		int idealMaxCount = mandatoryLang.size() + optionalLang.size();

		//max-count is (0 / less than 0) OR ( greater than mandatory + optional size), set to mandatory + optional size
		return maxLanguagesCount <= 0 || maxLanguagesCount > idealMaxCount ? idealMaxCount :
				maxLanguagesCount < minCount ? minCount : maxLanguagesCount;
	}


	/**
	 * create success response.
	 *
	 * @param responseDTO the response DTO
	 * @param message     the message
	 * @param attributes  the attributes
	 * @return ResponseDTO returns the responseDTO after creating appropriate
	 *         success response and mapping to it
	 */
	public ResponseDTO setSuccessResponse(ResponseDTO responseDTO, String message, Map<String, Object> attributes) {

		/** Success Response */
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();

		successResponseDTO.setMessage(message);
		successResponseDTO.setCode(RegistrationConstants.ALERT_INFORMATION);

		/** Adding attributes to success response */
		successResponseDTO.setOtherAttributes(attributes);

		responseDTO.setSuccessResponseDTO(successResponseDTO);
		return responseDTO;
	}

	/**
	 * create error response.
	 *
	 * @param response   the response
	 * @param message    the message
	 * @param attributes the attributes
	 * @return ResponseDTO returns the responseDTO after creating appropriate error
	 *         response and mapping to it
	 */
	protected ResponseDTO setErrorResponse(final ResponseDTO response, final String message,
			final Map<String, Object> attributes) {

		/** Create list of Error Response */
		List<ErrorResponseDTO> errorResponses = (response.getErrorResponseDTOs() != null)
				? response.getErrorResponseDTOs()
				: new LinkedList<>();

		/** Error response */
		ErrorResponseDTO errorResponse = new ErrorResponseDTO();

		errorResponse.setCode(RegistrationConstants.ERROR);
		errorResponse.setMessage(message);

		errorResponse.setOtherAttributes(attributes);

		errorResponses.add(errorResponse);

		/** Adding list of error responses to response */
		response.setErrorResponseDTOs(errorResponses);
		return response;

	}

	/**
	 * Get User Id using session context.
	 *
	 * @return user id
	 */
	public String getUserIdFromSession() {
		String userId = null;
		if (SessionContext.isSessionContextAvailable()) {
			userId = SessionContext.userId();
			if (userId.equals(RegistrationConstants.AUDIT_DEFAULT_USER)) {
				userId = RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
			}
		} else {
			userId = RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
		}
		return userId;
	}


	/**
	 * Checks if is null.
	 *
	 * @param list the list
	 * @return true, if is null
	 */
	public boolean isNull(List<?> list) {
		/* Check Whether the list is Null or not */
		return list == null;

	}

	/**
	 * Checks if is empty.
	 *
	 * @param list the list
	 * @return true, if is empty
	 */
	public boolean isEmpty(List<?> list) {
		/* Check Whether the list is empty or not */
		return list.isEmpty();
	}

	/**
	 * Gets the station id.
	 *
	 * @return the station id
	 */
	public String getStationId() {
		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		MachineMaster machineMaster = machineMasterRepository.findByNameIgnoreCase(machineName.toLowerCase());

		if(machineMaster != null && machineMaster.getId() != null && machineMaster.getIsActive())
			return machineMaster.getId();

		LOGGER.error("Machine fetched {}", machineMaster);
		return null;
	}
	
	public MachineMaster getMachine() throws RegBaseCheckedException {
		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		MachineMaster machineMaster = machineMasterRepository.findByNameIgnoreCase(machineName.toLowerCase());
		
		if (machineMaster == null) {
			throw new RegBaseCheckedException("REG-AUTH-001", "Machine not found");
		}
		return machineMaster;
	}


	/**
	 * Gets the center id.
	 * @return
	 */
	public String getCenterId() {
		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		MachineMaster machineMaster = machineMasterRepository.findByNameIgnoreCase(machineName.toLowerCase());

		if(machineMaster != null && machineMaster.getRegCenterId() != null) {
			Optional<RegistrationCenter> result = registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(
					machineMaster.getRegCenterId(),	ApplicationContext.applicationLanguage());

			LOGGER.debug("Active Reg center entry present {}", result.isPresent());
			return result.isPresent() ? result.get().getRegistartionCenterId().getId() : null;
		}

		LOGGER.error("Reg centerId fetched from machine entry {}", machineMaster);
		return null;
	}

	/**
	 * Get Global Param configuration value.
	 *
	 * @param key the name
	 * @return value
	 */
	public String getGlobalConfigValueOf(String key) {

		String val = null;
		if (key != null) {
			ApplicationContext.getInstance();
			// Check application map
			if (ApplicationContext.map().isEmpty() || ApplicationContext.map().get(key) == null) {
				// Load Global params if application map is empty
				Map<String, Object> globalProps = globalParamService.getGlobalParams();
				globalProps.putAll(localConfigService.getLocalConfigurations());
				ApplicationContext.setApplicationMap(globalProps);
			}

			// Get Value of global param
			val = (String) ApplicationContext.map().get(key);
		}
		return val;
	}

	/**
	 * Conversion of Registration to Packet Status DTO.
	 *
	 * @param registration the registration
	 * @return the packet status DTO
	 */
	public PacketStatusDTO preparePacketStatusDto(Registration registration) {
		PacketStatusDTO statusDTO = new PacketStatusDTO();
		statusDTO.setFileName(registration.getAppId());
		statusDTO.setPacketId(registration.getPacketId());
		statusDTO.setPacketClientStatus(registration.getClientStatusCode());
		statusDTO.setClientStatusComments(registration.getClientStatusComments());
		statusDTO.setPacketServerStatus(registration.getServerStatusCode());
		statusDTO.setPacketPath(registration.getAckFilename());
		statusDTO.setUploadStatus(registration.getFileUploadStatus());
		statusDTO.setPacketStatus(registration.getStatusCode());
		statusDTO.setSupervisorStatus(registration.getClientStatusCode());
		statusDTO.setSupervisorComments(registration.getClientStatusComments());
		statusDTO.setCreatedTime(regDateTimeConversion(registration.getCrDtime().toString()));
		statusDTO.setUserId(registration.getRegUsrId());

		try {
			if (registration.getAdditionalInfo() != null) {
				String additionalInfo = new String(registration.getAdditionalInfo());
				RegistrationDataDto registrationDataDto = (RegistrationDataDto) JsonUtils
						.jsonStringToJavaObject(RegistrationDataDto.class, additionalInfo);
				statusDTO.setName(registrationDataDto.getName());
				statusDTO.setPhone(registrationDataDto.getPhone());
				statusDTO.setEmail(registrationDataDto.getEmail());
				statusDTO.setSelectedLanguages(registrationDataDto.getLangCode());
			}
		} catch (JsonParseException | JsonMappingException | io.mosip.kernel.core.exception.IOException exception) {
			LOGGER.error("REGISTRATION_BASE_SERVICE", APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}

		try (FileInputStream fis = new FileInputStream(FileUtils.getFile(registration.getAckFilename().replace(
				RegistrationConstants.ACKNOWLEDGEMENT_FILE_EXTENSION, RegistrationConstants.ZIP_FILE_EXTENSION)))) {
			byte[] byteArray = new byte[(int) fis.available()];
			fis.read(byteArray);
			statusDTO.setPacketHash(HMACUtils2.digestAsPlainText(byteArray));
			statusDTO.setPacketSize(BigInteger.valueOf(byteArray.length));

		} catch (IOException | NoSuchAlgorithmException ioException) {
			LOGGER.error("REGISTRATION_BASE_SERVICE", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}

		return statusDTO;
	}

	/**
	 * Registration date conversion.
	 *
	 * @param timestamp the timestamp
	 * @return the string
	 */
	protected String regDateConversion(Timestamp timestamp) {

		DateFormat dateFormat = new SimpleDateFormat(RegistrationConstants.EOD_PROCESS_DATE_FORMAT);
		Date date = new Date(timestamp.getTime());
		return dateFormat.format(date);
	}

	protected String regDateTimeConversion(String time) {
		try {
			String formattedTime = Timestamp.valueOf(time).toLocalDateTime()
					.format(DateTimeFormatter.ofPattern(RegistrationConstants.UTC_PATTERN));
			LocalDateTime dateTime = DateUtils.parseUTCToLocalDateTime(formattedTime);
			return dateTime.format(DateTimeFormatter.ofPattern(RegistrationConstants.TEMPLATE_DATE_FORMAT));
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			return time + RegistrationConstants.UTC_APPENDER;
		}
	}

	protected boolean isNull(String val) {
		return (val == null || val.equalsIgnoreCase("NULL"));
	}

	/**
	 * Common method to throw {@link RegBaseCheckedException} based on the
	 * {@link RegistrationExceptionConstants} enum passed as parameter. Extracts the
	 * error code and error message from the enum parameter.
	 * 
	 * @param exceptionEnum the enum of {@link RegistrationExceptionConstants}
	 *                      containing the error code and error message to be thrown
	 * @throws RegBaseCheckedException the checked exception
	 */
	protected void throwRegBaseCheckedException(RegistrationExceptionConstants exceptionEnum)
			throws RegBaseCheckedException {
		throw new RegBaseCheckedException(exceptionEnum.getErrorCode(), exceptionEnum.getErrorMessage());
	}

	/**
	 * Validates the input {@link List} is either <code>null</code> or empty
	 * 
	 * @param listToBeValidated the {@link List} object to be validated
	 * @return <code>true</code> if {@link List} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isListEmpty(List<?> listToBeValidated) {
		return listToBeValidated == null || listToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link Set} is either <code>null</code> or empty
	 * 
	 * @param setToBeValidated the {@link Set} object to be validated
	 * @return <code>true</code> if {@link Set} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isSetEmpty(Set<?> setToBeValidated) {
		return setToBeValidated == null || setToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link String} is either <code>null</code> or empty
	 * 
	 * @param stringToBeValidated the {@link String} object to be validated
	 * @return <code>true</code> if input {@link String} is either <code>null</code>
	 *         or empty, else <code>false</code>
	 */
	protected boolean isStringEmpty(String stringToBeValidated) {
		return stringToBeValidated == null || stringToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link Map} is either <code>null</code> or empty
	 * 
	 * @param mapToBeValidated the {@link Map} object to be validated
	 * @return <code>true</code> if {@link Map} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isMapEmpty(Map<?, ?> mapToBeValidated) {
		return mapToBeValidated == null || mapToBeValidated.isEmpty();
	}

	/**
	 * Validates the input byte array is either <code>null</code> or empty
	 * 
	 * @param byteArrayToBeValidated the byte array to be validated
	 * @return <code>true</code> if byte array is either <code>null</code> or empty,
	 *         else <code>false</code>
	 */
	protected boolean isByteArrayEmpty(byte[] byteArrayToBeValidated) {
		return byteArrayToBeValidated == null || byteArrayToBeValidated.length == 0;
	}

	/**
	 * Validates if the error code of the input {@link Exception} is same of the
	 * error code of Auth Token Empty
	 * 
	 * @param exception the {@link Exception} to be validated
	 * @return <code>true</code> if error code is same as Auth Token empty
	 */
	protected boolean isAuthTokenEmptyException(Exception exception) {
		return exception instanceof RegBaseCheckedException
				&& RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode()
						.equals(((RegBaseCheckedException) exception).getErrorCode());
	}


	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	protected Map<String, Object> getRegistrationDTODemographics() {
		return (Map<String, Object>)SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA_DEMO);
	}

	public BIR buildBir(String bioAttribute, long qualityScore, byte[] iso, ProcessedLevelType processedLevelType) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Building BIR for captured biometrics to pass them for quality check with SDK");

		BiometricType biometricType = Biometric.getSingleTypeByAttribute(bioAttribute);
		
		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(biometricType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore(qualityScore);

		return new BIR.BIRBuilder().withBdb(iso)
				.withVersion(new VersionType(1, 1))
				.withCbeffversion(new VersionType(1, 1))
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, bioAttribute))
						.withPurpose(PurposeType.IDENTIFY).withLevel(processedLevelType)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();

	}

	public BIR buildBir(BiometricsDto biometricsDto) {
		LOGGER.info("Building BIR for captured biometrics to pass them for quality check with SDK");

		BiometricType biometricType = Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute());

		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(biometricType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) biometricsDto.getQualityScore());

		return new BIR.BIRBuilder().withBdb(biometricsDto.getAttributeISO())
				.withVersion(new VersionType(1, 1))
				.withCbeffversion(new VersionType(1, 1))
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, biometricsDto.getBioAttribute()))
						.withPurpose(PurposeType.IDENTIFY).withLevel(ProcessedLevelType.RAW)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();
	}

	private List<String> getSubTypes(BiometricType biometricType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();
		switch (biometricType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			if (bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger"))
						.value());
			}
			break;
		case IRIS:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			break;
		case FACE:
			subtypes.add(BiometricType.FACE.value());
			break;
		default:
			break;
		}
		LOGGER.info("Building BIR with subtypes : {}", subtypes);
		return subtypes;
	}

	/**
	 * Converts string to java.sql.Timestamp
	 *
	 * @param time
	 * @return
	 * @throws RegBaseCheckedException
	 */
	public Timestamp getTimestamp(String time) throws RegBaseCheckedException {
		try {
			Date date = simpleDateFormat.parse(time);
			Timestamp timestamp = new Timestamp(date.getTime());
			return timestamp;
		} catch (ParseException e) {
			LOGGER.error("", APPLICATION_NAME, APPLICATION_ID, e.getMessage());
		}
		throw new RegBaseCheckedException(RegistrationConstants.SYNC_TRANSACTION_RUNTIME_EXCEPTION,
				"Failed to parse lastSyncTime from server : " + time);
	}

	public ResponseDTO getHttpResponseErrors(ResponseDTO responseDTO, LinkedHashMap<String, Object> httpResponse) {
		List<ErrorResponseDTO> erResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setCode(RegistrationConstants.ERRORS);
		String errorMessage = RegistrationConstants.API_CALL_FAILED;
		if(httpResponse != null && httpResponse.get(RegistrationConstants.ERRORS) != null) {
			List<HashMap<String, String>> errors = (List<HashMap<String, String>>) httpResponse.get(RegistrationConstants.ERRORS);
			LOGGER.error("Response Errors >>>> {}", errors);
			errorMessage = errors.isEmpty() ? RegistrationConstants.API_CALL_FAILED : errors.get(0).get(RegistrationConstants.ERROR_MSG);
		}
		errorResponseDTO.setMessage(errorMessage);
		erResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(erResponseDTOs);
		return responseDTO;
	}

	public void commonPreConditionChecks(String action) throws PreConditionCheckException {
		if(!serviceDelegateUtil.isNetworkAvailable())
			throw new PreConditionCheckException(PreConditionChecks.NO_CONNECTION.name(),
					action + " forbidden as User is inactive");

		if(SessionContext.isSessionContextAvailable() &&
				!userDetailService.isValidUser(SessionContext.userId()) && !isInitialSync())
			throw new PreConditionCheckException(PreConditionChecks.USER_INACTIVE.name(),
					action + " forbidden as User is inactive");
	}

	public void proceedWithMasterAndKeySync(String jobId) throws PreConditionCheckException {
		commonPreConditionChecks("Sync");

		//Donot validate pre-conditions as its initial sync
		if(isInitialSync()) {
			LOGGER.warn("", APPLICATION_NAME, APPLICATION_ID, "Ignoring pre-checks as its Initial sync");
			return;
		}

		//check if remap is in progress
		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Sync forbidden as machine is marked for center remap");

		String machineId = getStationId();
		if(RegistrationConstants.OPT_TO_REG_PDS_J00003.equals(jobId) && machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Pre Registration Data Sync action forbidden as machine is inactive");

		//check regcenter table for center status
		//if center is inactive, sync is not allowed
		if(!registrationCenterDAO.isMachineCenterActive())
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Sync action forbidden as center is inactive");
	}

	public void proceedWithPacketSync() throws PreConditionCheckException {
		commonPreConditionChecks("Packet Sync");
	}

	public void proceedWithMachineCenterRemap() throws PreConditionCheckException {
		commonPreConditionChecks("Center Remap");
	}

	public void proceedWithSoftwareUpdate() throws PreConditionCheckException {
		commonPreConditionChecks("Software update");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Software update forbidden as machine is marked for center remap");

		//TODO - check if this shld be allowed when machine / center is inactive
	}

	public void proceedWithOperatorOnboard() throws PreConditionCheckException {
		commonPreConditionChecks("Onboarding");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Onboarding forbidden as machine is marked for center remap");

		//RegistrationUIConstants.getMessageLanguageSpecific("CENTER_MACHINE_INACTIVE
		String machineId = getStationId();
		if(machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Onboarding action forbidden as machine is inactive");


		if(!registrationCenterDAO.isMachineCenterActive())
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Onboarding action forbidden as center is inactive");
	}

	public void proceedWithRegistration() throws PreConditionCheckException {
		if(!SessionContext.isSessionContextAvailable() ||
				!userDetailService.isValidUser(SessionContext.userId()))
			throw new PreConditionCheckException(PreConditionChecks.USER_INACTIVE.name(),
					"Registration forbidden as User is inactive");

		if(isInitialSync())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_INITIAL_SETUP.name(),
					"Registration forbidden as machine is marked for initial setup");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Registration forbidden as machine is marked for center remap");

		//RegistrationUIConstants.getMessageLanguageSpecific("CENTER_MACHINE_INACTIVE
		String machineId = getStationId();
		if(machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Registration forbidden as machine is inactive");

		if(!registrationCenterDAO.isMachineCenterActive())
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Registration forbidden as center is inactive");

		ResponseDTO responseDTO = policySyncService.checkKeyValidation();
		if(responseDTO == null || responseDTO.getSuccessResponseDTO() == null ||
				!responseDTO.getSuccessResponseDTO().getMessage().equals(RegistrationConstants.VALID_KEY))
			throw new PreConditionCheckException(PreConditionChecks.NO_OR_INVALID_POLICY_KEY.name(),
					"Registration forbidden as client POLICY_KEY is INVALID");
	}

	public void proceedWithReRegistration() throws PreConditionCheckException {
		if(SessionContext.isSessionContextAvailable() &&
				!userDetailService.isValidUser(SessionContext.userId()) && !isInitialSync())
			throw new PreConditionCheckException(PreConditionChecks.USER_INACTIVE.name(),
					"Registration forbidden as User is inactive");

		String machineId = getStationId();
		if(machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Registration forbidden as machine is inactive");

		if(!registrationCenterDAO.isMachineCenterActive())
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Registration forbidden as center is inactive");

		ResponseDTO responseDTO = policySyncService.checkKeyValidation();
		if(responseDTO == null || responseDTO.getSuccessResponseDTO() == null ||
				!responseDTO.getSuccessResponseDTO().getMessage().equals(RegistrationConstants.VALID_KEY))
			throw new PreConditionCheckException(PreConditionChecks.NO_OR_INVALID_POLICY_KEY.name(),
					"Registration forbidden as client POLICY_KEY is INVALID");
	}

	/**
	 * Checks if this is initial launch
	 * @return
	 */
	public boolean isInitialSync() {
		return RegistrationConstants.ENABLE.equalsIgnoreCase(getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP));
	}

	public BufferedImage concatImages(byte[] image1, byte[] image2, byte[] image3, byte[] image4, String imagePath) {
		try {
			BufferedImage img1 = ImageIO.read((image1 == null || image1.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image1));
			BufferedImage img2 = ImageIO.read((image2 == null || image2.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image2));
			BufferedImage img3 = ImageIO.read((image3 == null || image3.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image3));
			BufferedImage img4 = ImageIO.read((image4 == null || image4.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image4));
			int offset = 2;
			int width = img1.getWidth() + img2.getWidth() + img3.getWidth() + img4.getWidth() + offset;
			int height = Math.max (Math.max(img1.getHeight(), img2.getHeight()), Math.max(img3.getHeight(), img4.getHeight()) )+ offset;
			BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = newImage.createGraphics();
			Color oldColor = g2.getColor();
			g2.setPaint(Color.WHITE);
			g2.fillRect(0, 0, width, height);
			g2.setColor(oldColor);
			g2.drawImage(img1, null, 0, 0);
			g2.drawImage(img2, null, img1.getWidth() + offset, 0);
			g2.drawImage(img3, null, img1.getWidth() + img2.getWidth() + offset, 0);
			g2.drawImage(img4, null, img1.getWidth() + img2.getWidth() + img3.getWidth() + offset, 0);
			g2.dispose();
			return newImage;
		} catch (IOException e) {
			LOGGER.error("Error while concat images", e);
		}
		return null;
	}

	public BufferedImage concatImages(byte[] image1, byte[] image2, String imagePath) {
		try {
			BufferedImage img1 = ImageIO.read((image1 == null || image1.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image1));
			BufferedImage img2 = ImageIO.read((image2 == null || image2.length == 0) ?
					this.getClass().getResourceAsStream(imagePath) :
					new ByteArrayInputStream(image2));
			int offset = 2;
			int width = img1.getWidth() + img2.getWidth() + offset;
			int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
			BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = newImage.createGraphics();
			Color oldColor = g2.getColor();
			g2.setPaint(Color.WHITE);
			g2.fillRect(0, 0, width, height);
			g2.setColor(oldColor);
			g2.drawImage(img1, null, 0, 0);
			g2.drawImage(img2, null, img1.getWidth() + offset, 0);
			g2.dispose();
			return newImage;
		} catch (IOException e) {
			LOGGER.error("Error while concat images", e);
		}
		return null;
	}

	public List<String> getConfiguredLangCodes() throws PreConditionCheckException {
		try {
			return ListUtils.union(getMandatoryLanguages(), getOptionalLanguages());
		} catch (PreConditionCheckException e) {
			throw e;
		}
	}
}
