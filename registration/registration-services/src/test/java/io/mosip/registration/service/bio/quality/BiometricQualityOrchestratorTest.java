package io.mosip.registration.service.bio.quality;

import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.bio.quality.aggregator.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BiometricQualityOrchestrator}.
 *
 * Covers: evaluator selection, all aggregation strategies (MEAN, MEDIAN,
 * WEIGHTED, PRIORITY), multi-evaluator collection, failure/fallback paths,
 * audit logging, and edge values.
 */
@RunWith(MockitoJUnitRunner.class)
public class BiometricQualityOrchestratorTest {

	// ---- Mocks ----
	@Mock
	private IBiometricQualityEvaluator sbiEvaluator;

	@Mock
	private IBiometricQualityEvaluator sdkEvaluator;

	@Mock
	private AuditManagerService auditFactory;

	// ---- Real aggregator implementations to verify math exactly ----
	private final MeanScoreAggregator meanAggregator = new MeanScoreAggregator();
	private final MedianScoreAggregator medianAggregator = new MedianScoreAggregator();
	private final WeightedAverageScoreAggregator weightedAggregator = new WeightedAverageScoreAggregator();
	private final PriorityScoreAggregator priorityAggregator = new PriorityScoreAggregator();

	@InjectMocks
	private BiometricQualityOrchestrator orchestrator;

	private BiometricsDto biometricsDto;

	@BeforeClass
	public static void initApplicationContext() {
		// Initialize the singleton so ApplicationContext.map() doesn't NPE
		ApplicationContext.getInstance();
	}

	@Before
	public void setUp() {
		// Fully clear the static applicationMap field (putAll-based setApplicationMap won't clear it)
		ReflectionTestUtils.setField(ApplicationContext.class, "applicationMap", new HashMap<String, Object>());
		// DaoConfig.keys is a static field shared across the whole test JVM fork -
		// clear it so aggregation-strategy config from another test class can't leak in.
		ReflectionTestUtils.setField(io.mosip.registration.config.DaoConfig.class, "keys", null);

		// Inject real aggregators and mocked evaluators into the orchestrator
		ReflectionTestUtils.setField(orchestrator, "evaluators",
				Arrays.asList(sbiEvaluator, sdkEvaluator));
		ReflectionTestUtils.setField(orchestrator, "aggregators",
				Arrays.asList(meanAggregator, medianAggregator, weightedAggregator, priorityAggregator));

		when(sbiEvaluator.getEvaluatorName()).thenReturn("SBI");
		when(sdkEvaluator.getEvaluatorName()).thenReturn("SDK");

		biometricsDto = new BiometricsDto("leftIndex", new byte[]{1, 2, 3}, 75.0);
	}

	// helper to put a key into the live ApplicationContext map, and also into
	// DaoConfig's file-backed properties so aggregation-strategy keys are seen
	// as "explicitly configured" (BiometricQualityOrchestrator checks the file,
	// not the runtime map, for that flag - see DaoConfig.isKeyPresentInPropertiesFile).
	private void putConfig(String key, String value) {
		Map<String, Object> m = new HashMap<>();
		m.put(key, value);
		ApplicationContext.setApplicationMap(m);
		java.util.Properties props = (java.util.Properties) ReflectionTestUtils.getField(
				io.mosip.registration.config.DaoConfig.class, "keys");
		if (props == null) {
			props = new java.util.Properties();
			ReflectionTestUtils.setField(io.mosip.registration.config.DaoConfig.class, "keys", props);
		}
		props.setProperty(key, value);
	}

	// =========================================================================
	// 1. EVALUATOR SELECTION TESTS
	// =========================================================================

	@Test
	public void testSingleSBIEvaluatorSelected() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(80L));

		double result = orchestrator.orchestrate(biometricsDto).getAggregatedScore();

		assertEquals(80.0, result, 0.001);
		verify(sbiEvaluator, times(1)).evaluate(biometricsDto);
		verify(sdkEvaluator, never()).evaluate(any());
	}

	@Test
	public void testSingleSDKEvaluatorSelected() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(90L));

		double result = orchestrator.orchestrate(biometricsDto).getAggregatedScore();

		assertEquals(90.0, result, 0.001);
		verify(sdkEvaluator, times(1)).evaluate(biometricsDto);
		verify(sbiEvaluator, never()).evaluate(any());
	}

	@Test
	public void testBothEvaluatorsSelected() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(70L));
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(90L));

		double result = orchestrator.orchestrate(biometricsDto).getAggregatedScore();

		// MEAN of (70 + 90) = 80.0
		assertEquals(80.0, result, 0.001);
		verify(sbiEvaluator).evaluate(biometricsDto);
		verify(sdkEvaluator).evaluate(biometricsDto);
	}

	@Test
	public void testAttributeSpecificEvaluatorOverridesDefault() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "leftIndex", "SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(85L));

		double result = orchestrator.orchestrate(biometricsDto).getAggregatedScore();

		assertEquals(85.0, result, 0.001);
		verify(sdkEvaluator).evaluate(biometricsDto);
		verify(sbiEvaluator, never()).evaluate(any());
	}

	// =========================================================================
	// 2. AGGREGATION STRATEGY TESTS
	// =========================================================================

	@Test
	public void testMeanAggregation() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(60L));
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(80L));

		// (60 + 80) / 2 = 70.0
		assertEquals(70.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	@Test
	public void testMeanAggregatorWithEmptyScoresReturnsZero() {
		assertEquals(0.0, meanAggregator.aggregate(Collections.emptyMap(), Collections.emptyMap()), 0.001);
	}

	@Test
	public void testMedianWithOddNumberOfValues() {
		Map<String, Double> scores = new LinkedHashMap<>();
		scores.put("A", 30.0);
		scores.put("B", 90.0);
		scores.put("C", 60.0);
		// Sorted: [30, 60, 90] → median = 60.0
		assertEquals(60.0, medianAggregator.aggregate(scores, Collections.emptyMap()), 0.001);
	}

	@Test
	public void testMedianWithEvenNumberOfValues() {
		Map<String, Double> scores = new LinkedHashMap<>();
		scores.put("A", 70.0);
		scores.put("B", 90.0);
		// (70 + 90) / 2 = 80.0
		assertEquals(80.0, medianAggregator.aggregate(scores, Collections.emptyMap()), 0.001);
	}

	@Test
	public void testMedianWithSingleValue() {
		assertEquals(75.0, medianAggregator.aggregate(Map.of("SBI", 75.0), Collections.emptyMap()), 0.001);
	}

	@Test
	public void testWeightedAverageViaOrchestrator() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		// WeightedAverageScoreAggregator strategy name is "WEIGHTED"
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "WEIGHTED");
		putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.SBI", "0.3");
		putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.SDK", "0.7");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(60L));
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(80L));

		// (60*0.3 + 80*0.7) / (0.3 + 0.7) = (18 + 56) / 1.0 = 74.0
		assertEquals(74.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	@Test
	public void testWeightedAverageAggregatorDirectly() {
		Map<String, Double> scores = Map.of("SBI", 60.0, "SDK", 80.0);
		Map<String, Double> weights = Map.of("SBI", 0.3, "SDK", 0.7);
		// (60*0.3 + 80*0.7) / 1.0 = 74.0
		assertEquals(74.0, weightedAggregator.aggregate(scores, weights), 0.001);
	}

	@Test
	public void testWeightedAggregatorWithEqualWeightsEqualsMean() {
		Map<String, Double> scores = Map.of("SBI", 60.0, "SDK", 80.0);
		Map<String, Double> weights = Map.of("SBI", 1.0, "SDK", 1.0);
		assertEquals(70.0, weightedAggregator.aggregate(scores, weights), 0.001);
	}

	@Test
	public void testPriorityAggregatorWithNoWeightsReturnsFirstEntry() {
		// No weights → returns first value in iteration order
		Map<String, Double> scores = new LinkedHashMap<>();
		scores.put("SBI", 55.0);
		scores.put("SDK", 90.0);
		// First entry is SBI=55.0
		assertEquals(55.0, priorityAggregator.aggregate(scores, Collections.emptyMap()), 0.001);
	}

	@Test
	public void testPriorityAggregatorWithWeightsSelectsLowestRankFirst() {
		// Lower weight value = higher priority
		Map<String, Double> scores = new LinkedHashMap<>();
		scores.put("SBI", 55.0);
		scores.put("SDK", 90.0);
		Map<String, Double> weights = Map.of("SBI", 2.0, "SDK", 1.0); // SDK has higher priority (rank 1)
		assertEquals(90.0, priorityAggregator.aggregate(scores, weights), 0.001);
	}

	@Test
	public void testDefaultAggregationFallsBackToMeanWhenUnknownStrategy() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "UNKNOWN_STRATEGY");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(60L));
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(80L));

		// Falls back to MEAN = 70.0
		assertEquals(70.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	// =========================================================================
	// 3. STRATEGY NAMES
	// =========================================================================

	@Test
	public void testAggregatorStrategyNames() {
		assertEquals("MEAN", meanAggregator.getStrategyName());
		assertEquals("MEDIAN", medianAggregator.getStrategyName());
		assertEquals("WEIGHTED", weightedAggregator.getStrategyName());
		assertEquals("PRIORITY", priorityAggregator.getStrategyName());
	}

	@Test
	public void testEvaluatorNames() {
		assertEquals("SBI", sbiEvaluator.getEvaluatorName());
		assertEquals("SDK", sdkEvaluator.getEvaluatorName());
	}

	// =========================================================================
	// 4. FAILURE / FALLBACK SCENARIOS
	// =========================================================================

	@Test(expected = RegBaseCheckedException.class)
	public void testNoMatchingEvaluatorThrowsException() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "NONEXISTENT");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		orchestrator.orchestrate(biometricsDto).getAggregatedScore();
	}

	@Test
	public void testNoMatchingEvaluatorAuditsFailure() {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "NONEXISTENT");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		try {
			orchestrator.orchestrate(biometricsDto).getAggregatedScore();
			fail("Expected RegBaseCheckedException");
		} catch (RegBaseCheckedException e) {
			// getMessage() returns "errorCode --> errorMessage", so check it starts with the code
			assertTrue(e.getMessage().startsWith(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode()));
			verify(auditFactory).audit(eq(AuditEvent.QUALITY_ORCH_FAILED),
					eq(Components.REG_BIOMETRICS), anyString(), anyString());
		}
	}

	@Test
	public void testOneEvaluatorFailsBlocksWithNoFallback() throws RegBaseCheckedException {
		// Spec: a configured evaluator's failure blocks immediately with no silent
		// fallback to whichever other evaluator happened to succeed (Error
		// Scenarios: "No fallback to SBI/SDK" for Invalid SDK Score / SDK Exception
		// / Missing SBI Score) - the old catch-and-continue behavior this test used
		// to assert has been intentionally removed.
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenThrow(
				new RegBaseCheckedException("ERR-001", "SBI unavailable"));
		// No SDK stub - SBI's failure blocks before SDK is ever invoked, so
		// stubbing it here would be an unnecessary/dead stub under strict Mockito.

		try {
			orchestrator.orchestrate(biometricsDto);
			fail("Expected orchestrate() to block when a configured evaluator fails");
		} catch (RegBaseCheckedException e) {
			assertEquals("ERR-001", e.getErrorCode());
		}
	}

	@Test(expected = RegBaseCheckedException.class)
	public void testAllEvaluatorsFailThrowsException() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		// No fallback: the first configured evaluator's failure (SBI) blocks
		// immediately, so SDK is never invoked - stubbing it would be dead/unreachable.
		when(sbiEvaluator.evaluate(biometricsDto)).thenThrow(
				new RegBaseCheckedException("ERR-001", "SBI failed"));

		orchestrator.orchestrate(biometricsDto).getAggregatedScore();
	}

	@Test
	public void testAllEvaluatorsFailAuditsFailure() {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		try {
			when(sbiEvaluator.evaluate(biometricsDto)).thenThrow(
					new RegBaseCheckedException("ERR-001", "SBI failed"));
			orchestrator.orchestrate(biometricsDto).getAggregatedScore();
			fail("Expected RegBaseCheckedException");
		} catch (RegBaseCheckedException e) {
			verify(auditFactory).audit(eq(AuditEvent.QUALITY_ORCH_FAILED),
					eq(Components.REG_BIOMETRICS), anyString(), anyString());
		}
	}

	@Test
	public void testEvaluatorReturningNullRaisesInvalidScore() {
		// A null QualityScore is an Invalid Score (REG-SDK-002), not a source to
		// silently skip - proceeding on the remaining evaluator's score alone
		// would be exactly the silent partial-fallback the Error Scenarios table
		// forbids, even if another configured source (SDK here) did succeed.
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		try {
			when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(null);
			orchestrator.orchestrate(biometricsDto);
			fail("Expected RegBaseCheckedException for null QualityScore");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-002", e.getErrorCode());
		}
	}

	// =========================================================================
	// 5. WEIGHT PARSING EDGE CASES
	// =========================================================================

	@Test
	public void testInvalidWeightConfigDefaultsToOne() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI,SDK");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "WEIGHTED");
		putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.SBI", "bad_value");
		putConfig(RegistrationConstants.QUALITY_WEIGHT_PREFIX + "leftIndex.SDK", "bad_value");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(60L));
		when(sdkEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(80L));

		// Both weights default to 1.0 → (60 + 80) / 2 = 70.0
		assertEquals(70.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	// =========================================================================
	// 6. AUDIT SUCCESS LOGGING
	// =========================================================================

	@Test
	public void testSuccessfulOrchestrationAuditsCompletion() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(75L));

		orchestrator.orchestrate(biometricsDto).getAggregatedScore();

		verify(auditFactory, times(1)).audit(eq(AuditEvent.QUALITY_ORCH_COMPLETED),
				eq(Components.REG_BIOMETRICS), anyString(), anyString());
	}

	// =========================================================================
	// 7. SCORE EDGE VALUES
	// =========================================================================

	@Test
	public void testScoreOfZeroIsValid() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(0L));

		assertEquals(0.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	@Test
	public void testScoreOf100IsValid() throws RegBaseCheckedException {
		putConfig(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI");
		putConfig(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN");
		when(sbiEvaluator.evaluate(biometricsDto)).thenReturn(scoreOf(100L));

		assertEquals(100.0, orchestrator.orchestrate(biometricsDto).getAggregatedScore(), 0.001);
	}

	// =========================================================================
	// Helper
	// =========================================================================
	private QualityScore scoreOf(long value) {
		QualityScore qs = new QualityScore();
		qs.setScore(value);
		return qs;
	}
}
