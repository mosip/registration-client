package io.mosip.registration.service.operator.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserDetailService;
import lombok.NonNull;

/**
 * Implementation for {@link UserDetailService}
 * 
 * @author Sreekar Chukka
 *
 */
@Service
public class UserDetailServiceImpl extends BaseService implements UserDetailService {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserDetailDAO userDetailDAO;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(UserDetailServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.UserDetailService#save()
	 */
	@Timed
	public synchronized ResponseDTO save(String triggerPoint) throws RegBaseCheckedException {
		ResponseDTO responseDTO = new ResponseDTO();
		LOGGER.info("Entering into user detail save method...");
		try {

			//Precondition check, proceed only if met, otherwise throws exception
			proceedWithMasterAndKeySync(null);

			LinkedHashMap<String, Object> userDetailSyncResponse = getUsrDetails(triggerPoint);

			if (null == userDetailSyncResponse.get(RegistrationConstants.RESPONSE))
				return getHttpResponseErrors(responseDTO, userDetailSyncResponse);

			LinkedHashMap<String, String> responseMap = (LinkedHashMap<String, String>) userDetailSyncResponse.get(RegistrationConstants.RESPONSE);

			List<UserDetailDto> userDtls = null;
			if (responseMap.containsKey("userDetails")) {
				byte[] data = clientCryptoFacade.decrypt(CryptoUtil.decodeURLSafeBase64(responseMap.get("userDetails")));
				userDtls = objectMapper.readValue(data,	new TypeReference<List<UserDetailDto>>() {});
			}

			if(userDtls == null) {
				LOGGER.error("userDetails not found in the response map, user sync failed");
				return setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);
			}

			//Remove users who are not part of current sync
			List<UserDetail> existingUserDetails = userDetailDAO.getAllUsers();
			for (UserDetail existingUserDetail : existingUserDetails) {
				Optional<UserDetailDto> result = userDtls.stream().filter(userDetailDto -> userDetailDto
						.getUserId().equalsIgnoreCase(existingUserDetail.getId())).findFirst();
				if (!result.isPresent()) {
					LOGGER.info("Deleting User : {} ", existingUserDetail.getId());
					userDetailDAO.deleteUser(existingUserDetail);
				}
			}

			for (UserDetailDto user : userDtls) {
				userDetailDAO.save(user);
			}

			responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);
			LOGGER.info("User Detail Sync Success......");

		} catch (RegBaseCheckedException | IOException exception) {
			LOGGER.error(exception.getMessage(), exception);
			setErrorResponse(responseDTO, exception.getMessage(), null);
		}
		return responseDTO;
	}

	//checks if roles are modified, need to prompt to re-login on change
	public Map<String, Object> checkLoggedInUserRoles(List<String> newRoles) {
		Map<String, Object> attributes = new HashMap<>();
		List<String> oldRoles = SessionContext.userContext().getRoles();
		if (oldRoles.size() == newRoles.size() && oldRoles.containsAll(newRoles)) {
			return null;
		}
		attributes.put(RegistrationConstants.ROLES_MODIFIED, RegistrationConstants.ENABLE);
		return attributes;
	}


	@SuppressWarnings("unchecked")
	private LinkedHashMap<String, Object> getUsrDetails(String triggerPoint) throws RegBaseCheckedException {
		LOGGER.debug("Entering into user detail rest calling method");

		// Setting uri Variables
		Map<String, String> requestParamMap = new LinkedHashMap<>();
		String keyIndex = CryptoUtil
				.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
		requestParamMap.put("keyindex", keyIndex);
		requestParamMap.put(RegistrationConstants.VERSION, getCurrentSoftwareVersion());

		try {

			return (LinkedHashMap<String, Object>) serviceDelegateUtil
					.get(RegistrationConstants.USER_DETAILS_SERVICE_NAME, requestParamMap, true, triggerPoint);

		} catch (Exception exception) {
			LOGGER.error("Failed invoking userdetails API", exception);
			throw new RegBaseCheckedException(exception.getMessage(),
					exception.getLocalizedMessage());
		}
	}


	@Override
	public List<UserDetail> getAllUsers() {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Get All Users from UserDetail");
		return userDetailDAO.getAllUsers();
	}
	
	@Override
	public List<String> getUserRoleByUserId(String userId) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Finding role for the UserID : " + userId);
		
		List<UserRole> userRoles = userDetailDAO.getUserRoleByUserId(userId);
		List<UserRoleId> userRoleIdList = userRoles.stream().map(UserRole::getUserRoleId).collect(Collectors.toList());
		return userRoleIdList.stream().map(UserRoleId::getRoleCode).collect(Collectors.toList());	
	}

	@Counted(recordFailuresOnly = true)
	@Override
	public boolean isValidUser(@NonNull String userId) {
		return (null == userDetailDAO.getUserDetail(userId)) ? false : true;
	}
}
