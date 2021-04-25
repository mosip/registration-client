package io.mosip.registration.test.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.impl.LocalConfigDAOImpl;
import io.mosip.registration.entity.PermittedLocalConfig;
import io.mosip.registration.service.config.impl.LocalConfigServiceImpl;

public class LocalConfigServiceImplTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private LocalConfigServiceImpl localConfigServiceImpl;
	
	@Mock
	private LocalConfigDAOImpl localConfigDAOImpl;
	
	@Test
	public void getAllPermittedLocalConfigsTest() {
		List<PermittedLocalConfig> params = new ArrayList<>();		
		Mockito.when(localConfigDAOImpl.getAllPermittedLocalConfigs()).thenReturn(params);
		assertEquals(params, localConfigServiceImpl.getAllPermittedLocalConfigs());
	}
	
	@Test
	public void getPermittedJobsTest() {
		List<String> permittedJobs = new ArrayList<>();
		permittedJobs.add("Test Job");
		Mockito.when(localConfigDAOImpl.getPermittedJobs(Mockito.anyString())).thenReturn(permittedJobs);
		assertEquals(permittedJobs, localConfigServiceImpl.getPermittedJobs("JOB"));
	}
	
	@Test
	public void getLocalConfigurationsTest() {
		Map<String, String> localConfigMap = new HashMap<>();
		localConfigMap.put("mosip.test.config", "test");
		Mockito.when(localConfigDAOImpl.getLocalConfigurations()).thenReturn(localConfigMap);		
		assertEquals(localConfigMap, localConfigServiceImpl.getLocalConfigurations());
	}
	
	@Test
	public void getPermittedConfigurationsTest() {		
		List<String> permittedConfigurations = new ArrayList<>();
		permittedConfigurations.add("Test Configuration");
		Mockito.when(localConfigDAOImpl.getPermittedConfigurations(Mockito.anyString())).thenReturn(permittedConfigurations);
		assertEquals(permittedConfigurations, localConfigServiceImpl.getPermittedConfigurations(RegistrationConstants.PERMITTED_CONFIG_TYPE));
	}
	
	@Test
	public void modifyJobTest() {
		Mockito.doNothing().when(localConfigDAOImpl).modifyJob(Mockito.anyString(), Mockito.anyString());
		localConfigServiceImpl.modifyJob("Test Job", "0 0 11 * * ?");
	}
	
	@Test
	public void modifyConfigurationsTest() {
		Map<String, String> localPreferencesMap = new HashMap<>();
		localPreferencesMap.put("mosip.test.config", "test");
		Mockito.doNothing().when(localConfigDAOImpl).modifyConfigurations(Mockito.anyMap());
		localConfigServiceImpl.modifyConfigurations(localPreferencesMap);
	}
	
	@Test
	public void getValueTest() {
		Mockito.when(localConfigDAOImpl.getValue(Mockito.anyString())).thenReturn("test");
		assertEquals("test", localConfigServiceImpl.getValue("mosip.test.config"));
	}

}
