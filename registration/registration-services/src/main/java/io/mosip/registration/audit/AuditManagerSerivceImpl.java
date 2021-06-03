package io.mosip.registration.audit;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.auditmanager.builder.AuditRequestBuilder;
import io.mosip.kernel.auditmanager.request.AuditRequestDto;
import io.mosip.kernel.core.auditmanager.spi.AuditHandler;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

/**
 * Class to Audit the events of Registration Client.
 * <p>
 * This class creates a wrapper around {@link AuditRequestBuilder} class. This
 * class creates a {@link AuditRequestBuilder} object for each audit event and
 * persists the same using {@link AuditHandler} .
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Service
public class AuditManagerSerivceImpl extends BaseService implements AuditManagerService {

	private static final Logger LOGGER = AppConfig.getLogger(AuditManagerSerivceImpl.class);
	
	@Autowired
	private AuditHandler<AuditRequestDto> auditHandler;

	@Autowired
	private AuditDAO auditDAO;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.audit.AuditFactory#audit(io.mosip.registration.
	 * constants.AuditEvent, io.mosip.registration.constants.Components,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void audit(AuditEvent auditEventEnum, Components appModuleEnum, String refId, String refIdType) {

		// Getting Host IP Address and Name
		String hostIP = "localhost";
		String hostName = RegistrationSystemPropertiesChecker.getMachineId();
		hostIP = hostIP != null ? hostIP : String.valueOf(ApplicationContext.map().get(RegistrationConstants.DEFAULT_HOST_IP));
		hostName = hostName != null ? hostName : String.valueOf(ApplicationContext.map().get(RegistrationConstants.DEFAULT_HOST_NAME));

		if (auditEventEnum.getId().contains(RegistrationConstants.REGISTRATION_EVENTS)
				&& getRegistrationDTOFromSession() != null
				&& getRegistrationDTOFromSession().getRegistrationId() != null) {
			refId = getRegistrationDTOFromSession().getRegistrationId();
			refIdType = AuditReferenceIdTypes.REGISTRATION_ID.getReferenceTypeId();
		} else if (SessionContext.userId() != null && !SessionContext.userId().equals("NA")) {
			refId = SessionContext.userId();
			refIdType = AuditReferenceIdTypes.USER_ID.getReferenceTypeId();
		}

		AuditRequestBuilder auditRequestBuilder = new AuditRequestBuilder();
		auditRequestBuilder.setActionTimeStamp(DateUtils.getUTCCurrentDateTime())
				.setApplicationId(String.valueOf(ApplicationContext.map().get(RegistrationConstants.APP_ID)))
				.setApplicationName(String.valueOf(ApplicationContext.map().get(RegistrationConstants.APP_NAME)))
				.setCreatedBy(SessionContext.userName()).setDescription(auditEventEnum.getDescription())
				.setEventId(auditEventEnum.getId()).setEventName(auditEventEnum.getName())
				.setEventType(auditEventEnum.getType()).setHostIp(hostIP).setHostName(hostName).setId(refId)
				.setIdType(refIdType).setModuleId(appModuleEnum.getId()).setModuleName(appModuleEnum.getName())
				.setSessionUserId(SessionContext.userId()).setSessionUserName(SessionContext.userName());

		auditHandler.addAudit(auditRequestBuilder.build());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.audit.AuditService#deleteAuditLogs()
	 */
	@Override
	public synchronized ResponseDTO deleteAuditLogs() {

		LOGGER.info(LoggerConstants.AUDIT_SERVICE_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Deletion of Audit Logs Started");

		ResponseDTO responseDTO = new ResponseDTO();

		String val = getGlobalConfigValueOf(RegistrationConstants.AUDIT_TIMESTAMP);

		if (val != null) {
			try {
				/* Delete Audits before given Time */
				auditDAO.deleteAudits(Timestamp.valueOf(val).toLocalDateTime());

				setSuccessResponse(responseDTO, RegistrationConstants.AUDIT_LOGS_DELETION_SUCESS_MSG, null);

			} catch (RuntimeException runtimeException) {
				LOGGER.error(LoggerConstants.AUDIT_SERVICE_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, runtimeException.getMessage());

				setErrorResponse(responseDTO, RegistrationConstants.AUDIT_LOGS_DELETION_FLR_MSG, null);
			}
		} else {
			setErrorResponse(responseDTO, RegistrationConstants.AUDIT_LOGS_DELETION_FLR_MSG, null);
		}
		LOGGER.info(LoggerConstants.AUDIT_SERVICE_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Deletion of Audit Logs Completed");

		return responseDTO;
	}
}
