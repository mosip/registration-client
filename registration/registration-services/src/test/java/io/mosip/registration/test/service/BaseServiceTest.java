package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class, ApplicationContext.class, RegistrationSystemPropertiesChecker.class })
public class BaseServiceTest {

	@Mock
	private MachineMappingDAO machineMappingDAO;
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private BaseService baseService;

	@Mock
	private UserOnboardDAO onboardDAO;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private GlobalParamService globalParamService;
	
	@Mock
	private LocalConfigService localConfigService;
	
	@Before
	public void init() throws Exception {
		
		Map<String,Object> appMap = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, false);
		ApplicationContext.getInstance().setApplicationMap(map);
		
		List<String> mandatoryLanguages = getMandaoryLanguages();
		List<String> optionalLanguages =  getOptionalLanguages();	
		int minLanguagesCount = 1;
		int maxLanguagesCount = 10;
		
		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn("eng").when(ApplicationContext.class, "applicationLanguage");
		PowerMockito.doReturn("test").when(RegistrationSystemPropertiesChecker.class, "getMachineId");
		
		ReflectionTestUtils.setField(baseService, "mandatoryLanguages", mandatoryLanguages);
		ReflectionTestUtils.setField(baseService, "optionalLanguages", optionalLanguages);
		ReflectionTestUtils.setField(baseService, "minLanguagesCount", minLanguagesCount);
		ReflectionTestUtils.setField(baseService, "maxLanguagesCount", maxLanguagesCount);
	}


	@Test
	public void getUserIdTest() {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(SessionContext.userId()).thenReturn("MYUSERID");
		Assert.assertSame(baseService.getUserIdFromSession(), "MYUSERID");
	}

	@Test
	public void isNullTest() {
		Assert.assertSame(baseService.isNull(null), true);

	}

	@Test
	public void isEmptyTest() {
		Assert.assertSame(baseService.isEmpty(new LinkedList<>()), true);
	}

	@Test
	public void getStationIdTest() {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
		Assert.assertSame("11002", baseService.getStationId());
	}

	@Test
	public void getNegativeStationIdTest() {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(false);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
		Assert.assertSame(null, baseService.getStationId());
	}
	
	@Test
	public void getStationIdTrueTest() {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
		Assert.assertSame(machine.getId(), baseService.getStationId());
	}
	
	@Test
	public void getMandatoryLanguagesTest() {
	    assertTrue(baseService.getMandatoryLanguages().size()>0);
	}
	
	@Test
	public void getOptionalLanguagesTest()  throws PreConditionCheckException{
	    assertTrue(baseService.getOptionalLanguages().size()>0);
	}
	
	@Ignore
	@Test
	public void getOptionalLanguagesFailureTest()  throws PreConditionCheckException{
		List<String> mandatoryLanguages = new ArrayList<String>();
		List<String> optionalLanguages = new ArrayList<String>();	
		ReflectionTestUtils.setField(baseService, "mandatoryLanguages", mandatoryLanguages);
		ReflectionTestUtils.setField(baseService, "optionalLanguages", optionalLanguages);
	    baseService.getOptionalLanguages();
	}
		
	@Test
	public void getMinLanguagesCountTest()  throws PreConditionCheckException{			
	    assertTrue(baseService.getMaxLanguagesCount()>0);
	}
	
	@Test
	public void getMaxLanguagesCountTest()  throws PreConditionCheckException{
	    assertTrue(baseService.getMaxLanguagesCount()>0);
	}
	
	@Test
	public void setSuccessResponseTest() {
		ResponseDTO responseDTO = new ResponseDTO();
		Map<String, Object> attributes = new HashMap<String, Object>();
		assertNotNull(baseService.setSuccessResponse(responseDTO,"message", attributes));
	}
	
	@Ignore
	@Test
	public void getGlobalConfigValueOfTest() {
		Map<String, Object> globalProps = new HashMap<String, Object>(); 
		Map<String, String> localProps = new HashMap<String, String>(); 
		JSONObject ageGroupConfig = new JSONObject(
				(String) ApplicationContext.map().get(RegistrationConstants.AGE_GROUP_CONFIG));
		Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when( globalParamService.getGlobalParams()).thenReturn(globalProps);
		Mockito.when( localConfigService.getLocalConfigurations()).thenReturn(localProps);
		assertNotNull(baseService.getGlobalConfigValueOf(RegistrationConstants.AGE_GROUP_CONFIG));
	}
	
	@Test(expected=RegBaseCheckedException.class)
	public void getMachineTest() throws RegBaseCheckedException{
		MachineMaster machineMaster = null;
		Mockito.when( machineMasterRepository.findByNameIgnoreCase(Mockito.any())).thenReturn(machineMaster);
		baseService.getMachine();
	}
	
	private List<String> getMandaoryLanguages(){		
		List<String> mandLanguages = new ArrayList<String>();
		mandLanguages.add("English");
		return mandLanguages;
	}
	
	private List<String> getOptionalLanguages(){		
		List<String> mandLanguages = new ArrayList<String>();
		mandLanguages.add("Hindi");
		return mandLanguages;
		
	}

}
