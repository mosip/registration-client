package io.mosip.registration.bio.service.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.impl.BioProviderImpl_V_0_9;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.impl.SignatureServiceImpl;
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
import io.mosip.registration.mdm.spec_0_9_5.dto.response.*;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.test.config.TestDaoConfig;
import io.mosip.registration.util.common.BIRBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestDaoConfig.class })
public class BioServiceTest {

    private static final String JWT_FORMAT = "header.%s.signature";

    @Autowired
    private BioAPIFactory bioAPIFactory; //mock bean created in TestDaoConfig

    @Autowired
    private MosipDeviceSpecificationFactory deviceSpecificationFactory;

    @Autowired
    private IdentitySchemaService identitySchemaService;

    @Autowired
    private BioService bioService;
    
    @Autowired
    private BIRBuilder birBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

    @Autowired
    private SignatureServiceImpl signatureService; //mock bean created in TestDaoConfig

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
    public void init() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void portCheckTest() {
        int portFrom = deviceSpecificationFactory.getPortFrom();
        Assert.assertEquals(4501, portFrom);

        int portTo = deviceSpecificationFactory.getPortTo();
        Assert.assertEquals(4600, portTo);

        ApplicationContext.map().put(RegistrationConstants.MDM_START_PORT_RANGE, "4500");
        ApplicationContext.map().put(RegistrationConstants.MDM_END_PORT_RANGE, "4501");

        portFrom = deviceSpecificationFactory.getPortFrom();
        Assert.assertEquals(4500, portFrom);

        portTo = deviceSpecificationFactory.getPortTo();
        Assert.assertEquals(4501, portTo);
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
        Assert.assertFalse(status);

        serviceUrl = mosipDeviceSpecificationHelper.buildUrl(4600, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);
        status = MosipDeviceSpecificationFactory.checkServiceAvailability(serviceUrl, "MOSIPDINFO");
        Assert.assertFalse(status);
    }




    @Test
    public void getFaceMDMQualityThresholdTest() {
        double threshold = bioService.getMDMQualityThreshold(Modality.FACE);
        Assert.assertEquals(0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.IRIS_DOUBLE);
        Assert.assertEquals(0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(0, threshold, 0);

        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(0, threshold, 0);
    }

    @Test
    public void getFaceMDMQualityThresholdTest2() {
        ApplicationContext.map().put(RegistrationConstants.FACE_THRESHOLD, "60");
        ApplicationContext.map().put(RegistrationConstants.IRIS_THRESHOLD, "70");
        ApplicationContext.map().put(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD, "90");
        ApplicationContext.map().put(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD, "95");
        ApplicationContext.map().put(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD, "98");

        double threshold = bioService.getMDMQualityThreshold(Modality.FACE);
        Assert.assertEquals(60, threshold, 0);
        threshold = bioService.getMDMQualityThreshold(Modality.IRIS_DOUBLE);
        Assert.assertEquals(70, threshold, 0);
        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(90, threshold, 0);
        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(95, threshold, 0);
        threshold = bioService.getMDMQualityThreshold(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(98, threshold, 0);

        ApplicationContext.map().remove(RegistrationConstants.FACE_THRESHOLD);
        ApplicationContext.map().remove(RegistrationConstants.IRIS_THRESHOLD);
        ApplicationContext.map().remove(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
        ApplicationContext.map().remove(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
        ApplicationContext.map().remove(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
    }

    @Test(expected = NullPointerException.class)
    public void getMDMQualityThresholdInvalidModalityTest() {
        bioService.getMDMQualityThreshold(null);
    }

    @Test
    public void getRetryCountTest() {
        double attempts = bioService.getRetryCount(Modality.FACE);
        Assert.assertEquals(0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.IRIS_DOUBLE);
        Assert.assertEquals(0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(0, attempts, 0);

        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(0, attempts, 0);
    }

    @Test
    public void getRetryCountTest2() {
        ApplicationContext.map().put(RegistrationConstants.FACE_RETRY_COUNT, "3");
        ApplicationContext.map().put(RegistrationConstants.IRIS_RETRY_COUNT, "6");
        ApplicationContext.map().put(RegistrationConstants.FINGERPRINT_RETRIES_COUNT, "1");
        ApplicationContext.map().put(RegistrationConstants.PHOTO_RETRY_COUNT, "9");

        double attempts = bioService.getRetryCount(Modality.FACE);
        Assert.assertEquals(3, attempts, 0);
        attempts = bioService.getRetryCount(Modality.IRIS_DOUBLE);
        Assert.assertEquals(6, attempts, 0);
        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(1, attempts, 0);
        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(1, attempts, 0);
        attempts = bioService.getRetryCount(Modality.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(1, attempts, 0);
        attempts = bioService.getRetryCount(Modality.EXCEPTION_PHOTO);
        Assert.assertEquals(9, attempts, 0);

        ApplicationContext.map().remove(RegistrationConstants.FACE_RETRY_COUNT);
        ApplicationContext.map().remove(RegistrationConstants.IRIS_RETRY_COUNT);
        ApplicationContext.map().remove(RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
        ApplicationContext.map().remove(RegistrationConstants.PHOTO_RETRY_COUNT);
    }

    @Test(expected = NullPointerException.class)
    public void getRetryCountNullModalityTest() {
        bioService.getRetryCount(null);
    }

    @Test
    public void getSupportedBioAttributesWithEmptyInputTest() {
       Map result = bioService.getSupportedBioAttributes(Collections.EMPTY_LIST);
       Assert.assertTrue(result.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void getSupportedBioAttributesWithNullInputTest() {
        bioService.getSupportedBioAttributes(null);
    }

    @Test
    public void getSupportedBioAttributesTest() {
        List<String> modalities = new ArrayList<>();
        modalities.add("test");
        Map result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertTrue(result.isEmpty());
        modalities.clear();

        modalities.add(RegistrationConstants.FACE);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.faceUiAttributes, result.get(RegistrationConstants.FACE));
        modalities.clear();
        modalities.add(RegistrationConstants.FACE_FULLFACE);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.faceUiAttributes, result.get(RegistrationConstants.FACE_FULLFACE));
        modalities.clear();

        modalities.add(RegistrationConstants.IRIS);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.eyesUiAttributes, result.get(RegistrationConstants.IRIS));
        modalities.clear();
        modalities.add(RegistrationConstants.IRIS_DOUBLE);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.eyesUiAttributes, result.get(RegistrationConstants.IRIS_DOUBLE));
        modalities.clear();

        modalities.add(RegistrationConstants.FINGERPRINT_SLAB_THUMBS);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.twoThumbsUiAttributes, result.get(RegistrationConstants.FINGERPRINT_SLAB_THUMBS));
        modalities.clear();
        modalities.add(RegistrationConstants.FINGERPRINT_SLAB_RIGHT);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.rightHandUiAttributes, result.get(RegistrationConstants.FINGERPRINT_SLAB_RIGHT));
        modalities.clear();
        modalities.add(RegistrationConstants.FINGERPRINT_SLAB_LEFT);
        result = bioService.getSupportedBioAttributes(modalities);
        Assert.assertEquals(RegistrationConstants.leftHandUiAttributes, result.get(RegistrationConstants.FINGERPRINT_SLAB_LEFT));
        modalities.clear();
    }

    @Test
    public void getSDKScoreTest() throws BiometricException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, Float.valueOf("45.0"));
        BioProviderImpl_V_0_9 providerImpl_v_0_9 = Mockito.mock(BioProviderImpl_V_0_9.class);

        Mockito.when(bioAPIFactory.getBioProvider(Mockito.any(), Mockito.any())).thenReturn(providerImpl_v_0_9);
        Mockito.when(providerImpl_v_0_9.getModalityQuality(Mockito.any(), Mockito.any())).thenReturn(qualityMap);

        BiometricsDto biometricsDto = new BiometricsDto();
        biometricsDto.setBioAttribute("face");
        biometricsDto.setQualityScore(70.0);
        biometricsDto.setAttributeISO(new byte[0]);
        biometricsDto.setModalityName(Modality.FACE.name());
        double score = bioService.getSDKScore(biometricsDto);
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

    @Test
    public void getStreamTest() throws RegBaseCheckedException, IOException {
        initializeDeviceMapTest();

        //queued response for device discovery - device_availability
        MockResponse deviceDiscoveryResponse1 = new MockResponse();
        deviceDiscoveryResponse1.setBody(getDeviceDiscoveryResponse("0.9.5", "READY"));
        mockWebServer.enqueue(deviceDiscoveryResponse1); //queued for actual MOSIPDISC request
        //queued response for device discovery - device_availability
        MockResponse deviceDiscoveryResponse2 = new MockResponse();
        deviceDiscoveryResponse2.setBody(getDeviceDiscoveryResponse("0.9.5", "READY"));
        mockWebServer.enqueue(deviceDiscoveryResponse2); //queued for actual MOSIPDISC request

        //queued for stream request
        MockResponse streamResponse = new MockResponse();
        Buffer image = ByteBuffer.wrap(this.getClass().getClassLoader().getResourceAsStream("applicantPhoto.jpg").readAllBytes());
        streamResponse.setBody(String.valueOf(image));
        mockWebServer.enqueue(streamResponse);

        ApplicationContext.map().put(RegistrationConstants.CAPTURE_TIME_OUT, "20000");

        String errorCode = null;
        long timeStart = System.currentTimeMillis();
        long timeEnd = 0;
        try {
            bioService.getStream(Modality.FACE.name());
        } catch (RegBaseCheckedException exception) {
            timeEnd = System.currentTimeMillis();
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

        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(getDeviceInfoResponse("0.9.5", "READY", "4501"));
        mockWebServer.enqueue(mockResponse); //queued for actual MOSIPDIFO request
        mockWebServer.enqueue(mockResponse); //queued for actual MOSIPDIFO request

        JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = new JWTSignatureVerifyResponseDto();
        jwtSignatureVerifyResponseDto.setSignatureValid(true);
        jwtSignatureVerifyResponseDto.setTrustValid(SignatureConstant.TRUST_VALID);
        Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureVerifyResponseDto);
        ApplicationContext.map().put(RegistrationConstants.INITIAL_SETUP, "N");

        deviceSpecificationFactory.initializeDeviceMap(false);
        Assert.assertEquals(1, deviceSpecificationFactory.getAvailableDeviceInfoMap().size());

        MockResponse captureResponse = new MockResponse();
        captureResponse.setBody(getRCaptureResponse());
        mockWebServer.enqueue(captureResponse); //queued for actual RCAPTURE request
        
        String errorCode = null;
        try {
            bioService.captureModality(mdmRequestDto);
        } catch (RegBaseCheckedException e) {
            errorCode = e.getCause() != null ? ((RegBaseCheckedException)e.getCause()).getErrorCode() : e.getErrorCode();
        }
        Assert.assertEquals("REG-MDS-003", errorCode);
    }

    private void initializeDeviceMapTest() throws IOException {
        //queued for check_service_availability
        mockWebServer.enqueue(new MockResponse());

        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(getDeviceInfoResponse("0.9.5", "READY", "4501"));
        mockWebServer.enqueue(mockResponse); //queued for actual MOSIPDIFO request

        JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = new JWTSignatureVerifyResponseDto();
        jwtSignatureVerifyResponseDto.setSignatureValid(true);
        jwtSignatureVerifyResponseDto.setTrustValid(SignatureConstant.TRUST_VALID);
        Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureVerifyResponseDto);
        ApplicationContext.map().put(RegistrationConstants.INITIAL_SETUP, "N");

        deviceSpecificationFactory.initializeDeviceMap(false);
        Assert.assertEquals(1, deviceSpecificationFactory.getAvailableDeviceInfoMap().size());
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
                mdmDeviceInfo.setDeviceSubId(new int[] { 1, 2 });
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
