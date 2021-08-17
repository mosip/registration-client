package io.mosip.registration.test.service.packet.encryption;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.packet.impl.PacketUploadServiceImpl;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

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

	@Test(expected = RegBaseCheckedException.class)
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
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj1);

		assertEquals(RegistrationClientStatusCode.UPLOAD_ERROR_STATUS.getCode(), packetUploadServiceImpl.uploadPacket("test").getUploadStatus());
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
	public void testUploadPacket() throws RegBaseCheckedException, ConnectionException {
		Registration registration = new Registration();
		List<Registration> regList = new ArrayList<>();
		registration.setId("123456789");
		registration.setAckFilename("..//registration-services/src/test/resources/123456789_Ack.png");
		registration.setUploadCount((short) 0);
		registration.setClientStatusCode("PUSHED");
		registration.setFileUploadStatus("S");
		registration.setCrDtime(Timestamp.from(Instant.now()));
		regList.add(registration);

		LinkedHashMap<String, Object> respObj = new LinkedHashMap<>();
		respObj.put("response", "Success");
		respObj.put("error", null);
		//respObj = "PACKET_UPLOADED_TO_VIRUS_SCAN";
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(respObj);
		List<Registration> packetList = new ArrayList<>();
		Registration registration1 = new Registration();
		packetList.add(registration);
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		Mockito.when(registrationDAO.updateRegStatus(Mockito.anyObject())).thenReturn(registration1);
		packetUploadServiceImpl.uploadPacket("123456789");
		assertEquals("PUSHED", registration.getClientStatusCode());
		assertEquals("S", registration.getFileUploadStatus());

	}

	@Test
	public void testUploadPacket1() throws RegBaseCheckedException, ConnectionException {
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
		registration.setAckFilename("..//registration-services/src/test/resources/1234567895_Ack.png");
		registration.setUploadCount((short) 0);
		regList.add(registration);
		registration.setCrDtime(Timestamp.from(Instant.now()));

		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		packetUploadServiceImpl.uploadPacket("123456789");
		assertEquals(null, registration.getFileUploadStatus());

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
}
