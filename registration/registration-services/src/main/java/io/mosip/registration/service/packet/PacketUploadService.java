package io.mosip.registration.service.packet;

import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * Service interface for Packet Upload to the server
 * 
 * @author saravanakumar gnanaguru
 * @since 1.0.0
 */
public interface PacketUploadService {

	 
	/**
	 * Uploads the required registration packet to the server after creation of
	 * registration packet.
	 * 
	 * <p>
	 * The registration packet will be sync with the server and then packet will be
	 * uploaded
	 * </p>
	 * <p>
	 * The client and server statuses will be updated after packet is uploaded
	 * </p>
	 * <p>
	 * The above process will be done only when EOD process is turned OFF
	 * </p>
	 *
	 * @param rid
	 *            the registration id of the Registration Packet to be uploaded to
	 *            the server
	 *
	 * @throws ConnectionException
	 */
	PacketStatusDTO uploadPacket(String rid) throws RegBaseCheckedException;


	/**
	 * Uploads all the registration packets which are already sync with the server
	 * during Machine Re-Mapping process
	 * 
	 * <p>
	 * The client and server statuses will be updated after packet is uploaded
	 * </p>
	 * @return 
	 */
	ResponseDTO uploadAllSyncedPackets();

	/**
	 * Uploads limited registration packets which are already sync with the server
	 * by job
	 * @return
	 */
	ResponseDTO uploadSyncedPackets();
}
