package io.mosip.registration.service.bio.quality;

import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.bio.quality.aggregator.FormulaScoreAggregator;
import io.mosip.registration.service.bio.quality.aggregator.MedianScoreAggregator;
import io.mosip.registration.service.bio.quality.aggregator.WeightedAverageScoreAggregator;
import io.mosip.registration.service.bio.quality.config.FormulaContext;
import io.mosip.registration.service.bio.quality.mock.MockVendor1Evaluator;
import io.mosip.registration.service.bio.quality.mock.MockVendor2Evaluator;
import io.mosip.registration.service.bio.quality.mock.MockVendor3Evaluator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Multi-Vendor SDK Evaluator Integration Tests using Standalone Mock Classes.
 *
 * <p>Simulates a realistic deployment where three named vendor SDK evaluators
 * ({@link MockVendor1Evaluator}, {@link MockVendor2Evaluator}, {@link MockVendor3Evaluator})
 * coexist alongside SBI.
 *
 * <p>Test scores returned by standalone files:
 * <pre>
 *   MockVendor1Evaluator (MOCK_VENDOR_1) → 60.0  (e.g. Neurotechnology finger SDK)
 *   MockVendor2Evaluator (MOCK_VENDOR_2) → 80.0  (e.g. Innovatrics palm SDK)
 *   MockVendor3Evaluator (MOCK_VENDOR_3) → 90.0  (e.g. Idemia iris SDK)
 * </pre>
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiVendorOrchestratorTest {

    // ── Instantiate standalone vendor evaluator classes ────────────────────
    private final IBiometricQualityEvaluator vendor1 = new MockVendor1Evaluator();
    private final IBiometricQualityEvaluator vendor2 = new MockVendor2Evaluator();
    private final IBiometricQualityEvaluator vendor3 = new MockVendor3Evaluator();

    // ── Real aggregator strategy implementations ────────────────────────────
    private final WeightedAverageScoreAggregator weightedAggregator = new WeightedAverageScoreAggregator();
    private final MedianScoreAggregator          medianAggregator   = new MedianScoreAggregator();
    private final FormulaScoreAggregator         formulaAggregator  = new FormulaScoreAggregator();

    @Mock
    private AuditManagerService auditFactory;

    @InjectMocks
    private BiometricQualityOrchestrator orchestrator;

    private BiometricsDto biometricsDto;

    @BeforeClass
    public static void initContext() {
        ApplicationContext.getInstance();
    }

    @Before
    public void setUp() {
        // Clean application config map before every test
        ReflectionTestUtils.setField(ApplicationContext.class, "applicationMap", new HashMap<String, Object>());

        // Inject all three vendor evaluators into the orchestrator
        ReflectionTestUtils.setField(orchestrator, "evaluators",
                Arrays.asList(vendor1, vendor2, vendor3));

        // Inject all three aggregator strategies into the orchestrator
        ReflectionTestUtils.setField(orchestrator, "aggregators",
                Arrays.asList(weightedAggregator, medianAggregator, formulaAggregator));

        biometricsDto = new BiometricsDto("leftIndex", new byte[]{1, 2, 3}, 75.0);

        // Always clear any leftover SpEL formula between tests
        FormulaContext.clear();
    }

    // helper
    private void putConfig(String key, String value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        ApplicationContext.setApplicationMap(m);
    }

    // =========================================================================
    // TEST 1: Three standalone vendors → WEIGHTED strategy
    // =========================================================================
    @Test
    public void testThreeVendorsWithWeightedAverageStrategy() throws RegBaseCheckedException {
        putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default",
                "MOCK_VENDOR_1, MOCK_VENDOR_2, MOCK_VENDOR_3");
        putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "WEIGHTED");

        // Set per-vendor weights
        putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.MOCK_VENDOR_1", "0.2");
        putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.MOCK_VENDOR_2", "0.3");
        putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.MOCK_VENDOR_3", "0.5");

        double result = orchestrator.orchestrate(biometricsDto);

        // (60*0.2 + 80*0.3 + 90*0.5) / 1.0 = 81.0
        assertEquals(81.0, result, 0.001);
    }

    // =========================================================================
    // TEST 2: Three standalone vendors → MEDIAN strategy
    // =========================================================================
    @Test
    public void testThreeVendorsWithMedianStrategy() throws RegBaseCheckedException {
        putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default",
                "MOCK_VENDOR_1, MOCK_VENDOR_2, MOCK_VENDOR_3");
        putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEDIAN");

        double result = orchestrator.orchestrate(biometricsDto);

        // Sorted [60, 80, 90] → middle element = 80.0
        assertEquals(80.0, result, 0.001);
    }

    // =========================================================================
    // TEST 3: Three standalone vendors → FORMULA strategy (SpEL)
    // =========================================================================
    @Test
    public void testThreeVendorsWithFormulaStrategy() throws RegBaseCheckedException {
        putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default",
                "MOCK_VENDOR_1, MOCK_VENDOR_2, MOCK_VENDOR_3");
        putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "FORMULA");

        FormulaContext.setFormula(
            "#scores['MOCK_VENDOR_1'] * 0.2 + #scores['MOCK_VENDOR_2'] * 0.3 + #scores['MOCK_VENDOR_3'] * 0.5"
        );

        double result = orchestrator.orchestrate(biometricsDto);

        // 60*0.2 + 80*0.3 + 90*0.5 = 81.0
        assertEquals(81.0, result, 0.001);
    }
}
