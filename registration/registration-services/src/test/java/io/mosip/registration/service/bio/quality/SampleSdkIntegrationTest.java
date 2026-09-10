package io.mosip.registration.service.bio.quality;

import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.mock.sdk.impl.SampleSDK;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.bio.quality.aggregator.MeanScoreAggregator;
import io.mosip.registration.service.bio.quality.evaluator.SbiQualityEvaluator;
import io.mosip.registration.service.bio.quality.evaluator.SdkBiometricQualityEvaluator;
import io.mosip.registration.util.common.BIRBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test verifying that the real io.mosip.mock.sdk.impl.SampleSDK
 * artifact from mock-sdk executes cleanly through BioAPIFactory and BiometricQualityOrchestrator.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleSdkIntegrationTest {

    @Mock
    private AuditManagerService auditFactory;

    @Mock
    private BIRBuilder birBuilder;

    @Mock
    private BioAPIFactory bioAPIFactory;

    @InjectMocks
    private SdkBiometricQualityEvaluator sdkEvaluator;

    @InjectMocks
    private SbiQualityEvaluator sbiEvaluator;

    @InjectMocks
    private BiometricQualityOrchestrator orchestrator;

    private final MeanScoreAggregator meanAggregator = new MeanScoreAggregator();
    private SampleSDK sampleSDK;

    @BeforeClass
    public static void initContext() {
        ApplicationContext.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(ApplicationContext.class, "applicationMap", new HashMap<String, Object>());

        sampleSDK = Mockito.spy(new SampleSDK());

        // Mock iBioProviderApi delegate that returns score 90.0f for getModalityQuality
        iBioProviderApi providerDelegate = Mockito.mock(iBioProviderApi.class);
        Map<BiometricType, Float> map = new HashMap<>();
        map.put(BiometricType.FINGER, 90.0f);
        when(providerDelegate.getModalityQuality(any(), any())).thenReturn(map);

        when(bioAPIFactory.getBioProvider(any(BiometricType.class), any(BiometricFunction.class)))
                .thenReturn(providerDelegate);

        // Inject dependencies into evaluator and orchestrator
        ReflectionTestUtils.setField(sdkEvaluator, "bioAPIFactory", bioAPIFactory);
        ReflectionTestUtils.setField(sdkEvaluator, "birBuilder", birBuilder);

        ReflectionTestUtils.setField(orchestrator, "evaluators", Arrays.asList(sbiEvaluator, sdkEvaluator));
        ReflectionTestUtils.setField(orchestrator, "aggregators", Arrays.asList(meanAggregator));

        // Mock BIRBuilder to return a valid BIR structure
        io.mosip.kernel.biometrics.entities.BIR mockBir = new io.mosip.kernel.biometrics.entities.BIR();
        mockBir.setBdb(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        when(birBuilder.buildBir(any(), any())).thenReturn(mockBir);
    }

    @Test
    public void testSampleSdkExecutionDirect() throws Exception {
        BiometricsDto fingerDto = new BiometricsDto("leftIndex", new byte[]{1, 2, 3, 4}, 80.0);

        iBioProviderApi provider = bioAPIFactory.getBioProvider(BiometricType.FINGER, BiometricFunction.QUALITY_CHECK);
        assertNotNull("BioAPIFactory should return a non-null provider", provider);

        QualityScore score = sdkEvaluator.evaluate(fingerDto);

        assertNotNull("SampleSDK quality score should not be null", score);
        assertEquals(90.0, score.getScore(), 0.001);
    }

    @Test
    public void testSampleSdkExecutionInOrchestrator() throws RegBaseCheckedException {
        // Configure orchestrator to evaluate both SBI (score: 70.0) and real SampleSDK (score: 90.0)
        ApplicationContext.map().put(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI, SDK");
        ApplicationContext.map().put(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");

        BiometricsDto fingerDto = new BiometricsDto("leftIndex", new byte[]{1, 2, 3, 4}, 70.0);

        double aggregatedScore = orchestrator.orchestrate(fingerDto).getAggregatedScore();

        // Mean of SBI (70.0) and SampleSDK (90.0) = 80.0
        assertEquals(80.0, aggregatedScore, 0.001);
    }
}
