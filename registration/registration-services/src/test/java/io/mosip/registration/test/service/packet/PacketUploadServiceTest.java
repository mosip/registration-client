package io.mosip.registration.test.service.packet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.packet.impl.PacketUploadServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, RegistrationAppHealthCheckUtil.class, RegistrationSystemPropertiesChecker.class, ApplicationContext.class, SessionContext.class, FileUtils.class })
public class PacketUploadServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private RegistrationDAO registrationDAO;

	@Mock
	private RequestHTTPDTO requestHTTPDTO;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private Environment environment;

	@Mock
	private RegistrationRepository registrationRepository;

	@InjectMocks
	private PacketUploadServiceImpl packetUploadServiceImpl;

	@Mock
    private RetryTemplate retryTemplate;
	
	@Mock
	private AuditManagerService auditFactory;
	
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
		int batchCount = 10;
		ReflectionTestUtils.setField(packetUploadServiceImpl, "batchCount", batchCount);
		
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.mockStatic(SessionContext.class);
				
		doNothing().when(auditFactory).audit(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString());
		
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn("eng").when(ApplicationContext.class, "applicationLanguage");
		PowerMockito.doReturn("test").when(RegistrationSystemPropertiesChecker.class, "getMachineId");

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		applicationMap.put(RegistrationConstants.USER_CENTER_ID, "10011");
		applicationMap.put(RegistrationConstants.USER_STATION_ID, "10011");
		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		ApplicationContext.setApplicationMap(applicationMap);
		Mockito.when(ApplicationContext.map()).thenReturn(applicationMap);
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");

		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);		
		packetUploadServiceImpl.init();
	}

	@Test(expected = RegBaseCheckedException.class)
	public void testPushPacket() throws RegBaseCheckedException, ConnectionException, URISyntaxException {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		respObj.put("response", "Success");
		respObj.put("error", null);
		File f = new File("");
		map.add("file", new FileSystemResource(f));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		Mockito.when(environment.getProperty(Mockito.anyString())).thenReturn(
				"http://104.211.209.102:8080/v0.1/registration-processor/packet-receiver/registrationpackets");
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		requestHTTPDTO.setHttpEntity(requestEntity);
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setUri(
				new URI("http://104.211.209.102:8080/v0.1/registration-processor/packet-receiver/registrationpackets"));
		requestHTTPDTO.setHttpMethod(HttpMethod.POST);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);

		assertEquals(RegistrationClientStatusCode.UPLOADED_SUCCESSFULLY.getCode(),
				packetUploadServiceImpl.uploadPacket("test").getUploadStatus());
	}

	@Test
	public void testPushPacketNegativeCase() throws URISyntaxException, RegBaseCheckedException, ConnectionException {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		LinkedHashMap<String, Object> respObj1 = new LinkedHashMap<>();
		LinkedHashMap<String, String> msg = new LinkedHashMap<>();
		List<LinkedHashMap<String, String>> errList = new ArrayList<>();
		msg.put("message", "error");
		errList.add(msg);
		respObj1.put("response", null);
		respObj1.put("errors", errList);
		File f = new File("");
		map.add("file", new FileSystemResource(f));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		Mockito.when(environment.getProperty(Mockito.anyString())).thenReturn(
				"http://104.211.209.102:8080/v0.1/registration-processor/packet-receiver/registrationpackets");
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
		requestHTTPDTO.setHttpEntity(requestEntity);
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setUri(
				new URI("http://104.211.209.102:8080/v0.1/registration-processor/packet-receiver/registrationpackets"));
		requestHTTPDTO.setHttpMethod(HttpMethod.POST);
		
		Registration registration = new Registration();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("APPROVED");
		registration.setFileUploadStatus("S");
		registration.setCrDtime(Timestamp.from(Instant.now()));
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj1);

		Assert.assertNotNull(packetUploadServiceImpl.uploadPacket("test").getUploadStatus());
	}

	/*@Test
	public void testUpdateStatus() {
		List<PacketStatusDTO> packetList = new ArrayList<>();
		Registration registration = new Registration();
		PacketStatusDTO dto= new PacketStatusDTO();
		packetList.add(dto);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.anyObject())).thenReturn(registration);
		assertTrue(packetUploadServiceImpl.updateStatus(packetList));
	}*/

	@Test(expected = RegBaseCheckedException.class)
	public void testHttpException() throws RegBaseCheckedException, ConnectionException {
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString()))
				.thenThrow(new HttpClientErrorException(HttpStatus.ACCEPTED));
		
		assertEquals(respObj, packetUploadServiceImpl.uploadPacket("test"));
	}
	
	/*@Test(expected = RegBaseCheckedException.class)
	public void testSocketTimeoutExceptionException() throws  RegBaseCheckedException, ConnectionException {
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString()))
				.thenThrow(new SocketTimeoutException());
		assertEquals(respObj, packetUploadServiceImpl.pushPacket(f));
	}*/

	@Test(expected = RegBaseCheckedException.class)
	public void testRuntimeException() throws  RegBaseCheckedException, ConnectionException {
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenThrow(new RuntimeException());
		assertEquals(respObj, packetUploadServiceImpl.uploadPacket("test"));
	}

	@Test
	public void testUploadPacket() throws Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("SYNCED");
		registration.setFileUploadStatus("S");
		registration.setCrDtime(Timestamp.from(Instant.now()));
		regList.add(registration);

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class, FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("../pom.xml"));
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("status", "PUSHED");
		respObj.put("response", responseMap);
		respObj.put("error", null);
		//respObj = "PACKET_UPLOADED_TO_VIRUS_SCAN";
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("123456789");
		registration1.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration1.setUploadCount((short) 1);
		registration1.setCrDtime(Timestamp.from(Instant.now()));
		registration1.setClientStatusCode("PUSHED");
		packetList.add(registration);
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.any())).thenReturn(registration1);
		assertEquals("PUSHED", packetUploadServiceImpl.uploadPacket("123456789").getPacketClientStatus());
	}

	@Test
	public void testUploadAllSyncedPackets() throws Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("SYNCED");
		registration.setFileUploadStatus("S");
		registration.setCrDtime(Timestamp.from(Instant.now()));
		registration.setUpdDtimes(Timestamp.from(Instant.now()));
		
		when(registrationRepository.findTopByOrderByUpdDtimesDesc()).thenReturn(registration);
		
		Registration reg1 = new Registration();
		reg1.setId("909090");
		reg1.setServerStatusCode(RegistrationConstants.PACKET_STATUS_CODE_REREGISTER);
		
		regList.add(registration);
		regList.add(reg1);
		
		Slice<Registration> slice = getSlice(regList);
		Mockito.when(registrationRepository.findByClientStatusCodeOrServerStatusCodeOrFileUploadStatusAndUpdDtimesLessThanEqual(Mockito.anyString(), Mockito.anyString(), 
				Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(slice);
		
		Mockito.when(registrationDAO.getRegistrationByStatus(Mockito.anyList())).thenReturn(regList);

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class, FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(new File("../pom.xml"));
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> respList = new ArrayList<>();
		responseMap.put("errorCode", "RPR-UPD-001");
		responseMap.put("message", "duplicate packet");
		respList.add(responseMap);
		respObj.put(RegistrationConstants.ERRORS, respList);
		
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		
		Registration registration1 = new Registration();
		registration1.setId("123456789");
		registration1.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration1.setUploadCount((short) 1);
		registration1.setCrDtime(Timestamp.from(Instant.now()));
		registration1.setClientStatusCode("PUSHED");
		registration1.setFileUploadStatus("E");
		
		Mockito.when(registrationDAO.updateRegStatus(Mockito.any())).thenReturn(registration1);
		
		Assert.assertNotNull(packetUploadServiceImpl.uploadAllSyncedPackets().getErrorResponseDTOs());
	}
	
	@Test
	public void testUploadPacket1() throws Exception {
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setFileUploadStatus("E");
		registration.setCrDtime(Timestamp.from(Instant.now()));
		regList.add(registration);
		
		Object respObj = new Object();
		respObj = "PACKET_FAILED_TO_UPLOAD";

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		packetList.add(registration);
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.any())).thenReturn(registration1);
		packetUploadServiceImpl.uploadPacket("123456789");
		assertEquals("E", registration.getFileUploadStatus());
	}

	@Test
	public void testPacketNotExists() throws RegBaseCheckedException, ConnectionException {
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setClientStatusCode("APPROVED");
		registration.setAckFilename("..//registration-services/src/test/resources/1234567895_Ack.png");
		registration.setUploadCount((short) 0);
		regList.add(registration);
		registration.setCrDtime(Timestamp.from(Instant.now()));

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		assertEquals(null, packetUploadServiceImpl.uploadPacket("123456789").getUploadStatus());

	}

	@Test(expected = RegBaseCheckedException.class)
	public void testRuntimeException1() throws RegBaseCheckedException, ConnectionException {
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		regList.add(registration);
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenThrow(new RuntimeException());
		packetUploadServiceImpl.uploadPacket("12345");
		//assertEquals(respObj, packetUploadServiceImpl.up(f));
		assertEquals("E", registration.getFileUploadStatus());

	}

	@Test(expected = RegBaseCheckedException.class)
	public void testRuntimeException2() throws RegBaseCheckedException, ConnectionException {
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		regList.add(registration);
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString()))
				.thenThrow(new HttpClientErrorException(HttpStatus.ACCEPTED));
		packetUploadServiceImpl.uploadPacket("12345");
		//assertEquals(respObj, packetUploadServiceImpl.pushPacket(f));
		assertEquals("E", registration.getFileUploadStatus());
	}

	/*@Test
	public void testuploadEODPackets()
			throws HttpClientErrorException, ResourceAccessException, SocketTimeoutException, RegBaseCheckedException {

		List<String> regIds = new ArrayList<>();
		regIds.add("123456789");

		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("PUSHED");
		registration.setFileUploadStatus("S");
		regList.add(registration);
		
		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		respObj.put("response", "Success");
		respObj.put("error", null);
		//respObj = "PACKET_UPLOADED_TO_VIRUS_SCAN";
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		Mockito.when(registrationDAO.get(Mockito.anyList())).thenReturn(regList);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		packetList.add(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.anyObject())).thenReturn(registration1);
		packetUploadServiceImpl.uploadEODPackets(regIds);
		assertEquals("PUSHED", registration.getClientStatusCode());
		assertEquals("S", registration.getFileUploadStatus());

	}*/
	
	/*@Test
	public void testuploadEODPackets1()
			throws HttpClientErrorException, ResourceAccessException, SocketTimeoutException, RegBaseCheckedException {

		List<String> regIds = new ArrayList<>();
		regIds.add("123456789");

		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("PUSHED");
		registration.setFileUploadStatus("S");
		regList.add(registration);

		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		LinkedHashMap<String, String> msg = new LinkedHashMap<>();
		msg.put("message", "duplicate");
		respObj.put("response", null);
		respObj.put("error", msg);
		//respObj = "PACKET_UPLOADED_TO_VIRUS_SCAN";
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		Mockito.when(registrationDAO.get(Mockito.anyList())).thenReturn(regList);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		packetList.add(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.anyObject())).thenReturn(registration1);
		packetUploadServiceImpl.uploadEODPackets(regIds);
		assertEquals("PUSHED", registration.getClientStatusCode());
		assertEquals("S", registration.getFileUploadStatus());

	}
	
	@Test
	public void testuploadEODPackets2()
			throws HttpClientErrorException, ResourceAccessException, SocketTimeoutException, RegBaseCheckedException {

		List<String> regIds = new ArrayList<>();
		regIds.add("123456789");

		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setFileUploadStatus("E");
		regList.add(registration);

		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		respObj.put("response", null);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		Mockito.when(registrationDAO.get(Mockito.anyList())).thenReturn(regList);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		packetList.add(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.anyObject())).thenReturn(registration1);
		packetUploadServiceImpl.uploadEODPackets(regIds);
		assertEquals("E", registration.getFileUploadStatus());

	}

	@Test(expected = RegBaseCheckedException.class)
	public void testHttpServerException() throws URISyntaxException, RegBaseCheckedException, HttpClientErrorException,
			HttpServerErrorException, ResourceAccessException, SocketTimeoutException {
		File f = new File("");
		Object respObj = new Object();
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString()))
				.thenThrow(new HttpServerErrorException(HttpStatus.ACCEPTED));
		assertEquals(respObj, packetUploadServiceImpl.pushPacket(f));
	}*/
	
	private Slice<Registration> getSlice(List<Registration> list) {
		// TODO Auto-generated method stub
		return new Slice<Registration>() {
			
			@Override
			public Iterator<Registration> iterator() {
				// TODO Auto-generated method stub
				return list.iterator();
			}
			
			@Override
			public Pageable previousPageable() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Pageable nextPageable() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public <U> Slice<U> map(Function<? super Registration, ? extends U> converter) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean isLast() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isFirst() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasPrevious() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasContent() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public Sort getSort() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int getSize() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int getNumberOfElements() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int getNumber() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public List<Registration> getContent() {
				// TODO Auto-generated method stub
				return list;
			}
		};
	}
}
