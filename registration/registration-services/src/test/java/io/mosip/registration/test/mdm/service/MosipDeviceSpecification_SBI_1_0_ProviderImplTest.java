package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.Biometric;
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
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiDeviceDiscoveryMDSResponse;
import org.apache.http.HttpEntity;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.sbi.spec_1_0.service.impl.MosipDeviceSpecification_SBI_1_0_ProviderImpl;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest(value = {CryptoUtil.class, Biometric.class})
public class MosipDeviceSpecification_SBI_1_0_ProviderImplTest {
	
	@Mock
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

	@Mock
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

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
	public void getMdmDevices_withWrapper_returnsDevices() throws Exception{

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
	public void stream_whenMocked_returnsMockInputStream() throws Exception{

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
	public void getMdmDevices_withJson_returnsDevices() throws Exception {
		String deviceInfoResponse = "[{\"deviceInfo\":\"encodedDeviceInfo\"}]";
		int port = 4501;

		MdmDeviceInfoResponse mdmDeviceInfoResponse = new MdmDeviceInfoResponse();
		mdmDeviceInfoResponse.setDeviceInfo("encodedDeviceInfo");

		List<MdmDeviceInfoResponse> deviceInfoResponses = new ArrayList<>();
		deviceInfoResponses.add(mdmDeviceInfoResponse);

		MdmSbiDeviceInfoWrapper deviceInfo = new MdmSbiDeviceInfoWrapper();

		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(objectMapper);
		when(objectMapper.readValue(eq(deviceInfoResponse), any(TypeReference.class))).thenReturn(deviceInfoResponses);
		when(mosipDeviceSpecificationHelper.getDeviceInfoDecoded(anyString(), any())).thenReturn(deviceInfo);

		List<MdmBioDevice> result = mockObject.getMdmDevices(deviceInfoResponse, port);

		assertNotNull("Result should not be null", result);
	}

	@Test (expected = RegBaseCheckedException.class)
	public void stream_withHelper_returnsJpeg_throwsRegBaseCheckedException() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setSerialNumber("12345");

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/stream");
		when(mosipDeviceSpecificationHelper.getJPEGByteArray(any(), anyLong())).thenReturn(new byte[10]);

		mockObject.stream(bioDevice, "IRIS_DOUBLE");
	}

	@Test
	public void rCapture_withEmptyBiometrics_returnsEmptyList() throws Exception {
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
	public void getRCaptureRequest_withValidInput_returnsRequest() throws Exception {
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
	public void isDeviceAvailable_withNoDevices_returnsFalse() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4501);
		bioDevice.setDeviceType("Finger");

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString()))
				.thenReturn("http://localhost/device");

		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(
				anyString(), anyString(), anyString()))
				.thenReturn("[]");

		boolean result = mockObject.isDeviceAvailable(bioDevice);

		assertFalse(result);
	}

	@Test
	public void getDevicCode_fingerprint_returnsFIR() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				RegistrationConstants.FINGERPRINT_UPPERCASE
		);

		assertEquals("FIR", result);
	}

	@Test
	public void getDevicCode_iris_returnsIIR() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				RegistrationConstants.IRIS
		);

		assertEquals("IIR", result);
	}

	@Test
	public void getDevicCode_default_returnsFACE() throws Exception {

		String result = Whitebox.invokeMethod(
				MosipDeviceSpecification_SBI_1_0_ProviderImpl.class,
				"getDevicCode",
				"FACE"
		);

		assertEquals("FACE", result);
	}

	@Test
	public void getSbiDigitalId_withValidToken_returnsSbiDigitalId() throws Exception {

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
	public void getExceptions_nullInput_returnsNull() throws Exception {

		String[] result = Whitebox.invokeMethod(
				mockObject,
				"getExceptions",
				new Object[] { null }
		);

		assertNull(result);
	}

	@Test
	public void getExceptions_singleValue_returnsTransformed() throws Exception {

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
	public void getExceptions_multipleValues_returnsTransformed() throws Exception {

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
	public void getSpecVersion_called_returns1_0() {
		assertEquals("1.0", mockObject.getSpecVersion());
	}

	@Test
	public void getDeviceType_called_returnsSame() throws Exception {
		String result = Whitebox.invokeMethod(
				mockObject,
				"getDeviceType",
				"Finger"
		);

		assertEquals("Finger", result);
	}

	@Test
	public void getDeviceSubId_left_returns1() throws Exception {
		assertEquals("1", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "left_index"));
	}

	@Test
	public void getDeviceSubId_right_returns2() throws Exception {
		assertEquals("2", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "right_thumb"));
	}

	@Test
	public void getDeviceSubId_double_returns3() throws Exception {
		assertEquals("3", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "double_finger"));
	}

	@Test
	public void getDeviceSubId_default_returns0() throws Exception {
		assertEquals("0", Whitebox.invokeMethod(mockObject, "getDeviceSubId", "face"));
	}

	@Test
	public void getRCaptureRequest_bioDeviceNull_returnsNull() throws Exception {
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
	public void getRCaptureRequest_validInput_returnsRequestWithEnv() throws Exception {
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
	public void getBioDevice_withNullDeviceInfo_returnsNull() throws Exception {
		MdmSbiDeviceInfoWrapper wrapper = new MdmSbiDeviceInfoWrapper();
		wrapper.deviceInfo = null;

		MdmBioDevice result = Whitebox.invokeMethod(
				mockObject,
				"getBioDevice",
				wrapper
		);

		assertNull(result);
	}

	// ── stream happy-path & edge cases ───────────────────────────────────────

	@Test
	public void stream_whenEntityNull_returnsNull() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4502);
		bioDevice.setSerialNumber("SN1");

		MosipDeviceSpecification_SBI_1_0_ProviderImpl spy = Mockito.spy(mockObject);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(bioDevice);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/stream");

		CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
		when(httpResponse.getEntity()).thenReturn(null);
		when(mosipDeviceSpecificationHelper.getHttpClientResponse(anyString(), anyString(), anyString()))
				.thenReturn(httpResponse);
		when(mosipDeviceSpecificationHelper.getJPEGByteArray(any(), anyLong())).thenReturn(null);

		InputStream result = spy.stream(bioDevice, "FACE");
		assertNull(result);
	}

	@Test
	public void stream_whenEntityHasContent_andJpegNull_returnsUrlStream() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4503);
		bioDevice.setSerialNumber("SN2");

		MosipDeviceSpecification_SBI_1_0_ProviderImpl spy = Mockito.spy(mockObject);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(bioDevice);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/stream");

		byte[] bytes = new byte[]{1, 2, 3};
		InputStream urlStream = new ByteArrayInputStream(bytes);
		HttpEntity entity = Mockito.mock(HttpEntity.class);
		when(entity.getContent()).thenReturn(urlStream);

		CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
		when(httpResponse.getEntity()).thenReturn(entity);
		when(mosipDeviceSpecificationHelper.getHttpClientResponse(anyString(), anyString(), anyString()))
				.thenReturn(httpResponse);
		when(mosipDeviceSpecificationHelper.getJPEGByteArray(any(), anyLong())).thenReturn(null);

		InputStream result = spy.stream(bioDevice, "FACE");
		assertNotNull(result);
	}

	// ── rCapture exception wrapping ───────────────────────────────────────────

	@Test(expected = RegBaseCheckedException.class)
	public void rCapture_whenHelperThrows_wrapsException() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4504);

		MDMRequestDto req = new MDMRequestDto("FACE", null, "Registration", "dev", 5000, 1, 70);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenThrow(new RuntimeException("boom"));

		mockObject.rCapture(bioDevice, req);
	}

	// ── isDeviceAvailable returning true branches ─────────────────────────────

	@Test
	public void isDeviceAvailable_specVersionMatches_returnsTrue() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4505);
		bioDevice.setDeviceType("Finger");
		bioDevice.setCertification("OTHER");

		SbiDeviceDiscoveryMDSResponse resp = new SbiDeviceDiscoveryMDSResponse();
		resp.setSpecVersion(new String[]{"1.0"});
		resp.setDeviceStatus("NotReady");
		resp.setCertification("CERT_OTHER");

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(Arrays.asList(resp));

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/device");
		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(mapper);
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn(json);

		assertTrue(mockObject.isDeviceAvailable(bioDevice));
	}

	@Test
	public void isDeviceAvailable_deviceStatusReady_returnsTrue() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4506);
		bioDevice.setDeviceType("Finger");
		bioDevice.setCertification("X");

		SbiDeviceDiscoveryMDSResponse resp = new SbiDeviceDiscoveryMDSResponse();
		resp.setSpecVersion(new String[]{"9.9"});
		resp.setDeviceStatus("Ready");
		resp.setCertification("Y");

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(Arrays.asList(resp));

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/device");
		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(mapper);
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn(json);

		assertTrue(mockObject.isDeviceAvailable(bioDevice));
	}

	@Test
	public void isDeviceAvailable_certificationMatches_returnsTrue() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4507);
		bioDevice.setDeviceType("Finger");
		bioDevice.setCertification("CERT_MATCH");

		SbiDeviceDiscoveryMDSResponse resp = new SbiDeviceDiscoveryMDSResponse();
		resp.setSpecVersion(new String[]{"9.9"});
		resp.setDeviceStatus("NotReady");
		resp.setCertification("CERT_MATCH");

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(Arrays.asList(resp));

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/device");
		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(mapper);
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn(json);

		assertTrue(mockObject.isDeviceAvailable(bioDevice));
	}

	@Test
	public void isDeviceAvailable_whenHelperThrows_returnsFalse() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4508);
		bioDevice.setDeviceType("Finger");

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/device");
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenThrow(new RuntimeException("connection refused"));

		assertFalse(mockObject.isDeviceAvailable(bioDevice));
	}

	// ── getBioDevice happy path ───────────────────────────────────────────────

	@Test
	public void getBioDevice_withValidDeviceInfo_populatesFields() throws Exception {
		String digitalIdPayload = "{\"type\":\"Finger\",\"deviceSubType\":\"Slap\",\"make\":\"MOSIP\"," +
				"\"model\":\"SLAP01\",\"serialNo\":\"SN123\",\"deviceProvider\":\"MOSIP\"," +
				"\"deviceProviderId\":\"MOSIP.PROXY.SBI\",\"dateTime\":\"2021-04-29T05:56:29Z\"}";
		String base64Payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(digitalIdPayload.getBytes(StandardCharsets.UTF_8));
		String jwtToken = "header." + base64Payload + ".sig";

		MdmSbiDeviceInfo deviceInfo = new MdmSbiDeviceInfo();
		deviceInfo.setFirmware("FW1.0");
		deviceInfo.setCertification("L1");
		deviceInfo.setServiceVersion("1.0");
		deviceInfo.setSpecVersion(new String[]{"1.0"});
		deviceInfo.setPurpose("Registration");
		deviceInfo.setCallbackId("http://127.0.0.1:4501");
		deviceInfo.setDigitalId(jwtToken);

		MdmSbiDeviceInfoWrapper wrapper = new MdmSbiDeviceInfoWrapper();
		wrapper.deviceInfo = deviceInfo;

		Mockito.doNothing().when(mosipDeviceSpecificationHelper).validateJWTResponse(anyString(), anyString());
		when(mosipDeviceSpecificationHelper.getPayLoad(anyString())).thenReturn(base64Payload);
		when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(new ObjectMapper());
		when(deviceSpecificationFactory.getLatestSpecVersion(any())).thenReturn("1.0");

		MdmBioDevice result = Whitebox.invokeMethod(mockObject, "getBioDevice", wrapper);

		assertNotNull(result);
		assertEquals("FW1.0", result.getFirmWare());
		assertEquals("L1", result.getCertification());
	}

	// ── rCapture happy path with biometric data ───────────────────────────────

	@Test
	public void rCapture_withBiometricData_returnsPopulatedList() throws Exception {
		MdmBioDevice bioDevice = new MdmBioDevice();
		bioDevice.setPort(4509);
		bioDevice.setSerialNumber("SN99");
		bioDevice.setDeviceType("Finger");
		bioDevice.setSpecVersion("1.0");
		bioDevice.setPurpose("Registration");

		MDMRequestDto req = new MDMRequestDto("FINGERPRINT_SLAB_LEFT", null, "Registration", "dev", 5000, 1, 70);

		String bioPayloadJson = "{\"bioSubType\":\"Left IndexFinger\",\"qualityScore\":\"80.0\"," +
				"\"bioValue\":\"dGVzdA==\",\"transactionId\":\"TXN1\",\"timestamp\":\"2021-04-29T05:56:29\"}";
		String base64Payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(bioPayloadJson.getBytes(StandardCharsets.UTF_8));

		SbiRCaptureResponseBiometricsDTO bio = new SbiRCaptureResponseBiometricsDTO();
		bio.setData("header." + base64Payload + ".sig");
		bio.setSpecVersion("1.0");

		SbiRCaptureResponseDTO captureResponse = new SbiRCaptureResponseDTO();
		captureResponse.setBiometrics(Arrays.asList(bio));

		ObjectMapper mapper = new ObjectMapper();
		String responseJson = mapper.writeValueAsString(captureResponse);

		when(mosipDeviceSpecificationHelper.buildUrl(anyInt(), anyString())).thenReturn("http://localhost/rcapture");
		when(mosipDeviceSpecificationHelper.getHttpClientResponseEntity(anyString(), anyString(), anyString()))
				.thenReturn(responseJson);
		when(mosipDeviceSpecificationHelper.generateMDMTransactionId()).thenReturn("TXN1");
		Mockito.doNothing().when(mosipDeviceSpecificationHelper).validateJWTResponse(anyString(), anyString());
		when(mosipDeviceSpecificationHelper.getPayLoad(anyString())).thenReturn(base64Payload);
		when(mosipDeviceSpecificationHelper.getSignature(anyString())).thenReturn("sig");

		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(base64Payload))
				.thenReturn(bioPayloadJson.getBytes(StandardCharsets.UTF_8));

		PowerMockito.mockStatic(Biometric.class);
		PowerMockito.when(Biometric.getUiSchemaAttributeName(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("leftIndex");

		List<BiometricsDto> result = mockObject.rCapture(bioDevice, req);
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("leftIndex", result.get(0).getBioAttribute());
	}

}
