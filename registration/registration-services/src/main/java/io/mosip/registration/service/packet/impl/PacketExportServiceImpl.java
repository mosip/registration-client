package io.mosip.registration.service.packet.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.service.packet.PacketExportService;
import io.mosip.registration.util.advice.AuthenticationAdvice;
import io.mosip.registration.util.advice.PreAuthorizeUserId;

/**
 * Implementation class for {@link PacketExportService}
 * 
 * @author saravanakumar gnanaguru
 *
 */
@Service
public class PacketExportServiceImpl implements PacketExportService {

	@Autowired
	private RegistrationDAO registrationDAO;

	private static final Logger LOGGER = AppConfig.getLogger(PacketExportServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.impl.PacketExportService#
	 * getSynchedRecords()
	 */
	@Override
	public List<PacketStatusDTO> getSynchedRecords() {

		LOGGER.debug("REGISTRATION - FETCH_EXPORT_PACKETS - PACKET_EXPORT_SERVICE", APPLICATION_NAME, APPLICATION_ID,
				"Fetch the packets that needs to be exported");

		List<PacketStatusDTO> packetDto = new ArrayList<>();
		List<Registration> synchedPackets =  registrationDAO.getPacketsToBeSynched(RegistrationConstants.PACKET_EXPORT_STATUS);
		synchedPackets.forEach(packet -> {
			PacketStatusDTO packetStatusDTO =new PacketStatusDTO();
			packetStatusDTO.setClientStatusComments(packet.getClientStatusComments());
			packetStatusDTO.setFileName(packet.getId());
			packetStatusDTO.setPacketClientStatus(packet.getClientStatusCode());
			packetStatusDTO.setPacketPath(packet.getAckFilename());
			packetStatusDTO.setPacketServerStatus(packet.getServerStatusCode());
			packetStatusDTO.setUploadStatus(packet.getFileUploadStatus());
			packetDto.add(packetStatusDTO);
		});
		return packetDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.packet.impl.PacketExportService#
	 * updateRegistrationStatus(java.util.List)
	 */
	@Override
	@PreAuthorizeUserId(roles= {AuthenticationAdvice.OFFICER_ROLE,AuthenticationAdvice.SUPERVISOR_ROLE, AuthenticationAdvice.ADMIN_ROLE,AuthenticationAdvice.DEFAULT_ROLE})
	public void updateRegistrationStatus(List<PacketStatusDTO> exportedPackets) {

		LOGGER.debug("Updating the table with the exported status");

		exportedPackets.forEach(regPacket -> {
			registrationDAO.updateRegStatus(regPacket);
		});
	}
}
