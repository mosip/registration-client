package io.mosip.registration.test.packetStatusSync;

import static io.mosip.kernel.core.util.JsonUtils.javaObjectToJsonString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.entity.CenterMachine;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.id.CenterMachineId;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.junit.AfterClass;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegPacketStatusDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.RegistrationPacketSyncDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.packet.impl.RegPacketStatusServiceImpl;
import io.mosip.registration.service.sync.PacketSynchService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class ,
		RegistrationSystemPropertiesChecker.class })
public class RegPacketStatusServiceTest {
	private Map<String, Object> applicationMap = new HashMap<>();

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	@Mock
	private RegPacketStatusDAO packetStatusDao;
	@Mock
	private PacketSynchService packetSynchService;

	@InjectMocks
	private RegPacketStatusServiceImpl packetStatusService;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	RegistrationDAO registrationDAO;

	@Mock
	private BaseService baseService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private CenterMachineRepository centerMachineRepository;

	@Before
	public void initiate() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn("eng").when(ApplicationContext.class, "applicationLanguage");
		PowerMockito.doReturn("test").when(RegistrationSystemPropertiesChecker.class, "getMachineId");
		when(baseService.getGlobalConfigValueOf(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS)).thenReturn("5");

		PowerMockito.mockStatic(HMACUtils2.class, RegistrationAppHealthCheckUtil.class);
		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		ApplicationContext.setApplicationMap(applicationMap);
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");

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
	}

	@AfterClass
	public static void destroy() {
		SessionContext.destroySession();
	}

	@Test
	public void packetSyncStatusSuccessTest() throws RegBaseCheckedException, ConnectionException {
		List<LinkedHashMap<String, String>> registrations = new ArrayList<>();
		LinkedHashMap<String, String> registration = new LinkedHashMap<>();
		registration.put("registrationId", "12345");
		registration.put("statusCode", RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		registrations.add(registration);

		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put(RegistrationConstants.RESPONSE, registrations);

		LinkedHashMap<String, String> registration12 = new LinkedHashMap<>();

		registration12.put("registrationId", "12345");
		registration12.put("statusCode", RegistrationConstants.PACKET_STATUS_CODE_PROCESSED + "123");
		registrations.add(registration12);

		List<Registration> list = new LinkedList<>();
		Registration regis = new Registration();
		regis.setId("12345");
		regis.setAckFilename("..//PacketStore/02-Jan-2019/2018782130000102012019115112_Ack.png");
		regis.setClientStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		list.add(regis);

		when(packetStatusDao.getPacketIdsByStatusUploaded()).thenReturn(list);

		when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(response);
		Assert.assertNotNull(packetStatusService.syncServerPacketStatus("System").getSuccessResponseDTO());

		when(packetStatusDao.update(Mockito.any())).thenThrow(RuntimeException.class);
		packetStatusService.syncServerPacketStatus("System");

	}

	@Test
	public void packetSyncStatusSuccessTestWithEmptyPackets()
			throws RegBaseCheckedException, ConnectionException{
		List<LinkedHashMap<String, String>> registrations = new ArrayList<>();
		LinkedHashMap<String, String> registration = new LinkedHashMap<>();
		registration.put("registrationId", "12345");
		registration.put("statusCode", RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		registrations.add(registration);

		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put(RegistrationConstants.RESPONSE, registrations);

		LinkedHashMap<String, String> registration12 = new LinkedHashMap<>();

		registration12.put("registrationId", "12345");
		registration12.put("statusCode", RegistrationConstants.PACKET_STATUS_CODE_PROCESSED + "123");
		registrations.add(registration12);

		List<Registration> list = new LinkedList<>();
		when(packetStatusDao.getPacketIdsByStatusUploaded()).thenReturn(list);

		when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString())).thenReturn(response);
		Assert.assertNotNull(packetStatusService.syncServerPacketStatus("System").getSuccessResponseDTO());

		when(packetStatusDao.update(Mockito.any())).thenThrow(RuntimeException.class);
		packetStatusService.syncServerPacketStatus("System");
	}

	@Test
	public void packetSyncStatusExceptionTest()
			throws RegBaseCheckedException, ConnectionException {

		List<Registration> list = new LinkedList<>();
		Registration regis = new Registration();
		regis.setId("12345");
		regis.setAckFilename("..//PacketStore/02-Jan-2019/2018782130000102012019115112_Ack.png");
		regis.setClientStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		list.add(regis);
		when(packetStatusDao.getPacketIdsByStatusUploaded()).thenReturn(list);

		when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString()))
				.thenThrow(ConnectionException.class);
		Assert.assertNotNull(packetStatusService.syncServerPacketStatus("System").getErrorResponseDTOs());

		packetStatusService.syncServerPacketStatus("System");
	}

	@Test
	public void packetSyncStatusRuntimeExceptionTest()
			throws RegBaseCheckedException, ConnectionException {

		List<Registration> list = new LinkedList<>();
		Registration regis = new Registration();
		regis.setId("12345");
		regis.setAckFilename("..//PacketStore/02-Jan-2019/2018782130000102012019115112_Ack.png");
		regis.setClientStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		list.add(regis);
		when(packetStatusDao.getPacketIdsByStatusUploaded()).thenReturn(list);

		when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString()))
				.thenThrow(RuntimeException.class);
		Assert.assertNotNull(packetStatusService.syncServerPacketStatus("System").getErrorResponseDTOs());

		packetStatusService.syncServerPacketStatus("System");
	}

	@Test
	public void packetSyncStatusFailureTest()
			throws RegBaseCheckedException, ConnectionException {
		List<Registration> list = new LinkedList<>();
		Registration regis = new Registration();
		regis.setId("12345");
		regis.setAckFilename("..//PacketStore/02-Jan-2019/2018782130000102012019115112_Ack.png");
		regis.setClientStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		list.add(regis);

		when(packetStatusDao.getPacketIdsByStatusUploaded()).thenReturn(list);

		List<LinkedHashMap<String, String>> registrations = new ArrayList<>();

		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put(RegistrationConstants.RESPONSE, registrations);

		when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString())).thenReturn(response);
		Assert.assertNotNull(packetStatusService.syncServerPacketStatus("System").getErrorResponseDTOs());

		when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenThrow(HttpClientErrorException.class);
		packetStatusService.syncServerPacketStatus("System");
	}

	@Test
	public void deleteReRegistrationPacketsTest() {
		List<Registration> list = prepareSamplePackets();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setMessage(RegistrationConstants.REGISTRATION_DELETION_BATCH_JOBS_SUCCESS);

		when(registrationDAO.get(Mockito.any(), Mockito.anyString())).thenReturn(list);

		Mockito.doNothing().when(packetStatusDao).delete(Mockito.any());

		ResponseDTO responseDTO = packetStatusService.deleteRegistrationPackets();
		assertNotNull(responseDTO);
		assertNotNull(responseDTO.getSuccessResponseDTO().getMessage());
	}

	protected List<Registration> prepareSamplePackets() {
		List<Registration> list = new LinkedList<>();
		Registration regis = new Registration();
		regis.setId("12345");
		regis.setAckFilename("..//PacketStore/02-Jan-2019/2018782130000102012019115112_Ack.png");
		regis.setClientStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		regis.setStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);
		regis.setServerStatusCode(RegistrationConstants.PACKET_STATUS_CODE_PROCESSED);

		list.add(regis);
		return list;
	}

	@Test
	public void deleteReRegistrationPacketsFailureTest() {
		List<Registration> list = prepareSamplePackets();

		when(registrationDAO.get(Mockito.any(), Mockito.anyString())).thenThrow(RuntimeException.class);

		assertSame(RegistrationConstants.REGISTRATION_DELETION_BATCH_JOBS_FAILURE,
				packetStatusService.deleteRegistrationPackets().getErrorResponseDTOs().get(0).getMessage());
	}


	@Test
	public void deleteAllProcessedRegPacketsTest() {
		List<Registration> list = prepareSamplePackets();
		Mockito.when(
				registrationDAO.findByServerStatusCodeIn(RegistrationConstants.PACKET_STATUS_CODES_FOR_REMAPDELETE))
				.thenReturn(list);
		packetStatusService.deleteAllProcessedRegPackets();

	}
}
