package io.mosip.registration.dao.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserPassword;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.UserToken;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.repositories.UserBiometricRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.repositories.UserPwdRepository;
import io.mosip.registration.repositories.UserRoleRepository;
import io.mosip.registration.repositories.UserTokenRepository;

/**
 * The implementation class of {@link UserDetailDAO}.
 *
 * @author Sravya Surampalli
 * @since 1.0.0
 */
@Repository
@Transactional
public class UserDetailDAOImpl implements UserDetailDAO {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserDetailDAOImpl.class);

	/** The userDetail repository. */
	@Autowired
	private UserDetailRepository userDetailRepository;

	/** The userPwd repository. */
	@Autowired
	private UserPwdRepository userPwdRepository;

	/** The userRole repository. */
	@Autowired
	private UserRoleRepository userRoleRepository;

	/** The userBiometric repository. */
	@Autowired
	private UserBiometricRepository userBiometricRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;
	
	@Autowired
	private UserMachineMappingRepository userMachineMappingRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationUserDetailDAO#getUserDetail(java.lang.
	 * String)
	 */
	public UserDetail getUserDetail(String userId) {
		LOGGER.info("Fetching User details");

		UserDetail userDetail = userDetailRepository.findByIdIgnoreCase(userId);

		if(userDetail != null && userDetail.getIsActive())
			return userDetail;

		LOGGER.info("User details fetched with status : {}", (userDetail==null? null : userDetail.getIsActive()));
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.UserDetailDAO#updateLoginParams(io.
	 * mosip.registration.entity.UserDetail)
	 */
	public void updateLoginParams(UserDetail userDetail) {

		LOGGER.info("Updating Login params");

		userDetailRepository.save(userDetail);

		LOGGER.info("Updated Login params successfully");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationUserDetailDAO#getAllActiveUsers(java.
	 * lang. String)
	 */
	public List<UserBiometric> getAllActiveUsers(String attrCode) {
		LOGGER.info("Fetching all active users");
		return userBiometricRepository.findByUserBiometricIdBioAttributeCodeAndIsActiveTrue(attrCode);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mosip.registration.dao.RegistrationUserDetailDAO#
	 * getUserSpecificBioDetails(java.lang. String, java.lang.String)
	 */
	public List<UserBiometric> getUserSpecificBioDetails(String userId, String bioType) {
		LOGGER.info("Fetching user specific biometric details");
		return userBiometricRepository
				.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeIgnoreCase(userId, bioType);
	}

	public void save(UserDetailDto userDetailDto) {
		UserPassword usrPwd = new UserPassword();
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCase(userDetailDto.getUserId());
		boolean userStatus = userDetailDto.getIsActive() != null ? userDetailDto.getIsActive().booleanValue() : true;

		if(userDetail == null) {
			userDetail = new UserDetail();
			userDetail.setId(userDetailDto.getUserId());
			userDetail.setName(userDetailDto.getUserId());
		}
		else {
			usrPwd.setPwd(userDetail.getUserPassword().getPwd());
		}

		if(!userStatus) {//delete authtoken and biometrics of inactive users
			userTokenRepository.deleteByUsrId(userDetailDto.getUserId());
			userBiometricRepository.deleteByUserBiometricIdUsrId(userDetailDto.getUserId());
			userMachineMappingRepository.deleteByUserMachineMappingIdUsrId(userDetailDto.getUserId());
			userDetail.setUserToken(null);
			userDetail.setUserBiometric(Collections.emptySet());
			userDetail.setUserMachineMapping(Collections.emptySet());
		}

		usrPwd.setUsrId(userDetailDto.getUserId());
		usrPwd.setStatusCode("00");
		usrPwd.setIsActive(userStatus);
		usrPwd.setLangCode(ApplicationContext.applicationLanguage());
		usrPwd.setCrBy(SessionContext.isSessionContextAvailable() ? SessionContext.userContext().getUserId() :
				RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		usrPwd.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

		userDetail.setUserPassword(usrPwd);
		userDetail.setLangCode(ApplicationContext.applicationLanguage());
		userDetail.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDetail.setIsActive(userStatus);
		userDetail.setRegCenterId(userDetailDto.getRegCenterId());
		userDetail.setIsDeleted(userDetailDto.getIsDeleted());
		userDetail.setCrBy(SessionContext.isSessionContextAvailable() ? SessionContext.userContext().getUserId() :
				RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		userDetail.setStatusCode("00");

		userDetailRepository.saveAndFlush(userDetail);
		userPwdRepository.save(usrPwd);

		LOGGER.info("leaving user detail save method...");
	}

	@Override
	public UserBiometric getUserSpecificBioDetail(String userId, String bioType, String subType) {
		LOGGER.info("Fetching user specific subtype level biometric detail");

		return userBiometricRepository
				.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeAndUserBiometricIdBioAttributeCodeIgnoreCase(
						userId, bioType, subType);
	}

	@Override
	public List<UserBiometric> findAllActiveUsers(String bioType) {
		LOGGER.info("Fetching all local users for bioType >>> {}", bioType);
		return userBiometricRepository.findByUserBiometricIdBioTypeCodeAndIsActiveTrue(bioType);
	}

	@Override
	public List<UserBiometric> findAllActiveUsersExceptCurrentUser(String bioType, String userId) {
		LOGGER.info("Fetching all local users except login userid for bioType >>> {}", bioType);
		return userBiometricRepository.findByUserBiometricIdUsrIdNotAndUserBiometricIdBioTypeCodeAndIsActiveTrue(userId, bioType);
	}

	@Override
	public void updateAuthTokens(String userId, String authToken, String refreshToken, long tokenExpiry,
			long refreshTokenExpiry) {
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);
		UserToken userToken = null;
		if (userDetail != null) {
			if (userDetail.getUserToken() == null) {
				userToken = new UserToken();
				userToken.setUsrId(userId);
				userToken.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				userToken.setCrBy("System");
				userToken.setIsActive(true);
			} else
				userToken = userDetail.getUserToken();

			userToken.setToken(authToken);
			userToken.setRefreshToken(refreshToken);
			userToken.setTokenExpiry(tokenExpiry);
			userToken.setRtokenExpiry(refreshTokenExpiry);
			userToken.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

			userTokenRepository.save(userToken);

			userDetail.setUserToken(userToken);
			userDetailRepository.save(userDetail);
		}
	}

	@Override
	public void updateUserPwd(String userId, String password) throws Exception {
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);
		if (userDetail != null) {
			if (userDetail.getSalt() == null)
				userDetail
						.setSalt(CryptoUtil.encodeToURLSafeBase64(DateUtils.formatToISOString(LocalDateTime.now()).getBytes()));

			userDetail.getUserPassword().setPwd(HMACUtils2.digestAsPlainTextWithSalt(password.getBytes(),
					ClientCryptoUtils.decodeBase64Data(userDetail.getSalt())));
			userDetail.getUserPassword().setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

			userPwdRepository.save(userDetail.getUserPassword());
			userDetailRepository.save(userDetail);
		}
	}

	@Override
	public List<UserDetail> getAllUsers() {
		LOGGER.info("Fetching All User details");
		return userDetailRepository.findAll();
	}

	@Override
	public void deleteUser(UserDetail userDetail) {
		LOGGER.info("Deleting user");
		userDetailRepository.delete(userDetail);
	}

	@Override
	public void deleteUserRole(String userName) {
		LOGGER.info("Deleting Roles for user");
		List<UserRole> roles = userRoleRepository.findByUserRoleIdUsrId(userName);
		userRoleRepository.deleteInBatch(roles);
	}

	@Override
	public void update(UserDetail userDetail) {
		userDetailRepository.update(userDetail);
	}
	
	@Override
	public List<UserRole> getUserRoleByUserId(String userId) {
		return userRoleRepository.findByUserRoleIdUsrId(userId);
	}

	@Override
	public void updateUserRolesAndUsername( String userId, String username, List<String> roles) {
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCase(userId);

		if(userDetail == null) {
			LOGGER.info("User entry not found for the logged in user to update roles");
			return;
		}

		userDetail.setName(username);

		List<UserRole> existingRoles = userRoleRepository.findByUserRoleIdUsrId(userId);
		if(existingRoles != null && !existingRoles.isEmpty()) {
			userDetail.getUserRole().removeAll(existingRoles);
		}

		userDetailRepository.saveAndFlush(userDetail);

		if(roles != null) {
			for (String role : roles) {
				UserRole userRole = new UserRole();
				userRole.setIsActive(true);
				userRole.setLangCode(ApplicationContext.applicationLanguage());
				userRole.setCrBy(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
				userRole.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				UserRoleId roleId = new UserRoleId();
				roleId.setRoleCode(role);
				roleId.setUsrId(userId);
				userRole.setUserRoleId(roleId);
				userRoleRepository.saveAndFlush(userRole);
			}
		}
	}

}
