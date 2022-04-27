package io.mosip.registration.service.packet.impl;

import static io.mosip.kernel.core.util.JsonUtils.javaObjectToJsonString;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.File;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.mosip.registration.repositories.RegistrationRepository;
import org.assertj.core.util.Files;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationTransactionType;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegPacketStatusDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.PacketStatusReaderDTO;
import io.mosip.registration.dto.RegistrationIdDTO;
import io.mosip.registration.dto.RegistrationPacketSyncDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.SyncRegistrationDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.packet.RegPacketStatusService;
import io.mosip.registration.service.sync.PacketSynchService;

/**
 * The implementation class of {@link RegPacketStatusService}to update status of
 * the registration packets based on Packet Status Reader service and delete the
 * Registration Packets based on the status of the packets
 * 
 * @author Himaja Dhanyamraju
 * @since 1.0.0
 */
@Service
public class RegPacketStatusServiceImpl extends BaseService implements RegPacketStatusService {

	@Autowired
	private RegPacketStatusDAO regPacketStatusDAO;

	@Autowired
	private RegistrationDAO registrationDAO;

	@Autowired
	private PacketSynchService packetSynchService;

	@Autowired
    @Qualifier("OfflinePacketCryptoServiceImpl")
    private IPacketCryptoService offlinePacketCryptoServiceImpl;

	@Autowired
	private RegistrationRepository registrationRepository;

	@Value("${mosip.registration.rid_sync_batch_size:10}")
	private int batchCount;

	private static final Logger LOGGER = AppConfig.getLogger(RegPacketStatusServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.RegPacketStatusService#
	 * deleteRegistrationPackets()
	 */
	@Override
	public synchronized ResponseDTO deleteRegistrationPackets() {

		LOGGER.info(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID, "Delete  Reg-packets started");

		ResponseDTO responseDTO = new ResponseDTO();

		try {
			/* Get Registrations to be deleted */
			List<Registration> registrations = registrationDAO.get(
					getPacketDeletionLastDate(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())),
					RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);

			if (!isNull(registrations) && !isEmpty(registrations)) {
				deleteRegistrations(registrations);

			}

			setSuccessResponse(responseDTO, RegistrationConstants.REGISTRATION_DELETION_BATCH_JOBS_SUCCESS, null);

		} catch (RuntimeException runtimeException) {

			LOGGER.error(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

			setErrorResponse(responseDTO, RegistrationConstants.REGISTRATION_DELETION_BATCH_JOBS_FAILURE, null);
		}

		LOGGER.info(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID, "Delete  Reg-packets ended");

		return responseDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.RegPacketStatusService#
	 * deleteAllProcessedRegPackets()
	 */
	@Override
	public void deleteAllProcessedRegPackets() {

		LOGGER.info("REGISTRATION - DELETE-PACKETS-WHEN-MACHINE-REMAPPED - REG_PACKET_STATUS_SERVICE", APPLICATION_NAME,
				APPLICATION_ID, "packet deletion when the machine is remapped is started");

		List<Registration> registrations = registrationDAO
				.findByServerStatusCodeIn(RegistrationConstants.PACKET_STATUS_CODES_FOR_REMAPDELETE);
		if (registrations != null && !registrations.isEmpty()) {

			for (Registration registration : registrations) {
				if (RegistrationConstants.PACKET_STATUS_CODE_REREGISTER
						.equalsIgnoreCase(registration.getServerStatusCode())) {
					if (RegistrationClientStatusCode.RE_REGISTER.getCode()
							.equalsIgnoreCase(registration.getClientStatusCode()))
						delete(registration);
				} else {
					delete(registration);
				}
			}
		}

	}

	private Timestamp getPacketDeletionLastDate(Timestamp reqTime) {

		/* Get Calendar instance */
		Calendar cal = Calendar.getInstance();
		cal.setTime(reqTime);
		cal.add(Calendar.DATE,
				-(Integer.parseInt(getGlobalConfigValueOf(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS))));

		/* To-Date */
		return new Timestamp(cal.getTimeInMillis());
	}

	/**
	 * update status for all packets that are synced with server
	 *
	 * @param registrationStatuses list of registration entities which are represented as
	 *                      LinkedHashMap which maps the attributes of registration
	 *                      entity to their respective values that are obtained
	 *                      after sync with server
	 */
	private void updatePacketIdsByServerStatus(List<LinkedHashMap<String, String>> registrationStatuses) {
		LOGGER.info(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID,
				"updating packets sync status from server has been started");
		try {
			for (LinkedHashMap<String, String> registrationStatus : registrationStatuses) {
				registrationRepository.updateRegistrationServerStatus(registrationStatus.get(RegistrationConstants.PACKET_STATUS_READER_REGISTRATION_ID),
						registrationStatus.get(RegistrationConstants.PACKET_STATUS_READER_STATUS_CODE),
						new Timestamp(System.currentTimeMillis()));
			}
			LOGGER.info(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID,
					"updating packets sync status from server has been ended");
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LoggerConstants.LOG_PKT_DELETE, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_UPDATE_STATUS,
					runtimeException.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.RegPacketStatusService#packetSyncStatus(
	 * java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public synchronized ResponseDTO packetSyncStatus(String triggerPoint) throws RegBaseCheckedException {

		LOGGER.info(LoggerConstants.LOG_PKT_SYNC, APPLICATION_NAME, APPLICATION_ID, "packet status sync called");

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithPacketSync();

		if(!validateTriggerPoint(triggerPoint)) {
			LOGGER.error(LoggerConstants.LOG_PKT_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Trigger point cannot be empty or null");
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorCode(),
					RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorMessage());
		}

		ResponseDTO response = new ResponseDTO();
		try {
			Registration registration = registrationRepository.findTopByOrderByUpdDtimesDesc();
			Timestamp currentTimeLimit = registration == null ? Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) : registration.getUpdDtimes();

			Pageable pageable = PageRequest.of(0, batchCount, Sort.by(Sort.Direction.ASC, "updDtimes"));
			Slice<Registration> registrationSlice = null;

			do {
				if(registrationSlice != null)
					pageable = registrationSlice.nextPageable();

				registrationSlice = registrationRepository.findByClientStatusCodeOrClientStatusCommentsAndUpdDtimesLessThanEqual(
						RegistrationClientStatusCode.UPLOADED_SUCCESSFULLY.getCode(),
						RegistrationClientStatusCode.EXPORT.getCode(), currentTimeLimit, pageable);

				if (!registrationSlice.getContent().isEmpty()) {
					try {
						syncPacketStatus(triggerPoint, registrationSlice.getContent());
					} catch (Exception e) {
						LOGGER.error("Failed to sync packet status of current batch", e);
					}
				}
			} while (registrationSlice != null && registrationSlice.hasNext());

			SuccessResponseDTO successResponse = new SuccessResponseDTO();
			successResponse.setCode(RegistrationConstants.ALERT_INFORMATION);
			successResponse.setMessage(RegistrationConstants.PACKET_STATUS_SYNC_SUCCESS_MESSAGE);
			Map<String, Object> otherAttributes = new WeakHashMap<>();
			/* sending empty success response as there are no packets to check status */
			otherAttributes.put(RegistrationConstants.PACKET_STATUS_SYNC_RESPONSE_ENTITY,
					RegistrationConstants.EMPTY);
			successResponse.setOtherAttributes(otherAttributes);
			response.setSuccessResponseDTO(successResponse);
			return response;

		} catch (Throwable t) {
			LOGGER.error("Failed to sync packet status", t);
		}
		setErrorResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_ERROR_RESPONSE, null);
		return response;
	}

	private void syncPacketStatus(String triggerPoint, List<Registration> registrationsToSyncStatus) throws Exception {
		if(registrationsToSyncStatus == null || registrationsToSyncStatus.isEmpty())
			return;

		PacketStatusReaderDTO packetStatusReaderDTO = new PacketStatusReaderDTO();
		List<RegistrationIdDTO> registrationIdDTOs = new ArrayList<>();
		for (Registration registration : registrationsToSyncStatus) {
			RegistrationIdDTO registrationIdDTO = new RegistrationIdDTO();
			registrationIdDTO.setRegistrationId(registration.getId());
			registrationIdDTOs.add(registrationIdDTO);
		}
		packetStatusReaderDTO.setRequest(registrationIdDTOs);
		final String SERVICE_NAME = RegistrationConstants.PACKET_STATUS_SYNC_SERVICE_NAME;
		packetStatusReaderDTO.setId(RegistrationConstants.PACKET_STATUS_READER_ID);
		packetStatusReaderDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);
		packetStatusReaderDTO.setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));

		LinkedHashMap<String, Object> packetStatusResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
				.post(SERVICE_NAME, packetStatusReaderDTO, triggerPoint);
		List<LinkedHashMap<String, String>> registrations = (List<LinkedHashMap<String, String>>) packetStatusResponse
				.get(RegistrationConstants.RESPONSE);
		if (registrations != null && !registrations.isEmpty()) { updatePacketIdsByServerStatus(registrations); }
	}

	private boolean validateTriggerPoint(String triggerPoint) {
		if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(LoggerConstants.LOG_PKT_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Trigger point is empty or null");
			return false;
		}
		return true;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.RegPacketStatusService#
	 * deleteRegistrations(java.util.List)
	 */
	@Override
	public void deleteRegistrations(final List<Registration> registrations) {
		for (Registration registration : registrations) {

			if (registration.getServerStatusCode()
					.equalsIgnoreCase(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED)) {
				/* Delete Registration */
				delete(registration);
			}
		}

	}

	private void delete(Registration registration) {
		File ackFile = null;
		File zipFile = null;
		String ackPath = registration.getAckFilename();
		ackFile = FileUtils.getFile(ackPath);
		String zipPath = ackPath.replace("_Ack.html", RegistrationConstants.ZIP_FILE_EXTENSION);
		zipFile = FileUtils.getFile(zipPath);

		if (zipFile.exists()) {

			Files.delete(ackFile);
			Files.delete(zipFile);

		}

		/* Delete row from DB */
		regPacketStatusDAO.delete(registration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.RegPacketStatusService#syncPacket(java.
	 * lang.String)
	 */
	public ResponseDTO syncPacket(String triggerPoint) {

		LOGGER.debug("REGISTRATION - SYNC_PACKETS_TO_SERVER - REG_PACKET_STATUS_SERVICE", APPLICATION_NAME,
				APPLICATION_ID, "Sync the packets to the server");
		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		List<ErrorResponseDTO> errorList = new ArrayList<>();
		try {

			//Precondition check, proceed only if met, otherwise throws exception
			proceedWithPacketSync();

			Registration registration = registrationRepository.findTopByOrderByUpdDtimesDesc();
			Timestamp currentTimeLimit = registration == null ? Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) : registration.getUpdDtimes();
			Pageable pageable = PageRequest.of(0, batchCount, Sort.by(Sort.Direction.ASC, "updDtimes"));
			Slice<Registration> registrationSlice = null;

			do {
				if(registrationSlice != null)
					pageable = registrationSlice.nextPageable();

				registrationSlice = registrationRepository.findByClientStatusCodeInAndUpdDtimesLessThanEqual(RegistrationConstants.PACKET_STATUS,
								currentTimeLimit, pageable);

				try {
					syncPacketBatch(triggerPoint, registrationSlice.getContent());
					successResponseDTO.setMessage(RegistrationConstants.SUCCESS);
					responseDTO.setSuccessResponseDTO(successResponseDTO);
					LOGGER.debug("Sync the packets to the server batch ended");
				} catch (Exception e) {
					LOGGER.error("Failed to sync registration to server batch", e);
				}
			} while(registrationSlice != null && registrationSlice.hasNext());

		} catch (Throwable t) {
			LOGGER.error("Registration sync batch failed", t);
			ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
			errorResponseDTO.setMessage(t.getMessage());
			errorList.add(errorResponseDTO);
			responseDTO.setErrorResponseDTOs(errorList);
		}
		return responseDTO;
	}

	private void syncPacketBatch(String triggerPoint, List<Registration> registrationToSync) throws Exception {
		if(registrationToSync == null || registrationToSync.isEmpty())
			return;

		List<PacketStatusDTO> packetDto = new ArrayList<>();
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		for (Registration reg : registrationToSync) {
			PacketStatusDTO packetStatusDTO = packetStatusDtoPreperation(reg);
			packetDto.add(packetStatusDTO);
			SyncRegistrationDTO syncDto = new SyncRegistrationDTO();
			syncDto.setLangCode(getGlobalConfigValueOf(RegistrationConstants.PRIMARY_LANGUAGE));
			syncDto.setRegistrationId(packetStatusDTO.getFileName());
			syncDto.setName(packetStatusDTO.getName());
			syncDto.setEmail(packetStatusDTO.getEmail());
			syncDto.setPhone(packetStatusDTO.getPhone());
			syncDto.setRegistrationType(packetStatusDTO.getPacketStatus().toUpperCase());
			syncDto.setPacketHashValue(packetStatusDTO.getPacketHash());
			syncDto.setPacketSize(packetStatusDTO.getPacketSize());
			if (RegistrationClientStatusCode.RE_REGISTER.getCode()
					.equalsIgnoreCase(packetStatusDTO.getPacketClientStatus())) {
				syncDto.setSupervisorStatus(RegistrationConstants.CLIENT_STATUS_APPROVED);
			} else {
				syncDto.setSupervisorStatus(packetStatusDTO.getSupervisorStatus());
			}
			syncDto.setSupervisorComment(packetStatusDTO.getSupervisorComments());
			syncDtoList.add(syncDto);
		}

		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		registrationPacketSyncDTO
				.setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		registrationPacketSyncDTO.setSyncRegistrationDTOs(syncDtoList);
		registrationPacketSyncDTO.setId(RegistrationConstants.PACKET_SYNC_STATUS_ID);
		registrationPacketSyncDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);
		String regId = registrationPacketSyncDTO.getSyncRegistrationDTOs().get(0).getRegistrationId();
		ResponseDTO response = packetSynchService.syncPacketsToServer(CryptoUtil.encodeBase64(offlinePacketCryptoServiceImpl
				.encrypt(regId, javaObjectToJsonString(registrationPacketSyncDTO).getBytes())), triggerPoint);

		List<PacketStatusDTO> synchedPackets = new ArrayList<>();
		if (response != null && response.getSuccessResponseDTO() != null) {
			for (PacketStatusDTO registration : packetDto) {
				String status = (String) response.getSuccessResponseDTO().getOtherAttributes().get(registration.getFileName());
				if (status != null && status.equalsIgnoreCase(RegistrationConstants.SUCCESS)) {
					registration.setPacketClientStatus(RegistrationClientStatusCode.META_INFO_SYN_SERVER.getCode());
					synchedPackets.add(registration);
				}
			}
			packetSynchService.updateSyncStatus(synchedPackets);
		}
	}

	private String createdByUser() {
		return SessionContext.isSessionContextAvailable() && SessionContext.userContext() != null
				&& SessionContext.userContext().getUserId() != null ? SessionContext.userContext().getUserId()
						: RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
	}

}
