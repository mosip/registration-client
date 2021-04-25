package io.mosip.registration.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.LocalConfigDAO;
import io.mosip.registration.entity.LocalPreferences;
import io.mosip.registration.entity.PermittedLocalConfig;
import io.mosip.registration.repositories.LocalPreferencesRepository;
import io.mosip.registration.repositories.PermittedLocalConfigRepository;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@Repository
public class LocalConfigDAOImpl implements LocalConfigDAO {
	
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(LocalConfigDAOImpl.class);

	@Autowired
	private PermittedLocalConfigRepository permittedLocalConfigRepository;

	@Autowired
	private LocalPreferencesRepository localPreferencesRepository;

	@Override
	public List<PermittedLocalConfig> getAllPermittedLocalConfigs() {
		LOGGER.info("Getting the list of permitted configurations");
		
		return permittedLocalConfigRepository.findByIsActiveTrue();
	}

	@Override
	public List<String> getPermittedJobs(String configType) {
		LOGGER.info("Getting the list of permitted configurations of type {}", configType);
		
		List<PermittedLocalConfig> permittedConfigs = permittedLocalConfigRepository.findByIsActiveTrueAndType(configType);
		List<String> permittedJobs = new ArrayList<>();
		if (permittedConfigs != null && !permittedConfigs.isEmpty()) {
			permittedJobs
					.addAll(permittedConfigs.stream().map(PermittedLocalConfig::getName).collect(Collectors.toList()));
		}
		return permittedJobs;
	}

	@Override
	public Map<String, String> getLocalConfigurations() {
		List<LocalPreferences> localPreferences = localPreferencesRepository
				.findByIsDeletedFalseAndConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		Map<String, String> localConfigMap = new HashMap<>();
		for (LocalPreferences localPreference : localPreferences) {
			localConfigMap.put(localPreference.getName(), localPreference.getVal());
		}
		return localConfigMap;
	}

	@Override
	public List<String> getPermittedConfigurations(String configType) {
		LOGGER.info("Getting the list of permitted configurations of type {}", configType);
		
		List<PermittedLocalConfig> permittedConfigs = permittedLocalConfigRepository.findByIsActiveTrueAndType(configType);
		List<String> permittedConfigurations = new ArrayList<>();
		if (permittedConfigs != null && !permittedConfigs.isEmpty()) {
			permittedConfigurations
					.addAll(permittedConfigs.stream().map(PermittedLocalConfig::getName).collect(Collectors.toList()));
		}
		return permittedConfigurations;
	}

	@Override
	public void modifyJob(String name, String value) {
		LOGGER.info("Modifying sync frequency for the job {}", name);
		
		LocalPreferences localPreferences = localPreferencesRepository.findByIsDeletedFalseAndName(name);
		if (localPreferences != null) {
			updateLocalPreference(localPreferences, RegistrationConstants.PERMITTED_JOB_TYPE);
		}
		saveLocalPreference(name, value, RegistrationConstants.PERMITTED_JOB_TYPE);
	}

	@Override
	public void modifyConfigurations(Map<String, String> localPreferences) {		
		for (Entry<String, String> entry : localPreferences.entrySet()) {
			LOGGER.info("Modifying configuration for key {}", entry.getKey());
			
			LocalPreferences localPreference = localPreferencesRepository.findByIsDeletedFalseAndName(entry.getKey());
			if (localPreference != null) {
				updateLocalPreference(localPreference, RegistrationConstants.PERMITTED_CONFIG_TYPE);
			}
			saveLocalPreference(entry.getKey(), entry.getValue(), RegistrationConstants.PERMITTED_CONFIG_TYPE);
		}
	}

	@Override
	public String getValue(String name) {
		LocalPreferences localPreference = localPreferencesRepository.findByIsDeletedFalseAndName(name);
		return localPreference != null ? localPreference.getVal() : RegistrationConstants.EMPTY;
	}

	private void updateLocalPreference(LocalPreferences localPreferences, String configType) {
		localPreferences.setConfigType(configType);
		localPreferences.setIsDeleted(true);
		localPreferences.setDelDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		localPreferences.setUpdBy(RegistrationConstants.JOB_TRIGGER_POINT_USER);
		localPreferences.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		localPreferencesRepository.update(localPreferences);
	}

	private void saveLocalPreference(String name, String value, String configType) {
		LocalPreferences localPreferences = new LocalPreferences();
		localPreferences.setId(UUID.randomUUID().toString());
		localPreferences.setName(name);
		localPreferences.setVal(value);
		localPreferences.setMachineName(RegistrationSystemPropertiesChecker.getMachineId());
		localPreferences.setConfigType(configType);
		localPreferences.setCrBy(RegistrationConstants.JOB_TRIGGER_POINT_USER);
		localPreferences.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		localPreferences.setIsDeleted(false);
		localPreferencesRepository.save(localPreferences);
	}

}
