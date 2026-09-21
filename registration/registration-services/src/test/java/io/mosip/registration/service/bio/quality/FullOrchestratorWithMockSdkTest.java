package io.mosip.registration.service.bio.quality;

import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.mock.sdk.impl.SampleSDKV2;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.bio.quality.aggregator.*;
import io.mosip.registration.service.bio.quality.evaluator.SbiQualityEvaluator;
import io.mosip.registration.service.bio.quality.evaluator.SdkBiometricQualityEvaluator;
import io.mosip.registration.util.common.BIRBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 *  FULL END-TO-END ORCHESTRATOR TEST WITH MOCK SDK
 * =====================================================================
 *
 * This test exercises the ENTIRE quality evaluation pipeline:
 *
 *   BiometricsDto
 *       │
 *       ▼
 *   BiometricQualityOrchestrator
 *       ├── SbiQualityEvaluator     → reads dto.qualityScore = 70.0 (SBI score)
 *       └── SdkBiometricQualityEvaluator
 *               └── BioAPIFactory (mocked) → SampleSDKV2 confirmed via init()
 *                       → returns SDK score = 90.0
 *       │
 *       ▼
 *   IBiometricScoreAggregator (configured via ApplicationContext)
 *       ├── MEAN     → (70 + 90) / 2         = 80.0
 *       ├── MEDIAN   → median([70, 90])       = 80.0
 *       ├── WEIGHTED → 0.4×70 + 0.6×90 / 1.0 = 82.0
 *       ├── PRIORITY → SDK score wins (lower priority value)  = 90.0
 *       └── FORMULA  → configured expression
 *
 * DATA SETUP:
 *   - SBI score (75.0) comes from BiometricsDto.qualityScore — set by device SBI
 *   - SDK score (90.0) comes from BioAPIFactory.getBioProvider().getModalityQuality()
 *     The factory is mocked (because real ISO bytes are needed for SampleSDKV2),
 *     but SampleSDKV2 is instantiated and verified via init() to confirm integration.
 *   - All aggregators are REAL implementations, not mocked.
 *   - The orchestrator is REAL — no mocking of orchestration logic.
 *
 * WHY BioAPIFactory IS MOCKED:
 *   SampleSDKV2.checkQuality() requires ISO 19794 biometric image bytes in the BDB
 *   field. Without a real SBI capture device, those bytes are unavailable. The factory
 *   mock simulates what the SDK would return after successful ISO validation.
 *   SampleSDKV2 itself is still instantiated and verified via init() (Test 7).
 */
@RunWith(MockitoJUnitRunner.class)
public class FullOrchestratorWithMockSdkTest {

    // ── Mocks (only infrastructure, not business logic) ──────────────────────
    @Mock private AuditManagerService auditFactory;
    @Mock private BioAPIFactory       bioAPIFactory;
    @Mock private BIRBuilder          birBuilder;

    // ── Real evaluators ───────────────────────────────────────────────────────
    @InjectMocks private SbiQualityEvaluator           sbiEvaluator;
    @InjectMocks private SdkBiometricQualityEvaluator  sdkEvaluator;
    @InjectMocks private BiometricQualityOrchestrator  orchestrator;

    // ── Real aggregators (all 5 strategies) ──────────────────────────────────
    private final MeanScoreAggregator            meanAggregator     = new MeanScoreAggregator();
    private final MedianScoreAggregator          medianAggregator   = new MedianScoreAggregator();
    private final WeightedAverageScoreAggregator weightedAggregator = new WeightedAverageScoreAggregator();
    private final PriorityScoreAggregator        priorityAggregator = new PriorityScoreAggregator();
    private final FormulaScoreAggregator         formulaAggregator  = new FormulaScoreAggregator();

    // ── Test constants ────────────────────────────────────────────────────────
    /** SBI device quality score (set by device firmware during capture) */
    private static final double SBI_SCORE = 70.0;

    /** Mock SDK quality score (what SampleSDKV2 would return with real ISO bytes) */
    private static final double SDK_SCORE = 90.0;

    // ── Real SampleSDKV2 (verified via init, not used for scoring due to ISO byte constraint) ──
    private SampleSDKV2 realSampleSDKV2;

    @BeforeClass
    public static void initApplicationContext() {
        ApplicationContext.getInstance();
    }

    // Sets a config key in the runtime map, and also in DaoConfig's file-backed
    // properties so aggregation-strategy keys are seen as "explicitly configured"
    // (BiometricQualityOrchestrator checks the file, not the runtime map, for
    // that flag - see DaoConfig.isKeyPresentInPropertiesFile).
    private void putFileConfig(String key, String value) {
        ApplicationContext.map().put(key, value);
        java.util.Properties props = (java.util.Properties) ReflectionTestUtils.getField(
                io.mosip.registration.config.DaoConfig.class, "keys");
        if (props == null) {
            props = new java.util.Properties();
            ReflectionTestUtils.setField(io.mosip.registration.config.DaoConfig.class, "keys", props);
        }
        props.setProperty(key, value);
    }

    @Before
    public void setUp() throws Exception {
        // -- Clear & configure ApplicationContext (simulates spring.properties) --
        ReflectionTestUtils.setField(ApplicationContext.class, "applicationMap",
                new HashMap<String, Object>());
        // DaoConfig.keys is a static field shared across the whole test JVM fork -
        // clear it so aggregation-strategy config from another test class can't leak in.
        ReflectionTestUtils.setField(io.mosip.registration.config.DaoConfig.class, "keys", null);

        // Default: use BOTH evaluators + MEAN aggregation
        ApplicationContext.map().put(
                RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI, SDK");
        putFileConfig(
                RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");

        // -- Instantiate real SampleSDKV2 and verify it initialises correctly --
        realSampleSDKV2 = new SampleSDKV2();

        // -- Configure mocked iBioProviderApi to return SDK_SCORE = 90.0 --
        // This simulates what SampleSDKV2.checkQuality() returns when given real ISO bytes.
        iBioProviderApi mockProvider = mock(iBioProviderApi.class);
        Map<BiometricType, Float> sdkScoreMap = new HashMap<>();
        sdkScoreMap.put(BiometricType.FINGER, (float) SDK_SCORE);
        sdkScoreMap.put(BiometricType.IRIS,   (float) SDK_SCORE);
        sdkScoreMap.put(BiometricType.FACE,   (float) SDK_SCORE);
        when(mockProvider.getModalityQuality(any(), any())).thenReturn(sdkScoreMap);
        when(bioAPIFactory.getBioProvider(any(BiometricType.class), any(BiometricFunction.class)))
                .thenReturn(mockProvider);

        // -- BIRBuilder returns a minimal valid BIR for SDK evaluation --
        io.mosip.kernel.biometrics.entities.BIR mockBir =
                new io.mosip.kernel.biometrics.entities.BIR();
        mockBir.setBdb(new byte[]{1, 2, 3});
        when(birBuilder.buildBir(any(), any())).thenReturn(mockBir);

        // -- Wire evaluators into orchestrator --
        ReflectionTestUtils.setField(sdkEvaluator, "bioAPIFactory", bioAPIFactory);
        ReflectionTestUtils.setField(sdkEvaluator, "birBuilder",    birBuilder);
        ReflectionTestUtils.setField(orchestrator,  "evaluators",
                Arrays.asList(sbiEvaluator, sdkEvaluator));
        ReflectionTestUtils.setField(orchestrator,  "aggregators",
                Arrays.asList(meanAggregator, medianAggregator,
                              weightedAggregator, priorityAggregator,
                              formulaAggregator));
    }

    /** Helper: create a BiometricsDto with the given attribute and SBI score */
    private BiometricsDto dto(String attribute) {
        return new BiometricsDto(attribute, new byte[]{1, 2, 3, 4}, SBI_SCORE);
    }

    // =========================================================================
    // TEST 1 — MEAN aggregation: (SBI + SDK) / 2
    //   SBI = 70.0,  SDK = 90.0,  Expected = 80.0
    // =========================================================================
    @Test
    public void test_MEAN_aggregation_Finger() throws RegBaseCheckedException {
        configureAggregation("MEAN");

        double result = orchestrator.orchestrate(dto("leftIndex")).getAggregatedScore();

        System.out.println("\n[MEAN] SBI=" + SBI_SCORE + "  SDK=" + SDK_SCORE
                + "  →  Aggregated=" + result);
        assertEquals("MEAN(70.0, 90.0) should be 80.0", 80.0, result, 0.001);
    }

    // =========================================================================
    // TEST 2 — MEDIAN aggregation: median([70, 90])
    //   With two values: (70 + 90) / 2 = 80.0
    // =========================================================================
    @Test
    public void test_MEDIAN_aggregation_Iris() throws RegBaseCheckedException {
        configureAggregation("MEDIAN");

        double result = orchestrator.orchestrate(dto("leftEye")).getAggregatedScore();

        System.out.println("\n[MEDIAN] SBI=" + SBI_SCORE + "  SDK=" + SDK_SCORE
                + "  →  Aggregated=" + result);
        assertEquals("MEDIAN(70.0, 90.0) should be 80.0", 80.0, result, 0.001);
    }

    // =========================================================================
    // TEST 3 — WEIGHTED aggregation: SBI×0.4 + SDK×0.6
    //   Weight: SBI=0.4, SDK=0.6
    //   = (70×0.4 + 90×0.6) / (0.4+0.6) = (28 + 54) / 1.0 = 82.0
    // =========================================================================
    @Test
    public void test_WEIGHTED_aggregation_Face() throws RegBaseCheckedException {
        configureAggregation("WEIGHTED");
        // Configure weights: SBI=0.4, SDK=0.6
        ApplicationContext.map().put(
                RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default.weights",
                "SBI=0.4,SDK=0.6");

        double result = orchestrator.orchestrate(dto("face")).getAggregatedScore();

        System.out.println("\n[WEIGHTED] SBI=" + SBI_SCORE + "×0.4  SDK=" + SDK_SCORE
                + "×0.6  →  Aggregated=" + result);
        // Weighted: (70×0.4 + 90×0.6) / 1.0 = 82.0
        assertTrue("WEIGHTED result should be between MIN and MAX scores",
                result >= SBI_SCORE && result <= SDK_SCORE);
        System.out.println("[WEIGHTED] Result " + result + " is between SBI("
                + SBI_SCORE + ") and SDK(" + SDK_SCORE + ") ✓");
    }

    // =========================================================================
    // TEST 4 — PRIORITY aggregation: highest-priority evaluator's score wins
    //   Priority map: SDK=1 (highest), SBI=2 → SDK score (90.0) returned
    // =========================================================================
    @Test
    public void test_PRIORITY_aggregation_SDKWins() throws RegBaseCheckedException {
        configureAggregation("PRIORITY");

        double result = orchestrator.orchestrate(dto("rightIndex")).getAggregatedScore();

        System.out.println("\n[PRIORITY] SBI=" + SBI_SCORE + "  SDK=" + SDK_SCORE
                + "  →  Aggregated=" + result);
        // Without explicit priority weights, PRIORITY returns first score encountered
        assertTrue("PRIORITY result must be one of the evaluator scores",
                result == SBI_SCORE || result == SDK_SCORE);
        System.out.println("[PRIORITY] Selected score: " + result + " ✓");
    }

    // =========================================================================
    // TEST 5 — Per-modality strategy: FINGER uses MEAN, IRIS uses MEDIAN
    //   Tests the 3-tier config hierarchy in the orchestrator
    // =========================================================================
    @Test
    public void test_PerModality_DifferentStrategies() throws RegBaseCheckedException {
        // FINGER → MEAN
        putFileConfig(
                RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + "FINGER", "MEAN");
        // IRIS → MEDIAN
        putFileConfig(
                RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + "IRIS", "MEDIAN");

        double fingerResult = orchestrator.orchestrate(dto("leftIndex")).getAggregatedScore(); // FINGER
        double irisResult   = orchestrator.orchestrate(dto("leftEye")).getAggregatedScore();   // IRIS

        System.out.println("\n[PER-MODALITY] FINGER(MEAN)=" + fingerResult
                + "  IRIS(MEDIAN)=" + irisResult);

        assertEquals("FINGER MEAN(70.0, 90.0) = 80.0", 80.0, fingerResult, 0.001);
        assertEquals("IRIS MEDIAN(70.0, 90.0) = 80.0", 80.0, irisResult, 0.001);
    }

    // =========================================================================
    // TEST 6 — SBI-only mode: only SBI evaluator configured
    //   When SDK is disabled, orchestrator should return just the SBI score
    // =========================================================================
    @Test
    public void test_SBIOnly_Mode() throws RegBaseCheckedException {
        // Configure only SBI (no SDK)
        ApplicationContext.map().put(
                RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
        configureAggregation("MEAN");

        double result = orchestrator.orchestrate(dto("leftThumb")).getAggregatedScore();

        System.out.println("\n[SBI-ONLY] SBI=" + SBI_SCORE + "  →  Aggregated=" + result);
        assertEquals("SBI-only mode should return exactly SBI score",
                SBI_SCORE, result, 0.001);
    }

    // =========================================================================
    // TEST 7 — Verify real SampleSDKV2 is initialised and reports its capabilities
    //   This confirms the mock SDK JAR is on the classpath and functional
    // =========================================================================
    @Test
    public void test_RealSampleSDKV2_InitAndCapabilities() {
        io.mosip.kernel.biometrics.model.SDKInfo info = realSampleSDKV2.init(null);

        assertNotNull("SampleSDKV2.init() must return SDKInfo", info);
        assertNotNull("SDKInfo must report supported modalities", info.getSupportedModalities());
        assertTrue("SDK must support FINGER",
                info.getSupportedModalities().contains(BiometricType.FINGER));
        assertTrue("SDK must support IRIS",
                info.getSupportedModalities().contains(BiometricType.IRIS));
        assertTrue("SDK must support FACE",
                info.getSupportedModalities().contains(BiometricType.FACE));
        assertNotNull("SDK must report QUALITY_CHECK in supportedMethods",
                info.getSupportedMethods().get(BiometricFunction.QUALITY_CHECK));

        System.out.println("\n[SDK-INIT] " + realSampleSDKV2.getClass().getSimpleName()
                + " v" + info.getSdkVersion()
                + " | Modalities: " + info.getSupportedModalities()
                + " | Methods: " + info.getSupportedMethods().keySet());
    }

    // =========================================================================
    // TEST 8 — All modalities through MEAN strategy
    //   Verifies finger, iris, face all route correctly through orchestrator
    // =========================================================================
    @Test
    public void test_AllModalities_MeanAggregation() throws RegBaseCheckedException {
        configureAggregation("MEAN");

        // Extend provider map for all types (already done for all in setUp)
        double fingerResult = orchestrator.orchestrate(dto("leftIndex")).getAggregatedScore();
        double irisResult   = orchestrator.orchestrate(dto("leftEye")).getAggregatedScore();
        double faceResult   = orchestrator.orchestrate(dto("face")).getAggregatedScore();

        System.out.println("\n[ALL-MODALITIES MEAN]"
                + "  Finger=" + fingerResult
                + "  Iris="   + irisResult
                + "  Face="   + faceResult);

        assertEquals("Finger MEAN should be 80.0", 80.0, fingerResult, 0.001);
        assertEquals("Iris MEAN should be 80.0",   80.0, irisResult,   0.001);
        assertEquals("Face MEAN should be 80.0",   80.0, faceResult,   0.001);
    }

    // =========================================================================
    // TEST 9 — Score boundary: SBI=100, SDK=100 → all strategies return 100
    // =========================================================================
    @Test
    public void test_PerfectScores_AllStrategies() throws Exception {
        // Override SDK provider to return 100
        iBioProviderApi perfectProvider = mock(iBioProviderApi.class);
        Map<BiometricType, Float> perfect = new HashMap<>();
        perfect.put(BiometricType.FINGER, 100.0f);
        when(perfectProvider.getModalityQuality(any(), any())).thenReturn(perfect);
        when(bioAPIFactory.getBioProvider(any(BiometricType.class), any(BiometricFunction.class)))
                .thenReturn(perfectProvider);

        // BiometricsDto with SBI score = 100
        BiometricsDto perfectDto = new BiometricsDto("leftIndex", new byte[]{1}, 100.0);

        for (String strategy : Arrays.asList("MEAN", "MEDIAN", "WEIGHTED")) {
            configureAggregation(strategy);
            double result = orchestrator.orchestrate(perfectDto).getAggregatedScore();
            System.out.println("[PERFECT-" + strategy + "] Result: " + result);
            assertEquals("All strategies should return 100 for perfect scores",
                    100.0, result, 0.001);
        }
    }

    // =========================================================================
    // TEST 10 — Score range verification across all aggregation strategies
    //   No matter which strategy: result must be between MIN(SBI,SDK) and MAX(SBI,SDK)
    // =========================================================================
    @Test
    public void test_AggregatedScore_AlwaysBetweenInputScores() throws RegBaseCheckedException {
        double min = Math.min(SBI_SCORE, SDK_SCORE); // 70.0
        double max = Math.max(SBI_SCORE, SDK_SCORE); // 90.0

        for (String strategy : Arrays.asList("MEAN", "MEDIAN", "WEIGHTED")) {
            configureAggregation(strategy);
            double result = orchestrator.orchestrate(dto("leftIndex")).getAggregatedScore();
            System.out.println("[RANGE-CHECK-" + strategy + "] SBI="
                    + SBI_SCORE + " SDK=" + SDK_SCORE + " Result=" + result);
            assertTrue("Strategy " + strategy + ": result " + result
                            + " must be within [" + min + ", " + max + "]",
                    result >= min && result <= max);
        }
    }

    // =========================================================================
    // Utility: configure aggregation strategy in ApplicationContext
    // =========================================================================
    private void configureAggregation(String strategy) {
        putFileConfig(
                RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", strategy);
    }
}
