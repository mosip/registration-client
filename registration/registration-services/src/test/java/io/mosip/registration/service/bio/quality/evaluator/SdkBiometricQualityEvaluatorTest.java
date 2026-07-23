package io.mosip.registration.service.bio.quality.evaluator;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.impl.BioProviderImpl_V_0_9;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.common.BIRBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration-style unit test for {@link SdkBiometricQualityEvaluator}.
 *
 * <p>Uses the same mock pattern established in {@code BioServiceTest#getSDKScoreTest()}:
 * {@code BioAPIFactory} is mocked to return a controlled {@code BioProviderImpl_V_0_9},
 * which simulates the behaviour of any real or mock Bio-SDK without requiring
 * Java 21, network access, or physical biometric hardware.
 *
 * <p>The mock-sdk from https://github.com/mosip/mosip-mock-services/tree/master/mock-sdk
 * targets Java 21 and is not published to Maven Central. The approach here
 * accurately replicates its contract: {@code getModalityQuality(BIR[], null)}
 * returns a {@code Map<BiometricType, Float>} with a quality score per modality.
 */
@RunWith(MockitoJUnitRunner.class)
public class SdkBiometricQualityEvaluatorTest {

    // ── Mock the SDK factory (same approach as BioServiceTest.getSDKScoreTest) ──
    @Mock
    private BioAPIFactory bioAPIFactory;

    @Mock
    private BIRBuilder birBuilder;

    @InjectMocks
    private SdkBiometricQualityEvaluator evaluator;

    // Simulates BioProviderImpl_V_0_9 / any real SDK implementing iBioProviderApi
    private BioProviderImpl_V_0_9 mockSdkProvider;

    @Before
    public void setUp() throws Exception {
        // Manually create a Mockito mock of the SDK provider
        mockSdkProvider = Mockito.mock(BioProviderImpl_V_0_9.class);

        // Wire mock provider into the factory - same as BioServiceTest line 310
        when(bioAPIFactory.getBioProvider(any(BiometricType.class), any(BiometricFunction.class)))
                .thenReturn(mockSdkProvider);

        // BIRBuilder mock: return a minimal non-null BIR for any input
        when(birBuilder.buildBir(any(BiometricsDto.class), any(ProcessedLevelType.class)))
                .thenReturn(new BIR());
    }

    // =========================================================================
    // 1. HAPPY PATH - Quality scores for each major biometric modality
    // =========================================================================

    @Test
    public void testFaceQualityEvaluationReturnsMockSdkScore() throws RegBaseCheckedException {
        // Mock SDK returns 45.0 for FACE (mirrors BioServiceTest.getSDKScoreTest)
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 45.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        BiometricsDto dto = buildDto("face", Modality.FACE);

        QualityScore result = evaluator.evaluate(dto);

        assertNotNull(result);
        assertEquals(45.0, result.getScore(), 0.0);
    }

    @Test
    public void testFingerprintQualityEvaluationReturnsMockSdkScore() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FINGER, 78.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        BiometricsDto dto = buildDto("leftIndex", Modality.FINGERPRINT_SLAB_LEFT);

        QualityScore result = evaluator.evaluate(dto);

        assertNotNull(result);
        assertEquals(78.0, result.getScore(), 0.0);
    }

    @Test
    public void testIrisQualityEvaluationReturnsMockSdkScore() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.IRIS, 91.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        BiometricsDto dto = buildDto("leftEye", Modality.IRIS_DOUBLE);

        QualityScore result = evaluator.evaluate(dto);

        assertNotNull(result);
        assertEquals(91.0, result.getScore(), 0.0);
    }

    @Test
    public void testSdkReturnsHighQualityScore() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 100.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        QualityScore result = evaluator.evaluate(buildDto("face", Modality.FACE));

        assertEquals(100.0, result.getScore(), 0.0);
    }

    @Test
    public void testSdkReturnsZeroQualityScore() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 0.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        QualityScore result = evaluator.evaluate(buildDto("face", Modality.FACE));

        assertEquals(0.0, result.getScore(), 0.0);
    }

    @Test
    public void testSdkReturnsMidRangeScore() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FINGER, 55.5f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        QualityScore result = evaluator.evaluate(buildDto("leftIndex", Modality.FINGERPRINT_SLAB_LEFT));

        // float 55.5 cast to long = 55; getScore() returns double
        assertEquals(55.0, result.getScore(), 0.0);
    }

    // =========================================================================
    // 2. EVALUATOR USES BioAPIFactory WITH CORRECT PARAMETERS
    // =========================================================================

    @Test
    public void testEvaluatorCallsBioAPIFactoryWithQualityCheckFunction() throws Exception {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 60.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        evaluator.evaluate(buildDto("face", Modality.FACE));

        // Verify factory is called with QUALITY_CHECK function - same SDK call path as BioService
        verify(bioAPIFactory, times(1))
                .getBioProvider(any(BiometricType.class), eq(BiometricFunction.QUALITY_CHECK));
    }

    @Test
    public void testEvaluatorCallsBIRBuilderWithRawProcessedLevel() throws Exception {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 60.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        BiometricsDto dto = buildDto("face", Modality.FACE);
        evaluator.evaluate(dto);

        // Verify BIR is built using the RAW processed level
        verify(birBuilder, times(1)).buildBir(eq(dto), eq(ProcessedLevelType.RAW));
    }

    @Test
    public void testSdkProviderReceivesNullFlagsAsPerProtocol() throws Exception {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, 70.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        evaluator.evaluate(buildDto("face", Modality.FACE));

        // The SDK is called with null flags (second arg) per iBioProviderApi contract
        verify(mockSdkProvider, times(1)).getModalityQuality(any(BIR[].class), isNull());
    }

    // =========================================================================
    // 3. FAILURE SCENARIOS - SDK returns null or invalid
    // =========================================================================

    @Test(expected = RegBaseCheckedException.class)
    public void testNullQualityMapFromSdkThrowsException() throws RegBaseCheckedException {
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(null);

        evaluator.evaluate(buildDto("face", Modality.FACE));
    }

    @Test(expected = RegBaseCheckedException.class)
    public void testSdkReturnsNegativeScoreThrowsException() throws RegBaseCheckedException {
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FACE, -1.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        evaluator.evaluate(buildDto("face", Modality.FACE));
    }

    @Test(expected = RegBaseCheckedException.class)
    public void testScoreMapDoesNotContainRequestedModalityThrowsException() throws RegBaseCheckedException {
        // SDK returns a score for FINGER but we asked for FACE
        Map<BiometricType, Float> qualityMap = new HashMap<>();
        qualityMap.put(BiometricType.FINGER, 75.0f);
        when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

        evaluator.evaluate(buildDto("face", Modality.FACE));
    }

    @Test(expected = RegBaseCheckedException.class)
    public void testRuntimeExceptionFromSdkWrappedInRegBaseCheckedException()
            throws RegBaseCheckedException {
        // getModalityQuality() doesn't declare throws BiometricException so we simulate
        // a runtime crash from the SDK (e.g. NullPointerException, IllegalStateException)
        when(mockSdkProvider.getModalityQuality(any(), any()))
                .thenThrow(new RuntimeException("Simulated SDK runtime failure"));

        evaluator.evaluate(buildDto("face", Modality.FACE));
    }


    // =========================================================================
    // 4. EVALUATOR NAME CONTRACT
    // =========================================================================

    @Test
    public void testEvaluatorNameIsSDK() {
        assertEquals("SDK", evaluator.getEvaluatorName());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private BiometricsDto buildDto(String bioAttribute, Modality modality) {
        BiometricsDto dto = new BiometricsDto();
        dto.setBioAttribute(bioAttribute);
        dto.setQualityScore(70.0);
        dto.setAttributeISO(new byte[]{1, 2, 3});
        dto.setModalityName(modality.name());
        return dto;
    }
}
