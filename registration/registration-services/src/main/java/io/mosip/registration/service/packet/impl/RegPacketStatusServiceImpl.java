package io.mosip.registration.service.packet.impl;


import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;

import org.assertj.core.util.Files;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegPacketStatusDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.PacketStatusReaderDTO;
import io.mosip.registration.dto.RegistrationIdDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.packet.RegPacketStatusService;
import io.mosip.registration.service.sync.PacketSynchService;
import lombok.NonNull;
import lombok.SneakyThrows;

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

	private static final Logger LOGGER = AppConfig.getLogger(RegPacketStatusServiceImpl.class);

	private HashMap<String, Registration> registrationMap = new HashMap<>();

	private RetryTemplate retryTemplate;

	@PostConstruct
	private void init() {
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod((Long) ApplicationContext.map().getOrDefault("mosip.registration.retry.delay.packet.statussync", 1000l));

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts((Integer) ApplicationContext.map().getOrDefault("mosip.registration.retry.maxattempts.packet.statussync", 2));

		retryTemplate = new RetryTemplateBuilder()
				.retryOn(ConnectionException.class)
				.customPolicy(retryPolicy)
				.customBackoff(backOffPolicy)
				.build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.RegPacketStatusService#
	 * deleteRegistrationPackets()
	 */
	@Override
	public synchronized ResponseDTO deleteRegistrationPackets() {

		LOGGER.info("Delete  Reg-packets started");

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
			LOGGER.error(runtimeException.getMessage(), runtimeException);

			setErrorResponse(responseDTO, RegistrationConstants.REGISTRATION_DELETION_BATCH_JOBS_FAILURE, null);
		}

		LOGGER.info("Delete  Reg-packets ended");

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

		LOGGER.info("packet deletion when the machine is remapped is started");

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
	 * Get all registrationIDs for which the clientStatus is post-sync
	 *
	 * @return List<String> list of registrationId's required for packet status sync
	 *         with server
	 */
	private List<String> getPacketIds() {
		LOGGER.debug("getting packetIds to sync server status started");

		List<Registration> registrationList = regPacketStatusDAO.getPacketIdsByStatusUploaded();
		registrationList.addAll(regPacketStatusDAO.getPacketIdsByStatusExported());

		List<String> packetIds = new ArrayList<>();
		for (Registration registration : registrationList) {
			String registrationId = registration.getId();
			registrationMap.put(registrationId, registration);
			packetIds.add(registrationId);
		}
		LOGGER.debug("getting packetIds to sync server status has been ended");
		return packetIds;
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
		LOGGER.info("packets status sync from server has been started");

		try {
			for (LinkedHashMap<String, String> registrationStatus : registrationStatuses) {
				Registration registration = registrationMap
						.get(registrationStatus.get(RegistrationConstants.PACKET_STATUS_READER_REGISTRATION_ID));
				registration.setServerStatusCode(
						registrationStatus.get(RegistrationConstants.PACKET_STATUS_READER_STATUS_CODE));
				registration.setServerStatusTimestamp(new Timestamp(System.currentTimeMillis()));

				registration = regPacketStatusDAO.update(registration);
			}

			LOGGER.info("packets status sync from server has been ended");
		} catch (RuntimeException runtimeException) {
			LOGGER.error(runtimeException.getMessage(), runtimeException);
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_UPDATE_STATUS,
					runtimeException.toString());
		}
	}

	public ResponseDTO syncServerPacketStatusWithRetryWrapper(String triggerPoint) throws RegBaseCheckedException, ConnectionException {
		RetryCallback<ResponseDTO, ConnectionException> retryCallback = new RetryCallback<ResponseDTO, ConnectionException>() {
			@SneakyThrows
			@Override
			public ResponseDTO doWithRetry(RetryContext retryContext) throws ConnectionException {
				LOGGER.info("Currently in Retry wrapper. Current counter : {}", retryContext.getRetryCount());
				return syncServerPacketStatus(triggerPoint);
			}
		};
		return retryTemplate.execute(retryCallback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.RegPacketStatusService#packetSyncStatus(
	 * java.lang.String)
	 */
	public synchronized ResponseDTO syncServerPacketStatus(@NonNull String triggerPoint) throws RegBaseCheckedException,
			ConnectionException {
		LOGGER.info("packet status sync called");

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithPacketSync();

		/* Create Response to Return to UI layer */
		ResponseDTO response = new ResponseDTO();

		//if (validateTriggerPoint(triggerPoint)) {
			List<String> packetIds = getPacketIds();
			LOGGER.info("PacketIds for sync with server have been retrieved : {}", packetIds.size());

			PacketStatusReaderDTO packetStatusReaderDTO = new PacketStatusReaderDTO();
			packetStatusReaderDTO.setId(RegistrationConstants.PACKET_STATUS_READER_ID);
			packetStatusReaderDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);
			packetStatusReaderDTO.setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
			packetStatusReaderDTO.setRequest(new ArrayList<>());

			try {
				for (String packetId : packetIds) {
					RegistrationIdDTO registrationIdDTO = new RegistrationIdDTO();
					registrationIdDTO.setRegistrationId(packetId);
					packetStatusReaderDTO.getRequest().add(registrationIdDTO);
				}

				List<LinkedHashMap<String, String>> registrations = null;
				if(!packetIds.isEmpty()) {
					/* Obtain RegistrationStatusDTO from service delegate util */
					LinkedHashMap<String, Object> packetStatusResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
							.post(RegistrationConstants.PACKET_STATUS_SYNC_SERVICE_NAME, packetStatusReaderDTO, triggerPoint);

					registrations = (List<LinkedHashMap<String, String>>) packetStatusResponse
							.get(RegistrationConstants.RESPONSE);

					if (registrations == null || registrations.isEmpty()) {
						setErrorResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_ERROR_RESPONSE, null);
						return response;
					}

					/* update the status of packets after sync with server */
					try {
						updatePacketIdsByServerStatus(registrations);
					} catch (RegBaseUncheckedException regBaseUncheckedException) {
						LOGGER.error(regBaseUncheckedException.getMessage(), regBaseUncheckedException);
						setErrorResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_ERROR_RESPONSE, null);
						return response;
					}
				}

				/* Create Success response */
				SuccessResponseDTO successResponse = new SuccessResponseDTO();
				successResponse.setCode(RegistrationConstants.ALERT_INFORMATION);
				successResponse.setMessage(RegistrationConstants.PACKET_STATUS_SYNC_SUCCESS_MESSAGE);
				Map<String, Object> otherAttributes = new WeakHashMap<>();
				otherAttributes.put(RegistrationConstants.PACKET_STATUS_SYNC_RESPONSE_ENTITY, registrations == null ?
						RegistrationConstants.EMPTY : registrations);
				successResponse.setOtherAttributes(otherAttributes);
				response.setSuccessResponseDTO(successResponse);
				LOGGER.info("Packet status sync - Success Response Created");

			} catch (ConnectionException e) {
				throw e;
			} catch (RuntimeException | RegBaseCheckedException e) {
				LOGGER.error(e.getMessage(), e);
			}
		setErrorResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_ERROR_RESPONSE, null);
		return response;
	}


	private Registration updateRegistration(final Registration registration) {
		LOGGER.info("Delete Registration Packet started");

		Registration updatedRegistration = regPacketStatusDAO.update(registration);
		LOGGER.info("Delete Registration Packet ended");

		return updatedRegistration;
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


	private String createdByUser() {
		return SessionContext.isSessionContextAvailable() && SessionContext.userContext() != null
				&& SessionContext.userContext().getUserId() != null ? SessionContext.userContext().getUserId()
						: RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
	}

}
