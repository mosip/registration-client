package io.mosip.registration.test.mdm.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.DeviceException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import io.mosip.registration.mdm.sbi.spec_1_0.service.impl.MosipDeviceSpecification_SBI_1_0_ProviderImpl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for MosipDeviceSpecificationHelper.
 * Uses MockitoJUnitRunner (not PowerMock) so JaCoCo coverage is captured correctly.
 * Real Base64-encoded payloads are used instead of mocking CryptoUtil.
 */
@RunWith(MockitoJUnitRunner.class)
public class MosipDeviceSpecificationHelperTest {

    @InjectMocks
    private MosipDeviceSpecificationHelper helper;

    @Mock
    private SignatureService signatureService;

    /** base64url("{}") with padding — used as JWT payload for decode tests */
    private static final String B64_EMPTY_JSON = Base64.getUrlEncoder().encodeToString(
            "{}".getBytes(StandardCharsets.UTF_8));   // "e30="

    /** Full JWT whose payload decodes to "{}" */
    private static final String JWT_EMPTY = "header." + B64_EMPTY_JSON + ".signature";

    @BeforeClass
    public static void setupApplicationContext() throws Exception {
        // Make applicationContext non-null so ApplicationContext.map() doesn't NPE
        Field acField = ApplicationContext.class.getDeclaredField("applicationContext");
        acField.setAccessible(true);
        if (acField.get(null) == null) {
            Constructor<ApplicationContext> ctor =
                    ApplicationContext.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            acField.set(null, ctor.newInstance());
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ─── getPayLoad ────────────────────────────────────────────────────────────

    @Test(expected = RegBaseCheckedException.class)
    public void getPayLoad_null_throwsException() throws Exception {
        helper.getPayLoad(null);
    }

    @Test(expected = RegBaseCheckedException.class)
    public void getPayLoad_empty_throwsException() throws Exception {
        helper.getPayLoad("");
    }

    @Test
    public void getPayLoad_validJWT_returnsPayload() throws Exception {
        assertEquals("payload", helper.getPayLoad("header.payload.signature"));
    }

    @Test(expected = RegBaseCheckedException.class)
    public void getPayLoad_singleDot_throwsMdsPayloadEmpty() throws Exception {
        helper.getPayLoad("headerOnly.nodot");
    }

    // ─── getSignature ──────────────────────────────────────────────────────────

    @Test(expected = RegBaseCheckedException.class)
    public void getSignature_null_throwsException() throws Exception {
        helper.getSignature(null);
    }

    @Test(expected = RegBaseCheckedException.class)
    public void getSignature_empty_throwsException() throws Exception {
        helper.getSignature("");
    }

    @Test
    public void getSignature_validJWT_returnsHeaderDotDotSignature() throws Exception {
        assertEquals("header..signature", helper.getSignature("header.payload.signature"));
    }

    @Test(expected = RegBaseCheckedException.class)
    public void getSignature_singleDot_throwsMdsSignatureEmpty() throws Exception {
        helper.getSignature("headerOnly.nodot");
    }

    // ─── validateJWTResponse ───────────────────────────────────────────────────

    @Test(expected = DeviceException.class)
    public void validateJWTResponse_signatureInvalid_throwsDeviceException() throws Exception {
        JWTSignatureVerifyResponseDto resp = new JWTSignatureVerifyResponseDto();
        resp.setSignatureValid(false);
        when(signatureService.jwtVerify(any())).thenReturn(resp);

        helper.validateJWTResponse("some.jwt.data", "DEVICE");
    }

    @Test(expected = DeviceException.class)
    public void validateJWTResponse_trustInvalid_throwsDeviceException() throws Exception {
        JWTSignatureVerifyResponseDto resp = new JWTSignatureVerifyResponseDto();
        resp.setSignatureValid(true);
        resp.setTrustValid("INVALID_TRUST");
        when(signatureService.jwtVerify(any())).thenReturn(resp);

        helper.validateJWTResponse("some.jwt.data", "DEVICE");
    }

    @Test
    public void validateJWTResponse_valid_doesNotThrow() throws Exception {
        JWTSignatureVerifyResponseDto resp = new JWTSignatureVerifyResponseDto();
        resp.setSignatureValid(true);
        resp.setTrustValid(SignatureConstant.TRUST_VALID);
        when(signatureService.jwtVerify(any())).thenReturn(resp);

        helper.validateJWTResponse("some.jwt.data", "DEVICE");
    }

    // ─── getDeviceInfoDecoded ─────────────────────────────────────────────────

    @Test
    public void getDeviceInfoDecoded_exceptionInValidation_returnsNull() {
        when(signatureService.jwtVerify(any())).thenThrow(new RuntimeException("error"));
        assertNull(helper.getDeviceInfoDecoded(JWT_EMPTY, String.class));
    }

    @Test
    public void getDeviceInfoDecoded_sbi10Class_returnsMdmSbiWrapper() {
        setupValidSignature();
        // JWT_EMPTY payload decodes to "{}" → MdmSbiDeviceInfoWrapper with null fields
        DeviceInfo result = helper.getDeviceInfoDecoded(
                JWT_EMPTY, MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);
        assertNotNull(result);
    }

    @Test
    public void getDeviceInfoDecoded_nonSbiClass_returnsMdmDeviceInfo() {
        setupValidSignature();
        // String.class is not a supertype of SBI_1_0 → MdmDeviceInfo branch
        DeviceInfo result = helper.getDeviceInfoDecoded(JWT_EMPTY, String.class);
        assertNotNull(result);
    }

    // ─── generateMDMTransactionId ─────────────────────────────────────────────

    @Test
    public void generateMDMTransactionId_returnsNonNullUUID() {
        String id = helper.generateMDMTransactionId();
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    // ─── buildUrl ─────────────────────────────────────────────────────────────

    @Test
    public void buildUrl_validPortAndEndpoint_returnsFormattedUrl() {
        assertEquals("http://127.0.0.1:4501/info", helper.buildUrl(4501, "info"));
    }

    // ─── getMapper ────────────────────────────────────────────────────────────

    @Test
    public void getMapper_returnsObjectMapper() {
        assertNotNull(helper.getMapper());
    }

    // ─── validateQualityScore ─────────────────────────────────────────────────

    @Test(expected = RegBaseCheckedException.class)
    public void validateQualityScore_null_throwsException() throws Exception {
        helper.validateQualityScore(null);
    }

    @Test(expected = RegBaseCheckedException.class)
    public void validateQualityScore_empty_throwsException() throws Exception {
        helper.validateQualityScore("");
    }

    @Test
    public void validateQualityScore_valid_doesNotThrow() throws Exception {
        helper.validateQualityScore("80.0");
    }

    // ─── getMDMConnectionTimeout ──────────────────────────────────────────────

    @Test
    public void getMDMConnectionTimeout_allNull_returnsDefault10000() throws Exception {
        withApplicationMap(map -> {
            map.remove(String.format(RegistrationConstants.METHOD_BASED_MDM_CONNECTION_TIMEOUT, "MOSIPDINFO"));
            map.remove(RegistrationConstants.MDM_CONNECTION_TIMEOUT);
            assertEquals(10000, MosipDeviceSpecificationHelper.getMDMConnectionTimeout("MOSIPDINFO"));
        });
    }

    @Test
    public void getMDMConnectionTimeout_methodSpecificSet_returnsMethodValue() throws Exception {
        withApplicationMap(map -> {
            String key = String.format(RegistrationConstants.METHOD_BASED_MDM_CONNECTION_TIMEOUT, "TESTMTD");
            map.put(key, "3000");
            assertEquals(3000, MosipDeviceSpecificationHelper.getMDMConnectionTimeout("testmtd"));
        });
    }

    @Test
    public void getMDMConnectionTimeout_generalTimeoutSet_returnsGeneralValue() throws Exception {
        withApplicationMap(map -> {
            map.put(RegistrationConstants.MDM_CONNECTION_TIMEOUT, "7000");
            assertEquals(7000, MosipDeviceSpecificationHelper.getMDMConnectionTimeout("MOSIPDINFO"));
        });
    }

    // ─── validateResponseTimestamp ────────────────────────────────────────────

    @Test(expected = RegBaseCheckedException.class)
    public void validateResponseTimestamp_null_throwsException() throws Exception {
        helper.validateResponseTimestamp(null);
    }

    @Test
    public void validateResponseTimestamp_currentUtcTime_doesNotThrow() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String responseTime = sdf.format(new java.util.Date());

        withApplicationMap(map -> {
            map.put(RegistrationConstants.MDS_RESP_ALLOWED_LAG_MINS, "5");
            helper.validateResponseTimestamp(responseTime);
        });
    }

    @Test(expected = RegBaseCheckedException.class)
    public void validateResponseTimestamp_pastTimeOutsideWindow_throwsException() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MINUTE, -10);
        String responseTime = sdf.format(cal.getTime());

        withApplicationMap(map -> {
            map.put(RegistrationConstants.MDS_RESP_ALLOWED_LAG_MINS, "5");
            helper.validateResponseTimestamp(responseTime);
        });
    }

    // ─── getJPEGByteArray ─────────────────────────────────────────────────────

    @Test
    public void getJPEGByteArray_validStream_returnsImageBytesWithFfPrefix() throws Exception {
        // Header: at least one char before "Content-Length:" so indexOf() > 0
        // Body: 0x00 is skipped, 0xFF signals JPEG start, then 5 image bytes
        byte[] header = "\r\nContent-Length:5\n".getBytes(StandardCharsets.US_ASCII);
        byte[] body = new byte[]{0x00, (byte) 0xFF, 0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] stream = new byte[header.length + body.length];
        System.arraycopy(header, 0, stream, 0, header.length);
        System.arraycopy(body, 0, stream, header.length, body.length);

        byte[] result = helper.getJPEGByteArray(
                new ByteArrayInputStream(stream), System.currentTimeMillis() + 10_000);

        assertNotNull(result);
        assertEquals(6, result.length);
        assertEquals((byte) 0xFF, result[0]);
    }

    @Test(expected = RegBaseCheckedException.class)
    public void getJPEGByteArray_timeoutExpired_throwsRegBaseCheckedException() throws Exception {
        // maxTimeLimit = 0 → System.currentTimeMillis() > 0 is always true → timeout
        helper.getJPEGByteArray(new ByteArrayInputStream(new byte[]{'X', 'Y', 'Z'}), 0L);
    }

    // ─── getHttpClientResponseEntity ─────────────────────────────────────────

    @Test
    public void getHttpClientResponseEntity_localServer_returnsResponseBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/mdm", exchange -> {
            byte[] body = "response-body".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();
        try {
            String result = helper.getHttpClientResponseEntity(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/mdm",
                    "POST", "{}");
            assertNotNull(result);
            assertEquals("response-body", result);
        } finally {
            server.stop(0);
        }
    }

    // ─── getHttpClientResponse ────────────────────────────────────────────────

    @Test
    public void getHttpClientResponse_localServer_returnsCloseableResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/stream", exchange -> {
            byte[] body = "stream-data".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();
        try (CloseableHttpResponse response = helper.getHttpClientResponse(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/stream",
                "POST", "{}")) {
            assertNotNull(response);
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            server.stop(0);
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void setupValidSignature() {
        JWTSignatureVerifyResponseDto resp = new JWTSignatureVerifyResponseDto();
        resp.setSignatureValid(true);
        resp.setTrustValid(SignatureConstant.TRUST_VALID);
        when(signatureService.jwtVerify(any())).thenReturn(resp);
    }

    @FunctionalInterface
    interface MapOp {
        void apply(Map<String, Object> map) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private void withApplicationMap(MapOp op) throws Exception {
        Field f = ApplicationContext.class.getDeclaredField("applicationMap");
        f.setAccessible(true);
        Map<String, Object> map = (Map<String, Object>) f.get(null);
        Map<String, Object> snapshot = new java.util.HashMap<>(map);
        try {
            op.apply(map);
        } finally {
            map.clear();
            map.putAll(snapshot);
        }
    }
}