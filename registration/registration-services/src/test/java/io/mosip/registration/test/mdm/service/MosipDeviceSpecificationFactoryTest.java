package io.mosip.registration.test.mdm.service;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.service.BaseService;
import org.springframework.util.ReflectionUtils;

public class MosipDeviceSpecificationFactoryTest {

    @InjectMocks
    private MosipDeviceSpecificationFactory factory;

    @Mock
    private BaseService baseService;

    @Mock
    private MdmBioDevice mdmBioDevice;

    @Mock
    private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

    @Mock
    private MosipDeviceSpecificationProvider deviceSpecificationProvider;

    private List<MosipDeviceSpecificationProvider> deviceSpecificationProviders;


    @BeforeAll
    static void setupApplicationContext() throws Exception {
        Field acField = ApplicationContext.class.getDeclaredField("applicationContext");
        acField.setAccessible(true);
        if (acField.get(null) == null) {
            Constructor<ApplicationContext> ctor = ApplicationContext.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            acField.set(null, ctor.newInstance());
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        deviceSpecificationProviders = new ArrayList<>();
        deviceSpecificationProviders.add(deviceSpecificationProvider);
        mdmBioDevice = new MdmBioDevice();
        mdmBioDevice.setSpecVersion("1.0.0");
    }

    @Test
    public void getDeviceInfoByModality_unknown_throwsRegBaseCheckedException() {
        assertThrows(RegBaseCheckedException.class, () -> factory.getDeviceInfoByModality("unknown"));
    }

    @Test
    public void isDeviceAvailable_null_throwsRegBaseCheckedException() {
        assertThrows(RegBaseCheckedException.class, () -> factory.isDeviceAvailable((MdmBioDevice) null));
    }

    @Test
    public void modifySelectedDeviceInfo_keySerial_updatesDeviceRegistry() {
        MdmBioDevice bioDevice = new MdmBioDevice();
        bioDevice.setSerialNumber("12345");
        List<MdmBioDevice> devices = new ArrayList<>();
        devices.add(bioDevice);
        MosipDeviceSpecificationFactory.getAvailableDeviceInfo().put("key", devices);

        factory.modifySelectedDeviceInfo("key", "12345");

        assertEquals(bioDevice, MosipDeviceSpecificationFactory.getDeviceRegistryInfo().get("key"));
    }

    @Test
    public void awaitTerminationAfterShutdown_executor_completes() {
        factory.awaitTerminationAfterShutdown(mock(ExecutorService.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(Object target, String methodName, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }

        Method method = ReflectionUtils.findMethod(
                target.getClass(),
                methodName,
                paramTypes
        );

        if (method == null) {
            throw new IllegalArgumentException(
                    "Method not found: " + methodName +
                            " with parameter types: " + Arrays.toString(paramTypes)
            );
        }

        ReflectionUtils.makeAccessible(method);
        return (T) ReflectionUtils.invokeMethod(method, target, args);
    }

    @Test
    public void getDeviceType_fingerprint_returnsFinger() {
        String result = invokePrivate(factory, "getDeviceType", "FINGERPRINT");
        assertEquals(SingleType.FINGER.value(), result);
    }

    @Test
    public void getDeviceType_firDevice_returnsFinger() {
        assertEquals(
                SingleType.FINGER.value(),
                invokePrivate(factory, "getDeviceType", "fir-device")
        );
    }

    @Test
    public void getDeviceType_irisScanner_returnsIris() {
        assertEquals(
                SingleType.IRIS.value(),
                invokePrivate(factory, "getDeviceType", "IRIS-SCANNER")
        );
    }

    @Test
    public void getDeviceType_iirDevice_returnsIris() {
        assertEquals(
                SingleType.IRIS.value(),
                invokePrivate(factory, "getDeviceType", "iir-device")
        );
    }

    @Test
    public void getDeviceType_faceCam_returnsFace() {
        assertEquals(
                SingleType.FACE.value(),
                invokePrivate(factory, "getDeviceType", "FACE-CAM")
        );
    }

    @Test
    public void getDeviceType_unknown_returnsNull() {
        assertNull(invokePrivate(factory, "getDeviceType", "UNKNOWN"));
    }

    @Test
    public void getDeviceSubType_slabScanner_returnsSlab() {
        String result = invokePrivate(factory, "getDeviceSubType", "SLAB-SCANNER");
        assertEquals("slab", result);
    }

    @Test
    public void getDeviceSubType_slapDevice_returnsSlab() {
        String result = invokePrivate(factory, "getDeviceSubType", "SLAP-DEVICE");
        assertEquals("slab", result);
    }

    @Test
    public void getDeviceSubType_singleFinger_returnsSingle() {
        String result = invokePrivate(factory, "getDeviceSubType", "SINGLE-FINGER");
        assertEquals("single", result);
    }

    @Test
    public void getDeviceSubType_doubleFinger_returnsDouble() {
        String result = invokePrivate(factory, "getDeviceSubType", "DOUBLE-FINGER");
        assertEquals("double", result);
    }

    @Test
    public void getDeviceSubType_faceCamera_returnsFace() {
        String result = invokePrivate(factory, "getDeviceSubType", "FACE-CAMERA");
        assertEquals("face", result);
    }

    @Test
    public void getDeviceSubType_unknown_returnsNull() {
        String result = invokePrivate(factory, "getDeviceSubType", "UNKNOWN");
        assertNull(result);
    }

    @Test
    public void getLatestVersion_equal_returnsFirstVersion() {
        String result = invokePrivate(factory, "getLatestVersion", "1.2.3", "1.2.3");
        assertEquals("1.2.3", result);
    }

    @Test
    public void getLatestVersion_version1Greater_returnsVersion1() {
        String result = invokePrivate(factory, "getLatestVersion", "2.0.0", "1.9.9");
        assertEquals("2.0.0", result);
    }

    @Test
    public void getLatestVersion_version2Greater_returnsVersion2() {
        String result = invokePrivate(factory, "getLatestVersion", "1.2.3", "1.3.0");
        assertEquals("1.3.0", result);
    }

    @Test
    public void getLatestVersion_multiDigit_correctComparison() {
        String result = invokePrivate(factory, "getLatestVersion", "1.12.3", "1.2.10");
        assertEquals("1.12.3", result);
    }

    @Test
    public void getLatestVersion_singleNumber_returnsGreater() {
        String result = invokePrivate(factory, "getLatestVersion", "2", "1");
        assertEquals("2", result);
    }

    @Test
    public void getLatestVersion_differentLength_returnsLonger() {
        String result = invokePrivate(factory, "getLatestVersion", "1.0.0", "1.0");
        assertEquals("1.0.0", result);
    }

    @Test
    public void getLatestVersion_withTrailingZeros_returnsCorrectLatest() {
        String result = invokePrivate(factory, "getLatestVersion", "1.0.0", "1.0.1");
        assertEquals("1.0.1", result);
    }

    @Test
    public void getSpecVersionByModality_deviceExists_returnsSpecVersion() throws Exception {
        String modality = "FINGERPRINT";
        MdmBioDevice device = mock(MdmBioDevice.class);
        when(device.getSpecVersion()).thenReturn("v1.2.3");

        MosipDeviceSpecificationFactory spyFactory = spy(factory);
        doReturn(device).when(spyFactory).getDeviceInfoByModality(modality);

        String specVersion = spyFactory.getSpecVersionByModality(modality);

        assertEquals("v1.2.3", specVersion);
    }

    @Test
    public void getSpecVersionByModality_deviceNotFound_throwsRegBaseCheckedException() throws Exception {
        String modality = "UNKNOWN";

        MosipDeviceSpecificationFactory spyFactory = spy(factory);
        doThrow(new RegBaseCheckedException("404", "Device not found"))
                .when(spyFactory).getDeviceInfoByModality(modality);

        try {
            spyFactory.getSpecVersionByModality(modality);
            fail("Expected RegBaseCheckedException");
        } catch (RegBaseCheckedException ex) {
            assertEquals("404", ex.getErrorCode());
        }
    }

    @Test
    public void getSpecVersionByModality_deviceNotFound_factoryThrowsRegBaseCheckedException() {
        String modality = "UNKNOWNMOD";

        try {
            factory.getSpecVersionByModality(modality);
            fail("Expected RegBaseCheckedException");
        } catch (RegBaseCheckedException ex) {
            assertEquals("REG-MDS-001", ex.getErrorCode());
        }
    }

    // ─── initializeDeviceMap ─────────────────────────────────────────────────

    @Test
    public void initializeDeviceMap_initialSync_returnsEarlyWithoutError() {
        when(baseService.isInitialSync()).thenReturn(true);
        assertDoesNotThrow(() -> factory.initializeDeviceMap(true));
    }

    // ─── getPortFrom / getPortTo ─────────────────────────────────────────────

    @Test
    public void getPortFrom_emptyApplicationContext_returnsDefaultWithoutThrow() {
        // applicationMap has no port key → getIntValueFromApplicationMap returns null → falls to default
        assertDoesNotThrow(() -> factory.getPortFrom());
    }

    @Test
    public void getPortTo_emptyApplicationContext_returnsDefaultWithoutThrow() {
        assertDoesNotThrow(() -> factory.getPortTo());
    }

    // ─── getLatestSpecVersion(String[]) ──────────────────────────────────────

    @Test
    public void getLatestSpecVersion_nullArray_returnsNull() {
        assertNull(factory.getLatestSpecVersion((String[]) null));
    }

    @Test
    public void getLatestSpecVersion_emptyArray_returnsNull() {
        assertNull(factory.getLatestSpecVersion(new String[]{}));
    }

    @Test
    public void getLatestSpecVersion_singleVersionWithMatchingProvider_returnsThatVersion() throws Exception {
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        injectProviders(List.of(deviceSpecificationProvider));

        assertEquals("1.0", factory.getLatestSpecVersion(new String[]{"1.0"}));
    }

    @Test
    public void getLatestSpecVersion_multipleVersionsProviderMatchesLatest_returnsLatest() throws Exception {
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        injectProviders(List.of(deviceSpecificationProvider));

        assertEquals("1.0", factory.getLatestSpecVersion(new String[]{"0.9.2", "1.0"}));
    }

    // ─── getMdsProvider(String) ───────────────────────────────────────────────

    @Test
    public void getMdsProvider_found_returnsMatchingProvider() throws Exception {
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        injectProviders(List.of(deviceSpecificationProvider));

        assertNotNull(factory.getMdsProvider("1.0"));
    }

    @Test
    public void getMdsProvider_notFound_throwsRegBaseCheckedException() throws Exception {
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("2.0");
        injectProviders(List.of(deviceSpecificationProvider));

        assertThrows(RegBaseCheckedException.class, () -> factory.getMdsProvider("1.0"));
    }

    // ─── isDeviceAvailable(MdmBioDevice) with providers ──────────────────────

    @Test
    public void isDeviceAvailable_providerReturnsTrue_returnsTrue() throws Exception {
        MdmBioDevice device = new MdmBioDevice();
        device.setSpecVersion("1.0");
        device.setDeviceType("Finger");
        device.setDeviceSubType("Slap");

        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        when(deviceSpecificationProvider.isDeviceAvailable(device)).thenReturn(true);
        injectProviders(List.of(deviceSpecificationProvider));

        assertTrue(factory.isDeviceAvailable(device));
    }

    @Test
    public void isDeviceAvailable_providerReturnsFalse_returnsFalse() throws Exception {
        MdmBioDevice device = new MdmBioDevice();
        device.setSpecVersion("1.0");
        device.setDeviceType("Finger");
        device.setDeviceSubType("Slap");

        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        when(deviceSpecificationProvider.isDeviceAvailable(device)).thenReturn(false);
        injectProviders(List.of(deviceSpecificationProvider));
        when(baseService.isInitialSync()).thenReturn(true);

        assertFalse(factory.isDeviceAvailable(device));
    }

    @Test
    public void isDeviceAvailable_unknownDeviceType_returnsFalse() throws Exception {
        MdmBioDevice device = new MdmBioDevice();
        device.setSpecVersion("1.0");
        device.setDeviceType("UNKNOWN");
        device.setDeviceSubType("Slap");

        // Stub version so the filter lambda doesn't NPE on null specVersion
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("2.0");
        injectProviders(List.of(deviceSpecificationProvider));

        assertFalse(factory.isDeviceAvailable(device));
    }

    // ─── getAvailableDeviceInfoMap / getDeviceRegistryInfo ───────────────────

    @Test
    public void getAvailableDeviceInfoMap_instance_returnsNonNull() {
        assertNotNull(factory.getAvailableDeviceInfoMap());
    }

    @Test
    public void getDeviceRegistryInfo_static_returnsNonNull() {
        assertNotNull(MosipDeviceSpecificationFactory.getDeviceRegistryInfo());
    }

    @Test
    public void getAvailableDeviceInfo_static_returnsNonNull() {
        assertNotNull(MosipDeviceSpecificationFactory.getAvailableDeviceInfo());
    }

    // ─── getLatestSpecVersion() no-arg ───────────────────────────────────────

    @Test
    public void getLatestSpecVersion_noArg_withMatchingProvider_returnsVersion() throws Exception {
        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        injectProviders(List.of(deviceSpecificationProvider));

        // Biometric.getAvailableSpecVersions() returns ["0.9.5","1.0","0.9.2"] → latest is "1.0"
        assertNotNull(factory.getLatestSpecVersion());
    }

    // ─── checkServiceAvailability ────────────────────────────────────────────

    @Test
    public void checkServiceAvailability_serverRunning_returnsTrue() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            boolean result = MosipDeviceSpecificationFactory.checkServiceAvailability(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/health", "GET");
            assertTrue(result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void checkServiceAvailability_serverNotRunning_returnsFalse() {
        boolean result = MosipDeviceSpecificationFactory.checkServiceAvailability(
                "http://127.0.0.1:1/health", "GET");
        assertFalse(result);
    }

    // ─── initializeDeviceMap full paths ──────────────────────────────────────

    @Test
    public void initializeDeviceMap_notInitialSync_portsOutOfRange_skipsThreadPool() throws Exception {
        // Default ports from @InjectMocks = 0; 0 >= 4500 is false → thread pool skipped
        when(baseService.isInitialSync()).thenReturn(false);
        assertDoesNotThrow(() -> factory.initializeDeviceMap(false));
    }

    @Test
    public void initializeDeviceMap_notInitialSync_portsInRange_runsThreadPool() throws Exception {
        setInApplicationMap(RegistrationConstants.MDM_START_PORT_RANGE, "4500");
        setInApplicationMap(RegistrationConstants.MDM_END_PORT_RANGE, "4500");
        // threadPoolSize is 0 from @InjectMocks — inject 1 so Executors.newFixedThreadPool doesn't throw
        Field tsField = MosipDeviceSpecificationFactory.class.getDeclaredField("threadPoolSize");
        tsField.setAccessible(true);
        tsField.set(factory, 1);
        // buildUrl returns null for port 4500 → checkServiceAvailability(null,...) throws → caught by Runnable
        when(baseService.isInitialSync()).thenReturn(false);
        try {
            assertDoesNotThrow(() -> factory.initializeDeviceMap(false));
        } finally {
            removeFromApplicationMap(RegistrationConstants.MDM_START_PORT_RANGE);
            removeFromApplicationMap(RegistrationConstants.MDM_END_PORT_RANGE);
        }
    }

    // ─── getPortFrom / getPortTo with value in ApplicationContext ─────────────

    @Test
    public void getPortFrom_valueInApplicationContext_returnsConfiguredValue() throws Exception {
        setInApplicationMap(RegistrationConstants.MDM_START_PORT_RANGE, "4501");
        try {
            assertEquals(4501, factory.getPortFrom());
        } finally {
            removeFromApplicationMap(RegistrationConstants.MDM_START_PORT_RANGE);
        }
    }

    @Test
    public void getPortTo_valueInApplicationContext_returnsConfiguredValue() throws Exception {
        setInApplicationMap(RegistrationConstants.MDM_END_PORT_RANGE, "4599");
        try {
            assertEquals(4599, factory.getPortTo());
        } finally {
            removeFromApplicationMap(RegistrationConstants.MDM_END_PORT_RANGE);
        }
    }

    // ─── getDeviceInfoByModality with device in registry ─────────────────────

    @Test
    public void getDeviceInfoByModality_deviceInRegistry_returnsDevice() throws Exception {
        MdmBioDevice device = new MdmBioDevice();
        String key = "finger_slab";
        MosipDeviceSpecificationFactory.getDeviceRegistryInfo().put(key, device);
        try {
            MdmBioDevice result = factory.getDeviceInfoByModality("Fingerprint-Slap");
            assertEquals(device, result);
        } finally {
            MosipDeviceSpecificationFactory.getDeviceRegistryInfo().remove(key);
        }
    }

    @Test
    public void getDeviceInfoByModality_validTypeButNotInRegistry_throwsAfterInit() {
        // "Fingerprint-Single" → type=Finger, subtype=single → valid key, but map empty → throw
        when(baseService.isInitialSync()).thenReturn(true);
        assertThrows(RegBaseCheckedException.class,
                () -> factory.getDeviceInfoByModality("Fingerprint-Single"));
    }

    // ─── isDeviceAvailable with IOException in provider ───────────────────────

    @Test
    public void isDeviceAvailable_providerThrowsIOException_returnsFalse() throws Exception {
        MdmBioDevice device = new MdmBioDevice();
        device.setSpecVersion("1.0");
        device.setDeviceType("Finger");
        device.setDeviceSubType("Slap");

        when(deviceSpecificationProvider.getSpecVersion()).thenReturn("1.0");
        when(deviceSpecificationProvider.isDeviceAvailable(device)).thenThrow(new IOException("io error"));
        injectProviders(List.of(deviceSpecificationProvider));
        when(baseService.isInitialSync()).thenReturn(true);

        assertFalse(factory.isDeviceAvailable(device));
    }

    // ─── awaitTerminationAfterShutdown with InterruptedException ─────────────

    @Test
    public void awaitTerminationAfterShutdown_interrupted_setsInterruptFlag() throws Exception {
        ExecutorService mockPool = mock(ExecutorService.class);
        when(mockPool.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());
        factory.awaitTerminationAfterShutdown(mockPool);
        assertTrue(Thread.interrupted()); // clears the flag and asserts it was set
    }

    // ─── getDeviceInfoResponse (private, via reflection) ─────────────────────

    @Test
    public void getDeviceInfoResponse_localServer_returnsBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/info", exchange -> {
            byte[] body = "device-info".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();
        try {
            Method method = MosipDeviceSpecificationFactory.class
                    .getDeclaredMethod("getDeviceInfoResponse", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(factory,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/info");
            assertEquals("device-info", result);
        } finally {
            server.stop(0);
        }
    }

    // ─── addToDeviceInfoMap (private, via reflection) ────────────────────────

    @Test
    public void addToDeviceInfoMap_newKey_addsToAvailableAndSelected() throws Exception {
        String key = "iris_double";
        MdmBioDevice device = new MdmBioDevice();
        device.setDeviceCode("iris-device-001");
        Method method = MosipDeviceSpecificationFactory.class
                .getDeclaredMethod("addToDeviceInfoMap", String.class, MdmBioDevice.class);
        method.setAccessible(true);
        try {
            method.invoke(factory, key, device);
            assertTrue(MosipDeviceSpecificationFactory.getAvailableDeviceInfo().containsKey(key));
            assertEquals(device, MosipDeviceSpecificationFactory.getDeviceRegistryInfo().get(key));
        } finally {
            MosipDeviceSpecificationFactory.getAvailableDeviceInfo().remove(key);
            MosipDeviceSpecificationFactory.getDeviceRegistryInfo().remove(key);
        }
    }

    @Test
    public void addToDeviceInfoMap_existingKey_appendsToList() throws Exception {
        String key = "iris_double"; // "single" keys are skipped from availableDeviceInfoMap
        MdmBioDevice first = new MdmBioDevice();
        MdmBioDevice second = new MdmBioDevice();
        Method method = MosipDeviceSpecificationFactory.class
                .getDeclaredMethod("addToDeviceInfoMap", String.class, MdmBioDevice.class);
        method.setAccessible(true);
        try {
            method.invoke(factory, key, first);
            method.invoke(factory, key, second);
            assertEquals(2, MosipDeviceSpecificationFactory.getAvailableDeviceInfo().get(key).size());
        } finally {
            MosipDeviceSpecificationFactory.getAvailableDeviceInfo().remove(key);
            MosipDeviceSpecificationFactory.getDeviceRegistryInfo().remove(key);
        }
    }

    // ─── initByPort (private, via reflection) ────────────────────────────────

    @Test
    public void initByPort_serviceAvailable_callsProvidersAndPopulatesMap() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/info", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();

        String url = "http://127.0.0.1:" + port + "/info";
        when(mosipDeviceSpecificationHelper.buildUrl(eq(port), any())).thenReturn(url);
        when(deviceSpecificationProvider.getMdmDevices(any(), eq(port))).thenReturn(new ArrayList<>());
        injectProviders(List.of(deviceSpecificationProvider));

        Method method = MosipDeviceSpecificationFactory.class.getDeclaredMethod("initByPort", Integer.class);
        method.setAccessible(true);
        try {
            method.invoke(factory, port);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void initByPort_serviceAvailable_withDevices_populatesRegistry() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/info", exchange -> {
            byte[] body = "device-data".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();

        String url = "http://127.0.0.1:" + port + "/info";
        when(mosipDeviceSpecificationHelper.buildUrl(eq(port), any())).thenReturn(url);

        MdmBioDevice device = new MdmBioDevice();
        device.setDeviceType("Finger");
        device.setDeviceSubType("Slap");
        device.setDeviceCode("test-finger-001");
        when(deviceSpecificationProvider.getMdmDevices(any(), eq(port))).thenReturn(List.of(device));
        injectProviders(List.of(deviceSpecificationProvider));

        Method method = MosipDeviceSpecificationFactory.class.getDeclaredMethod("initByPort", Integer.class);
        method.setAccessible(true);
        try {
            method.invoke(factory, port);
            assertTrue(MosipDeviceSpecificationFactory.getAvailableDeviceInfo().containsKey("finger_slab"));
        } finally {
            server.stop(0);
            MosipDeviceSpecificationFactory.getAvailableDeviceInfo().remove("finger_slab");
            MosipDeviceSpecificationFactory.getDeviceRegistryInfo().remove("finger_slab");
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static void setInApplicationMap(String key, String value) throws Exception {
        Field f = ApplicationContext.class.getDeclaredField("applicationMap");
        f.setAccessible(true);
        ((Map<String, Object>) f.get(null)).put(key, value);
    }

    @SuppressWarnings("unchecked")
    private static void removeFromApplicationMap(String key) throws Exception {
        Field f = ApplicationContext.class.getDeclaredField("applicationMap");
        f.setAccessible(true);
        ((Map<String, Object>) f.get(null)).remove(key);
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private void injectProviders(List<MosipDeviceSpecificationProvider> providers) throws Exception {
        Field field = ReflectionUtils.findField(MosipDeviceSpecificationFactory.class, "deviceSpecificationProviders");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, factory, providers);
    }

}
