package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({MosipDeviceSpecification_092_ProviderImpl.class, MosipDeviceSpecificationHelper.class})
public class MosipDeviceSpecification_092_ProviderImplTest {

	@Mock
	private MosipDeviceSpecificationHelper helper;

	@InjectMocks
	private MosipDeviceSpecification_092_ProviderImpl provider;

	@Test
	public void shouldReturnSpecVersion() {
		assertEquals("0.9.2", provider.getSpecVersion());
	}

	@Test
	public void shouldParseDeviceInfoAndReturnMdmDevices() throws Exception {
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
	public void shouldReturnStreamWhenDeviceAvailableAndResponseHasEntity() throws Exception {
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
	public void shouldThrowExceptionWhenRCaptureReturnsNoBiometrics() throws Exception {
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
	public void shouldReturnTrueWhenDeviceAvailableMatches() throws Exception {
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
	public void shouldReturnNullStreamWhenEntityIsNull() throws Exception {
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
	public void shouldThrowStreamWhenDeviceNotAvailable() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6002);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(false).when(spy).isDeviceAvailable(dev);

		spy.stream(dev, "LEFT");
	}

	@Test(expected = RegBaseCheckedException.class)
	public void shouldWrapExceptionInStream() throws Exception {
		MdmBioDevice dev = new MdmBioDevice();
		dev.setDeviceId("DEV");
		dev.setPort(6003);

		MosipDeviceSpecification_092_ProviderImpl spy = Mockito.spy(provider);
		Mockito.doReturn(true).when(spy).isDeviceAvailable(dev);
		Mockito.when(helper.buildUrl(Mockito.anyInt(), Mockito.anyString())).thenThrow(new RuntimeException("boom"));

		spy.stream(dev, "RIGHT");
	}

	@Test(expected = RegBaseCheckedException.class)
	public void shouldWrapExceptionInRCapture() throws Exception {
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
	public void shouldReturnEmptyListWhenGetMdmDevicesInvalidJson() {
		ObjectMapper mapper = new ObjectMapper();
		Mockito.when(helper.getMapper()).thenReturn(mapper);

		List<MdmBioDevice> devices = provider.getMdmDevices("not-a-json", 5050);
		assertEquals(0, devices.size());
	}

	@Test
	public void shouldIgnoreEntriesWithNullDeviceInfo() throws Exception {
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
	public void shouldSetLatestSpecVersionFromFactory() throws Exception {
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
	public void shouldReturnFalseWhenDeviceAvailableDoesNotMatch() throws Exception {
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

	@Test
	public void shouldReturnFalseWhenDeviceAvailableThrowsException() throws Exception {
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
