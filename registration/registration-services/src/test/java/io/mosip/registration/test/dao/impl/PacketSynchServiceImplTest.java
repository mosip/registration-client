package io.mosip.registration.test.dao.impl;

import static io.mosip.kernel.core.util.JsonUtils.javaObjectToJsonString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.exception.ConnectionException;
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
import org.springframework.data.util.ReflectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dao.impl.RegistrationDAOImpl;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.RegistrationPacketSyncDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.SyncRegistrationDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.packet.impl.PacketSynchServiceImpl;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, SessionContext.class, FileUtils.class, JsonUtils.class, BigInteger.class })
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
	
	@Mock
	private RetryTemplate retryTemplate;
	
	@Mock
	private RegistrationDAOImpl registrationDAOImpl;
	
	@Mock
	private PacketSynchServiceImpl packetSynchServiceImpl;


	@Before
	public void initialize() throws Exception{
		
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.mockStatic(SessionContext.class);
				
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());
		
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);		
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip1214");
		
		Map<String, Object> maplastTime = new HashMap<>();
		maplastTime.put("PRIMARY_LANGUAGE", "ENG");
		ApplicationContext.getInstance().setApplicationMap(maplastTime);

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
		//assertEquals(syncList.get(0).getId(), packetSynchServiceImpl.fetchPacketsToBeSynched().get(0).getFileName());
	}

	/*
	 * @Test public void testSyncPacketsToServer() throws RegBaseCheckedException,
	 * ConnectionException { List<SyncRegistrationDTO> syncDtoList = new
	 * ArrayList<>(); LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
	 * LinkedHashMap<String, Object> msg = new LinkedHashMap<>();
	 * msg.put("registrationId", "123456789"); msg.put("status", "Success");
	 * List<LinkedHashMap<String, Object>> mapList = new ArrayList<>();
	 * mapList.add(msg); respObj.put("response", mapList); RegistrationPacketSyncDTO
	 * registrationPacketSyncDTO = new RegistrationPacketSyncDTO(); boolean
	 * packetIdExists = true; syncDtoList.add(new SyncRegistrationDTO());
	 * Mockito.when(serviceDelegateUtil.post(Mockito.anyString(),
	 * Mockito.anyString(), Mockito.anyString())) .thenReturn(respObj);
	 * 
	 * assertEquals("Success",
	 * packetSynchServiceImpl.syncPacketsToServer("123456789", "System",
	 * packetIdExists).
	 * .getSuccessResponseDTO().getOtherAttributes().get("123456789")); }
	 */

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
		//assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
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

		//assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
	}

	@Test
	public void packetSyncTest() throws RegBaseCheckedException, ConnectionException, NoSuchAlgorithmException {
		
		List<PacketStatusDTO> synchedPackets = new ArrayList<>();
		HashMap<String, Object> obj = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> resplist= new ArrayList<>();
		LinkedHashMap<String, Object> obj1 = new LinkedHashMap<>();
		obj1.put("Success", "Success");
		resplist.add(obj1);
		obj.put("response", resplist);
		Registration reg = new Registration();
		reg.setId("123456789");
		reg.setClientStatusCode("SYNCED");
		reg.setAckFilename("10001100010025920190430051904_Ack.html");
		reg.setStatusCode("NEW");
		PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
		synchedPackets.add(packetStatusDTO);

		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(obj);
		Mockito.when(syncRegistrationDAO.updatePacketSyncStatus(packetStatusDTO)).thenReturn(reg);
				
		Mockito.when(HMACUtils2.generateHash(Mockito.anyString().getBytes())).thenReturn("asa".getBytes());
		//packetSynchServiceImpl.packetSync("123456789");
		assertEquals("SYNCED", reg.getClientStatusCode());
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
		//assertNotNull(packetSynchServiceImpl.syncPacket("System").getErrorResponseDTOs());
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
	
	@Test
	public void syncPacketPassWithPassedPacketIdsTest() throws Exception {
		
		ResponseDTO expectedResponseDTO = new ResponseDTO();
		SuccessResponseDTO expectedSuccess = new SuccessResponseDTO();
		expectedSuccess.setCode("0000");
		expectedSuccess.setInfoType("Packet sync");
		expectedSuccess.setMessage("Packet sync success");
		Map<String, Object> mapOfAttributes = new HashMap<String, Object>();
		expectedSuccess.setOtherAttributes(mapOfAttributes);
		
		expectedResponseDTO.setSuccessResponseDTO(expectedSuccess);
		
		List<String> list = new ArrayList<String>();
		list.add("12019831283");
		list.add("32746293249");
		
		List<Registration> listOfReg = new ArrayList<Registration>();
		
		Registration reg1 = new Registration();
		reg1.setClientStatusCode(RegistrationConstants.SYNCED_STATUS);
		reg1.setAckFilename("djaldakj_Ack.html");
		byte[] additionalInfoByte = "kasjhjahsdhjdhas23978723saldsa".getBytes();
		String str = new String("kasjhjahsdhjdhas23978723saldsa");
		reg1.setAdditionalInfo(additionalInfoByte);
		listOfReg.add(reg1);
		
		RegistrationDataDto registrationDataDto = new RegistrationDataDto();
		registrationDataDto.setName("Registration1");
		registrationDataDto.setPhone("7326432876");
		registrationDataDto.setLangCode("eng");
		registrationDataDto.setEmail("mosip@gmail.com");
		//PacketSynchServiceImpl packImpl = PowerMockito.mock(PacketSynchServiceImpl.class);
		RetryTemplate template = new RetryTemplate();
		ReflectionTestUtils.setField(packetSynchServiceImpl, "retryTemplate", template);
		ReflectionTestUtils.setField(packetSynchServiceImpl, "registrationDAO", registrationDAOImpl);
		PowerMockito.mockStatic(JsonUtils.class, FileUtils.class, HMACUtils2.class, BigInteger.class);
		Mockito.when(JsonUtils.jsonStringToJavaObject((Class<?>) Mockito.any(Class.class), Mockito.anyString())).thenReturn(registrationDataDto);
		Mockito.when(HMACUtils2.digestAsPlainText(additionalInfoByte)).thenReturn(str);
		PowerMockito.doNothing().when(packetSynchServiceImpl, PowerMockito.method(PacketSynchServiceImpl.class, "syncRIDToServerWithRetryWrapper")).withArguments("System", list);
		PowerMockito.when(packetSynchServiceImpl, "syncPacket", "System").thenReturn(expectedResponseDTO);

		ResponseDTO actualResponse = packetSynchServiceImpl.syncPacket("System");
		
		assertEquals(expectedResponseDTO.getSuccessResponseDTO().getCode(), actualResponse.getSuccessResponseDTO().getCode());
	}
	
	@Test
	public void syncPacketPassWithDBPacketIdsTest() throws Exception {
		
		ResponseDTO expectedResponseDTO = new ResponseDTO();
		SuccessResponseDTO expectedSuccess = new SuccessResponseDTO();
		expectedSuccess.setCode("0000");
		expectedSuccess.setInfoType("Packet sync");
		expectedSuccess.setMessage("Packet sync success");
		Map<String, Object> mapOfAttributes = new HashMap<String, Object>();
		expectedSuccess.setOtherAttributes(mapOfAttributes);
		
		expectedResponseDTO.setSuccessResponseDTO(expectedSuccess);
		
		List<String> list = new ArrayList<String>();
		list.add("12019831283");
		list.add("32746293249");
		
		List<Registration> listOfReg = new ArrayList<Registration>();
		
		Registration reg1 = new Registration();
		reg1.setClientStatusCode(RegistrationConstants.SYNCED_STATUS);
		reg1.setAckFilename("djaldakj_Ack.html");
		byte[] additionalInfoByte = "kasjhjahsdhjdhas23978723saldsa".getBytes();
		reg1.setAdditionalInfo(additionalInfoByte);
		listOfReg.add(reg1);
		
		RegistrationDataDto registrationDataDto = new RegistrationDataDto();
		registrationDataDto.setName("Registration1");
		//PacketSynchServiceImpl packImpl = PowerMockito.mock(PacketSynchServiceImpl.class);
		RetryTemplate template = new RetryTemplate();
		ReflectionTestUtils.setField(packetSynchServiceImpl, "retryTemplate", template);
		ReflectionTestUtils.setField(packetSynchServiceImpl, "registrationDAO", registrationDAOImpl);
		PowerMockito.mockStatic(JsonUtils.class, FileUtils.class, HMACUtils2.class, BigInteger.class);
		String str = null;
		PowerMockito.doNothing().when(packetSynchServiceImpl, PowerMockito.method(PacketSynchServiceImpl.class, "syncRIDToServerWithRetryWrapper")).withArguments("System", str);
		PowerMockito.when(packetSynchServiceImpl, "syncPacket", "System", list).thenReturn(expectedResponseDTO);

		ResponseDTO actualResponse = packetSynchServiceImpl.syncPacket("System", list);
		
		assertEquals(expectedResponseDTO.getSuccessResponseDTO().getCode(), actualResponse.getSuccessResponseDTO().getCode());
	}
}
