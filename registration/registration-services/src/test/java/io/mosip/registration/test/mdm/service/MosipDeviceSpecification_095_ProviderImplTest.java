package io.mosip.registration.test.mdm.service;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.mdm.spec_0_9_5.service.impl.MosipDeviceSpecification_095_ProviderImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Method;

@RunWith(SpringRunner.class)
public class MosipDeviceSpecification_095_ProviderImplTest {

    @InjectMocks
    private MosipDeviceSpecification_095_ProviderImpl mockObject;


    @Test
    public void testGetExceptions_withNullInput() throws Exception {
        Method method = MosipDeviceSpecification_095_ProviderImpl.class
                .getDeclaredMethod("getExceptions", String[].class);
        method.setAccessible(true);

        String[] result = (String[]) method.invoke(mockObject, new Object[]{null});

        Assert.assertNull(result);
    }

    @Test
    public void testGetExceptions_withEmptyArray() throws Exception {
        Method method = MosipDeviceSpecification_095_ProviderImpl.class
                .getDeclaredMethod("getExceptions", String[].class);
        method.setAccessible(true);

        String[] input = new String[0];
        String[] result = (String[]) method.invoke(mockObject, new Object[]{input});

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testGetExceptions_withValidValues() throws Exception {
        Method method = MosipDeviceSpecification_095_ProviderImpl.class
                .getDeclaredMethod("getExceptions", String[].class);
        method.setAccessible(true);

        String[] input = new String[]{"LEFT_THUMB", "RIGHT_THUMB"};

        String[] result = (String[]) method.invoke(mockObject, new Object[]{input});

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.length);

        Assert.assertNotEquals("LEFT_THUMB", result[0]);
        Assert.assertNotEquals("RIGHT_THUMB", result[1]);
    }

    private String invokeGetDeviceSubId(String modality) throws Exception {
        Method method = MosipDeviceSpecification_095_ProviderImpl.class
                .getDeclaredMethod("getDeviceSubId", String.class);
        method.setAccessible(true);
        return (String) method.invoke(mockObject, modality);
    }

    @Test
    public void testGetDeviceSubId_Left() throws Exception {
        String result = invokeGetDeviceSubId("LEFT_INDEX");
        Assert.assertEquals("1", result);
    }

    @Test
    public void testGetDeviceSubId_Right() throws Exception {
        String result = invokeGetDeviceSubId("RIGHT_INDEX");
        Assert.assertEquals("2", result);
    }

    @Test
    public void testGetDeviceSubId_Double() throws Exception {
        String result = invokeGetDeviceSubId("IRIS_DOUBLE");
        Assert.assertEquals("3", result);
    }

    @Test
    public void testGetDeviceSubId_Thumbs() throws Exception {
        String result = invokeGetDeviceSubId("FINGERPRINT_THUMBS");
        Assert.assertEquals("3", result);
    }

    @Test
    public void testGetDeviceSubId_Two() throws Exception {
        String result = invokeGetDeviceSubId("TWO_FINGERS");
        Assert.assertEquals("3", result);
    }

    @Test
    public void testGetDeviceSubId_Default() throws Exception {
        String result = invokeGetDeviceSubId("FACE");
        Assert.assertEquals("0", result);
    }

    @Test
    public void testGetDeviceSubId_CaseInsensitive() throws Exception {
        String result = invokeGetDeviceSubId("LeFt_ThUmB");
        Assert.assertEquals("1", result);
    }

    private int invokeGetDefaultCount(String modality) throws Exception {
        Method method = MosipDeviceSpecification_095_ProviderImpl.class
                .getDeclaredMethod("getDefaultCount", String.class);
        method.setAccessible(true);
        return (int) method.invoke(mockObject, modality);
    }

    @Test
    public void testGetDefaultCount_NullModality() throws Exception {
        int result = invokeGetDefaultCount(null);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testGetDefaultCount_FaceFullFace() throws Exception {
        int result = invokeGetDefaultCount(RegistrationConstants.FACE_FULLFACE);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testGetDefaultCount_IrisDouble() throws Exception {
        int result = invokeGetDefaultCount(RegistrationConstants.IRIS_DOUBLE);
        Assert.assertEquals(2, result);
    }

    @Test
    public void testGetDefaultCount_FingerprintSlabRight() throws Exception {
        int result = invokeGetDefaultCount(RegistrationConstants.FINGERPRINT_SLAB_RIGHT);
        Assert.assertEquals(4, result);
    }

    @Test
    public void testGetDefaultCount_FingerprintSlabLeft() throws Exception {
        int result = invokeGetDefaultCount(RegistrationConstants.FINGERPRINT_SLAB_LEFT);
        Assert.assertEquals(4, result);
    }

    @Test
    public void testGetDefaultCount_FingerprintSlabThumbs() throws Exception {
        int result = invokeGetDefaultCount(RegistrationConstants.FINGERPRINT_SLAB_THUMBS);
        Assert.assertEquals(2, result);
    }

    @Test
    public void testGetDefaultCount_UnknownModality() throws Exception {
        int result = invokeGetDefaultCount("UNKNOWN_MODALITY");
        Assert.assertEquals(1, result);
    }


}
