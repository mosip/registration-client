package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.RegAuditRepository;

/**
 * The implementation class of {@link AuditDAO}
 * 
 * @author Balaji Sridharan
 * @author Yaswanth S
 * @since 1.0.0
 */
@Repository
public class AuditDAOImpl implements AuditDAO {

	@Autowired
	private RegAuditRepository regAuditRepository;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(AuditDAOImpl.class);

	@Override
	@Transactional
	public void deleteAudits(LocalDateTime auditLogToDtimes) {
		LOGGER.info("Deleting Audit Logs created before the given timestamp");
		regAuditRepository.deleteAllInBatchByCreatedAtLessThan(auditLogToDtimes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.dao.AuditDAO#getAudits(io.mosip.registration.entity.
	 * RegistrationAuditDates)
	 */
	@Override
	public List<Audit> getAudits(String registrationId, String timestamp) {
		LOGGER.info("REGISTRATION - FETCH_UNSYNCED_AUDITS - GET_ALL_AUDITS", APPLICATION_NAME, APPLICATION_ID,
				"Fetching of all the audits which are to be added to Registration packet started");

		try {
			List<Audit> audits;
			if (timestamp == null) {
				audits = regAuditRepository.findByIdOrderByCreatedAtAsc(registrationId);
			} else {
				audits = regAuditRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(
						Timestamp.valueOf(timestamp).toLocalDateTime());
			}

			LOGGER.info("REGISTRATION - FETCH_UNSYNCED_AUDITS - GET_ALL_AUDITS", APPLICATION_NAME, APPLICATION_ID,
					"Fetching of all the audits which are to be added to Registartion packet ended");

			return audits;
		} catch (RuntimeException exception) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.REG_GET_AUDITS_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_GET_AUDITS_EXCEPTION.getErrorMessage(), exception);
		}
	}

}
