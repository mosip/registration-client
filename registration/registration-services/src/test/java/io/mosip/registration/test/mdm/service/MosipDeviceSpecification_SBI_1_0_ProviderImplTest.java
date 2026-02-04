package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.request.SbiRCaptureRequestDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.sbi.spec_1_0.service.impl.MosipDeviceSpecification_SBI_1_0_ProviderImpl;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest(value = {MosipDeviceSpecification_SBI_1_0_ProviderImpl.class, MosipDeviceSpecificationHelper.class, CryptoUtil.class, io.mosip.registration.mdm.dto.Biometric.class})
public class MosipDeviceSpecification_SBI_1_0_ProviderImplTest {
	
	@Mock
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;
	
	@InjectMocks
	private MosipDeviceSpecification_SBI_1_0_ProviderImpl mockObject;

	@Mock
	private ObjectMapper objectMapper;
	
	@BeforeClass
	public static void initialize() throws IOException, java.io.IOException {

	}

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void getMdmDevicesTest() throws Exception{

		int port = 4051;
		String inputDeviceInfo = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

		MdmBioDevice expectedDevice = new MdmBioDevice();
		expectedDevice.setCallbackId("http://127.0.0.1:4501");
		expectedDevice.setCertification("1.0");
		expectedDevice.setDeviceStatus("Ready");
		expectedDevice.setPort(port);

		List<MdmBioDevice> expectedList = Arrays.asList(expectedDevice);

		List<MdmDeviceInfoResponse> deviceInfoResponses = new ArrayList<>();

		MdmDeviceInfoResponse deviceInfoResponse = new MdmDeviceInfoResponse();
		deviceInfoResponse.setDeviceInfo(inputDeviceInfo);

		io.mosip.registration.mdm.sbi.spec_1_0.dto.response.Error error =
				new io.mosip.registration.mdm.sbi.spec_1_0.dto.response.Error();
		error.setErrorCode("100");
		error.setErrorInfo("Success");

		deviceInfoResponse.setError(error);
		deviceInfoResponses.add(deviceInfoResponse);

		MdmSbiDeviceInfoWrapper wrapper = new MdmSbiDeviceInfoWrapper();
		MdmSbiDeviceInfo deviceInfo = new MdmSbiDeviceInfo();

		deviceInfo.setCallbackId("http://127.0.0.1:4501");
		deviceInfo.setCertification("1.0");
		deviceInfo.setDeviceStatus("Ready");

		wrapper.setDeviceInfo(deviceInfo);
		wrapper.setError(error);

		ObjectMapper mapper = new ObjectMapper();

		when(mosipDeviceSpecificationHelper.getMapper())
				.thenReturn(mapper);

		when(mosipDeviceSpecificationHelper.getDeviceInfoDecoded(
				anyString(),
				any()))
				.thenReturn(wrapper);

		List<MdmBioDevice> result =
				mockObject.getMdmDevices(inputDeviceInfo, port);

		assertNotNull(result);
	}

	@Test
	public void streamTest() throws Exception{
		
		int port = 4501;
		String str = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData = new byte[str.length()];
		InputStream expectedStream = new ByteArrayInputStream(byteData);
		MosipDeviceSpecification_SBI_1_0_ProviderImpl mockedObject = mock(MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);
		
		MdmBioDevice inputBioDevice = new MdmBioDevice();
		inputBioDevice.setCallbackId("http://127.0.0.1:4501");
		inputBioDevice.setCertification("1.0");
		inputBioDevice.setDeviceMake("MOSIP");
		inputBioDevice.setDeviceModel("SLAP01");
		inputBioDevice.setDeviceProviderId("MOSIP.PROXY.SBI");
		inputBioDevice.setDeviceProviderName("MOSIP");
		inputBioDevice.setDeviceStatus("Ready");
		String[] deviceSubId = {"1","2","3"};
		inputBioDevice.setDeviceSubId(deviceSubId);
		inputBioDevice.setDeviceSubType("Slap");
		inputBioDevice.setDeviceType("Finger");
		inputBioDevice.setFirmWare("1.0");
		inputBioDevice.setPort(port);
		inputBioDevice.setProviderId("MOSIP.PROXY.SBI");
		inputBioDevice.setProviderName("MOSIP");
		inputBioDevice.setPurpose("Registration");
		inputBioDevice.setSerialNumber("1");
		inputBioDevice.setSerialVersion("1.0");
		inputBioDevice.setSpecVersion("1.0");
		inputBioDevice.setTimestamp("2021-04-29T05:56:29.909Z");
		
		
		OngoingStubbing<InputStream> actualStream = when(mockedObject.stream(inputBioDevice, "IRIS_DOUBLE")).thenReturn(expectedStream);
		
		Assert.assertNotNull(actualStream.getMock());
	}

	@Test
	public void testGetMdmDevices() throws Exception {
		String deviceInfoResponse = "[{\"deviceInfo\":\"encodedDeviceInfo\"}]";
		int port = 4501;

		MdmDeviceInfoResponse mdmDeviceInfoResponse = new MdmDeviceInfoResponse();
		mdmDeviceInfoResponse.setDeviceInfo("encodedDeviceInfo");

		List<MdmDeviceInfoResponse> deviceInfoResponses = new ArrayList<>();
		deviceInfoResponses.add(mdmDeviceInfoResponse);

		MdmSbiDeviceInfoWrapper deviceInfo = new MdmSbiDeviceInfoWrapper();
		SbiDigitalId digitalId = new SbiDigitalId();
		digitalId.setDeviceSubType("Slap");
		digitalId.setType("Finger");
		digitalId.setDateTime("2021-04-29T05:56:29.909Z");
		digitalId.setDeviceProvider("MOSIP");
		digitalId.setDeviceProviderId("MOSIP.PROXY.SBI");
		digitalId.setModel("SLAP01");
		digitalId.setMake("MOSIP");
		digitalId.setSerialNo("1");

		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(objectMapper);
		when(objectMapper.readValue(eq(deviceInfoResponse), any(TypeReference.class))).thenReturn(deviceInfoResponses);
		when(mosipDeviceSpecificationHelper.getDeviceInfoDecoded(anyString(), any())).thenReturn(deviceInfo);

		List<MdmBioDevice> result = mockObject.getMdmDevices(deviceInfoResponse, port);

		assertNotNull("Result should not be null", result);
	}

	@Test (expected = RegBaseCheckedException.class)
	public void testStream_withException() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setSerialNumber("12345");

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/stream");
		when(mosipDeviceSpecificationHelper.getJPEGByteArray(any(), anyLong())).thenReturn(new byte[10]);

		mockObject.stream(bioDevice, "IRIS_DOUBLE");
	}

	@Test
	public void testRCapture() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setSerialNumber("12345");

		MDMRequestDto mdmRequestDto = new MDMRequestDto("FINGERPRINT_SLAP_RIGHT", new String[] {}, "Registration", "dev", 5000, 1, 22);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/rcapture");
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn("{\"biometrics\":[]}");

		List<BiometricsDto> result = mockObject.rCapture(bioDevice, mdmRequestDto);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testIsDeviceAvailable() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setDeviceType("Finger");

		SbiDeviceDiscoveryMDSResponse response = new SbiDeviceDiscoveryMDSResponse();
		response.setSpecVersion(new String[] { "1.0" });
		response.setDeviceStatus("Ready");
		response.setCertification("1.0");

		List<SbiDeviceDiscoveryMDSResponse> responses = new ArrayList<>();
		responses.add(response);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/device");
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn("[{\"deviceStatus\":\"Ready\"}]");
		when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(responses);

		boolean result =mockObject.isDeviceAvailable(bioDevice);

		assertNotNull(result);
	}

	@Test
	public void testGetRCaptureRequest() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setDeviceType("Finger");
		bioDevice.setSerialNumber("12345");
		bioDevice.setDeviceId("Device123");
		bioDevice.setDeviceSubType("Slap");
		bioDevice.setPurpose("Registration");
		bioDevice.setSpecVersion("1.0");

		MDMRequestDto mdmRequestDto = new MDMRequestDto("FINGERPRINT_SLAP_RIGHT", new String[] {}, "Registration", "dev", 5000, 1, 22);

		SbiRCaptureRequestDTO result = Whitebox.invokeMethod(mockObject, "getRCaptureRequest", bioDevice, mdmRequestDto);

		assertNotNull(result);
	}

	@Test
	public void testIsDeviceAvailable_NoDevices() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setDeviceType("Finger");

		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn("[]");

		boolean result = mockObject.isDeviceAvailable(bioDevice);

		assertFalse(result);
	}

	@Test
	public void testGetDevicCode_Fingerprint() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				RegistrationConstants.FINGERPRINT_UPPERCASE
		);

		assertEquals("FIR", result);
	}

	@Test
	public void testGetDevicCode_Iris() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				RegistrationConstants.IRIS
		);

		assertEquals("IIR", result);
	}

	@Test
	public void testGetDevicCode_Default() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				"FACE"
		);

		assertEquals("FACE", result);
	}

	@Test
	public void testGetSbiDigitalId_Success() throws Exception {

		String digitalId = "dummy.jwt.token";
		String payload = "encodedPayload";
		byte[] decodedPayload = "{\"type\":\"Finger\"}".getBytes();

		SbiDigitalId expected = new SbiDigitalId();
		expected.setType("Finger");

		PowerMockito.mockStatic(CryptoUtil.class);

		when(mosipDeviceSpecificationHelper.getPayLoad(digitalId))
				.thenReturn(payload);

		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(payload))
				.thenReturn(decodedPayload);

		when(mosipDeviceSpecificationHelper.getMapper())
				.thenReturn(objectMapper);

		when(objectMapper.readValue(anyString(), eq(SbiDigitalId.class)))
				.thenReturn(expected);

		Mockito.doNothing()
				.when(mosipDeviceSpecificationHelper)
				.validateJWTResponse(anyString(), anyString());

		SbiDigitalId result = Whitebox.invokeMethod(
				mockObject,
				"getSbiDigitalId",
				digitalId
		);

		assertNotNull(result);
		assertEquals("Finger", result.getType());
	}

	@Test
	public void testGetExceptions_NullInput() throws Exception {

		String[] result = Whitebox.invokeMethod(
				mockObject,
				"getExceptions",
				new Object[] { null }
		);

		assertNull(result);
	}

	@Test
	public void testGetExceptions_SingleValue() throws Exception {

		PowerMockito.mockStatic(io.mosip.registration.mdm.dto.Biometric.class);

		String[] input = { "LEFT_THUMB" };

		PowerMockito.when(
				io.mosip.registration.mdm.dto.Biometric
						.getmdmRequestAttributeName(eq("LEFT_THUMB"), anyString())
		).thenReturn("FINGER_LEFT_THUMB");

		String[] result = Whitebox.invokeMethod(
				mockObject,
				"getExceptions",
				new Object[]{ input }
		);

		assertNotNull(result);
		assertEquals(1, result.length);
		assertEquals("FINGER_LEFT_THUMB", result[0]);
	}

	@Test
	public void testGetExceptions_MultipleValues() throws Exception {

		PowerMockito.mockStatic(io.mosip.registration.mdm.dto.Biometric.class);

		String[] input = { "LEFT", "RIGHT" };

		PowerMockito.when(
				io.mosip.registration.mdm.dto.Biometric
						.getmdmRequestAttributeName(eq("LEFT"), anyString())
		).thenReturn("FINGER_LEFT");

		PowerMockito.when(
				io.mosip.registration.mdm.dto.Biometric
						.getmdmRequestAttributeName(eq("RIGHT"), anyString())
		).thenReturn("FINGER_RIGHT");

		String[] result = Whitebox.invokeMethod(
				mockObject,
				"getExceptions",
				new Object[]{ input }
		);

		assertEquals(2, result.length);
		assertEquals("FINGER_LEFT", result[0]);
		assertEquals("FINGER_RIGHT", result[1]);
	}

	@Test
	public void testGetSpecVersion() {
		assertEquals("1.0", mockObject.getSpecVersion());
	}

	@Test
	public void testGetDeviceType() throws Exception {
		String result = Whitebox.invokeMethod(
				mockObject,
				"getDeviceType",
				"Finger"
		);

		assertEquals("Finger", result);
	}

	@Test
	public void testGetDeviceSubId_Left() throws Exception {
		assertEquals("1", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "left_index"));
	}

	@Test
	public void testGetDeviceSubId_Right() throws Exception {
		assertEquals("2", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "right_thumb"));
	}

	@Test
	public void testGetDeviceSubId_Double() throws Exception {
		assertEquals("3", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "double_finger"));
	}

	@Test
	public void testGetDeviceSubId_Default() throws Exception {
		assertEquals("0", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "face"));
	}

	@Test
	public void testGetRCaptureRequest_BioDeviceNull() throws Exception {
		MDMRequestDto mdmRequestDto = new MDMRequestDto("FINGERPRINT_SLAP_RIGHT", new String[] {}, "Registration", "dev", 5000, 1, 22);

		SbiRCaptureRequestDTO result = Whitebox.invokeMethod(
				mockObject,
				"getRCaptureRequest",
				(MdmBioDevice) null,
				mdmRequestDto
		);

		assertNull(result);
	}

	@Test
	public void testGetRCaptureRequest_Valid() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setDeviceType("Finger");
		bioDevice.setSerialNumber("123");
		bioDevice.setDeviceId("DEV01");
		bioDevice.setDeviceSubType("LEFT");
		bioDevice.setPurpose("REG");
		bioDevice.setSpecVersion("1.0");

		MDMRequestDto mdmRequestDto = new MDMRequestDto("FINGERPRINT_SLAP_RIGHT", new String[] {}, "Registration", "PROD", 1000, 1, 70);
		mdmRequestDto.setEnvironment("PROD");
		mdmRequestDto.setRequestedScore(70);
		mdmRequestDto.setTimeout(1000);
		mdmRequestDto.setModality("left_index");

		Mockito.when(mosipDeviceSpecificationHelper.generateMDMTransactionId())
				.thenReturn("TXN123");

		SbiRCaptureRequestDTO result = Whitebox.invokeMethod(
				mockObject,
				"getRCaptureRequest",
				bioDevice,
				mdmRequestDto
		);

		assertNotNull(result);
		assertEquals("PROD", result.getEnv());
	}

	@Test
	public void testGetBioDevice_DeviceInfoNull() throws Exception {
		MdmSbiDeviceInfoWrapper wrapper = new MdmSbiDeviceInfoWrapper();
		wrapper.deviceInfo = null;

		MdmBioDevice result = Whitebox.invokeMethod(
				mockObject,
				"getBioDevice",
				wrapper
		);

		assertNull(result);
	}

}
