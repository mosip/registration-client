package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.LocalConfigDAOImpl;
import io.mosip.registration.entity.LocalPreferences;
import io.mosip.registration.entity.PermittedLocalConfig;
import io.mosip.registration.repositories.LocalPreferencesRepository;
import io.mosip.registration.repositories.PermittedLocalConfigRepository;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class LocalConfigDAOImplTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private LocalConfigDAOImpl localConfigDAOImpl;
	
	@Mock
	private PermittedLocalConfigRepository permittedLocalConfigRepository;

	@Mock
	private LocalPreferencesRepository localPreferencesRepository;
	
	@Mock
	private ApplicationContext applicationContext;
	
	Map<String,Object> appMap = new HashMap<>();
	
	@Before
	public void initialize() throws Exception {
		appMap.put(RegistrationConstants.DEFAULT_HOST_NAME, "b2ml27210");
		PowerMockito.mockStatic(ApplicationContext.class);
		ApplicationContext.setApplicationMap(appMap);
	}
	
	@Test
	public void getAllPermittedLocalConfigsTest() {
		List<PermittedLocalConfig> params = new ArrayList<>();		
		Mockito.when(permittedLocalConfigRepository.findByIsActiveTrue()).thenReturn(params);
		assertEquals(params, localConfigDAOImpl.getAllPermittedLocalConfigs());
	}
	
	@Test
	public void getPermittedJobsTest() {
		List<String> permittedJobs = new ArrayList<>();
		permittedJobs.add("Test Job");
		List<PermittedLocalConfig> permittedConfigs = new ArrayList<>();
		PermittedLocalConfig config = new PermittedLocalConfig();
		config.setName("Test Job");
		config.setConfigType("JOB");
		permittedConfigs.add(config);
		Mockito.when(permittedLocalConfigRepository.findByIsActiveTrueAndConfigType(Mockito.anyString())).thenReturn(permittedConfigs);
		assertEquals(permittedJobs, localConfigDAOImpl.getPermittedJobs("JOB"));
	}
	
	@Test
	public void getLocalConfigurationsTest() {
		List<LocalPreferences> localPreferencesList = new ArrayList<>();
		LocalPreferences localPreference = new LocalPreferences();
		localPreference.setConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		localPreference.setName("mosip.test.config");
		localPreference.setVal("test");
		localPreferencesList.add(localPreference);
		Mockito.when(localPreferencesRepository
				.findByIsDeletedFalseAndConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE)).thenReturn(localPreferencesList);
		Map<String, String> localConfigMap = new HashMap<>();
		localConfigMap.put("mosip.test.config", "test");
		assertEquals(localConfigMap, localConfigDAOImpl.getLocalConfigurations());
	}
	
	@Test
	public void getPermittedConfigurationsTest() {		
		List<PermittedLocalConfig> permittedConfigs = new ArrayList<>();
		PermittedLocalConfig config = new PermittedLocalConfig();
		config.setName("Test Configuration");
		config.setConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		permittedConfigs.add(config);
		Mockito.when(permittedLocalConfigRepository.findByIsActiveTrueAndConfigType(Mockito.anyString())).thenReturn(permittedConfigs);
		List<String> permittedConfigurations = new ArrayList<>();
		permittedConfigurations.add("Test Configuration");
		assertEquals(permittedConfigurations, localConfigDAOImpl.getPermittedConfigurations(RegistrationConstants.PERMITTED_CONFIG_TYPE));
	}
	
	@Test
	public void modifyJobTestUpdate() {
		LocalPreferences localPreferences = new LocalPreferences();
		localPreferences.setConfigType(RegistrationConstants.PERMITTED_JOB_TYPE);
		localPreferences.setName("Test Job");
		localPreferences.setVal("0 0 11 * * ?");
		Mockito.when(localPreferencesRepository.findByIsDeletedFalseAndName(Mockito.anyString())).thenReturn(localPreferences);
		localConfigDAOImpl.modifyJob("Test Job", "0 0 11 * * ?");
	}
	
	@Test
	public void modifyJobTestSave() {
		Mockito.when(localPreferencesRepository.findByIsDeletedFalseAndName(Mockito.anyString())).thenReturn(null);
		localConfigDAOImpl.modifyJob("Test Job", "0 0 11 * * ?");
	}
	
	@Test
	public void modifyConfigurationsTestUpdate() {
		Map<String, String> localPreferencesMap = new HashMap<>();
		localPreferencesMap.put("mosip.test.config", "test");
		LocalPreferences localPreferences = new LocalPreferences();
		localPreferences.setConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		localPreferences.setName("mosip.test.config");
		localPreferences.setVal("test");
		Mockito.when(localPreferencesRepository.findByIsDeletedFalseAndName(Mockito.anyString())).thenReturn(localPreferences);
		localConfigDAOImpl.modifyConfigurations(localPreferencesMap);
	}
	
	@Test
	public void modifyConfigurationsTestSave() {
		Map<String, String> localPreferencesMap = new HashMap<>();
		localPreferencesMap.put("mosip.test.config", "test");
		Mockito.when(localPreferencesRepository.findByIsDeletedFalseAndName(Mockito.anyString())).thenReturn(null);
		localConfigDAOImpl.modifyConfigurations(localPreferencesMap);
	}
	
	@Test
	public void getValueTest() {
		LocalPreferences localPreferences = new LocalPreferences();
		localPreferences.setConfigType(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		localPreferences.setName("mosip.test.config");
		localPreferences.setVal("test");
		Mockito.when(localPreferencesRepository.findByIsDeletedFalseAndName(Mockito.anyString())).thenReturn(localPreferences);
		assertEquals(localPreferences.getVal(), localConfigDAOImpl.getValue("mosip.test.config"));
	}

}
