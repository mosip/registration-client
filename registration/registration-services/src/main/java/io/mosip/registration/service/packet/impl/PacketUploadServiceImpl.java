package io.mosip.registration.service.packet.impl;

import java.io.File;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import io.mosip.kernel.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import com.google.common.annotations.VisibleForTesting;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.packet.PacketUploadService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * This class will update the packet status in the table and also push the
 * packets to the server.
 * 
 * @author SaravanaKumar G
 * @since 1.0.0
 */
@Service
@Transactional
public class PacketUploadServiceImpl extends BaseService implements PacketUploadService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = AppConfig.getLogger(PacketUploadServiceImpl.class);

	/** The registration DAO. */
	@Autowired
	private RegistrationDAO registrationDAO;

	/** The service delegate util. */
	@Autowired
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Autowired
	private AuditManagerService auditFactory;

	@Value("${mosip.registration.packet_upload_batch_size:10}")
	private int batchCount;
	
	@Autowired
	private RegistrationRepository registrationRepository;

	private RetryTemplate retryTemplate;

	@PostConstruct
	public void init() {
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod((Long) ApplicationContext.map().getOrDefault("mosip.registration.retry.delay.packet.upload", 1000l));

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts((Integer) ApplicationContext.map().getOrDefault("mosip.registration.retry.maxattempts.packet.upload", 2));

		retryTemplate = new RetryTemplateBuilder()
				.retryOn(ConnectionException.class)
				.customPolicy(retryPolicy)
				.customBackoff(backOffPolicy)
				.build();
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.service.packet.PacketUploadService#uploadPacket(java.
	 * lang.String)
	 */
	@Override
	public PacketStatusDTO uploadPacket(@NonNull String packetId) throws RegBaseCheckedException {
		proceedWithPacketSync();

		Registration registration = registrationDAO.getRegistrationByPacketId(packetId);
		if(registration == null) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PKT_ID.getErrorCode(),
					RegistrationExceptionConstants.REG_PKT_ID.getErrorMessage());
		}

		if(RegistrationConstants.PACKET_UPLOAD_STATUS.contains(registration.getClientStatusCode())
			&& !( registration.getServerStatusCode() != null && registration.getServerStatusCode().equals(RegistrationConstants.PACKET_STATUS_CODE_REREGISTER))) {
			registration = uploadSyncedPacket(preparePacketStatusDto(registration));
		}
		return preparePacketStatusDto(registration);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.registration.service.packet.PacketUploadService#
	 * uploadAllSyncedPackets()
	 */
	@Override
	public ResponseDTO uploadAllSyncedPackets() {
		return uploadSyncedPackets();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.registration.service.packet.PacketUploadService#
	 * uploadSyncedPackets()
	 */
	@Override
	public ResponseDTO uploadSyncedPackets() {
		LOGGER.info("Started uploading specific number of packets with count {}", batchCount);
		ResponseDTO responseDTO = new ResponseDTO();
		
		Registration reg = registrationRepository.findTopByOrderByUpdDtimesDesc();
		Timestamp currentTimeLimit = reg == null ? Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) : reg.getUpdDtimes();

		Pageable pageable = PageRequest.of(0, batchCount, Sort.by(Sort.Direction.ASC, "updDtimes"));
		Slice<Registration> registrationSlice = null;

		do {
			if(registrationSlice != null)
				pageable = registrationSlice.nextPageable();

		    registrationSlice = registrationRepository.findByClientStatusCodeOrServerStatusCodeOrFileUploadStatusAndUpdDtimesLessThanEqual(
					RegistrationConstants.SYNCED_STATUS, RegistrationConstants.SERVER_STATUS_RESEND, "E", currentTimeLimit, pageable);
		    		    
		    if (!registrationSlice.getContent().isEmpty()) {
				LOGGER.debug("current batch count {}", registrationSlice.getContent().size());
				for(Registration registration : registrationSlice.getContent()) {
					if (RegistrationConstants.PACKET_STATUS_CODE_REREGISTER.equalsIgnoreCase(registration.getServerStatusCode()))
						continue;

					Registration updatedRegDetail = uploadSyncedPacket(preparePacketStatusDto(registration));
					if(updatedRegDetail.getFileUploadStatus().equals(RegistrationClientStatusCode.UPLOAD_ERROR_STATUS.getCode())) {
						setErrorResponse(responseDTO, RegistrationClientStatusCode.UPLOAD_ERROR_STATUS.name(), null);
					} else {
						setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);
					}
				}
			} else {
				SuccessResponseDTO successResponseDTO =new SuccessResponseDTO();
				successResponseDTO.setMessage(RegistrationConstants.SUCCESS);
				responseDTO.setSuccessResponseDTO(successResponseDTO);
			}
		} while (registrationSlice != null && registrationSlice.hasNext());
		return responseDTO;
	}


	/**
	 * Upload synced packets.
	 *
	 * @param packetStatusDTO
	 *            the synced packets
	 */
	private synchronized Registration uploadSyncedPacket(PacketStatusDTO packetStatusDTO) {
		LOGGER.info("uploadSyncedPacket invoked");
		String ackFileName = packetStatusDTO.getPacketPath();
		int lastIndex = ackFileName.indexOf(RegistrationConstants.ACKNOWLEDGEMENT_FILE);
		String packetPath = ackFileName.substring(0, lastIndex);
		File packet = FileUtils.getFile(packetPath + RegistrationConstants.ZIP_FILE_EXTENSION);

		packetStatusDTO.setUploadStatus(RegistrationClientStatusCode.UPLOAD_ERROR_STATUS.getCode());
		try {
			String status = pushPacketWithRetryWrapper(packet);
			packetStatusDTO.setPacketClientStatus(RegistrationClientStatusCode.UPLOADED_SUCCESSFULLY.getCode());
			packetStatusDTO.setUploadStatus(RegistrationClientStatusCode.UPLOAD_SUCCESS_STATUS.getCode());
			packetStatusDTO.setPacketServerStatus(status);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Error while pushing packets to the server", exception);
			
			packetStatusDTO.setPacketServerStatus(exception.getErrorText());
			
			if(exception.getMessage().toLowerCase().contains(RegistrationConstants.PACKET_DUPLICATE)) {
				packetStatusDTO.setPacketClientStatus(RegistrationClientStatusCode.UPLOADED_SUCCESSFULLY.getCode());
				packetStatusDTO.setUploadStatus(RegistrationClientStatusCode.UPLOAD_SUCCESS_STATUS.getCode());
				packetStatusDTO.setPacketServerStatus(RegistrationConstants.PACKET_DUPLICATE.toUpperCase());
			}
		} catch (Throwable t) {
			LOGGER.error("Error while pushing packets to the server", t);
		} 
		//Update status in registration table
		return registrationDAO.updateRegStatus(packetStatusDTO);
	}


	private String pushPacketWithRetryWrapper(File packet) throws RegBaseCheckedException, ConnectionException {
		RetryCallback<String, ConnectionException> retryCallback = new RetryCallback<String, ConnectionException>() {
			@SneakyThrows
			@Override
			public String doWithRetry(RetryContext retryContext) throws ConnectionException {
				LOGGER.info("Currently in Retry wrapper. Current counter : {}", retryContext.getRetryCount());
				return pushPacket(packet);
			}
		};
		return retryTemplate.execute(retryCallback);
	}

	/**
	 * Push the {@link Registration} packet to the server using Packet Receiver
	 * service. The packet has to be synced before pushing to server.
	 *
	 * @param packet
	 * @return
	 * @throws ConnectionException
	 */
	@VisibleForTesting
	private String pushPacket(File packet) throws ConnectionException, RegBaseCheckedException {
		LOGGER.debug("Push packets to the server. fileName : {}", packet.getName());
		if(!packet.exists())
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_FILE_NOT_FOUND_ERROR_CODE.getErrorCode(),
					RegistrationExceptionConstants.REG_FILE_NOT_FOUND_ERROR_CODE.getErrorMessage());

		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add(RegistrationConstants.PACKET_TYPE, new FileSystemResource(packet));
		LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil
				.post(RegistrationConstants.PACKET_UPLOAD, map, RegistrationConstants.JOB_TRIGGER_POINT_USER);

		if (response.get(RegistrationConstants.ERRORS) != null) {
			LOGGER.error("Packet upload failed {}", response.get(RegistrationConstants.ERRORS));
			
			LinkedHashMap<String, String> error = ((List<LinkedHashMap<String, String>>) response.get(RegistrationConstants.ERRORS)).get(0);
			throw new RegBaseCheckedException(error.get("errorCode"), error.get("message"));
		}

		if (response.get(RegistrationConstants.RESPONSE) != null) {
			auditFactory.audit(AuditEvent.PACKET_UPLOADED, Components.PACKET_UPLOAD,
					RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId());
			return (String) ((LinkedHashMap<String, Object>)response.get(RegistrationConstants.RESPONSE)).get(RegistrationConstants.UPLOAD_STATUS);
		}

		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_UPLOAD_ERROR.getErrorCode(),
				RegistrationExceptionConstants.REG_PACKET_UPLOAD_ERROR.getErrorMessage());
	}

}
