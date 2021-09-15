package io.mosip.registration.service.packet;

import java.util.List;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.logger.logback.util.MetricTag;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * The interface to handle the registration data to create packet out of it and
 * save the encrypted packet data in the configured local system
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
public interface PacketHandlerService {

	/**
	 * Creates the in-memory zip file (packet) out of the {@link RegistrationDTO}
	 * object, then encrypt the in-memory zip file and save the encrypted data in
	 * the local storage
	 * 
	 * <p>
	 * Returns the {@link ResponseDTO} object.
	 * </p>
	 * 
	 * <p>
	 * If all the above processes had completed successfully,
	 * {@link SuccessResponseDTO} will be set in {@link ResponseDTO} object
	 * </p>
	 * 
	 * <p>
	 * If any exception occurs, {@link ErrorResponseDTO} will be set in
	 * {@link ResponseDTO} object
	 * </p>
	 * 
	 * @param registrationDTO
	 *            the registration data out of which in-memory zip file has to be
	 *            created
	 * @return the {@link ResponseDTO} object
	 */
	public ResponseDTO handle(@MetricTag(value = "process", extractor = "arg.processId") RegistrationDTO registrationDTO);
	
	public List<Registration> getAllRegistrations();

	/**
	 * Creates an instance of registrationDTO {@link RegistrationDTO}, initializing it with applicationId
	 * IdSchemaVersion and process details
	 * @param id
	 * @param processId
	 * @return
	 * @throws RegBaseCheckedException
	 */
	public RegistrationDTO startRegistration(String id,  @MetricTag("process") String processId) throws RegBaseCheckedException;

	public List<PacketStatusDTO> getAllPackets();

	/**
	 * Creates, signs and encrypts Acknowledgement receipt with the provided content.
	 * signature is stored in registration table
	 * @param packetId
	 * @param content
	 * @param format
	 * @throws IOException
	 */
	public void createAcknowledgmentReceipt(String packetId, byte[] content, String format) throws IOException;

	/**
	 * Reads the ack receipt, decrypts the content and verifies the signature,
	 * returns the receipt content only when signature is valid
	 * @param packetId
	 * @param filepath
	 * @return
	 * @throws RegBaseCheckedException
	 * @throws IOException
	 */
	public String getAcknowledgmentReceipt(String packetId, String filepath)
			throws RegBaseCheckedException, IOException;
}
