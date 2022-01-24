package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.constants.RegistrationConstants.MACHINE_MAPPING_LOGGER_TITLE;

import java.util.List;

import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;

/**
 * This DAO implementation of {@link MachineMappingDAO}
 * 
 * @author YASWANTH S
 * @author Brahmananda Reddy
 * @since 1.0.0
 *
 */
@Repository
public class MachineMappingDAOImpl implements MachineMappingDAO {

	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(MachineMappingDAOImpl.class);

	/**
	 * machineMasterRepository instance creation using autowired annotation
	 */
	@Autowired
	private MachineMasterRepository machineMasterRepository;

	/**
	 * machineMappingRepository instance creation using autowired annotation
	 */
	@Autowired
	private UserMachineMappingRepository machineMappingRepository;


	/*
	 * (non-Javadoc) Getting station id based on machineName
	 * 
	 * @see
	 * io.mosip.registration.dao.MachineMappingDAO#getStationID(java.lang.String)
	 */
	@Override
	public String getStationID(String machineName) throws RegBaseCheckedException {

		LOGGER.info(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
				"getStationID() machineName --> " + machineName);

		try {
			MachineMaster machineMaster = machineMasterRepository
					.findByIsActiveTrueAndNameIgnoreCase(machineName.toLowerCase());

			if (machineMaster != null && machineMaster.getId() != null) {
				return machineMaster.getId();
			} else {
				return null;
			}
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.MACHINE_MAPPING_STATIONID_RUN_TIME_EXCEPTION,
					runtimeException.getMessage());
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MachineMappingDAO#getUserMappingDetails(java.lang.
	 * String)
	 */
	@Override
	public List<UserMachineMapping> getUserMappingDetails(String machineId) {
		return machineMappingRepository.findByIsActiveTrueAndUserMachineMappingIdMachineId(machineId);
	}


	@Override
	public boolean isExists(String userId) {
		LOGGER.info("checking if user has onboarded to machine or not");
		return machineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(userId) != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.MachineMappingDAO#getKeyIndexByMachineName(java.
	 * lang. String)
	 */
	@Override
	public String getKeyIndexByMachineName(String machineName) {
		LOGGER.info(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
				"Fetching Key Index of Machine based on Machine name");

		MachineMaster machineMaster = machineMasterRepository
				.findByIsActiveTrueAndNameIgnoreCase(machineName.toLowerCase());

		LOGGER.info(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
				"Completed fetching Key Index of Machine based on Machine name");
		return machineMaster == null ? null : machineMaster.getKeyIndex();
	}

	@Override
	public MachineMaster getMachine() {
		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		return machineMasterRepository
				.findByIsActiveTrueAndNameIgnoreCase(machineName.toLowerCase());
	}

}