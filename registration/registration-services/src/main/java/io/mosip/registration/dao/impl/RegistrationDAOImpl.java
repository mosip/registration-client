package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_SAVE_PKT;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.IdentitySchemaService;
import lombok.NonNull;

/**
 * The implementation class of {@link RegistrationDAO}.
 *
 * @author Balaji Sridharan
 * @author Mahesh Kumar
 * @author Saravanakumar Gnanaguru
 * @since 1.0.0
 */
@Repository
@Transactional
public class RegistrationDAOImpl implements RegistrationDAO {

	/** The registration repository. */
	@Autowired
	private RegistrationRepository registrationRepository;
	
	@Autowired
	private IdentitySchemaService identitySchemaService;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationDAOImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.RegistrationDAO#save(java.lang.String,
	 * io.mosip.registration.dto.RegistrationDTO)
	 */
	@Override
	public void save(String zipFileName, RegistrationDTO registrationDTO) throws RegBaseCheckedException {
		try {
			LOGGER.info(LOG_SAVE_PKT, APPLICATION_NAME, APPLICATION_ID, "Save Registartion has been started");

			Timestamp time = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

			Registration registration = new Registration();
			registration.setId(registrationDTO.getRegistrationId());
			registration.setRegType(registrationDTO.getFlowType().getRegistrationTypeCode());
			registration.setStatusCode(registrationDTO.getProcessId().toUpperCase());
			registration.setLangCode(RegistrationConstants.ENGLISH_LANG_CODE);
			registration.setStatusTimestamp(time);
			registration.setAckFilename(zipFileName + "_Ack." + RegistrationConstants.ACKNOWLEDGEMENT_FORMAT);
			registration.setClientStatusCode(RegistrationClientStatusCode.CREATED.getCode());
			registration.setServerStatusCode(null);
			registration.setUploadCount((short) 0);
			registration.setRegCntrId(
					SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterId());
			registration.setIsActive(true);
			registration.setCrBy(SessionContext.userContext().getUserId());
			registration.setCrDtime(time);
			registration.setRegUsrId(SessionContext.userContext().getUserId());
			registration.setApproverUsrId(SessionContext.userContext().getUserId());
			registration.setPreRegId(registrationDTO.getPreRegistrationId());
			registration.setAppId(registrationDTO.getAppId());
			registration.setPacketId(registrationDTO.getPacketId());
			registration.setAdditionalInfoReqId(registrationDTO.getAdditionalInfoReqId());
			
			RegistrationDataDto registrationDataDto = new RegistrationDataDto();
			
			List<String> fullName = new ArrayList<>();
			String fullNameKey = getKey(registrationDTO, RegistrationConstants.UI_SCHEMA_SUBTYPE_FULL_NAME);
			if(fullNameKey != null) {
				List<String> fullNameKeys = Arrays.asList(fullNameKey.split(RegistrationConstants.COMMA));
				for (String key : fullNameKeys) {
					Object fullNameObj = registrationDTO.getDemographics().get(key);
					fullName.add(getAdditionalInfo(fullNameObj));
				}
			}

			Object emailObj = registrationDTO.getDemographics().get(getKey(registrationDTO, RegistrationConstants.UI_SCHEMA_SUBTYPE_EMAIL));
			Object phoneObj = registrationDTO.getDemographics().get(getKey(registrationDTO, RegistrationConstants.UI_SCHEMA_SUBTYPE_PHONE));
			
			fullName.removeIf(Objects::isNull);
			registrationDataDto.setName(String.join(RegistrationConstants.SPACE, fullName));
			registrationDataDto.setEmail(getAdditionalInfo(emailObj));
			registrationDataDto.setPhone(getAdditionalInfo(phoneObj));
			registrationDataDto.setLangCode(String.join(RegistrationConstants.COMMA,
					registrationDTO.getSelectedLanguagesByApplicant()));
			
			String additionalInfo = JsonUtils.javaObjectToJsonString(registrationDataDto);
			registration.setAdditionalInfo(additionalInfo.getBytes());

			registration.setHasBwords(!registrationDTO.BLOCKLISTED_CHECK.isEmpty());
			
			registrationRepository.save(registration);

			LOGGER.info(LOG_SAVE_PKT, APPLICATION_NAME, APPLICATION_ID, "Save Registration has been ended");
		} catch (RuntimeException | JsonProcessingException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.REG_PACKET_SAVE_TO_DB_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_PACKET_SAVE_TO_DB_EXCEPTION.getErrorMessage(), runtimeException);
		}
	}
	
	private String getKey(RegistrationDTO registrationDTO, String subType) throws RegBaseCheckedException {
		List<String> key = new ArrayList<>();
		List<UiFieldDTO> schemaFields = identitySchemaService.getAllFieldSpec(registrationDTO.getProcessId(), registrationDTO.getIdSchemaVersion());
		for (UiFieldDTO schemaField : schemaFields) {
			if (schemaField.getSubType() != null && schemaField.getSubType().equalsIgnoreCase(subType)) {
				if (subType.equalsIgnoreCase(RegistrationConstants.UI_SCHEMA_SUBTYPE_FULL_NAME)) {
					key.add(schemaField.getId());
				} else {
					key.add(schemaField.getId());
					break;
				}
			}
		}
		return String.join(RegistrationConstants.COMMA, key);
	}

	private String getAdditionalInfo(Object fieldValue) {
		if(fieldValue == null) { return null; }

		if (fieldValue instanceof List<?>) {
			Optional<SimpleDto> demoValueInRequiredLang = ((List<SimpleDto>) fieldValue).stream()
					.filter(valueDTO -> valueDTO.getLanguage().equals(ApplicationContext.applicationLanguage())).findFirst();

			if (demoValueInRequiredLang.isPresent()) {
				return demoValueInRequiredLang.get().getValue();
			}
		}

		if (fieldValue instanceof String) {
			return (String) fieldValue;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationDAO#updateStatus(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Registration updateRegistration(String packetID, String statusComments, String clientStatusCode) {
		try {
			LOGGER.info("REGISTRATION - UPDATE_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
					"Packet updation has been started");

			Timestamp timestamp = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
			Registration registration = registrationRepository.getOne(packetID);
			// registration.setStatusCode(clientStatusCode);
			registration.setStatusTimestamp(timestamp);
			registration.setClientStatusCode(clientStatusCode);
			registration.setClientStatusTimestamp(timestamp);
			registration.setClientStatusComments(statusComments);
			registration.setApproverUsrId(SessionContext.userContext().getUserId());
			registration.setApproverRoleCode(SessionContext.userContext().getRoles().get(0));
			registration.setUpdBy(SessionContext.userContext().getUserId());
			registration.setUpdDtimes(timestamp);

			LOGGER.info("REGISTRATION - UPDATE_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
					"Packet updation has been ended");

			return registrationRepository.update(registration);
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_UPDATE_STATUS,
					runtimeException.toString());
		}
	}
	
	@Override
	public Registration updateRegistrationWithPacketId(String packetId, String statusComments, String clientStatusCode) {
		try {
			LOGGER.info("REGISTRATION - UPDATE_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
					"Packet updation has been started");

			Timestamp timestamp = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
			Registration registration = registrationRepository.findByPacketId(packetId);
			// registration.setStatusCode(clientStatusCode);
			registration.setStatusTimestamp(timestamp);
			registration.setClientStatusCode(clientStatusCode);
			registration.setClientStatusTimestamp(timestamp);
			registration.setClientStatusComments(statusComments);
			registration.setApproverUsrId(SessionContext.userContext().getUserId());
			registration.setApproverRoleCode(SessionContext.userContext().getRoles().get(0));
			registration.setUpdBy(SessionContext.userContext().getUserId());
			registration.setUpdDtimes(timestamp);

			LOGGER.info("REGISTRATION - UPDATE_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
					"Packet updation has been ended");

			return registrationRepository.update(registration);
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_UPDATE_STATUS,
					runtimeException.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationDAO#getEnrollmentByStatus(java.lang.
	 * String)
	 */
	@Override
	public List<Registration> getEnrollmentByStatus(String status) {
		LOGGER.info("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Retrieving packets based on status");

		return registrationRepository.findByclientStatusCodeOrderByCrDtime(status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegistrationDAO#getPacketsToBeSynched(java.util.
	 * List)
	 */
	public List<Registration> getPacketsToBeSynched(List<String> statusCodes) {
		return registrationRepository.findByClientStatusCodeInOrderByUpdDtimesDesc(statusCodes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegistrationDAO#getRegistrationByStatus(java.util.
	 * List)
	 */
	@Override
	public List<Registration> getRegistrationByStatus(List<String> packetStatus) {
		LOGGER.info("get the packet details by status");

		return registrationRepository.findByStatusCodes(packetStatus.get(0), packetStatus.get(1), packetStatus.get(2),
				packetStatus.get(3));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationDAO#updateRegStatus(java.lang.String)
	 */
	public Registration updateRegStatus(PacketStatusDTO registrationPacket) {
		LOGGER.info("Updating the packet details in the Registration table");

		Timestamp timestamp = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());

		Registration reg = registrationRepository.findByPacketId(registrationPacket.getPacketId());
		reg.setClientStatusCode(registrationPacket.getPacketClientStatus());
		if (registrationPacket.getUploadStatus() != null) {
			reg.setFileUploadStatus(registrationPacket.getUploadStatus());
		}
		reg.setIsActive(true);
		reg.setUploadTimestamp(timestamp);
		reg.setClientStatusTimestamp(timestamp);
		reg.setClientStatusComments(registrationPacket.getClientStatusComments());
		reg.setUpdDtimes(timestamp);
		reg.setUploadCount((short) (reg.getUploadCount() + 1));
		reg.setUpdBy(SessionContext.userContext().getUserId());
		reg.setServerStatusCode(registrationPacket.getPacketServerStatus());
		return registrationRepository.update(reg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegistrationDAO#updatePacketSyncStatus(io.mosip.
	 * registration.entity.Registration)
	 */
	public Registration updatePacketSyncStatus(PacketStatusDTO packet) {
		LOGGER.info("REGISTRATION - UPDATE_THE_PACKET_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Updating the packet details in the Registration table");

		Timestamp timestamp = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
		Registration reg = registrationRepository.getOne(packet.getPacketId());
		// reg.setStatusCode(packet.getPacketClientStatus());
		reg.setClientStatusCode(packet.getPacketClientStatus());
		reg.setIsActive(true);
		reg.setUploadTimestamp(timestamp);
		return registrationRepository.update(reg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegistrationDAO#getAllReRegistrationPackets(java.
	 * lang.String[])
	 */
	public List<Registration> getAllReRegistrationPackets(String clientStatus, List<String> serverStatus) {
		return registrationRepository.findByClientStatusCodeAndServerStatusCodeIn(clientStatus, serverStatus);
	}

	@Override
	public List<Registration> get(List<String> packetIds) {
		LOGGER.debug("Get Registrations based on packetIds started");

		return registrationRepository.findByPacketIdIn(packetIds);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.RegistrationDAO#get(java.sql.Timestamp,
	 * java.lang.String)
	 */
	@Override
	public List<Registration> get(Timestamp crDtimes, List<String> serverStatusCodes) {

		LOGGER.debug("Retrieving Registrations based on crDtime and status codes");

		return registrationRepository.findByCrDtimeBeforeAndServerStatusCodeIn(crDtimes, serverStatusCodes);

	}

	@Override
	public List<Registration> findByServerStatusCodeIn(List<String> serverStatusCodes) {

		LOGGER.debug("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Retrieving Registrations based on server status codes");

		return registrationRepository.findByServerStatusCodeIn(serverStatusCodes);

	}

	@Override
	public List<Registration> findByServerStatusCodeNotIn(List<String> serverStatusCodes) {

		LOGGER.debug("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Retrieving Registrations based on server status codes");

		return registrationRepository.findByServerStatusCodeNotInOrServerStatusCodeIsNull(serverStatusCodes);

	}

	public List<Registration> fetchPacketsToUpload(List<String> clientStatus, String serverStatus) {

		LOGGER.debug("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Retrieving Registrations based on client and server status codes");

		return registrationRepository.findByClientStatusCodeInOrServerStatusCodeOrderByUpdDtimesDesc(clientStatus,
				serverStatus);
	}
	
	@Override
	public List<Registration> fetchReRegisterPendingPackets() {

		LOGGER.debug("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"fetchReRegisterPendingPackets -Retrieving Registrations based on client and server status codes");

		return registrationRepository.findByClientStatusCodeNotInAndServerStatusCodeIn(
				Arrays.asList(RegistrationClientStatusCode.RE_REGISTER.getCode()),
				Arrays.asList(RegistrationConstants.PACKET_STATUS_CODE_REREGISTER));
	}
	
	@Override
	public List<Registration> getAllRegistrations() {
		LOGGER.debug("REGISTRATION - BY_STATUS - REGISTRATION_DAO", APPLICATION_NAME, APPLICATION_ID,
				"fetch all the registration entries");
		
		return registrationRepository.findAll();
	}

	@Override
	public List<Object[]> getStatusBasedCount() {
		return registrationRepository.getStatusBasedCount();
	}

	@Override
	public List<String> getRegistrationIds(@NonNull List<String> appIds) {
		List<String> regIds = new ArrayList<>();
		for (String appId : appIds) {
			regIds.add(registrationRepository.getRIDByAppId(appId));
		}
		return regIds;
	}
	
	@Override
	public Registration getRegistrationByPacketId(String packetId) {
		return registrationRepository.findByPacketId(packetId);
	}

	@Override
	public Registration updateAckReceiptSignature(String packetId, String signature) {
		Timestamp timestamp = Timestamp.valueOf(DateUtils.getUTCCurrentDateTime());
		Registration registration = registrationRepository.findByPacketId(packetId);
		registration.setAckSignature(signature);
		registration.setUpdBy(SessionContext.userContext().getUserId());
		registration.setUpdDtimes(timestamp);
		return registrationRepository.update(registration);
	}
}
