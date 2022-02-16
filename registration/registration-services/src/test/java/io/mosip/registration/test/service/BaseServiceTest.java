package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
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

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;
	
	@Before
	public void init() throws Exception {

		Map<String, Object> appMap = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, false);
		map.put(RegistrationConstants.AGE_GROUP_CONFIG, "{'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}");
		ApplicationContext.getInstance().setApplicationMap(map);		
	
		List<String> mandatoryLanguages = getMandaoryLanguages();
		List<String> optionalLanguages = getOptionalLanguages();
		int minLanguagesCount = 1;
		int maxLanguagesCount = 10;

		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
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
		assertTrue(baseService.getMandatoryLanguages().size() > 0);
	}

	@Test
	public void getOptionalLanguagesTest() throws PreConditionCheckException {
		assertTrue(baseService.getOptionalLanguages().size() > 0);
	}

	@Ignore
	@Test
	public void getOptionalLanguagesFailureTest() throws PreConditionCheckException {
		List<String> mandatoryLanguages = new ArrayList<String>();
		List<String> optionalLanguages = new ArrayList<String>();
		ReflectionTestUtils.setField(baseService, "mandatoryLanguages", mandatoryLanguages);
		ReflectionTestUtils.setField(baseService, "optionalLanguages", optionalLanguages);
		baseService.getOptionalLanguages();
	}

	@Test
	public void getMinLanguagesCountTest() throws PreConditionCheckException {
		assertTrue(baseService.getMaxLanguagesCount() > 0);
	}

	@Test
	public void getMaxLanguagesCountTest() throws PreConditionCheckException {
		assertTrue(baseService.getMaxLanguagesCount() > 0);
	}

	@Test
	public void setSuccessResponseTest() {
		ResponseDTO responseDTO = new ResponseDTO();
		Map<String, Object> attributes = new HashMap<String, Object>();
		assertNotNull(baseService.setSuccessResponse(responseDTO, "message", attributes));
	}

	@Test
	public void getGlobalConfigValueOfTest() {
		Map<String, Object> globalProps = new HashMap<String, Object>();
		Map<String, String> localProps = new HashMap<String, String>();
		localProps.put("key", "value");
		Mockito.when(globalParamService.getGlobalParams()).thenReturn(globalProps);
		Mockito.when(localConfigService.getLocalConfigurations()).thenReturn(localProps);
		assertSame(null,baseService.getGlobalConfigValueOf(RegistrationConstants.AGE_GROUP_CONFIG));
	}

	@Test(expected = RegBaseCheckedException.class)
	public void getMachineTest() throws RegBaseCheckedException {
		MachineMaster machineMaster = null;
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.any())).thenReturn(machineMaster);
		baseService.getMachine();
	}

	@Test
	public void getCenterIdTest() {
		MachineMaster machine = new MachineMaster();
		machine.setRegCenterId("regCenterId");
		RegistrationCenter registrationCenter = getRegistrationCenter();				
		Optional<RegistrationCenter> registrationCenterList = Optional.of(registrationCenter);	
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode("mosip","eng"))
		.thenReturn(registrationCenterList);		
		Assert.assertSame(null, baseService.getCenterId());
	}
	
	@Test
	@Ignore
	public void getPreparePacketStatusDtoTest() throws Throwable,IOException  {
		Registration registration = getRegistration();
		RegistrationDataDto registrationDataDto = getRegistrationDto();
		PowerMockito.mockStatic(JsonUtils.class);
		Mockito.when(JsonUtils.jsonStringToJavaObject(Mockito.any(), Mockito.anyString())).thenReturn(registrationDataDto);
		File mockFile = Mockito.mock(File.class);
		Mockito.when(FileUtils.getFile(Mockito.any())).thenReturn(mockFile);
		Assert.assertSame(null, baseService.preparePacketStatusDto(registration));
	}
	
	private List<String> getMandaoryLanguages() {
		List<String> mandLanguages = new ArrayList<String>();
		mandLanguages.add("English");
		return mandLanguages;
	}

	private List<String> getOptionalLanguages() {
		List<String> mandLanguages = new ArrayList<String>();
		mandLanguages.add("Hindi");
		return mandLanguages;
	}
	
	private RegistrationDataDto getRegistrationDto() {
		RegistrationDataDto registrationDataDto=new RegistrationDataDto();
		registrationDataDto.setName("test");
		registrationDataDto.setEmail("test@gmail.com");
		registrationDataDto.setPhone("9999999999");
		registrationDataDto.setLangCode("eng,fra");
		return registrationDataDto;
		
	}
	
	
	private RegistrationCenter getRegistrationCenter() {		
		RegistrationCenter registrationCenter = new RegistrationCenter();
		RegistartionCenterId registartionCenterId = new RegistartionCenterId();
		registartionCenterId.setId("10011");
		registrationCenter.setRegistartionCenterId(registartionCenterId);
		return registrationCenter;
	}
	
	private Registration getRegistration() {		
		Registration registration = new Registration();
		byte[] additionalInfo = "slkdalskdjslkajdjadj".getBytes();
		registration.setAppId("appId");
		registration.setPacketId("packetId");
		registration.setClientStatusCode("clientStatusCode");
		registration.setClientStatusComments("clientStatusComments");
		registration.setServerStatusCode("serverStatusCode");
		registration.setAckFilename("ackFilename");
		registration.setFileUploadStatus("fileUploadStatus");
		registration.setStatusCode("statusCode");
		registration.setClientStatusCode("clientStatusCode");
		registration.setClientStatusComments("clientStatusComments");
		registration.setCrDtime(new Timestamp(System.currentTimeMillis()));
		registration.setRegUsrId("regUsrId");
		registration.setAdditionalInfo(additionalInfo);
		return registration;
		
	}

}
