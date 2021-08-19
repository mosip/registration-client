package io.mosip.registration.service.sync;

import java.net.URISyntaxException;
import java.util.List;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import lombok.NonNull;

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
public interface PacketSynchService {

	/**
	 * This method is used to fetch the packets from the table which needs to be
	 * synched with the server from client machine. It picks the packet which are in
	 * the state of 'approved', 'reregisterapproved' and 'resend'.
	 *
	 * @return the list of packet Status DTO
	 */
	List<PacketStatusDTO> fetchPacketsToBeSynched();

	/**
	 * Fetch the required packet related information from input 'PacketStatusDTO'
	 * object and map it to 'SyncRegistrationDTO' object. It encrypts the request
	 * before invoking the external service. Then it invokes 'syncPacketsToServer'
	 * to make a call to external service 'Packet sync' to sync the packet with the
	 * server.
	 *
	 * <p>
	 * The status of the packets has to be any one of the following:
	 * </p>
	 * <ul>
	 * <li>APPROVED</li>
	 * <li>REJECTED</li>
	 * <li>RE_REGISTER_APPROVED</li>
	 * </ul>
	 *
	 * <p>
	 * On successful sync of packet with the server through Packet Sync Service, the
	 * Server Status Code of that packet would be updated to PUSHED
	 * </p>
	 *
	 * @param triggerpoint
	 * @return
	 */
	ResponseDTO syncPacket(@NonNull String triggerpoint);

	ResponseDTO syncPacket(@NonNull String triggerPoint, @NonNull List<String> rids);

	ResponseDTO syncAllPackets(String triggerPoint);
}