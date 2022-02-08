package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.impl.UserMachineMappingServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class, RegistrationSystemPropertiesChecker.class })
public class UserMachineMappingServiceTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private UserMachineMappingServiceImpl userMachineMappingServiceImpl;
	
	@Mock
	private MachineMappingDAO machineMappingDAO;
	
	@Mock
	private UserMachineMappingRepository userMachineMappingRepository;
	
	@Mock
	private BaseService baseService;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
	private RegistrationCenterRepository registrationCenterRepository;
	
	@Mock
	private MachineMasterRepository machineMasterRepository;
	
	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationSystemPropertiesChecker.class);
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");		
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");

		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		machine.setRegCenterId("10011");
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
		
		RegistrationCenter registrationCenter = new RegistrationCenter();
		registrationCenter.setRegistartionCenterId(new RegistartionCenterId());
		registrationCenter.getRegistartionCenterId().setId("10011");
		registrationCenter.getRegistartionCenterId().setLangCode("eng");
		registrationCenter.setIsActive(true);
		Optional<RegistrationCenter> mockedCenter = Optional.of(registrationCenter);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(mockedCenter);
	}

	@Test
	public void syncUserDetailsTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		List<UserMachineMapping> list = new ArrayList<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		UserDetail userDetail = new UserDetail();
		userDetail.setId("id");
		userDetail.setIsActive(true);
		userMachineMapping.setUserDetail(userDetail);
		list.add(userMachineMapping);
		Mockito.when(machineMappingDAO.getUserMappingDetails(Mockito.anyString())).thenReturn(list);
		//new code
		Map<String,Object> myMap=new HashMap<>();
		myMap.put(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.ENABLE);
		Map<String, String> map = new HashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, String> masterSyncMap = new LinkedHashMap<>();
		masterSyncMap.put("lastSyncTime", "2019-03-27T11:07:34.408Z");
		responseMap.put("response", masterSyncMap);
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
				
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(responseMap);
		
        ResponseDTO responseDTO=new ResponseDTO();
        SuccessResponseDTO sucessResponse=new SuccessResponseDTO();
        sucessResponse.setCode(RegistrationConstants.MASTER_SYNC_SUCESS_MSG_CODE);
		sucessResponse.setInfoType(RegistrationConstants.ALERT_INFORMATION);
		sucessResponse.setMessage(RegistrationConstants.MASTER_SYNC_SUCCESS);

		responseDTO.setSuccessResponseDTO(sucessResponse);

		assertNotNull(userMachineMappingServiceImpl.syncUserDetails());
	}

	@Test
	public void syncUserDetailsOffLineTest() {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		assertNotNull(userMachineMappingServiceImpl.syncUserDetails());

	}
	
	@Test
	public void isUserNewToMachineSuccessTest() {		
		Mockito.when(machineMappingDAO.isExists(RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(true);		
		assertNotNull(userMachineMappingServiceImpl.isUserNewToMachine(RegistrationConstants.JOB_TRIGGER_POINT_USER).getSuccessResponseDTO());
	}
	
	@Test
	public void isUserNewToMachineFailureTest() {
		Mockito.when(machineMappingDAO.isExists(RegistrationConstants.JOB_TRIGGER_POINT_USER)).thenReturn(false);		
		assertNotNull(userMachineMappingServiceImpl.isUserNewToMachine(RegistrationConstants.JOB_TRIGGER_POINT_USER).getErrorResponseDTOs());
	}
	@Test
	public void syncfailureTest() throws RegBaseCheckedException, ConnectionException {
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new LinkedHashMap<>();
		userDetailsMap.put("errorCode", "KER-SNC-303");
		userDetailsMap.put("message", "Registration center user not found ");
		List<Map<String, Object>> userFailureList=new ArrayList<>();
		userFailureList.add(userDetailsMap);
		responseMap.put("errors", userFailureList);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		List<UserMachineMapping> list = new ArrayList<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		UserDetail userDetail = new UserDetail();
		userDetail.setId("id");
		userDetail.setIsActive(true);
		userMachineMapping.setUserDetail(userDetail);
		list.add(userMachineMapping);
		Mockito.when(machineMappingDAO.getUserMappingDetails(Mockito.anyString())).thenReturn(list);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(responseMap);
		assertNotNull(userMachineMappingServiceImpl.syncUserDetails());
	}
	
	@Test
	public void syncExceptionTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		List<UserMachineMapping> list = new ArrayList<>();
		UserMachineMapping userMachineMapping = new UserMachineMapping();
		UserDetail userDetail = new UserDetail();
		userDetail.setId("id");
		userDetail.setIsActive(true);
		userMachineMapping.setUserDetail(userDetail);
		list.add(userMachineMapping);
		Mockito.when(machineMappingDAO.getUserMappingDetails(Mockito.anyString())).thenReturn(list);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenThrow(ConnectionException.class);
		assertNotNull(userMachineMappingServiceImpl.syncUserDetails().getErrorResponseDTOs());
	}

}
