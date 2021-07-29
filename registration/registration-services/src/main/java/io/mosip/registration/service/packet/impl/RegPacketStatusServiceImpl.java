package io.mosip.registration.service.packet.impl;


import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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
import io.mosip.registration.dto.ResponseDTO;
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
			List<Registration> registrations = registrationDAO.get(RegistrationClientStatusCode.RE_REGISTER.getCode(),
					getPacketDeletionLastDate(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())),
					RegistrationConstants.PACKET_PROCESSED_STATUS);

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
	private HashMap<String, List<String>> getPacketIds() {
		LOGGER.debug("getting packetIds to sync server status started");
		
		HashMap<String, List<String>> packets = new HashMap<>();

		List<Registration> registrationList = regPacketStatusDAO.getPacketIdsByStatusUploaded();
		registrationList.addAll(regPacketStatusDAO.getPacketIdsByStatusExported());

		List<String> registrationIds = new ArrayList<>();
		List<String> packetIds = new ArrayList<>();
		
		for (Registration registration : registrationList) {
			if (registration.getPacketId() != null) {
				registrationMap.put(registration.getPacketId(), registration);
				packetIds.add(registration.getPacketId());
			} else {
				registrationMap.put(registration.getId(), registration);
				registrationIds.add(registration.getId());
			}
		}
		packets.put("packetIds", packetIds);
		packets.put("registrationIds", registrationIds);
		
		LOGGER.debug("getting packetIds to sync server status has been ended");
		return packets;
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
						.get(registrationStatus.containsKey(RegistrationConstants.PACKET_ID) ? registrationStatus.get(RegistrationConstants.PACKET_ID) : 
							registrationStatus.get(RegistrationConstants.REGISTRATION_ID));
				registration.setServerStatusCode(
						registrationStatus.get(RegistrationConstants.STATUS_CODE));
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
		HashMap<String, List<String>> packetIds = getPacketIds();

		response = syncServerStatus(packetIds.get("packetIds"), triggerPoint, true)
				&& syncServerStatus(packetIds.get("registrationIds"), triggerPoint, false)
						? setSuccessResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_SUCCESS_MESSAGE, null)
						: setErrorResponse(response, RegistrationConstants.PACKET_STATUS_SYNC_ERROR_RESPONSE, null);
						
		LOGGER.info("Packet status sync - Response Created");
		
		return response;
	}


	private boolean syncServerStatus(List<String> ids, String triggerPoint, boolean packetIdExists) throws ConnectionException {
		if(!ids.isEmpty()) {			
			LOGGER.info("PacketIds for sync with server have been retrieved : {}", ids.size());
			
			PacketStatusReaderDTO packetStatusReaderDTO = new PacketStatusReaderDTO();
			packetStatusReaderDTO.setId(RegistrationConstants.PACKET_STATUS_READER_ID);
			packetStatusReaderDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);
			packetStatusReaderDTO.setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
			
			List<LinkedHashMap<String, String>> packets = new ArrayList<>();

			try {
				for (String packetId : ids) {
					LinkedHashMap<String, String> packetIdMap = new LinkedHashMap<>();
					packetIdMap.put(packetIdExists ? RegistrationConstants.PACKET_ID : RegistrationConstants.REGISTRATION_ID, packetId);
					packets.add(packetIdMap);
				}
				packetStatusReaderDTO.setRequest(packets);
				
				List<LinkedHashMap<String, String>> registrations = null;
				
				/* Obtain RegistrationStatusDTO from service delegate util */
				LinkedHashMap<String, Object> packetStatusResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
						.post(packetIdExists ? RegistrationConstants.PACKET_EXTERNAL_STATUS_SYNC_SERVICE_NAME : RegistrationConstants.PACKET_STATUS_SYNC_SERVICE_NAME, packetStatusReaderDTO, triggerPoint);

				registrations = (List<LinkedHashMap<String, String>>) packetStatusResponse
						.get(RegistrationConstants.RESPONSE);

				if (registrations == null || registrations.isEmpty()) {
					return false;
				}
				/* update the status of packets after sync with server */
				try {
					updatePacketIdsByServerStatus(registrations);
				} catch (RegBaseUncheckedException regBaseUncheckedException) {
					LOGGER.error(regBaseUncheckedException.getMessage(), regBaseUncheckedException);
					return false;
				}
				return true;
			} catch (ConnectionException e) {
				throw e;
			} catch (RuntimeException | RegBaseCheckedException e) {
				LOGGER.error(e.getMessage(), e);
				return false;
			}
		}
		return true;
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
