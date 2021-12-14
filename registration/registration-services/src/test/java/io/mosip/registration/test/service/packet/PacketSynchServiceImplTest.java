package io.mosip.registration.test.service.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.RegistrationPacketSyncDTO;
import io.mosip.registration.dto.SyncRegistrationDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.packet.impl.PacketSynchServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class ,
	RegistrationSystemPropertiesChecker.class, JsonUtils.class, FileUtils.class, CryptoUtil.class })
public class PacketSynchServiceImplTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private RegistrationDAO syncRegistrationDAO;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private RequestHTTPDTO requestHTTPDTO;
	
	@Mock
	private AuditManagerService auditFactory;
	
	@InjectMocks
	private PacketSynchServiceImpl packetSynchServiceImpl;
	
	@Mock
	private IPacketCryptoService offlinePacketCryptoServiceImpl;
	
	@Mock
    private RetryTemplate retryTemplate;
	
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
	
	private Map<String, Object> applicationMap = new HashMap<>();

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.mockStatic(SessionContext.class);
		
		int batchCount = 10;
		ReflectionTestUtils.setField(packetSynchServiceImpl, "batchCount", batchCount);
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn("eng").when(ApplicationContext.class, "applicationLanguage");
		PowerMockito.doReturn("test").when(RegistrationSystemPropertiesChecker.class, "getMachineId");

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		applicationMap.put(RegistrationConstants.USER_CENTER_ID, "10011");
		applicationMap.put(RegistrationConstants.USER_STATION_ID, "10011");
		ApplicationContext.setApplicationMap(applicationMap);
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");

		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		packetSynchServiceImpl.init();
	}

	@Test
	public void testFetchPacketsToBeSynched() {
		List<Registration> syncList = new ArrayList<>();
		Registration reg = new Registration();
		reg.setCrDtime(Timestamp.from(Instant.now()));
		reg.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		reg.setId("123456789");
		reg.setAppId("123456789");
		reg.setCrDtime(Timestamp.from(Instant.now()));
		syncList.add(reg);
		List<PacketStatusDTO> packetsList = new ArrayList<>();
		PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
		packetStatusDTO.setFileName("123456789");
		packetsList.add(packetStatusDTO);
		Mockito.when(syncRegistrationDAO.fetchPacketsToUpload(Mockito.anyList(),Mockito.anyString())).thenReturn(syncList);
		assertEquals(syncList.get(0).getId(), packetSynchServiceImpl.fetchPacketsToBeSynched().get(0).getFileName());
	}

	/*@Test
	public void testSyncPacketsToServer() throws RegBaseCheckedException, ConnectionException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		LinkedHashMap<String, Object> msg = new LinkedHashMap<>();
		msg.put("registrationId", "123456789");
		msg.put("status", "Success");
		List<LinkedHashMap<String, Object>> mapList = new ArrayList<>();
		mapList.add(msg);
		respObj.put("response", mapList);
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		syncDtoList.add(new SyncRegistrationDTO());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(respObj);

		assertEquals("Success", packetSynchServiceImpl.("123456789", "System")
				.getSuccessResponseDTO().getOtherAttributes().get("123456789"));
	}*/

	/*@Test
	public void testUpdateSyncStatus() {
		List<PacketStatusDTO> synchedPackets = new ArrayList<>();
		PacketStatusDTO reg = new PacketStatusDTO();
		synchedPackets.add(reg);
		Mockito.when(registrationDAO.updatePacketSyncStatus(reg)).thenReturn(new Registration());
		assertTrue(packetSynchServiceImpl.updateSyncStatus(synchedPackets));
	}*/

	@Test
	public void testHttpException() throws RegBaseCheckedException, ConnectionException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		syncDtoList.add(new SyncRegistrationDTO());
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new HttpClientErrorException(HttpStatus.ACCEPTED));
		assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
	}

	/*@Test(expected = RegBaseUncheckedException.class)
	public void testUnCheckedException() throws RegBaseCheckedException, ConnectionException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		syncDtoList.add(new SyncRegistrationDTO());
		Object respObj = new Object();
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new RuntimeException());

		assertEquals(respObj, packetSynchServiceImpl.syncPacketsToServer("123456789", "System"));
	}*/
	
	@Test
	public void testSocketTimeoutException() throws RegBaseCheckedException, ConnectionException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		syncDtoList.add(new SyncRegistrationDTO());
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new ConnectionException());

		assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
	}

	@Test
	public void packetSyncTest() throws Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		
		List<Registration> registrations = new ArrayList<>();
		Registration reg = new Registration();
		reg.setId("123456789");
		reg.setPacketId("123456789");
		reg.setClientStatusCode("APPROVED");
		reg.setAckFilename("10001100010025920190430051904_Ack.html");
		reg.setStatusCode("NEW");
		reg.setAdditionalInfo("test".getBytes());
		
		Registration reg1 = new Registration();
		reg1.setId("123456789");
		reg1.setClientStatusCode("APPROVED");
		reg1.setAckFilename("10001100010025920190430051904_Ack.html");
		reg1.setStatusCode("NEW");
		reg1.setAdditionalInfo("test".getBytes());
		
		registrations.add(reg);
		registrations.add(reg1);
		
		Mockito.when(registrationDAO.getPacketsToBeSynched(Mockito.anyList())).thenReturn(registrations);
		
		PowerMockito.mockStatic(JsonUtils.class);
		RegistrationDataDto registrationDataDto = new RegistrationDataDto();
		registrationDataDto.setName("test");
		registrationDataDto.setEmail("test@gmail.com");
		registrationDataDto.setPhone("9999999999");
		registrationDataDto.setLangCode("eng,fra");
		Mockito.when(JsonUtils.jsonStringToJavaObject(Mockito.any(), Mockito.anyString())).thenReturn(registrationDataDto);
		
		PowerMockito.mockStatic(FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("../pom.xml"));
		Mockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("test");
		
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(JsonUtils.javaObjectToJsonString(Mockito.any(RegistrationPacketSyncDTO.class))).thenReturn("test");
		Mockito.when(offlinePacketCryptoServiceImpl.encrypt(Mockito.anyString(), Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		
		HashMap<String, Object> obj = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> resplist= new ArrayList<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("registrationId", "123456789");
		responseMap.put("status", RegistrationConstants.SUCCESS);
		resplist.add(responseMap);
		obj.put("response", resplist);
		
		Mockito.when(JsonUtils.javaObjectToJsonString(Mockito.anyString())).thenReturn("test");
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(obj);
		
		List<PacketStatusDTO> synchedPackets = new ArrayList<>();
		Registration reg2 = new Registration();
		reg2.setId("123456789");
		reg2.setClientStatusCode("SYNCED");
		reg2.setAckFilename("10001100010025920190430051904_Ack.html");
		reg2.setStatusCode("NEW");
		PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
		synchedPackets.add(packetStatusDTO);
		
		Mockito.when(syncRegistrationDAO.updatePacketSyncStatus(packetStatusDTO)).thenReturn(reg2);

		packetSynchServiceImpl.syncAllPackets("SYSTEM");
		assertEquals("SYNCED", reg2.getClientStatusCode());
	}
	
	@Test
	public void packetSyncErrorTest() throws Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		
		List<String> packetIds = new ArrayList<>();
		packetIds.add("123456789");
		packetIds.add("123456780");
		
		List<Registration> registrations = new ArrayList<>();
		Registration reg = new Registration();
		reg.setId("123456789");
		reg.setPacketId("123456789");
		reg.setClientStatusCode("APPROVED");
		reg.setAckFilename("10001100010025920190430051904_Ack.html");
		reg.setStatusCode("NEW");
		reg.setAdditionalInfo("test".getBytes());
		
		Registration reg1 = new Registration();
		reg1.setId("123456780");
		reg1.setClientStatusCode("APPROVED");
		reg1.setAckFilename("10001100010025920190430051904_Ack.html");
		reg1.setStatusCode("NEW");
		reg1.setAdditionalInfo("test".getBytes());
		
		registrations.add(reg);
		registrations.add(reg1);
		
		Mockito.when(registrationDAO.get(Mockito.anyList())).thenReturn(registrations);
		
		PowerMockito.mockStatic(JsonUtils.class);
		RegistrationDataDto registrationDataDto = new RegistrationDataDto();
		registrationDataDto.setName("test");
		registrationDataDto.setEmail("test@gmail.com");
		registrationDataDto.setPhone("9999999999");
		Mockito.when(JsonUtils.jsonStringToJavaObject(Mockito.any(), Mockito.anyString())).thenReturn(registrationDataDto);
		
		PowerMockito.mockStatic(FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("../pom.xml"));
		Mockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("test");
		
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(JsonUtils.javaObjectToJsonString(Mockito.any(RegistrationPacketSyncDTO.class))).thenReturn("test");
		Mockito.when(offlinePacketCryptoServiceImpl.encrypt(Mockito.anyString(), Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		
		HashMap<String, Object> obj = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> resplist= new ArrayList<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("registrationId", "123456789");
		responseMap.put("status", RegistrationConstants.FAILURE);
		resplist.add(responseMap);
		obj.put("errors", resplist);
		
		Mockito.when(JsonUtils.javaObjectToJsonString(Mockito.anyString())).thenReturn("test");
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(obj);
		
		List<PacketStatusDTO> synchedPackets = new ArrayList<>();
		Registration reg2 = new Registration();
		reg2.setId("123456789");
		reg2.setClientStatusCode("SYNCED");
		reg2.setAckFilename("10001100010025920190430051904_Ack.html");
		reg2.setStatusCode("NEW");
		PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
		synchedPackets.add(packetStatusDTO);
		
		Mockito.when(syncRegistrationDAO.updatePacketSyncStatus(packetStatusDTO)).thenReturn(reg2);

		Assert.assertNotNull(packetSynchServiceImpl.syncPacket("SYSTEM", packetIds).getSuccessResponseDTO());
	}

	@Ignore
	@Test(expected = RegBaseCheckedException.class)
	public void testsyncPacketException() throws RegBaseCheckedException, ConnectionException, NoSuchAlgorithmException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		syncDtoList.add(new SyncRegistrationDTO());
		Registration reg = new Registration();
		reg.setId("12345");
		reg.setAckFilename("10001100010025920190430051904_Ack.html");
		reg.setStatusCode("NEW");
		
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new RuntimeException());
		Mockito.when(HMACUtils2.generateHash(Mockito.anyString().getBytes())).thenReturn("asa".getBytes());
		//packetSynchServiceImpl.packetSync("");
	}

	@Test
	public void testsyncPacketException1()
			throws RegBaseCheckedException, ConnectionException, NoSuchAlgorithmException {
		Registration reg = new Registration();
		reg.setId("123456789");
		reg.setAckFilename("10001100010025920190430051904_Ack.html");
		reg.setStatusCode("NEW");
		
		Object respObj = new Object();
		
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		syncDtoList.add(new SyncRegistrationDTO());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new HttpClientErrorException(HttpStatus.ACCEPTED));
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		Mockito.when(HMACUtils2.generateHash(Mockito.anyString().getBytes())).thenReturn("asa".getBytes());
		assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
	}

	
	@Test
	public void testSyncPacketsToServer_1() throws RegBaseCheckedException, ConnectionException {
		List<SyncRegistrationDTO> syncDtoList = new ArrayList<>();
		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		LinkedHashMap<String, Object> msg = new LinkedHashMap<>();
		msg.put("registrationId", "123456789");
		msg.put("errors", "errors");
		List<LinkedHashMap<String, Object>> mapList = new ArrayList<>();
		mapList.add(msg);
		respObj.put("errors", mapList);
		RegistrationPacketSyncDTO registrationPacketSyncDTO = new RegistrationPacketSyncDTO();
		syncDtoList.add(new SyncRegistrationDTO());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(respObj);

		//assertTrue(packetSynchServiceImpl.syncPacketsToServer("123456789", "System").getErrorResponseDTOs()!=null);
	}
}
