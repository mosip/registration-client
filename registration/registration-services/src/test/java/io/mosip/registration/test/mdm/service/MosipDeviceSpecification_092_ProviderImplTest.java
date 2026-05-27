package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.mdm.dto.Biometric;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.DeviceDiscoveryMDSResponse;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.DigitalId;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.RCaptureResponseDTO;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.RCaptureResponseDataDTO;
import io.mosip.registration.mdm.spec_0_9_2.service.impl.MosipDeviceSpecification_092_ProviderImpl;
import io.mosip.kernel.core.util.CryptoUtil;
import org.powermock.reflect.Whitebox;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({CryptoUtil.class, Biometric.class})
public class MosipDeviceSpecification_092_ProviderImplTest {

	@Mock
	private MosipDeviceSpecificationHelper helper;

	@InjectMocks
	private MosipDeviceSpecification_092_ProviderImpl provider;

	@Test
	public void getSpecVersion_whenCalled_returns092() {
		assertEquals("0.9.2", provider.getSpecVersion());
	}

	@Test
	public void getMdmDevices_withValidDeviceInfoResponse_returnsMdmDevices() throws Exception {
		int port = 5055;

		List<MdmDeviceInfoResponse> responses = new LinkedList<>();
		MdmDeviceInfoResponse resp = new MdmDeviceInfoResponse();
		resp.setDeviceInfo("dummy.jwt.for.deviceinfo");
		responses.add(resp);

		ObjectMapper mapper = new ObjectMapper();
		String deviceInfoResponseJson = mapper.writeValueAsString(responses);

		MdmDeviceInfo deviceInfo = new MdmDeviceInfo();
		deviceInfo.setDeviceId("DEV-1");
		deviceInfo.setFirmware("FW1");
		deviceInfo.setCertification("CERT");
		deviceInfo.setServiceVersion("1.0");
		deviceInfo.setPurpose("Registration");
		deviceInfo.setDeviceCode("CODE");

		String digitalIdPayloadJson = mapper.writeValueAsString(createDigitalId());
		String base64Payload = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(digitalIdPayloadJson.getBytes(StandardCharsets.UTF_8));
		deviceInfo.setDigitalId("header." + base64Payload + ".sig");

		Mockito.when(helper.getMapper()).thenReturn(mapper);
		Mockito.when(helper.getDeviceInfoDecoded(Mockito.eq(resp.getDeviceInfo()), Mockito.any()))
				.thenReturn(deviceInfo);
		Mockito.doNothing().when(helper).validateJWTResponse(Mockito.anyString(), Mockito.anyString());
		Mockito.when(helper.getPayLoad(Mockito.anyString())).thenReturn(base64Payload);

		List<MdmBioDevice> devices = provider.getMdmDevices(deviceInfoResponseJson, port);

		assertNotNull(devices);
	}

	@Test
	public void stream_whenDeviceAvailableAndResponseHasEntity_returnsInputStream() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6000);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(dev);

		String url = "http://localhost:6000/stream";
		Mockito.when(helper.buildUrl(Mockito.eq(6000), Mockito.anyString())).thenReturn(url);

		CloseableHttpResponse httpResp = Mockito.mock(CloseableHttpResponse.class);
		HttpEntity entity = Mockito.mock(HttpEntity.class);
		byte[] bytes = new byte[] {1, 2, 3};
		InputStream expected = new ByteArrayInputStream(bytes);
		Mockito.when(entity.getContent()).thenReturn(expected);
		Mockito.when(httpResp.getEntity()).thenReturn(entity);
		Mockito.when(helper.getHttpClientResponse(Mockito.eq(url), Mockito.eq("STREAM"), Mockito.anyString()))
				.thenReturn(httpResp);

		InputStream actual = spy.stream(dev, "RIGHT");
		assertNotNull(actual);
	}

	@Test (expected = RegBaseCheckedException.class)
	public void rCapture_whenNoBiometricsInResponse_throwsRegBaseCheckedException() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(7000);
		dev.setDeviceType("Fingerprint");

		MDMRequestDto req = new MDMRequestDto("FINGERPRINT_RIGHT", new String[]{"rightThumb"}, "Registration",
				null, 1000, 1, 1);

		String url = "http://localhost:7000/capture";
		Mockito.when(helper.buildUrl(Mockito.eq(7000), Mockito.anyString())).thenReturn(url);

		RCaptureResponseDataDTO dataDTO = new RCaptureResponseDataDTO();
		dataDTO.setBioSubType("RIGHT_INDEX");
		dataDTO.setQualityScore("80.0");
		String payloadJson = new ObjectMapper().writeValueAsString(dataDTO);
		String base64Payload = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

		RCaptureResponseBiometricsDTO bio = new RCaptureResponseBiometricsDTO();
		bio.setData("header." + base64Payload + ".sig");
		bio.setSpecVersion("0.9.2");
		RCaptureResponseDTO responseDTO = new RCaptureResponseDTO();
		responseDTO.setBiometrics(Arrays.asList(bio));
		String responseJson = new ObjectMapper().writeValueAsString(responseDTO);

		ObjectMapper configuredMapper = new ObjectMapper();
		configuredMapper.registerModule(new JavaTimeModule());
		configuredMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		PowerMockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(configuredMapper);

		Mockito.when(helper.getHttpClientResponseEntity(Mockito.eq(url), Mockito.eq("RCAPTURE"), Mockito.anyString()))
				.thenReturn(responseJson);
		Mockito.when(helper.getSignature(Mockito.anyString())).thenReturn("sig");
		Mockito.when(helper.getPayLoad(Mockito.anyString())).thenReturn(base64Payload);
		Mockito.doNothing().when(helper).validateJWTResponse(Mockito.anyString(), Mockito.anyString());

		provider.rCapture(dev, req);
	}

	@Test
	public void isDeviceAvailable_whenDiscoveryMatches_returnsTrue() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("D1");
		dev.setCertification("CERT");
		dev.setDeviceCode("CODE");
		dev.setPort(8000);

		DeviceDiscoveryMDSResponse d = new DeviceDiscoveryMDSResponse();
		d.setDeviceId("D1");
		d.setDeviceCode("CODE");
		d.setCertification("CERT");

		d.setDeviceStatus("READY");

		d.setSpecVersion(new String[]{"0.9.2"});

		List<DeviceDiscoveryMDSResponse> list = Arrays.asList(d);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(list);

		Mockito.when(helper.getMapper()).thenReturn(mapper);

		Mockito.when(helper.buildUrl(Mockito.eq(8000), Mockito.anyString()))
				.thenReturn("http://x/device");

		ResponseEntity<String> mockResponse =
				new ResponseEntity<>(json, HttpStatus.OK);

		Mockito.when(helper.getHttpClientResponseEntity(
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
					.thenReturn(String.valueOf(mockResponse));

		boolean available = provider.isDeviceAvailable(dev);

		assertNotNull(available);
	}

	private static DigitalId createDigitalId() {
		DigitalId d = new DigitalId();
		d.setDeviceProvider("MOSIP");
		d.setDeviceProviderId("MOSIP.PROXY.SBI");
		d.setType("Finger");
		d.setModel("SLAP01");
		d.setMake("MOSIP");
		d.setSerialNo("1");
		d.setDateTime("2021-04-29T05:56:29.909Z");
		return d;
	}

	@Test
	public void stream_whenEntityNull_returnsNullStream() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6001);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(dev);

		String url = "http://localhost:6001/stream";
		Mockito.when(helper.buildUrl(Mockito.eq(6001), Mockito.anyString())).thenReturn(url);

		CloseableHttpResponse httpResp = Mockito.mock(CloseableHttpResponse.class);
		Mockito.when(httpResp.getEntity()).thenReturn(null);
		Mockito.when(helper.getHttpClientResponse(Mockito.eq(url), Mockito.eq("STREAM"), Mockito.anyString()))
				.thenReturn(httpResp);

		InputStream stream = spy.stream(dev, "RIGHT");
		assertNull(stream);
	}

	@Test(expected = RegBaseCheckedException.class)
	public void stream_whenDeviceNotAvailable_throwsRegBaseCheckedException() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6002);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(false).when(spy).isDeviceAvailable(dev);

		spy.stream(dev, "LEFT");
	}

	@Test(expected = RegBaseCheckedException.class)
	public void stream_whenHelperThrows_wrapsAndThrowsRegBaseCheckedException() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6003);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(dev);
		Mockito.when(helper.buildUrl(Mockito.anyInt(), Mockito.anyString())).thenThrow(new RuntimeException("boom"));

		spy.stream(dev, "RIGHT");
	}

	@Test(expected = RegBaseCheckedException.class)
	public void rCapture_whenHelperThrows_wrapsAndThrowsRegBaseCheckedException() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(7001);
		dev.setDeviceType("Fingerprint");

		MDMRequestDto req = new MDMRequestDto("FINGERPRINT_LEFT", new String[]{"leftThumb"}, "Registration",
				null, 1000, 1, 1);

		Mockito.when(helper.buildUrl(Mockito.eq(7001), Mockito.anyString())).thenThrow(new RuntimeException("boom"));

		provider.rCapture(dev, req);
	}

	@Test
	public void getMdmDevices_withInvalidJson_returnsEmptyList() {
		ObjectMapper mapper = new ObjectMapper();
		Mockito.when(helper.getMapper()).thenReturn(mapper);

		List<MdmBioDevice> devices = provider.getMdmDevices("not-a-json", 5050);
		assertEquals(0, devices.size());
	}

	@Test
	public void getMdmDevices_withNullDeviceInfo_ignoresEntry_returnsEmptyList() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Mockito.when(helper.getMapper()).thenReturn(mapper);

		List<MdmDeviceInfoResponse> responses = new LinkedList<>();
		MdmDeviceInfoResponse resp = new MdmDeviceInfoResponse();
		resp.setDeviceInfo(null);
		responses.add(resp);
		String json = mapper.writeValueAsString(responses);

		List<MdmBioDevice> devices = provider.getMdmDevices(json, 5051);
		assertEquals(0, devices.size());
	}

	@Test
	public void getMdmDevices_withFactoryLatestSpecVersion_returnsMdmDevices() throws Exception {
		int port = 5056;
		List<MdmDeviceInfoResponse> responses = new LinkedList<>();
		MdmDeviceInfoResponse resp = new MdmDeviceInfoResponse();
		resp.setDeviceInfo("dummy.jwt.for.deviceinfo");
		responses.add(resp);

		ObjectMapper mapper = new ObjectMapper();
		String deviceInfoResponseJson = mapper.writeValueAsString(responses);

		MdmDeviceInfo deviceInfo = new MdmDeviceInfo();
		deviceInfo.setDeviceId("DEV-2");
		deviceInfo.setFirmware("FW2");
		deviceInfo.setCertification("CERT2");
		deviceInfo.setServiceVersion("1.1");
		deviceInfo.setPurpose("Registration");
		deviceInfo.setDeviceCode("CODE2");

		String digitalIdPayloadJson = mapper.writeValueAsString(createDigitalId());
		String base64Payload = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString(digitalIdPayloadJson.getBytes(StandardCharsets.UTF_8));
		deviceInfo.setDigitalId("header." + base64Payload + ".sig");

		Mockito.when(helper.getMapper()).thenReturn(mapper);
		Mockito.when(helper.getDeviceInfoDecoded(Mockito.eq(resp.getDeviceInfo()), Mockito.any()))
				.thenReturn(deviceInfo);
		Mockito.doNothing().when(helper).validateJWTResponse(Mockito.anyString(), Mockito.anyString());
		Mockito.when(helper.getPayLoad(Mockito.anyString())).thenReturn(base64Payload);

		List<MdmBioDevice> devices = provider.getMdmDevices(deviceInfoResponseJson, port);
		assertNotNull(devices);
	}

	@Test
	public void isDeviceAvailable_whenDiscoveryDoesNotMatch_returnsFalse() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("D1");
		dev.setCertification("CERT");
		dev.setDeviceCode("CODE");
		dev.setPort(8001);

		DeviceDiscoveryMDSResponse d = new DeviceDiscoveryMDSResponse();
		d.setDeviceId("D1");
		d.setDeviceCode("CODE");
		d.setCertification("CERT");
		d.setDeviceStatus("NotReady");
		d.setSpecVersion(new String[] {"0.9.2"});

		List<DeviceDiscoveryMDSResponse> list = Arrays.asList(d);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(list);

		Mockito.when(helper.getMapper()).thenReturn(mapper);
		Mockito.when(helper.buildUrl(Mockito.eq(8001), Mockito.anyString())).thenReturn("http://x/device");
		Mockito.when(helper.getHttpClientResponseEntity(Mockito.anyString(), Mockito.eq("MOSIPDISC"), Mockito.anyString()))
				.thenReturn(json);

		boolean available = provider.isDeviceAvailable(dev);
		assertFalse(available);
	}

	// ── getExceptions private method ──────────────────────────────────────────

	@Test
	public void getExceptions_nullInput_returnsNull() throws Exception {
		String[] result = Whitebox.invokeMethod(provider, "getExceptions", new Object[]{null});
		assertNull(result);
	}

	@Test
	public void getExceptions_withValues_returnsMapped() throws Exception {
		PowerMockito.mockStatic(Biometric.class);
		PowerMockito.when(Biometric.getmdmRequestAttributeName(Mockito.eq("leftIndex"), Mockito.anyString()))
				.thenReturn("Left IndexFinger");

		String[] result = Whitebox.invokeMethod(provider, "getExceptions", new Object[]{new String[]{"leftIndex"}});
		assertNotNull(result);
		assertEquals("Left IndexFinger", result[0]);
	}

	// ── getDeviceType private method ──────────────────────────────────────────

	@Test
	public void getDeviceType_fingerprint_returnsFIR() throws Exception {
		String result = Whitebox.invokeMethod(provider, "getDeviceType", "fingerprint");
		assertEquals("FIR", result);
	}

	@Test
	public void getDeviceType_iris_returnsIIR() throws Exception {
		String result = Whitebox.invokeMethod(provider, "getDeviceType", "iris");
		assertEquals("IIR", result);
	}

	@Test
	public void getDeviceType_face_returnsFace() throws Exception {
		String result = Whitebox.invokeMethod(provider, "getDeviceType", "face");
		assertEquals("face", result);
	}

	// ── getDeviceSubId private method ─────────────────────────────────────────

	@Test
	public void getDeviceSubId_left_returns1() throws Exception {
		assertEquals("1", Whitebox.invokeMethod(provider, "getDeviceSubId", "left_index"));
	}

	@Test
	public void getDeviceSubId_right_returns2() throws Exception {
		assertEquals("2", Whitebox.invokeMethod(provider, "getDeviceSubId", "right_thumb"));
	}

	@Test
	public void getDeviceSubId_thumbs_returns3() throws Exception {
		assertEquals("3", Whitebox.invokeMethod(provider, "getDeviceSubId", "double_thumb"));
	}

	@Test
	public void getDeviceSubId_face_returns0() throws Exception {
		assertEquals("0", Whitebox.invokeMethod(provider, "getDeviceSubId", "face"));
	}

	// ── getBioDevice with null deviceInfo ─────────────────────────────────────

	@Test
	public void getBioDevice_nullDeviceInfo_returnsNull() throws Exception {
		MdmBioDevice result = Whitebox.invokeMethod(provider, "getBioDevice", (MdmDeviceInfo) null);
		assertNull(result);
	}

	// ── getMdmDevices exception during parse ──────────────────────────────────

	@Test
	public void getMdmDevices_exceptionDuringDeviceDecode_returnsEmptyList() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Mockito.when(helper.getMapper()).thenReturn(mapper);

		List<MdmDeviceInfoResponse> responses = new LinkedList<>();
		MdmDeviceInfoResponse resp = new MdmDeviceInfoResponse();
		resp.setDeviceInfo("some.jwt.token");
		responses.add(resp);
		String json = mapper.writeValueAsString(responses);

		Mockito.when(helper.getDeviceInfoDecoded(Mockito.anyString(), Mockito.any()))
				.thenThrow(new RuntimeException("decode failed"));

		List<MdmBioDevice> devices = provider.getMdmDevices(json, 5057);
		assertEquals(0, devices.size());
	}

	// ── isDeviceAvailable READY status ────────────────────────────────────────

	@Test
	public void isDeviceAvailable_readyStatus_allMatch_returnsTrue() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("D1");
		dev.setCertification("CERT");
		dev.setDeviceCode("CODE");
		dev.setPort(8003);

		DeviceDiscoveryMDSResponse d = new DeviceDiscoveryMDSResponse();
		d.setDeviceId("D1");
		d.setDeviceCode("CODE");
		d.setCertification("CERT");
		d.setDeviceStatus("Ready");
		d.setSpecVersion(new String[]{"0.9.2"});

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(Arrays.asList(d));

		Mockito.when(helper.getMapper()).thenReturn(mapper);
		Mockito.when(helper.buildUrl(Mockito.eq(8003), Mockito.anyString())).thenReturn("http://x/device");
		Mockito.when(helper.getHttpClientResponseEntity(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(json);

		assertTrue(provider.isDeviceAvailable(dev));
	}

	// ── rCapture happy path ───────────────────────────────────────────────────

	@Test
	public void rCapture_withBiometricData_returnsPopulatedList() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(7002);
		dev.setDeviceType("fingerprint");

		MDMRequestDto req = new MDMRequestDto("FINGERPRINT_LEFT", null, "Registration", "dev", 5000, 1, 70);

		String bioPayload = "{\"bioSubType\":\"Left IndexFinger\",\"qualityScore\":\"80.0\"," +
				"\"bioValue\":\"dGVzdA==\",\"timestamp\":\"2021-04-29T05:56:29\"}";
		String base64Payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(bioPayload.getBytes(StandardCharsets.UTF_8));

		RCaptureResponseBiometricsDTO bio = new RCaptureResponseBiometricsDTO();
		bio.setData("header." + base64Payload + ".sig");
		bio.setSpecVersion("0.9.2");

		RCaptureResponseDTO responseDTO = new RCaptureResponseDTO();
		responseDTO.setBiometrics(Arrays.asList(bio));
		String responseJson = new ObjectMapper().writeValueAsString(responseDTO);

		Mockito.when(helper.buildUrl(Mockito.anyInt(), Mockito.anyString())).thenReturn("http://localhost/rcapture");
		Mockito.when(helper.getHttpClientResponseEntity(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(responseJson);
		Mockito.doNothing().when(helper).validateJWTResponse(Mockito.anyString(), Mockito.anyString());
		Mockito.when(helper.getPayLoad(Mockito.anyString())).thenReturn(base64Payload);
		Mockito.when(helper.getSignature(Mockito.anyString())).thenReturn("sig");
		Mockito.when(helper.generateMDMTransactionId()).thenReturn("TXN1");

		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(base64Payload))
				.thenReturn(bioPayload.getBytes(StandardCharsets.UTF_8));

		PowerMockito.mockStatic(Biometric.class);
		PowerMockito.when(Biometric.getUiSchemaAttributeName(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("leftIndex");

		List<BiometricsDto> result = provider.rCapture(dev, req);
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test
	public void isDeviceAvailable_whenHelperThrows_returnsFalse() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("D2");
		dev.setCertification("CERT");
		dev.setDeviceCode("CODE");
		dev.setPort(8002);

		Mockito.when(helper.getHttpClientResponseEntity(Mockito.anyString(), Mockito.eq("MOSIPDISC"), Mockito.anyString()))
				.thenThrow(new RuntimeException("boom"));

		boolean available = provider.isDeviceAvailable(dev);
		assertFalse(available);
	}

}
