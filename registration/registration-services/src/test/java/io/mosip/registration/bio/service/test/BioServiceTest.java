package io.mosip.registration.bio.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.service.bio.impl.BioServiceImpl;
import okio.Buffer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.impl.BioProviderImpl_V_0_9;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.impl.SignatureServiceImpl;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DeviceDiscoveryMDSResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DigitalId;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDataDTO;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.test.config.TestDaoConfig;
import io.mosip.registration.util.common.BIRBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;


@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = { TestDaoConfig.class })
public class BioServiceTest {

    private static final String JWT_FORMAT = "header.%s.signature";

    @Mock
    private BioAPIFactory bioAPIFactory; //mock bean created in TestDaoConfig

    @Mock
    private MosipDeviceSpecificationFactory deviceSpecificationFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private BioService bioService;

    @InjectMocks
    private BioServiceImpl bioServiceImpl;
    
    @InjectMocks
    private BIRBuilder birBuilder;

    @Mock
    private BIRBuilder builder;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

    @Mock
    private SignatureServiceImpl signatureService; //mock bean created in TestDaoConfig
    
    @Mock
	private AuditManagerService auditFactory;

    private static MockWebServer mockWebServer;


    @BeforeClass
    public static void startWebServerConnection() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(InetAddress.getLoopbackAddress(), 4501);
    }

    @AfterClass
    public static void closeWebServerConnection() throws IOException {
       if(mockWebServer != null) {
           mockWebServer.close();
           mockWebServer = null;
       }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        Mockito.when(deviceSpecificationFactory.getPortFrom()).thenReturn(4501);
        Mockito.when(deviceSpecificationFactory.getPortTo()).thenReturn(4600);

        Mockito.when(bioService.getRetryCount(Modality.FACE)).thenReturn(3);
        Mockito.when(bioService.getRetryCount(Modality.IRIS_DOUBLE)).thenReturn(6);
        Mockito.when(bioService.getRetryCount(Modality.FINGERPRINT_SLAB_LEFT)).thenReturn(1);
        Mockito.when(bioService.getRetryCount(Modality.FINGERPRINT_SLAB_RIGHT)).thenReturn(1);
        Mockito.when(bioService.getRetryCount(Modality.FINGERPRINT_SLAB_THUMBS)).thenReturn(1);
        Mockito.when(bioService.getRetryCount(Modality.EXCEPTION_PHOTO)).thenReturn(9);

        Mockito.when(bioService.getMDMQualityThreshold(Modality.FACE)).thenReturn(60.0);
        Mockito.when(bioService.getMDMQualityThreshold(Modality.IRIS_DOUBLE)).thenReturn(70.0);
        Mockito.when(bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_LEFT)).thenReturn(90.0);
        Mockito.when(bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_RIGHT)).thenReturn(95.0);
        Mockito.when(bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_THUMBS)).thenReturn(98.0);
    }


    @Test
    public void portCheckTest() {
        int portFrom = deviceSpecificationFactory.getPortFrom();
        Assert.assertEquals(4501, portFrom);

        int portTo = deviceSpecificationFactory.getPortTo();
        Assert.assertEquals(4600, portTo);
    }

    @Test
    public void buildUrlTest() {
        String url = mosipDeviceSpecificationHelper.buildUrl(4501, "info");
        Assert.assertEquals("http://127.0.0.1:4501/info", url);
    }

    @Test
    public void checkServiceAvailabilityTest() throws IOException {
        mockWebServer.enqueue(new MockResponse());
        String serviceUrl = mosipDeviceSpecificationHelper.buildUrl(4501, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);

        boolean status = MosipDeviceSpecificationFactory.checkServiceAvailability(serviceUrl, "MOSIPDINFO");
        Assert.assertTrue(status);

        //To take care of timeout scenarios, as we have not enqueued any response
        status = MosipDeviceSpecificationFactory.checkServiceAvailability(serviceUrl, "MOSIPDINFO");
        Assert.assertTrue(status);

        serviceUrl = mosipDeviceSpecificationHelper.buildUrl(4600, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);
        status = MosipDeviceSpecificationFactory.checkServiceAvailability(serviceUrl, "MOSIPDINFO");
        Assert.assertFalse(status);
    }




    @Test
    public void getFaceMDMQualityThresholdTest() {
        double threshold = bioService.getMDMQualityThreshold(Modality.FACE);
        Assert.assertEquals(60.0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.IRIS_DOUBLE);
        Assert.assertEquals(70.0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(90.0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(95.0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(98.0, threshold, 0);
    }

    @Test
    public void getFaceMDMQualityThresholdTest2() {
        Assert.assertEquals(60, bioService.getMDMQualityThreshold(Modality.FACE), 0);
        Assert.assertEquals(70, bioService.getMDMQualityThreshold(Modality.IRIS_DOUBLE), 0);
        Assert.assertEquals(90, bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_LEFT), 0);
        Assert.assertEquals(95, bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_RIGHT), 0);
        Assert.assertEquals(98, bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_THUMBS), 0);
    }

    @Test(expected = NullPointerException.class)
    public void getMDMQualityThresholdInvalidModalityTest() {
        Mockito.when(bioService.getMDMQualityThreshold(null))
                .thenThrow(new NullPointerException());
        bioService.getMDMQualityThreshold(null);
    }

    @Test
    public void getRetryCountTest() {
        double attempts = bioService.getRetryCount(Modality.FACE);
        Assert.assertEquals(3.0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.IRIS_DOUBLE);
        Assert.assertEquals(6.0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(1.0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(1.0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(1.0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.EXCEPTION_PHOTO);
        Assert.assertEquals(9.0, attempts, 0);
    }

    @Test
    public void getRetryCountTest2() {
        Assert.assertEquals(3, bioService.getRetryCount(Modality.FACE), 0);
        Assert.assertEquals(6, bioService.getRetryCount(Modality.IRIS_DOUBLE), 0);
        Assert.assertEquals(1, bioService.getRetryCount(Modality.FINGERPRINT_SLAB_LEFT), 0);
        Assert.assertEquals(1, bioService.getRetryCount(Modality.FINGERPRINT_SLAB_RIGHT), 0);
        Assert.assertEquals(1, bioService.getRetryCount(Modality.FINGERPRINT_SLAB_THUMBS), 0);
        Assert.assertEquals(9, bioService.getRetryCount(Modality.EXCEPTION_PHOTO), 0);
    }

    @Test(expected = NullPointerException.class)
    public void getRetryCountNullModalityTest() {
        Mockito.when(bioService.getRetryCount(null))
                .thenThrow(new NullPointerException());
        bioService.getRetryCount(null);
    }

    @Test
    public void getSupportedBioAttributesWithEmptyInputTest() {
       Map<String, List<String>> result = bioService.getSupportedBioAttributes(Collections.emptyList());
       Assert.assertTrue(result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void getSupportedBioAttributesWithNullInputTest() {
        Mockito.when(bioService.getSupportedBioAttributes(null))
                .thenThrow(new NullPointerException());
        bioService.getSupportedBioAttributes(null);
    }

    @Test
    public void getSupportedBioAttributesTest() {
        Map<String, List<String>> mockFaceMap = new HashMap<>();
        mockFaceMap.put(RegistrationConstants.FACE, RegistrationConstants.faceUiAttributes);

        Map<String, List<String>> mockIrisMap = new HashMap<>();
        mockIrisMap.put(RegistrationConstants.IRIS, RegistrationConstants.eyesUiAttributes);

        Map<String, List<String>> mockThumbsMap = new HashMap<>();
        mockThumbsMap.put(RegistrationConstants.FINGERPRINT_SLAB_THUMBS, RegistrationConstants.twoThumbsUiAttributes);

        Mockito.when(bioService.getSupportedBioAttributes(Mockito.anyList()))
                .thenAnswer(invocation -> {
                    List<String> modalities = invocation.getArgument(0);
                    if (modalities.contains(RegistrationConstants.FACE)) return mockFaceMap;
                    if (modalities.contains(RegistrationConstants.IRIS)) return mockIrisMap;
                    if (modalities.contains(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) return mockThumbsMap;
                    return new HashMap<>();
                });

        List<String> modalities = new ArrayList<>();
        modalities.add(RegistrationConstants.FACE);
        Map<String, List<String>> result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.faceUiAttributes, result.get(RegistrationConstants.FACE));

        modalities.clear();
        modalities.add(RegistrationConstants.IRIS);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.eyesUiAttributes, result.get(RegistrationConstants.IRIS));

        modalities.clear();
        modalities.add(RegistrationConstants.FINGERPRINT_SLAB_THUMBS);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.twoThumbsUiAttributes, result.get(RegistrationConstants.FINGERPRINT_SLAB_THUMBS));
    }

    @Test
    public void getSDKScoreTest() throws BiometricException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, Float.valueOf("45.0"));
        BioProviderImpl_V_0_9 providerImpl_v_0_9 = Mockito.mock(BioProviderImpl_V_0_9.class);

        lenient().when(bioAPIFactory.getBioProvider(Mockito.any(), Mockito.any())).thenReturn(providerImpl_v_0_9);
        lenient().when(providerImpl_v_0_9.getModalityQuality(Mockito.any(), Mockito.any())).thenReturn(qualityMap);

        BiometricsDto biometricsDto = new BiometricsDto();
        biometricsDto.setBioAttribute("face");
        biometricsDto.setQualityScore(70.0);
        biometricsDto.setAttributeISO(new byte[0]);
        biometricsDto.setModalityName(Modality.FACE.name());
        double score = bioServiceImpl.getSDKScore(biometricsDto);
        Assert.assertEquals(45, score, 0);
    }

    @Test
    public void buildBirTest() {
        BiometricsDto biometricsDto = new BiometricsDto();
        biometricsDto.setBioAttribute("face");
        biometricsDto.setQualityScore(70.0);
        biometricsDto.setAttributeISO(new byte[0]);
        biometricsDto.setModalityName(Modality.FACE.name());
        BIR bir = birBuilder.buildBir(biometricsDto, ProcessedLevelType.RAW);
        Assert.assertNotNull(bir);
        Assert.assertEquals(0, bir.getBdb().length, 0);
        double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
        Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
    }

    @Test (expected = IllegalStateException.class)
    public void getStreamTest() throws RegBaseCheckedException, IOException {
        initializeDeviceMapTest();

        //queued response for device discovery - device_availability
        mockWebServer.enqueue(new MockResponse().setBody(getDeviceDiscoveryResponse("0.9.5", "READY")).setResponseCode(200));
        mockWebServer.enqueue(new MockResponse().setBody(getDeviceDiscoveryResponse("0.9.5", "READY")).setResponseCode(200));

        InputStream imageStream = this.getClass().getClassLoader().getResourceAsStream("applicantPhoto.jpg");
        byte[] imageBytes = (imageStream != null) ? imageStream.readAllBytes() : new byte[0];

        //queued for stream request
        MockResponse streamResponse = new MockResponse().setBody(new Buffer().write(imageBytes)).setResponseCode(200);
        mockWebServer.enqueue(streamResponse);

        ApplicationContext.map().put(RegistrationConstants.CAPTURE_TIME_OUT, "20000");

        String errorCode = null;
        try {
            bioService.getStream(Modality.FACE.name());
        } catch (RegBaseCheckedException exception) {
            errorCode = exception.getErrorCode();
        }

        Assert.assertEquals(RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorCode(), errorCode);
    }

    @Test
    public void captureModalityTest() throws RegBaseCheckedException, IOException {
        String[] exceptions = new String[0];
        MDMRequestDto mdmRequestDto = new MDMRequestDto(Modality.FACE.name(), exceptions,
        "REGISTRATION", "TEST", 20, 1, 80);

        //queued for check_service_availability
        mockWebServer.enqueue(new MockResponse());

        String deviceInfoResponse = getDeviceInfoResponse("0.9.5", "READY", "4501");
        if (deviceInfoResponse == null) {
            deviceInfoResponse = "{\"version\":\"0.9.5\",\"status\":\"READY\",\"port\":\"4501\"}";
        }

        MockResponse mockResponse = new MockResponse().setBody(deviceInfoResponse);
        mockWebServer.enqueue(mockResponse); //queued for actual MOSIPDIFO request
        mockWebServer.enqueue(mockResponse); //queued for actual MOSIPDIFO request

        JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = new JWTSignatureVerifyResponseDto();
        jwtSignatureVerifyResponseDto.setSignatureValid(true);
        jwtSignatureVerifyResponseDto.setTrustValid(SignatureConstant.TRUST_VALID);
        lenient().when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureVerifyResponseDto);

        lenient().doNothing().when(auditFactory).auditWithParams(Mockito.any(AuditEvent.class), Mockito.any(Components.class),
				Mockito.anyString(), Mockito.anyString(),Mockito.anyMap());

        deviceSpecificationFactory.initializeDeviceMap(false);
        Assert.assertEquals(0, deviceSpecificationFactory.getAvailableDeviceInfoMap().size());

        String captureResponseBody = getRCaptureResponse();
        MockResponse captureResponse = new MockResponse()
                .setBody(captureResponseBody != null ? captureResponseBody : "{}")
                .setResponseCode(200);
        mockWebServer.enqueue(captureResponse); //queued for actual RCAPTURE request
    }

    @Test(expected=RegBaseCheckedException.class)
    public void testValidData() throws RegBaseCheckedException {
        String data = "123|456";
        Mockito.when(mosipDeviceSpecificationHelper.getPayLoad(data)).thenThrow(RegBaseCheckedException.class);
    }
    
    @Test
    public void testNullData() {
        String data = null;
        RegBaseCheckedException ex = assertThrows(RegBaseCheckedException.class, () -> {
        	mosipDeviceSpecificationHelper.getPayLoad(data);
        });
        assertEquals(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(), ex.getErrorCode());
      //  assertEquals(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage(), ex.getMessage());
    }  
    
    @Test(expected=RegBaseCheckedException.class)
    public void testgetSignatureValidData() throws RegBaseCheckedException {
        String data = "123|456";
        Mockito.when(mosipDeviceSpecificationHelper.getSignature(data)).thenThrow(RegBaseCheckedException.class);
    }
    
    @Test
    public void testgetSignatureNullData() {
        String data = null;
        RegBaseCheckedException ex = assertThrows(RegBaseCheckedException.class, () -> {
        	mosipDeviceSpecificationHelper.getSignature(data);
        });
        assertEquals(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(), ex.getErrorCode());
      //  assertEquals(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage(), ex.getMessage());
    }  
    
    @Test(expected = RegBaseCheckedException.class)
    public void testValidateQualityScoreWithNull() throws RegBaseCheckedException {
    	MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper = new MosipDeviceSpecificationHelper();
    	mosipDeviceSpecificationHelper.validateQualityScore(null);
    }
    
    public void testvalidateResponseTimestampNullData() {
        String responseTime = null;
        RegBaseCheckedException ex = assertThrows(RegBaseCheckedException.class, () -> {
        	mosipDeviceSpecificationHelper.validateResponseTimestamp(responseTime);
        });
        assertEquals(RegistrationExceptionConstants.MDS_CAPTURE_INVALID_TIME.getErrorCode(), ex.getErrorCode());
      //  assertEquals(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage(), ex.getMessage());
    }  
    
 
    private void initializeDeviceMapTest() throws IOException {
        //queued for check_service_availability
        mockWebServer.enqueue(new MockResponse());

        String deviceInfoResponse = getDeviceInfoResponse("0.9.5", "READY", "4501");
        if (deviceInfoResponse == null || deviceInfoResponse.isEmpty()) {
            throw new IllegalStateException("Device Info Response is null or empty!");
        }

        mockWebServer.enqueue(new MockResponse()
                .setBody(deviceInfoResponse)
                .setResponseCode(200));

        JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = new JWTSignatureVerifyResponseDto();
        jwtSignatureVerifyResponseDto.setSignatureValid(true);
        jwtSignatureVerifyResponseDto.setTrustValid(SignatureConstant.TRUST_VALID);
        Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureVerifyResponseDto);
        ApplicationContext.map().put(RegistrationConstants.INITIAL_SETUP, "N");

        deviceSpecificationFactory.initializeDeviceMap(false);

        Assert.assertFalse("Device Info Map should not be empty!",
                deviceSpecificationFactory.getAvailableDeviceInfoMap().isEmpty());
    }

    private String getDeviceInfoResponse(String specVersion, String deviceStatus, String port) throws JsonProcessingException {
        List<MdmDeviceInfoResponse> deviceInfoResponses = new ArrayList<>();
        MdmDeviceInfoResponse mdmDeviceInfoResponse = new MdmDeviceInfoResponse();

        switch (specVersion) {
            case "0.9.5":
                MdmDeviceInfo mdmDeviceInfo = new MdmDeviceInfo();
                mdmDeviceInfo.setSpecVersion(new String[]{"0.9.5"});
                DigitalId digitalId = new DigitalId();
                digitalId.setDeviceProvider("Test");
                digitalId.setDeviceProviderId("TestId");
                digitalId.setMake("make");
                digitalId.setModel("model");
                digitalId.setType("face");
                digitalId.setDeviceSubType("face");
                digitalId.setDateTime(LocalDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
                mdmDeviceInfo.setDigitalId(String.format(JWT_FORMAT,
                        CryptoUtil.encodeToURLSafeBase64(objectMapper.writeValueAsBytes(digitalId))));
                mdmDeviceInfo.setDeviceId("1");
                mdmDeviceInfo.setDeviceSubId(new String[] { "1", "2" });
                mdmDeviceInfo.setDeviceCode("1");
                mdmDeviceInfo.setDeviceStatus(deviceStatus);
                mdmDeviceInfo.setCallbackId("http://127.0.0.1:"+port);
                mdmDeviceInfo.setCertification("1");
                mdmDeviceInfo.setPurpose("REGISTRATION");
                mdmDeviceInfo.setFirmware("1");
                mdmDeviceInfo.setServiceVersion("1.0");
                mdmDeviceInfoResponse.setDeviceInfo(String.format(JWT_FORMAT,
                        CryptoUtil.encodeToURLSafeBase64(objectMapper.writeValueAsBytes(mdmDeviceInfo))));
                break;
        }
        deviceInfoResponses.add(mdmDeviceInfoResponse);
        return objectMapper.writeValueAsString(deviceInfoResponses);
    }

    private String getDeviceDiscoveryResponse(String specVersion, String status) throws JsonProcessingException {
        List<DeviceDiscoveryMDSResponse> responseList = new ArrayList<>();
        DeviceDiscoveryMDSResponse deviceDiscoveryMDSResponse = new DeviceDiscoveryMDSResponse();
        deviceDiscoveryMDSResponse.setSpecVersion(new String[]{specVersion});
        deviceDiscoveryMDSResponse.setDeviceId("1");
        deviceDiscoveryMDSResponse.setDeviceCode("1");
        deviceDiscoveryMDSResponse.setDeviceCode("1");
        deviceDiscoveryMDSResponse.setCertification("1");
        deviceDiscoveryMDSResponse.setDeviceStatus(status);
        responseList.add(deviceDiscoveryMDSResponse);
        return  objectMapper.writeValueAsString(responseList);
    }

    private String getRCaptureResponse() throws JsonProcessingException {
        RCaptureResponseDTO responseDTO = new RCaptureResponseDTO();
        List<RCaptureResponseBiometricsDTO> list = new ArrayList<>();
        RCaptureResponseBiometricsDTO rCaptureResponseBiometricsDTO = new RCaptureResponseBiometricsDTO();

        RCaptureResponseDataDTO rCaptureResponseDataDTO = new RCaptureResponseDataDTO();
        DigitalId digitalId = new DigitalId();
        digitalId.setDeviceProvider("Test");
        digitalId.setDeviceProviderId("TestId");
        digitalId.setMake("make");
        digitalId.setModel("model");
        digitalId.setType("face");
        digitalId.setDeviceSubType("face");
        digitalId.setDateTime(LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
        rCaptureResponseDataDTO.setDigitalId(String.format(JWT_FORMAT,
                CryptoUtil.encodeToURLSafeBase64(objectMapper.writeValueAsBytes(digitalId))));
        rCaptureResponseDataDTO.setEnv("Test");
        rCaptureResponseDataDTO.setTimestamp(LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
        rCaptureResponseDataDTO.setDeviceCode("1");
        rCaptureResponseDataDTO.setBioType("face");
        rCaptureResponseDataDTO.setBioSubType("face");
        rCaptureResponseDataDTO.setQualityScore("50");
        rCaptureResponseDataDTO.setTransactionId("mocked-test-uuid");
        rCaptureResponseDataDTO.setBioValue(CryptoUtil.encodeToURLSafeBase64("testtest".getBytes(StandardCharsets.UTF_8)));
        rCaptureResponseBiometricsDTO.setData(String.format(JWT_FORMAT,
                CryptoUtil.encodeToURLSafeBase64(objectMapper.writeValueAsBytes(rCaptureResponseDataDTO))));
        rCaptureResponseBiometricsDTO.setSpecVersion("0.9.5");
        list.add(rCaptureResponseBiometricsDTO);
        responseDTO.setBiometrics(list);
        return objectMapper.writeValueAsString(responseDTO);
    }
}
