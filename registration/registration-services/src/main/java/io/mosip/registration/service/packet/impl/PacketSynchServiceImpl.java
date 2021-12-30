package io.mosip.registration.service.packet.impl;

import static io.mosip.kernel.core.util.JsonUtils.javaObjectToJsonString;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

import com.google.common.annotations.VisibleForTesting;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.keymanagerservice.exception.KeymanagerServiceException;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.RegistrationPacketSyncDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.SyncRegistrationDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.PacketSynchService;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * This class invokes the external MOSIP service 'Packet Sync' to sync the
 * packet ids, which are ready for upload to the server from client. The packet
 * upload can't be done, without synching the packet ids to the server. While
 * sending this request, the data would be encrypted using MOSIP public key and
 * same can be decrypted at Server end using the respective private key.
 *
 * @author saravanakumar gnanaguru
 *
 */
@Service
public class PacketSynchServiceImpl extends BaseService implements PacketSynchService {

	private static final Logger LOGGER = AppConfig.getLogger(PacketSynchServiceImpl.class);

	@Autowired
	private RegistrationDAO syncRegistrationDAO;

	@Autowired
	protected AuditManagerService auditFactory;

	@Autowired
	@Qualifier("OfflinePacketCryptoServiceImpl")
	private IPacketCryptoService offlinePacketCryptoServiceImpl;

	@Autowired
	private RegistrationRepository registrationRepository;

	@Value("${mosip.registration.rid_sync_batch_size:10}")
	private int batchCount;

	private RetryTemplate retryTemplate;

	@PostConstruct
	public void init() {
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod((Long) ApplicationContext.map().getOrDefault("mosip.registration.retry.delay.packet.sync", 1000l));

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts((Integer) ApplicationContext.map().getOrDefault("mosip.registration.retry.maxattempts.packet.sync", 2));

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
	 * io.mosip.registration.service.sync.PacketSynchService#fetchPacketsToBeSynched
	 * ()
	 */
	@Override
	public List<PacketStatusDTO> fetchPacketsToBeSynched() {
		LOGGER.info("Fetch the packets that needs to be synced to the server");
		List<PacketStatusDTO> idsToBeSynched = new ArrayList<>();
		List<Registration> packetsToBeSynched = syncRegistrationDAO.fetchPacketsToUpload(
				RegistrationConstants.PACKET_STATUS_UPLOAD, RegistrationConstants.SERVER_STATUS_RESEND);
		packetsToBeSynched.forEach(reg -> {
			if (reg.getServerStatusCode() == null
					|| (reg.getClientStatusTimestamp() != null
					&& !(RegistrationConstants.SERVER_STATUS_RESEND.equalsIgnoreCase(reg.getServerStatusCode())
					&& (reg.getServerStatusTimestamp() != null && reg.getClientStatusTimestamp().after(reg.getServerStatusTimestamp()))))) {
				idsToBeSynched.add(preparePacketStatusDto(reg));
			}
		});
		return idsToBeSynched;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.service.sync.PacketSynchService#syncPacket(java
	 * .lang.String)
	 */
	@Override
	public ResponseDTO syncPacket(String triggerPoint) {
		LOGGER.info("Syncing specific number of packets to the server with count {}", batchCount);
		ResponseDTO responseDTO = new ResponseDTO();
		try {

			syncRIDToServerWithRetryWrapper(triggerPoint, null);
			setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);

		} catch (ConnectionException | RegBaseCheckedException | JsonProcessingException exception) {
			LOGGER.error("Exception in RID sync", exception);
			setErrorResponse(responseDTO, exception.getMessage(), null);
		}
		setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);
		return responseDTO;
	}


	@Override
	public ResponseDTO syncPacket(@NonNull String triggerPoint, @NonNull List<String> packetIDs) {
		LOGGER.info("Syncing specific rids to the server with count {}", packetIDs.size());
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			syncRIDToServerWithRetryWrapper(triggerPoint, packetIDs);
			setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);

		} catch (ConnectionException | RegBaseCheckedException | JsonProcessingException | KeymanagerServiceException exception) {
			LOGGER.error("Exception in RID sync", exception);
			setErrorResponse(responseDTO, exception.getMessage(), null);
		}
		return responseDTO;
	}

	@Override
	public ResponseDTO syncAllPackets(String triggerPoint) {
		return syncPacket(triggerPoint);
	}

	private void syncRIDToServerWithRetryWrapper(String triggerPoint, List<String> packetIDs)
			throws KeymanagerServiceException, RegBaseCheckedException, JsonProcessingException, ConnectionException {
		RetryCallback<Boolean, ConnectionException> retryCallback = new RetryCallback<Boolean, ConnectionException>() {
			@SneakyThrows
			@Override
			public Boolean doWithRetry(RetryContext retryContext) throws ConnectionException {
				LOGGER.info("Currently in Retry wrapper. Current counter : {}", retryContext.getRetryCount());
				syncRIDToServer(triggerPoint, packetIDs);
				return true;
			}
		};
		retryTemplate.execute(retryCallback);
	}

	@VisibleForTesting
	private synchronized void syncRIDToServer(String triggerPoint, List<String> packetIDs)
		      throws KeymanagerServiceException, RegBaseCheckedException, JsonProcessingException, ConnectionException {
		   //Precondition check, proceed only if met, otherwise throws exception
		   proceedWithPacketSync();

		   Timestamp currentTimeLimit = null;
		   if (packetIDs == null) {
		      Registration registration = registrationRepository.findTopByOrderByUpdDtimesDesc();
		      currentTimeLimit = registration == null ? Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) : registration.getUpdDtimes();
		   }

		   Pageable pageable = PageRequest.of(0, batchCount, Sort.by(Sort.Direction.ASC, "updDtimes"));
		   Slice<Registration> registrationSlice = null;

		   do {
			   if(registrationSlice != null)
				   pageable = registrationSlice.nextPageable();

			   registrationSlice = (packetIDs != null) ? registrationRepository.findByPacketIdIn(packetIDs, pageable) :
		            registrationRepository.findByClientStatusCodeInAndUpdDtimesLessThanEqual(RegistrationConstants.CLIENT_STATUS_TO_BE_SYNCED,
		                  currentTimeLimit, pageable);

		      List<SyncRegistrationDTO> syncDtoList = getPacketSyncDtoList(registrationSlice.getContent());
		      //This filtering is done for backward compatibility. For older version packets, registrationId will be copied to packetId column
		      List<SyncRegistrationDTO> syncDtoWithPacketId = syncDtoList.stream().filter(dto -> !dto.getRegistrationId().equals(dto.getPacketId())).collect(Collectors.toList());
		      List<SyncRegistrationDTO> syncDtoWithoutPacketId = syncDtoList.stream().filter(dto -> dto.getRegistrationId().equals(dto.getPacketId())).collect(Collectors.toList());

		      if(syncDtoList != null && !syncDtoList.isEmpty()) {
		         syncRID(syncDtoWithoutPacketId, triggerPoint, false);
		         syncRID(syncDtoWithPacketId, triggerPoint, true);
		      }

		   } while(registrationSlice != null && registrationSlice.hasNext());
		   
		   LOGGER.debug("Sync the packets to the server ending");
		}


	private void syncRID(List<SyncRegistrationDTO> syncDtoList, String triggerPoint, boolean packetIdExists) throws KeymanagerServiceException, RegBaseCheckedException, ConnectionException, JsonProcessingException {
		if (!syncDtoList.isEmpty()) {
			RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
			registrationPacketSyncDTO
					.setRequesttime(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
			registrationPacketSyncDTO.setSyncRegistrationDTOs(syncDtoList);
			registrationPacketSyncDTO.setId(RegistrationConstants.PACKET_SYNC_STATUS_ID);
			registrationPacketSyncDTO.setVersion(RegistrationConstants.PACKET_SYNC_VERSION);
			
			String refId = String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID))
					.concat(RegistrationConstants.UNDER_SCORE)
					.concat(String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID)));
			
			ResponseDTO response = syncPacketsToServer(CryptoUtil.encodeToURLSafeBase64(offlinePacketCryptoServiceImpl
					.encrypt(refId, javaObjectToJsonString(registrationPacketSyncDTO).getBytes())), triggerPoint, packetIdExists);

			if (response != null && response.getSuccessResponseDTO() != null) {
				for (SyncRegistrationDTO dto : syncDtoList) {
					String status = (String) response.getSuccessResponseDTO().getOtherAttributes().get(dto.getRegistrationId());

					if (status != null && status.equalsIgnoreCase(RegistrationConstants.SUCCESS)) {
						PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
						packetStatusDTO.setFileName(dto.getRegistrationId());
						packetStatusDTO.setPacketId(dto.getPacketId());
						packetStatusDTO.setPacketClientStatus(RegistrationClientStatusCode.META_INFO_SYN_SERVER.getCode());
						// TODO - check on re-register status logic
						syncRegistrationDAO.updatePacketSyncStatus(packetStatusDTO);
					}
				}
			}
		}
	}


	private List<SyncRegistrationDTO> getPacketSyncDtoList(@NonNull List<Registration> registrations) {
		LOGGER.debug("RID Sync current batch count {}", registrations.size());
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		for(Registration registration : registrations) {
			if(registration.getClientStatusCode().equals(RegistrationConstants.SYNCED_STATUS))
				continue;

			SyncRegistrationDTO syncDto = new SyncRegistrationDTO();
			syncDto.setRegistrationId(registration.getId());
			syncDto.setRegistrationType(registration.getStatusCode().toUpperCase());
			syncDto.setPacketId(registration.getPacketId());
			syncDto.setAdditionalInfoReqId(registration.getAdditionalInfoReqId());

			try {
				if (registration.getAdditionalInfo() != null) {
					String additionalInfo = new String(registration.getAdditionalInfo());
					RegistrationDataDto registrationDataDto = (RegistrationDataDto) JsonUtils
							.jsonStringToJavaObject(RegistrationDataDto.class, additionalInfo);
					syncDto.setName(registrationDataDto.getName());
					syncDto.setPhone(registrationDataDto.getPhone());
					syncDto.setEmail(registrationDataDto.getEmail());
					syncDto.setLangCode(registrationDataDto.getLangCode() != null ?
							registrationDataDto.getLangCode().split(",")[0] :
							ApplicationContext.applicationLanguage());
				}
			} catch (JsonParseException | JsonMappingException | io.mosip.kernel.core.exception.IOException exception) {
				LOGGER.error(exception.getMessage(), exception);
			}

			try (FileInputStream fis = new FileInputStream(FileUtils.getFile(registration.getAckFilename().replace(
					RegistrationConstants.ACKNOWLEDGEMENT_FILE_EXTENSION, RegistrationConstants.ZIP_FILE_EXTENSION)))) {
				byte[] byteArray = new byte[(int) fis.available()];
				fis.read(byteArray);
				syncDto.setPacketHashValue(HMACUtils2.digestAsPlainText(byteArray));
				syncDto.setPacketSize(BigInteger.valueOf(byteArray.length));
			} catch (IOException | NoSuchAlgorithmException ioException) {
				LOGGER.error(ioException.getMessage(), ioException);
			}

			if (RegistrationClientStatusCode.RE_REGISTER.getCode()
					.equalsIgnoreCase(registration.getClientStatusCode())) {
				syncDto.setSupervisorStatus(RegistrationConstants.CLIENT_STATUS_APPROVED);
			} else {
				syncDto.setSupervisorStatus(registration.getClientStatusCode());
			}
			syncDto.setSupervisorComment(registration.getClientStatusComments());
			syncDtoList.add(syncDto);
		}
		return syncDtoList;
	}

	/**
	 * This method makes the actual service call to push the packet sync related
	 * data to server. It makes only external service call and doesn't have any db
	 * call.
	 *
	 * @param encodedString
	 *            the sync dto list
	 * @param triggerPoint
	 *            the trigger point
	 * @param packetIdExists 
	 * @return the response DTO
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 * @throws ConnectionException
	 *             the ConnectionException
	 */
	@VisibleForTesting
	private ResponseDTO syncPacketsToServer(@NonNull String encodedString, @NonNull String triggerPoint, boolean packetIdExists)
			throws RegBaseCheckedException, ConnectionException {
		LOGGER.info("Sync the packets to the server");
		ResponseDTO responseDTO = new ResponseDTO();

		try {
			LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil
					.post(packetIdExists ? RegistrationConstants.PACKET_SYNC_V2 : RegistrationConstants.PACKET_SYNC, javaObjectToJsonString(encodedString), triggerPoint);
			if (response.get("response") != null) {
				SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
				Map<String, Object> statusMap = new LinkedHashMap<>();
				for (LinkedHashMap<String, Object> responseMap : (List<LinkedHashMap<String, Object>>) response
						.get("response")) {
					statusMap.put((String) responseMap.get("registrationId"), responseMap.get("status"));
				}
				successResponseDTO.setOtherAttributes(statusMap);
				responseDTO.setSuccessResponseDTO(successResponseDTO);
			} else if (response.get("errors") != null) {
				List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
				ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
				errorResponseDTO.setMessage(response.get("errors").toString());
				errorResponseDTOs.add(errorResponseDTO);
				responseDTO.setErrorResponseDTOs(errorResponseDTOs);
				LOGGER.error(response.get("errors").toString());
			}
		} catch (ConnectionException e) {
			throw e;
		} catch (JsonProcessingException | RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_SYNC_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_PACKET_SYNC_EXCEPTION.getErrorMessage());
		}
		return responseDTO;
	}
}
