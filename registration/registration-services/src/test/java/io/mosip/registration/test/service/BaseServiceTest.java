package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import io.mosip.registration.dto.*;
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
import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dao.UserOnboardDAO;
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
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ SessionContext.class, ApplicationContext.class, RegistrationSystemPropertiesChecker.class,JsonUtils.class,FileUtils.class,ImageIO.class,RegistrationAppHealthCheckUtil.class})
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
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;	
	
	@Mock
	private UserDetailService userDetailService;
	
	@Mock
	private UserDetailDAO userDetailDAO;
	
	@Mock
	private CenterMachineReMapService centerMachineReMapService;
	
	@Mock
	private PolicySyncService policySyncService;
	
		
	@Before
	public void init() throws Exception {

		Map<String, Object> appMap = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, false);
		map.put(RegistrationConstants.AGE_GROUP_CONFIG, "{'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}");
		ApplicationContext.getInstance();
		ApplicationContext.setApplicationMap(map);	
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
	public void getDefaultUserIdTest() throws Exception{
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		PowerMockito.doReturn("NA").when(SessionContext.class, "userId");
		Assert.assertSame(baseService.getUserIdFromSession(),"System");
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
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		assertNotNull(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP));
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
	public void getPreparePacketStatusDtoTest() throws Throwable,IOException  {
		Registration registration = getRegistration();
		PowerMockito.mockStatic(JsonUtils.class);
		PowerMockito.mockStatic(FileUtils.class);
		RegistrationDataDto registrationDataDto = getRegistrationDto();		
		Mockito.when(JsonUtils.jsonStringToJavaObject(Mockito.any(), Mockito.anyString())).thenReturn(registrationDataDto);		
		PowerMockito.mockStatic(FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("../pom.xml"));
		Assert.assertNotNull(baseService.preparePacketStatusDto(registration));
	}
	
	@Test
	public void getConfiguredLangCodesTest() throws Throwable,IOException  {
		Assert.assertNotNull(baseService.getConfiguredLangCodes());
	}
	
	@Test
	public void concatImagesTest() throws Throwable,IOException  {
		BufferedImage image = getBufferedScannedImage();
        PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(
				baseService.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH))).thenReturn(image);
		Assert.assertNotNull(baseService.concatImages(null, null,RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
	}

	@Test
	public void concatwithMultipleImagesTest() throws Throwable,IOException  {		
		BufferedImage image = getBufferedScannedImage();
        PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(
				baseService.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH))).thenReturn(image);
		Assert.assertNotNull(baseService.concatImages(null, null,null, null,RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
	}

	@Test
	public void concatImagesNotNullTest() throws Throwable, IOException {
		byte[] image1 = "image1".getBytes();
		try {
			Assert.assertNotNull(
					baseService.concatImages(image1, image1, RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
		} catch (Exception e) {

		}
	}
	
	@Ignore
	@Test(expected = IOException.class)
	public void concatImagesWithIOExceptionTest() throws Throwable,IOException  {
		BufferedImage image = getBufferedScannedImage();
        PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(
				baseService.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH))).thenReturn(image);
		Assert.assertNotNull(baseService.concatImages(null, null,"image"));
	}
	
	@Ignore
	@Test(expected = IOException.class)
	public void concatwithMultipleImagesIOExceptionTest() throws Throwable,IOException  {		
		BufferedImage image = getBufferedScannedImage();
        PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(
				baseService.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH))).thenReturn(image);
		Assert.assertNotNull(baseService.concatImages(null, null,null, null,RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
	}
	
	@Test
	public void concatwithMultipleImagesNotNullTest() throws Throwable,IOException  {		
		byte[] image1 = "image1".getBytes();
		try {
			Assert.assertNotNull(baseService.concatImages(image1, image1, image1, image1,RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
		} catch (Exception e) {

		}
	}
	
	@Test
	public void getHttpResponseErrorsTest()  {		
		ResponseDTO responseDTO = new ResponseDTO();
		LinkedHashMap<String, Object> httpResponse = new LinkedHashMap<String, Object>();
		HashMap<String, String> errorMsgs = new HashMap<String,String>();
		List<HashMap<String, String>> errorMsgsList = new ArrayList<HashMap<String, String>>();
		errorMsgs.put("REG-MDM-101", "JSON parsing error");
		errorMsgsList.add(errorMsgs);
		httpResponse.put(RegistrationConstants.ERRORS, errorMsgsList);
		assertNotNull(baseService.getHttpResponseErrors(responseDTO, httpResponse));
	}
	
	@Ignore
	@Test
	public void commonPreConditionChecksTest() throws PreConditionCheckException,Exception {	
		Map<String, Object> globalProps = new HashMap<String, Object>();
		Map<String, String> localProps = new HashMap<String, String>();
		localProps.put("key", "value");
		Mockito.when(globalParamService.getGlobalParams()).thenReturn(globalProps);
		Mockito.when(localConfigService.getLocalConfigurations()).thenReturn(localProps);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
		Mockito.when(userDetailService.isValidUser("12345")).thenReturn(false);
		Mockito.when(baseService.isInitialSync()).thenReturn(false);		
		baseService.commonPreConditionChecks("action");
	}
	@Test
	public void proceedWithMasterAndKeySynTest() throws PreConditionCheckException,Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		baseService.proceedWithMasterAndKeySync(RegistrationConstants.INITIAL_SETUP);
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithMasterAndKeySyncForMachineRemappedTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(true);
		baseService.proceedWithMasterAndKeySync(RegistrationConstants.INITIAL_SETUP);
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithMasterAndKeySyncForStationIdTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(baseService.getStationId()).thenReturn(null);
		baseService.proceedWithMasterAndKeySync(RegistrationConstants.OPT_TO_REG_PDS_J00003);
	}	
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithMasterAndKeySyncForIsMachineCenterActiveTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(baseService.getStationId()).thenReturn(null);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(false);
		baseService.proceedWithMasterAndKeySync(RegistrationConstants.INITIAL_SETUP);
	}	
	
	@Test
	public void proceedWithMachineCenterRemapTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		baseService.proceedWithMachineCenterRemap();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithSoftwareUpdateTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(true);
		baseService.proceedWithSoftwareUpdate();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithOperatorOnboardMachineRemappedTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(true);
		baseService.proceedWithOperatorOnboard();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithOperatorOnboardMachineIdTest() throws PreConditionCheckException,Exception {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(baseService.getStationId()).thenReturn(null);
		baseService.proceedWithOperatorOnboard();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithOperatorOnboardMachineCenterActiveTest() throws PreConditionCheckException, Exception {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(false);
		baseService.proceedWithOperatorOnboard();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationforSessionContextAvailableTest() throws PreConditionCheckException,Exception {		
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		baseService.proceedWithRegistration();
	}	
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationforInitialSyncTest() throws PreConditionCheckException,Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationforMachineRemappedTest() throws PreConditionCheckException,Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationMachineRemappedTest() throws PreConditionCheckException,Exception {
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(true);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationStationIdTest() throws PreConditionCheckException,Exception {
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(baseService.getStationId()).thenReturn(null);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationMachineCenterActiveTest() throws PreConditionCheckException, Exception {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(false);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationForCheckKeyValidationTest() throws PreConditionCheckException, Exception {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
		Mockito.when(policySyncService.checkKeyValidation()).thenReturn(null);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithRegistrationForCheckKeyValidationDTONotNullTest() throws PreConditionCheckException, Exception {
		ResponseDTO responseDTO = getResponseDTO();
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
		Mockito.when(policySyncService.checkKeyValidation()).thenReturn(responseDTO);
		baseService.proceedWithRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void commonPreConditionChecksforSessionContextAvailableTest() throws PreConditionCheckException,Exception {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(SessionContext.userId()).thenReturn("110011");		
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(false);
		baseService.commonPreConditionChecks("action");
	}
	
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithReRegistrationMachineIdTest() throws PreConditionCheckException, Exception {	
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(baseService.getStationId()).thenReturn(null);
		baseService.proceedWithReRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithReRegistrationMachineCenterActiveTest() throws PreConditionCheckException, Exception {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(false);
		baseService.proceedWithReRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithReRegistrationForCheckKeyValidationTest() throws PreConditionCheckException, Exception {
		ResponseDTO responseDTO = null;
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
		Mockito.when(policySyncService.checkKeyValidation()).thenReturn(responseDTO);
		baseService.proceedWithReRegistration();
	}
	
	@Test(expected = PreConditionCheckException.class)
	public void proceedWithReRegistrationForCheckKeyValidationDTONotNullTest() throws PreConditionCheckException, Exception {
		ResponseDTO responseDTO = getResponseDTO();
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		try {
			Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
			Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
			Mockito.when(baseService.getStationId()).thenReturn("11002");
		} catch (Exception e) {

		}
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
		Mockito.when(policySyncService.checkKeyValidation()).thenReturn(responseDTO);
		baseService.proceedWithReRegistration();
	}
	
	private ResponseDTO getResponseDTO() {
		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setMessage("message");;
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		return responseDTO;
		
	}
	private BufferedImage getBufferedScannedImage() {
		DocScannerFacade facade = new DocScannerFacade();
		DocScannerService serviceImpl = getMockDocScannerService();
		ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
		ScanDevice device = new ScanDevice();
		device.setDeviceType(DeviceType.SCANNER);
		device.setId("SCANNER_001");
		device.setName("SCANNER_001");
		device.setServiceName("Test-Scanner");
		BufferedImage image = facade.scanDocument(device, ".*");
		return image;
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
	 private DocScannerService getMockDocScannerService() {
	        return new DocScannerService() {
	            @Override
	            public String getServiceName() {
	                return "Test-Scanner";
	            }

	            @Override
	            public BufferedImage scan(ScanDevice docScanDevice, String deviceType) {
	                try {
	                    return ImageIO.read(this.getClass().getResourceAsStream("/images/stubdoc.png"));
	                } catch (IOException e) { }
	                return null;
	            }

	            @Override
	            public List<ScanDevice> getConnectedDevices() {
	                List<ScanDevice> devices = new ArrayList<>();
	                ScanDevice device1 = new ScanDevice();
	                device1.setDeviceType(DeviceType.SCANNER);
	                device1.setId("SCANNER_001");
	                device1.setName("SCANNER_001");
	                device1.setServiceName(getServiceName());
	                devices.add(device1);
	                ScanDevice device2 = new ScanDevice();
	                device2.setDeviceType(DeviceType.CAMERA);
	                device2.setId("CAMERA_001");
	                device2.setName("CAMERA_001");
	                device2.setServiceName(getServiceName());
	                devices.add(device2);
	                return devices;
	            }

	            @Override
	            public void stop(ScanDevice docScanDevice) {
	                //Do nothing
	            }
	        };
	    }
	}
