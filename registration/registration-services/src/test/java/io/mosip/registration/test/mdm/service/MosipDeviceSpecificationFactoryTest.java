package io.mosip.registration.test.mdm.service;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.service.BaseService;
import org.springframework.beans.factory.annotation.Value;
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


}
