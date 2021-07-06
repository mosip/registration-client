package io.mosip.registration.test.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.*;
import io.mosip.registration.entity.*;
import io.mosip.registration.entity.id.*;
import io.mosip.registration.repositories.*;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.CertificateSyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import org.junit.Assert;
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

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.AppAuthenticationDAO;
import io.mosip.registration.dao.AppAuthenticationDetails;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.ScreenAuthorizationDAO;
import io.mosip.registration.dao.ScreenAuthorizationDetails;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.login.impl.LoginServiceImpl;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PublicKeySync;
import io.mosip.registration.service.sync.TPMPublicKeySyncService;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class ,
		RegistrationSystemPropertiesChecker.class })
public class LoginServiceTest {

	@Mock
	private AuditManagerSerivceImpl auditFactory;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private LoginServiceImpl loginServiceImpl;

	@Mock
	private AppAuthenticationRepository appAuthenticationRepository;

	@Mock
	private AppAuthenticationDAO appAuthenticationDAO;

	@Mock
	private UserDetailRepository userDetailRepository;

	@Mock
	private UserDetailDAO userDetailDAO;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private ScreenAuthorizationRepository screenAuthorizationRepository;

	@Mock
	private ScreenAuthorizationDAO screenAuthorizationDAO;

	@Mock
	private PublicKeySync publicKeySyncImpl;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private MasterSyncService masterSyncService;

	@Mock
	private UserDetailService userDetailService;

	@Mock
	private UserOnboardService userOnboardService;

	@Mock
	private TPMPublicKeySyncService tpmPublicKeySyncService;

	@Mock
	private CertificateSyncService certificateSyncService;

	@Mock
	private BaseService baseService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private CenterMachineRepository centerMachineRepository;

	private Map<String, Object> applicationMap = new HashMap<>();

	@Mock
	private AuthTokenUtilService authTokenUtilService;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private ClientCryptoService clientCryptoService;

	@Mock
	private LocalConfigService localConfigService;

	@Before
	public void initialize() throws Exception {
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());

		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		PowerMockito.mockStatic(HMACUtils2.class, RegistrationAppHealthCheckUtil.class,
				ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		when(baseService.getGlobalConfigValueOf(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS)).thenReturn("5");

		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		ApplicationContext.setApplicationMap(applicationMap);
		SessionContext.UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");

		Mockito.when(baseService.getCenterId(Mockito.anyString())).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive(Mockito.anyString())).thenReturn(true);

		//Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");

		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);

		CenterMachine centerMachine = new CenterMachine();
		CenterMachineId centerMachineId = new CenterMachineId();
		centerMachineId.setMachineId("11002");
		centerMachineId.setRegCenterId("10011");
		centerMachine.setCenterMachineId(centerMachineId);
		centerMachine.setIsActive(true);
		Mockito.when(centerMachineRepository.findByCenterMachineIdMachineId(Mockito.anyString())).thenReturn(centerMachine);

		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn("testststststs".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void getModesOfLoginTest() {

		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		Set<String> roleSet = new HashSet<>();
		roleSet.add("OFFICER");
		Mockito.when(appAuthenticationRepository
				.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence(
						"LOGIN", roleSet))
				.thenReturn(loginList);

		List<String> modes = new ArrayList<>();
		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode())
				.collect(Collectors.toList());

		Mockito.when(appAuthenticationRepository
				.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence(
						"LOGIN", roleSet))
				.thenReturn(loginList);

		Mockito.when(authTokenUtilService.hasAnyValidToken()).thenReturn(false);
		Mockito.when(appAuthenticationDAO.getModesOfLogin("LOGIN", roleSet)).thenReturn(modes);
		assertEquals(modes, loginServiceImpl.getModesOfLogin("LOGIN", roleSet));
	}
	
	@Test
	public void getModesOfLoginNegativeTest() {
		Set<String> roleSet = new HashSet<>();		
		loginServiceImpl.getModesOfLogin("LOGIN", roleSet);		
	}
	
	@Test
	public void getUserDetailTest() {

		UserDetail userDetail = new UserDetail();
		userDetail.setId("mosip");
		Mockito.when(userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(Mockito.anyString()))
				.thenReturn(userDetail);

		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);

		UserDTO userDTO = new UserDTO();
		userDTO.setId(userDetail.getId());

		assertEquals(userDTO, loginServiceImpl.getUserDetail("mosip"));
	}

	@Test
	public void getUserDetailFailureTest() {
		loginServiceImpl.getUserDetail("");
	}

	@Test
	public void getRegistrationCenterDetailsTest() {

		RegistrationCenter registrationCenter = new RegistrationCenter();

		RegistrationCenterDetailDTO centerDetailDTO = new RegistrationCenterDetailDTO();
		Optional<RegistrationCenter> registrationCenterList = Optional.of(registrationCenter);
		Mockito.when(
				registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(
						Mockito.anyString(), Mockito.anyString()))
				.thenReturn(registrationCenterList);

		Mockito.when(registrationCenterDAO.getRegistrationCenterDetails(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(centerDetailDTO);

		assertEquals(centerDetailDTO, loginServiceImpl.getRegistrationCenterDetails("mosip", "eng"));
	}
	
	@Test
	public void getRegistrationCenterDetailsFailureTest() {
		loginServiceImpl.getRegistrationCenterDetails("", "eng");
	}

	@Test
	public void getScreenAuthorizationDetailsTest() {

		Set<ScreenAuthorizationDetails> authorizationList = new HashSet<>();
		List<String> roleList = new ArrayList<>();
		roleList.add("REGISTRATION_OFFICER");
		
		AuthorizationDTO authorizationDTO = new AuthorizationDTO();
		authorizationDTO.setAuthorizationRoleCode(roleList);
		when(screenAuthorizationRepository
				.findByScreenAuthorizationIdRoleCodeInAndIsPermittedTrueAndIsActiveTrue(roleList))
				.thenReturn(authorizationList);
		when(screenAuthorizationDAO.getScreenAuthorizationDetails(roleList)).thenReturn(authorizationDTO);
		Assert.assertNotNull(loginServiceImpl.getScreenAuthorizationDetails(roleList).getAuthorizationRoleCode());

	}
	
	@Test
	public void getScreenAuthorizationDetailsFailureTest() {
		List<String> roleList = new ArrayList<>();
		loginServiceImpl.getScreenAuthorizationDetails(roleList);
	}

	@Test
	public void updateLoginParamsTest() {
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());
		doNothing().when(userDetailDAO).updateLoginParams(Mockito.any(UserDetail.class));

		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		userDTO.setUnsuccessfulLoginCount(0);
		userDTO.setLastLoginDtimes(new Timestamp(System.currentTimeMillis()));
		userDTO.setLastLoginMethod("PWD");
		userDTO.setUserlockTillDtimes(new Timestamp(System.currentTimeMillis()));

		UserDetail userDetail = new UserDetail();
		userDetail.setId(userDTO.getId());
		userDetail.setUnsuccessfulLoginCount(userDTO.getUnsuccessfulLoginCount());
		userDetail.setLastLoginDtimes(new Timestamp(System.currentTimeMillis()));
		userDetail.setLastLoginMethod(userDTO.getLastLoginMethod());
		userDetail.setUserlockTillDtimes(userDTO.getUserlockTillDtimes());

		Mockito.when(userDetailDAO.getUserDetail(userDTO.getId())).thenReturn(userDetail);

		loginServiceImpl.updateLoginParams(userDTO);
	}
	
	@Test
	public void updateLoginParamsFailureTest() {
		loginServiceImpl.updateLoginParams(null);
	}

	@Test
	public void initialSyncTest() throws Exception {
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		applicationMap.put(RegistrationConstants.USER_DTO, new LoginUserDTO());
		doNothing().when(userDetailDAO).updateUserPwd(Mockito.any(), Mockito.any());
		doNothing().when(baseService).commonPreConditionChecks(Mockito.anyString());
		doNothing().when(baseService).proceedWithMasterAndKeySync(Mockito.anyString());

		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setOtherAttributes(new HashMap<>());
		responseDTO.setSuccessResponseDTO(successResponseDTO);

		Mockito.when(tpmPublicKeySyncService.syncTPMPublicKey()).thenReturn(responseDTO);

		Mockito.when(publicKeySyncImpl.getPublicKey(Mockito.anyString())).thenReturn(responseDTO);

		Mockito.when(globalParamService.synchConfigData(false)).thenReturn(responseDTO);

		Mockito.when(masterSyncService.getMasterSync(Mockito.anyString(), Mockito.anyString())).thenReturn(responseDTO);

		Mockito.when(userDetailService.save(Mockito.anyString())).thenReturn(responseDTO);

		Mockito.when(certificateSyncService.getCACertificates(Mockito.anyString())).thenReturn(responseDTO);
		
		ApplicationContext.setApplicationMap(applicationMap);
		List<String> result = loginServiceImpl.initialSync(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);

		Assert.assertTrue(result.contains(RegistrationConstants.SUCCESS));
	}
	
	@Test
	public void initialSyncFailureTest() throws RegBaseCheckedException {
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.ENABLE);
		ApplicationContext.setApplicationMap(applicationMap);

		ResponseDTO responseDTO = new ResponseDTO();
		ErrorResponseDTO errorResponseDTO=new ErrorResponseDTO();
		
		List<ErrorResponseDTO> errorResponseDTOs=new LinkedList<>();
		errorResponseDTOs.add(errorResponseDTO);
		
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		
		Mockito.when(tpmPublicKeySyncService.syncTPMPublicKey()).thenReturn(responseDTO);

		Mockito.when(publicKeySyncImpl.getPublicKey(RegistrationConstants.JOB_TRIGGER_POINT_USER))
				.thenReturn(responseDTO);

		Mockito.when(globalParamService.synchConfigData(false)).thenReturn(responseDTO);

		Mockito.when(masterSyncService.getMasterSync(RegistrationConstants.OPT_TO_REG_MDS_J00001,
				RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(responseDTO);

		Mockito.when(userDetailService.save(RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(responseDTO);

		Assert.assertTrue(loginServiceImpl.initialSync(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM).contains(RegistrationConstants.FAILURE));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void initialSyncFailureExceptionTest() throws RegBaseCheckedException {
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		ApplicationContext.setApplicationMap(applicationMap);

		ResponseDTO responseDTO = new ResponseDTO();
		ErrorResponseDTO errorResponseDTO=new ErrorResponseDTO();
		
		List<ErrorResponseDTO> errorResponseDTOs=new LinkedList<>();
		errorResponseDTOs.add(errorResponseDTO);
		
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		
		Mockito.when(tpmPublicKeySyncService.syncTPMPublicKey()).thenThrow(RegBaseCheckedException.class);

		
		Assert.assertTrue(loginServiceImpl.initialSync(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM).contains(RegistrationConstants.FAILURE));
	}
	
	@Test
	public void initialSyncFalseTest() throws RegBaseCheckedException {
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.DISABLE);

		ApplicationContext.setApplicationMap(applicationMap);

		ResponseDTO responseDTO = new ResponseDTO();
		ErrorResponseDTO errorResponseDTO=new ErrorResponseDTO();
		
		List<ErrorResponseDTO> errorResponseDTOs=new LinkedList<>();
		errorResponseDTOs.add(errorResponseDTO);
		
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		
		Mockito.when(tpmPublicKeySyncService.syncTPMPublicKey()).thenReturn(responseDTO);

		Mockito.when(publicKeySyncImpl.getPublicKey(RegistrationConstants.JOB_TRIGGER_POINT_USER))
				.thenReturn(responseDTO);

		Mockito.when(globalParamService.synchConfigData(false)).thenReturn(responseDTO);

		Mockito.when(masterSyncService.getMasterSync(RegistrationConstants.OPT_TO_REG_MDS_J00001,
				RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(responseDTO);

		Mockito.when(userDetailService.save(RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(responseDTO);

		Assert.assertTrue(loginServiceImpl.initialSync(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM).contains(RegistrationConstants.FAILURE));
	}
	
	@Test
	public void validateUserTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		UserDetail userDetail = new UserDetail();
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("10011");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		userDetail.setStatusCode("ACTIVE");
		Set<UserMachineMapping> userMachineMappings = new HashSet<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setSerialNum("12345");
		userMachineMapping.setMachineMaster(machineMaster);
		UserMachineMappingID userMachineMappingId = new UserMachineMappingID();
		userMachineMapping.setUserMachineMappingId(userMachineMappingId);
		userMachineMapping.setIsActive(true);
		userMachineMappings.add(userMachineMapping);
		userDetail.setUserMachineMapping(userMachineMappings);
		Set<UserRole> userRoles = new HashSet<>();
		UserRole userRole = new UserRole();
		UserRoleId userRoleID = new UserRoleId();
		userRoleID.setRoleCode("REGISTRATION_OFFICER");
		userRoleID.setUsrId("mosip");
		userRole.setUserRoleId(userRoleID);
		userRole.setIsActive(true);
		userRoles.add(userRole);
		userDetail.setUserRole(userRoles);
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);

		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.getCenterId(Mockito.anyString())).thenReturn("10011");

		
		loginServiceImpl.getUserDetail("mosip");
		
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		map.put(RegistrationConstants.USER_STATION_ID,"10011");
		
		HashMap<String, Object> sessionMap = new HashMap<>();
		sessionMap.put(RegistrationConstants.USER_CENTER_ID, "11011");
		
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(sessionMap).when(ApplicationContext.class, "map");
		
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setMessage(RegistrationConstants.SUCCESS);
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		assertNotNull(loginServiceImpl.validateUser("mosip").getSuccessResponseDTO());
	}
	
	@Test
	public void validateUserFailureTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(null);
		
		List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setMessage(RegistrationConstants.USER_NAME_VALIDATION);
		errorResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		
		assertNotNull(loginServiceImpl.validateUser("mosip").getErrorResponseDTOs());
	}

	@Test
	public void validateUserFailure1Test() throws Exception {
		loginServiceImpl.validateUser("");
	}


	
	@Test
	public void validateUserStatusTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		UserDetail userDetail = new UserDetail();
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("10011");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		userDetail.setStatusCode("BLOCKED");
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);
		
		loginServiceImpl.getUserDetail("mosip");
		
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		
		HashMap<String, Object> sessionMap = new HashMap<>();
		sessionMap.put(RegistrationConstants.USER_CENTER_ID, "11011");
		
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(sessionMap).when(ApplicationContext.class, "map");
		
		List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setMessage(RegistrationConstants.BLOCKED_USER_ERROR);
		errorResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		assertNotNull(loginServiceImpl.validateUser("mosip").getErrorResponseDTOs());
	}
	
	@Test
	public void validateUserCenterTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		UserDetail userDetail = new UserDetail();
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("11234");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);
		
		loginServiceImpl.getUserDetail("mosip");
		
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		
		List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setMessage(RegistrationConstants.USER_MACHINE_VALIDATION_MSG);
		errorResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		assertNotNull(loginServiceImpl.validateUser("mosip").getErrorResponseDTOs());
	}
	
	@Test
	public void validateRoleTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		UserDetail userDetail = new UserDetail();
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("10011");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		userDetail.setStatusCode("ACTIVE");
		Set<UserMachineMapping> userMachineMappings = new HashSet<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setSerialNum("12345");
		userMachineMapping.setMachineMaster(machineMaster);
		UserMachineMappingID userMachineMappingId = new UserMachineMappingID();
		userMachineMapping.setUserMachineMappingId(userMachineMappingId);
		userMachineMapping.setIsActive(true);
		userMachineMappings.add(userMachineMapping);
		userDetail.setUserMachineMapping(userMachineMappings);
		Set<UserRole> userRoles = new HashSet<>();
		UserRole userRole = new UserRole();
		UserRoleId userRoleID = new UserRoleId();
		userRoleID.setRoleCode("OFFICER");
		userRoleID.setUsrId("mosip");
		userRole.setUserRoleId(userRoleID);
		userRole.setIsActive(true);
		userRoles.add(userRole);
		userDetail.setUserRole(userRoles);
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);
		
		loginServiceImpl.getUserDetail("mosip");
		
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		map.put(RegistrationConstants.USER_STATION_ID,"10011");
		
		HashMap<String, Object> sessionMap = new HashMap<>();
		sessionMap.put(RegistrationConstants.USER_CENTER_ID, "11011");
		
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(sessionMap).when(ApplicationContext.class, "map");
		
		List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setMessage(RegistrationConstants.ROLES_EMPTY_ERROR);
		errorResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		assertNotNull(loginServiceImpl.validateUser("mosip").getErrorResponseDTOs());
	}
	
	@Test
	public void validateRoleActiveTest() throws Exception {
		ResponseDTO responseDTO = new ResponseDTO();
		UserDetail userDetail = new UserDetail();
		RegCenterUser regCenterUser = new RegCenterUser();
		RegCenterUserId regCenterUserId = new RegCenterUserId();
		regCenterUserId.setRegCenterId("10011");
		regCenterUser.setRegCenterUserId(regCenterUserId);
		userDetail.setRegCenterUser(regCenterUser);
		userDetail.setStatusCode("ACTIVE");
		Set<UserMachineMapping> userMachineMappings = new HashSet<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setSerialNum("12345");
		userMachineMapping.setMachineMaster(machineMaster);
		UserMachineMappingID userMachineMappingId = new UserMachineMappingID();
		userMachineMapping.setUserMachineMappingId(userMachineMappingId);
		userMachineMapping.setIsActive(true);
		userMachineMappings.add(userMachineMapping);
		userDetail.setUserMachineMapping(userMachineMappings);
		Set<UserRole> userRoles = new HashSet<>();
		UserRole userRole = new UserRole();
		UserRoleId userRoleID = new UserRoleId();
		userRoleID.setRoleCode("OFFICER");
		userRoleID.setUsrId("mosip");
		userRole.setUserRoleId(userRoleID);
		userRole.setIsActive(false);
		userRoles.add(userRole);
		userDetail.setUserRole(userRoles);
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(userDetail);
		
		loginServiceImpl.getUserDetail("mosip");
		
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		map.put(RegistrationConstants.USER_STATION_ID,"10011");
		
		HashMap<String, Object> sessionMap = new HashMap<>();
		sessionMap.put(RegistrationConstants.USER_CENTER_ID, "11011");
		
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(sessionMap).when(ApplicationContext.class, "map");
		
		List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setMessage(RegistrationConstants.ROLES_EMPTY_ERROR);
		errorResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(errorResponseDTOs);
		assertNotNull(loginServiceImpl.validateUser("mosip").getErrorResponseDTOs());
	}
	
	@Test
	public void validateInvalidLoginTest() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		userDTO.setUnsuccessfulLoginCount(2);
		userDTO.setUserlockTillDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginMethod("PWD");		
		
		UserDetail userDetail = new UserDetail();
		
		Mockito.when(userDetailDAO.getUserDetail("mosip")).thenReturn(userDetail);
		
		Mockito.doNothing().when(userDetailDAO).updateLoginParams(userDetail);
		
		assertEquals("true", loginServiceImpl.validateInvalidLogin(userDTO, "", 1, -1));
	}
	
	@Test
	public void validateInvalidLoginTest2() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		userDTO.setUnsuccessfulLoginCount(3);
		userDTO.setUserlockTillDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginMethod("PWD");		
		
		UserDetail userDetail = new UserDetail();
		
		Mockito.when(userDetailDAO.getUserDetail("mosip")).thenReturn(userDetail);
		
		Mockito.doNothing().when(userDetailDAO).updateLoginParams(userDetail);
		
		assertEquals("ERROR", loginServiceImpl.validateInvalidLogin(userDTO, "sample", 1, 3));
	}
	
	@Test
	public void validateInvalidLoginTest3() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		userDTO.setUnsuccessfulLoginCount(1);
		userDTO.setUserlockTillDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDTO.setLastLoginMethod("PWD");		
		
		UserDetail userDetail = new UserDetail();
		
		Mockito.when(userDetailDAO.getUserDetail("mosip")).thenReturn(userDetail);
		
		Mockito.doNothing().when(userDetailDAO).updateLoginParams(userDetail);
		
		assertEquals("sample", loginServiceImpl.validateInvalidLogin(userDTO, "sample", 3, 3));
	}
	
	@Test
	public void validateInvalidLoginTest4() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		userDTO.setUnsuccessfulLoginCount(null);
		
		UserDetail userDetail = new UserDetail();
		
		Mockito.when(userDetailDAO.getUserDetail("mosip")).thenReturn(userDetail);
		
		Mockito.doNothing().when(userDetailDAO).updateLoginParams(userDetail);
		
		assertEquals("sample", loginServiceImpl.validateInvalidLogin(userDTO, "sample", 3, 3));
	}
	
	@Test
	public void validateInvalidLoginFailureTest() {
		loginServiceImpl.validateInvalidLogin(null, "sample", 3, 3);
	}
}
