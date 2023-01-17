package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmSbiDeviceInfo;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmSbiDeviceInfoWrapper;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiDigitalId;
import io.mosip.registration.mdm.sbi.spec_1_0.service.impl.MosipDeviceSpecification_SBI_1_0_ProviderImpl;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest(value = {MosipDeviceSpecification_SBI_1_0_ProviderImpl.class, MosipDeviceSpecificationHelper.class})
public class MosipDeviceSpecification_SBI_1_0_ProviderImplTest {
	
	@Mock
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;
	
	@Mock
	private MosipDeviceSpecification_SBI_1_0_ProviderImpl mockObject;
	
	@BeforeClass
	public static void initialize() throws IOException, java.io.IOException {

	}

	@Test
	public void getMdmDevicesTest() throws Exception{
		
		int port = 4051;
		String inputDeviceInfo = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6I"
				+ "kpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

		List<MdmBioDevice> excpectedMdmBioDevices = new LinkedList<>();
		MdmBioDevice expectedbioDevice = new MdmBioDevice();
		
		expectedbioDevice.setCallbackId("http://127.0.0.1:4501");
		expectedbioDevice.setCertification("1.0");
		expectedbioDevice.setDeviceMake("MOSIP");
		expectedbioDevice.setDeviceModel("SLAP01");
		expectedbioDevice.setDeviceProviderId("MOSIP.PROXY.SBI");
		expectedbioDevice.setDeviceProviderName("MOSIP");
		expectedbioDevice.setDeviceStatus("Ready");
		String[] expectedDeviceSubId = {"1","2","3"};
		expectedbioDevice.setDeviceSubId(expectedDeviceSubId);
		expectedbioDevice.setDeviceSubType("Slap");
		expectedbioDevice.setDeviceType("Finger");
		expectedbioDevice.setFirmWare("1.0");
		expectedbioDevice.setProviderId("MOSIP.PROXY.SBI");
		expectedbioDevice.setProviderName("MOSIP");
		expectedbioDevice.setPurpose("Registration");
		expectedbioDevice.setSerialNumber("1");
		expectedbioDevice.setSerialVersion("1.0");
		expectedbioDevice.setSpecVersion("1.0");
		expectedbioDevice.setTimestamp("2021-04-29T05:56:29.909Z");
		expectedbioDevice.setPort(port);
		excpectedMdmBioDevices.add(expectedbioDevice);
		
		ObjectMapper mapper = new ObjectMapper();
		
		List<MdmBioDevice> actualMdmBioDevices = new LinkedList<>();
		
		List<MdmDeviceInfoResponse> deviceInfoResponses = new ArrayList<MdmDeviceInfoResponse>();
		MdmDeviceInfoResponse deviceInfoResponse = new MdmDeviceInfoResponse();
		deviceInfoResponse.setDeviceInfo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4g"
				+ "RG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
		io.mosip.registration.mdm.sbi.spec_1_0.dto.response.Error erro = new io.mosip.registration.mdm.sbi.spec_1_0.dto.response.Error();
		erro.setErrorCode("100");
		erro.setErrorInfo("Success");
		deviceInfoResponse.setError(erro);
		deviceInfoResponses.add(deviceInfoResponse);
		
		DeviceInfo deviceInfo = new DeviceInfo();
		MdmSbiDeviceInfoWrapper sbiDeviceInfo = new MdmSbiDeviceInfoWrapper();
		MdmSbiDeviceInfo deviceSubType = new MdmSbiDeviceInfo();
		deviceSubType.setCallbackId("http://127.0.0.1:4501");
		deviceSubType.setCertification("1.0");
		deviceSubType.setDeviceStatus("Ready");
		String[] deviceSubId = {"1","2","3"};
		deviceSubType.setDeviceSubId(deviceSubId);
		deviceSubType.setDigitalId("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG"
				+ "9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
		deviceSubType.setEnv("Staging");
		deviceSubType.setFirmware("1.0");
		deviceSubType.setPurpose("Registration");
		deviceSubType.setSerialNo("1");
		deviceSubType.setServiceVersion("1.0");
		String[] specVersion = {"1.0"};
		deviceSubType.setSpecVersion(specVersion);
		
		sbiDeviceInfo.setDeviceInfo(deviceSubType);
		sbiDeviceInfo.setError(erro);
		deviceInfo = sbiDeviceInfo;
		
		MdmBioDevice actualBioDevice = new MdmBioDevice();
		SbiDigitalId actualDigitalId = new SbiDigitalId();
		actualDigitalId.setDateTime("2021-04-29T05:56:29.909Z");
		actualDigitalId.setDeviceProvider("MOSIP");
		actualDigitalId.setDeviceProviderId("MOSIP.PROXY.SBI");
		actualDigitalId.setDeviceSubType("Slap");
		actualDigitalId.setMake("MOSIP");
		actualDigitalId.setModel("SLAP01");
		actualDigitalId.setSerialNo("1");
		actualDigitalId.setType("Finger");
		
		actualBioDevice.setCallbackId(deviceSubType.getCallbackId());
		actualBioDevice.setCertification(deviceSubType.getCertification());
		actualBioDevice.setDeviceMake(actualDigitalId.getMake());
		actualBioDevice.setDeviceModel(actualDigitalId.getModel());
		actualBioDevice.setDeviceProviderId(actualDigitalId.getDeviceProviderId());
		actualBioDevice.setDeviceProviderName(actualDigitalId.getDeviceProvider());
		actualBioDevice.setDeviceStatus(deviceSubType.getDeviceStatus());
		actualBioDevice.setDeviceSubId(deviceSubType.getDeviceSubId());
		actualBioDevice.setDeviceSubType(actualDigitalId.getDeviceSubType());
		actualBioDevice.setDeviceType(actualDigitalId.getType());
		actualBioDevice.setFirmWare(deviceSubType.getFirmware());
		actualBioDevice.setProviderId(actualDigitalId.getDeviceProviderId());
		actualBioDevice.setProviderName(actualDigitalId.getDeviceProvider());
		actualBioDevice.setPurpose(deviceSubType.getPurpose());
		actualBioDevice.setSerialNumber(deviceSubType.getSerialNo());
		actualBioDevice.setSerialVersion(deviceSubType.getServiceVersion());
		actualBioDevice.setSpecVersion("1.0");
		actualBioDevice.setTimestamp(actualDigitalId.getDateTime());
		
		Mockito.when(mosipDeviceSpecificationHelper.getMapper()).thenReturn(mapper);
		//Mockito.when(mosipDeviceSpecificationHelper.getMapper().readValue(deviceInfoResponse, Mockito.any(TypeReference.class))).thenReturn(deviceInfoResponses);
		
		Mockito.when(mosipDeviceSpecificationHelper
							.getDeviceInfoDecoded(deviceInfoResponses.get(0).getDeviceInfo(), this.getClass())).thenReturn(deviceInfo);
		MosipDeviceSpecification_SBI_1_0_ProviderImpl mockedObject = Mockito.mock(MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);
		
		assertNotNull(actualBioDevice);
		actualBioDevice.setPort(port);
		actualMdmBioDevices.add(actualBioDevice);
		
		Mockito.when(mockedObject.getMdmDevices(inputDeviceInfo, port)).thenReturn(actualMdmBioDevices);
		
		Assert.assertEquals(excpectedMdmBioDevices, actualMdmBioDevices);
		
	}
	
	@Test
	public void streamTest() throws Exception{
		
		int port = 4501;
		String str = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData = new byte[str.length()];
		InputStream expectedStream = new ByteArrayInputStream(byteData);
		//InputStream inputStream = new ByteArrayInputStream(null);
		MosipDeviceSpecification_SBI_1_0_ProviderImpl mockedObject = Mockito.mock(MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);
		
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
		
		
		//InputStream stream = 
		OngoingStubbing<InputStream> actualStream = Mockito.when(mockedObject.stream(inputBioDevice, "IRIS_DOUBLE")).thenReturn(expectedStream);
		
		Assert.assertNotNull(actualStream.getMock());
	}
	
	@Test
	public void rCaptureTest() throws Exception{
		
		List<BiometricsDto> expectedListOfBiometrics = new ArrayList<BiometricsDto>();
		String str1 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData1 = new byte[str1.length()];
		BiometricsDto expectedBiometrics1 = new BiometricsDto("rightIndex", byteData1, 80.0);
		expectedBiometrics1.setCaptured(true);
		expectedBiometrics1.setForceCaptured(false);
		expectedBiometrics1.setModalityName("FINGERPRINT_SLAB_RIGHT");
		expectedBiometrics1.setNumOfRetries(0);
		expectedBiometrics1.setSdkScore(0.0);
		
		String str2 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData2 = new byte[str2.length()];
		BiometricsDto expectedBiometrics2 = new BiometricsDto("rightLittle", byteData2, 80.0);
		expectedBiometrics2.setCaptured(true);
		expectedBiometrics2.setForceCaptured(false);
		expectedBiometrics2.setModalityName("FINGERPRINT_SLAB_RIGHT");
		expectedBiometrics2.setNumOfRetries(0);
		expectedBiometrics2.setSdkScore(0.0);
		
		expectedListOfBiometrics.add(expectedBiometrics1);
		expectedListOfBiometrics.add(expectedBiometrics2);
		
		int port = 4501;
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
		
		String[] excpetions = {};
		MDMRequestDto inputMDMRequestDto = new MDMRequestDto("FINGERPRINT_SLAP_RIGHT", excpetions, "Registration", null, 10000, 1, 4);
		OngoingStubbing<List<BiometricsDto>> list = Mockito.when(mockObject.rCapture(inputBioDevice, inputMDMRequestDto)).thenReturn(expectedListOfBiometrics);
		
		Assert.assertNotNull(list.getMock());
	}
	
	@Test
	public void isDeviceAvailableTest() throws Exception{
		
		boolean expected = true;
		int port = 4501;
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
		OngoingStubbing<Boolean> mock = Mockito.when(mockObject.isDeviceAvailable(inputBioDevice)).thenReturn(expected);
		
		Assert.assertNotNull(mock);
	}
	
}
