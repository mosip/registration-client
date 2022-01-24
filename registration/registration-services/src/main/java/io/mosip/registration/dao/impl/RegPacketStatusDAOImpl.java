package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.dao.RegPacketStatusDAO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.repositories.RegistrationRepository;

/**
 * The implementation class of {@link RegPacketStatusDAO}.
 *
 * @author Himaja Dhanyamraju
 */
@Repository
public class RegPacketStatusDAOImpl implements RegPacketStatusDAO {

	/** The registration repository. */
	@Autowired
	private RegistrationRepository registrationRepository;
	
	/**
	 * Object for Logger
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegPacketStatusDAOImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegPacketStatusDAO#getPacketIdsByStatusUploaded()
	 */
	@Override
	public List<Registration> getPacketIdsByStatusUploadedOrExported() {
		LOGGER.info("REGISTRATION - PACKET_STATUS_SYNC - REG_PACKET_STATUS_DAO", APPLICATION_NAME, APPLICATION_ID,
				"getting packets by status uploaded-successfully has been started");

		return registrationRepository.findByClientStatusCodeOrClientStatusCommentsOrderByCrDtime(
				RegistrationClientStatusCode.UPLOADED_SUCCESSFULLY.getCode(),
				RegistrationClientStatusCode.EXPORT.getCode());
	}
	
	@Override
	public List<Registration> getPacketIdsByStatusExported() {
		LOGGER.info("Getting packets by status comment - EXPORTED has been started");

		return registrationRepository
				.findByClientStatusCommentsOrderByCrDtime(RegistrationClientStatusCode.EXPORT.getCode());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.RegPacketStatusDAO#update(io.mosip.registration.
	 * entity.Registration)
	 */
	@Override
	public Registration update(Registration registration) {
		LOGGER.info("REGISTRATION - PACKET_STATUS_SYNC - REG_PACKET_STATUS_DAO", APPLICATION_NAME, APPLICATION_ID,
				"Update registration has been started");
		return registrationRepository.update(registration);

	}

	@Override
	public void delete(Registration registration) {
		LOGGER.info("Delete registration has been started");

		/* Delete Registartion */
		registrationRepository.deleteById(registration.getPacketId());
	}

}
