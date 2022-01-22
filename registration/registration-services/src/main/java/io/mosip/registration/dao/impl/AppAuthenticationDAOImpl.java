package io.mosip.registration.dao.impl;

import java.util.*;
import java.util.stream.Collectors;

import io.mosip.registration.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.AppAuthenticationDAO;
import io.mosip.registration.dao.AppAuthenticationDetails;
import io.mosip.registration.dao.AppRolePriorityDetails;
import io.mosip.registration.repositories.AppAuthenticationRepository;
import io.mosip.registration.repositories.AppRolePriorityRepository;

/**
 * The implementation class of {@link AppAuthenticationDAO}.
 *
 * @author Sravya Surampalli
 * @since 1.0.0
 */
@Repository
public class AppAuthenticationDAOImpl implements AppAuthenticationDAO {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AppAuthenticationDAOImpl.class);

	private static final String DEFAULT_LOGIN_METHOD = "PWD";

	/** The AppAuthentication repository. */
	@Autowired
	private AppAuthenticationRepository appAuthenticationRepository;
	
	/** The AppRolePriority repository. */
	@Autowired
	private AppRolePriorityRepository appRolePriorityRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mosip.registration.dao.RegistrationAppLoginDAO#getModesOfLogin()
	 */
	public List<String> getModesOfLogin(String authType, Set<String> roleList) {
		LOGGER.info("Fetching list of login modes");

		List<String> loginModes = new ArrayList<>();

		if(roleList == null || roleList.isEmpty()) {
			LOGGER.info("Role list is empty for logged in user, returning back {} as default login method", DEFAULT_LOGIN_METHOD);
			loginModes.add(DEFAULT_LOGIN_METHOD);
			return loginModes;
		}

		//get role priorities based on auth-type
		List<AppRolePriorityDetails> appRolePriorityDetails = appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority(authType, roleList);

		Role role = Role.getHighestRankingRole(roleList);
		//if no app_role_priority entry is found, simply follow default role rankings
		String highPriorityRoleConsidered = (appRolePriorityDetails != null && !appRolePriorityDetails.isEmpty()) ?
				appRolePriorityDetails.get(0).getAppRolePriorityId().getRoleCode() : (role != null ? role.name() : null);


		if(highPriorityRoleConsidered != null) {
			List<AppAuthenticationDetails> loginList = appAuthenticationRepository
					.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeOrderByMethodSequence(authType,
							highPriorityRoleConsidered);

			if(loginList != null && !loginList.isEmpty()) {
				LOGGER.info("List of login modes fetched successfully, {}", loginList);
				return loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
			}
		}
		return loginModes;
	}
}
