package io.mosip.registration.service.bio.quality;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.impl.BioProviderImpl_V_0_9;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.bio.impl.BioServiceImpl;
import io.mosip.registration.service.bio.quality.aggregator.MeanScoreAggregator;
import io.mosip.registration.service.bio.quality.evaluator.SbiQualityEvaluator;
import io.mosip.registration.service.bio.quality.evaluator.SdkBiometricQualityEvaluator;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.util.common.BIRBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the SDK-based biometric quality user story's Alternate Scenarios
 * (AS-01..AS-05) and Error Scenarios table against the actual implementation.
 *
 * <p>Deliberately does NOT exercise any aggregation strategy (MEAN/MEDIAN/etc.)
 * - every test here uses a single evaluator source, so "the display score" is
 * unambiguous and the tests are independent of whatever aggregation.default
 * happens to be set to in the live spring.properties file on disk.
 *
 * <p>Scope: rows not implemented by the codebase (Partial Capture, Corrupt
 * Data, Configuration Error) are intentionally NOT covered here - there's no
 * production behavior yet for them to verify. See the class-level comment on
 * the corresponding rows in the Error Scenarios table for tracking.
 */
@RunWith(MockitoJUnitRunner.class)
public class QualityErrorScenariosTest {

	@Mock
	private BioAPIFactory bioAPIFactory;

	@Mock
	private BIRBuilder birBuilder;

	@Mock
	private AuditManagerService auditFactory;

	@Mock
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private LocalConfigService localConfigService;

	private SdkBiometricQualityEvaluator sdkEvaluator;
	private SbiQualityEvaluator sbiEvaluator;
	private BiometricQualityOrchestrator orchestrator;
	private BioServiceImpl bioService;

	private BioProviderImpl_V_0_9 mockSdkProvider;

	@Before
	public void setUp() throws Exception {
		mockSdkProvider = mock(BioProviderImpl_V_0_9.class);
		when(bioAPIFactory.getBioProvider(any(BiometricType.class), any(BiometricFunction.class)))
				.thenReturn(mockSdkProvider);
		when(birBuilder.buildBir(any(BiometricsDto.class), any(ProcessedLevelType.class)))
				.thenReturn(new BIR());

		sdkEvaluator = new SdkBiometricQualityEvaluator();
		ReflectionTestUtils.setField(sdkEvaluator, "bioAPIFactory", bioAPIFactory);
		ReflectionTestUtils.setField(sdkEvaluator, "birBuilder", birBuilder);
		ReflectionTestUtils.setField(sdkEvaluator, "auditFactory", auditFactory);

		sbiEvaluator = new SbiQualityEvaluator();
		ReflectionTestUtils.setField(sbiEvaluator, "auditFactory", auditFactory);

		orchestrator = new BiometricQualityOrchestrator();
		ReflectionTestUtils.setField(orchestrator, "evaluators", Arrays.asList(sdkEvaluator, sbiEvaluator));
		ReflectionTestUtils.setField(orchestrator, "aggregators", Collections.singletonList(new MeanScoreAggregator()));
		ReflectionTestUtils.setField(orchestrator, "auditFactory", auditFactory);

		bioService = new BioServiceImpl();
		ReflectionTestUtils.setField(bioService, "bioAPIFactory", bioAPIFactory);
		ReflectionTestUtils.setField(bioService, "deviceSpecificationFactory", deviceSpecificationFactory);
		ReflectionTestUtils.setField(bioService, "birBuilder", birBuilder);
		ReflectionTestUtils.setField(bioService, "biometricQualityOrchestrator", orchestrator);
		when(globalParamService.getGlobalParams()).thenReturn(new HashMap<>());
		when(localConfigService.getLocalConfigurations()).thenReturn(new HashMap<>());
		ReflectionTestUtils.setField(bioService, "globalParamService", globalParamService);
		ReflectionTestUtils.setField(bioService, "localConfigService", localConfigService);

		// Deterministic config, independent of whatever is currently in the real
		// spring.properties file on disk. No aggregation strategy is set anywhere.
		ApplicationContext.getInstance();
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SDK");
		ApplicationContext.map().remove("mosip.registration.quality.aggregation.default");
		ApplicationContext.map().put("mosip.registration.quality.timeout.SDK", "5000");
		ApplicationContext.map().put(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD, "40");
	}

	@After
	public void tearDown() {
		ApplicationContext.map().remove("mosip.registration.quality.evaluators.default");
		ApplicationContext.map().remove("mosip.registration.quality.aggregation.default");
		ApplicationContext.map().remove("mosip.registration.quality.timeout.SDK");
		ApplicationContext.map().remove(RegistrationConstants.QUALITY_CHECK_WITH_SDK);
		ApplicationContext.map().remove(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
	}

	// =====================================================================
	// Alternate Scenarios
	// =====================================================================

	/** AS-01: SDK available and valid -> SDK score used against existing threshold. */
	@Test
	public void as01_sdkAvailableAndValid_usesSdkScore() throws RegBaseCheckedException {
		stubSdkScore(BiometricType.FINGER, 78.0f);
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SDK");

		BiometricQualityOrchestrator.OrchestrationResult result =
				orchestrator.orchestrate(buildDto("leftIndex", 60.0));

		assertEquals(78.0, result.getEvaluatorScores().get("SDK"), 0.0);
		assertFalse("SBI should not have been evaluated when only SDK is configured",
				result.getEvaluatorScores().containsKey("SBI"));
	}

	/** AS-02: SDK unavailable (not configured) -> SBI score used, no SDK call made. */
	@Test
	public void as02_sdkUnavailable_fallsBackToSbi() throws RegBaseCheckedException {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI");

		BiometricQualityOrchestrator.OrchestrationResult result =
				orchestrator.orchestrate(buildDto("leftIndex", 65.0));

		assertEquals(65.0, result.getEvaluatorScores().get("SBI"), 0.0);
		assertFalse(result.getEvaluatorScores().containsKey("SDK"));
		verifyNoInteractions(bioAPIFactory);
	}

	/** AS-03: SDK disabled via config -> orchestrator never invoked, SBI score used directly. */
	@Test
	public void as03_sdkDisabled_orchestratorNeverInvoked() throws Exception {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "N");

		List<BiometricsDto> captured = runCaptureModality(72.0, "FINGERPRINT_SLAB_LEFT", "leftIndex");

		assertEquals(1, captured.size());
		assertEquals(72.0, captured.get(0).getQualityScore(), 0.0);
		verifyNoInteractions(bioAPIFactory);
		assertEquals("Orchestrator's own aggregatedScore field must stay unset when SDK check is off",
				0.0, captured.get(0).getAggregatedScore(), 0.0);
	}

	/** AS-04: quality below threshold -> re-capture enforced (captureModality throws and blocks). */
	@Test
	public void as04_qualityBelowThreshold_blocksAndEnforcesRecapture() {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "Y");
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI");
		// threshold is 40 (see setUp); score below that must block
		try {
			runCaptureModality(20.0, "FINGERPRINT_SLAB_LEFT", "leftIndex");
			fail("Expected capture to be blocked for below-threshold quality");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-006", e.getErrorCode());
			assertEquals("Biometric quality below acceptable threshold. Please re-capture.", e.getErrorText());
		} catch (Exception e) {
			fail("Expected RegBaseCheckedException, got: " + e);
		}
	}

	/** AS-05: configurable score display - single-source config (SDK) drives what's reported. */
	@Test
	public void as05_configurableDisplay_singleSourceSdk() throws RegBaseCheckedException {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SDK");
		stubSdkScore(BiometricType.FINGER, 88.0f);

		BiometricQualityOrchestrator.OrchestrationResult result =
				orchestrator.orchestrate(buildDto("leftIndex", 30.0));

		// Configured for SDK display: SDK's number wins even though SBI's raw score differs.
		assertEquals(88.0, result.getEvaluatorScores().get("SDK"), 0.0);
	}

	// =====================================================================
	// Error Scenarios
	// =====================================================================

	/** Invalid SDK Score: block, no fallback to SBI. */
	@Test
	public void err_invalidSdkScore_blocksNoFallback() throws Exception {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI, SDK");
		Map<BiometricType, Float> qualityMap = new HashMap<>();
		qualityMap.put(BiometricType.FINGER, -1.0f);
		when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);

		try {
			orchestrator.orchestrate(buildDto("leftIndex", 90.0));
			fail("Expected REG_SDK_INVALID_SCORE to propagate");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-002", e.getErrorCode());
			assertEquals("Invalid biometric quality score received from SDK.", e.getErrorText());
		}
	}

	/** SDK Timeout: block, no fallback to SBI. */
	@Test
	public void err_sdkTimeout_blocksNoFallback() throws Exception {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI, SDK");
		ApplicationContext.map().put("mosip.registration.quality.timeout.SDK", "50");
		when(mockSdkProvider.getModalityQuality(any(), any())).thenAnswer(invocation -> {
			Thread.sleep(500); // exceeds the 50ms timeout above
			Map<BiometricType, Float> qualityMap = new HashMap<>();
			qualityMap.put(BiometricType.FINGER, 90.0f);
			return qualityMap;
		});

		try {
			orchestrator.orchestrate(buildDto("leftIndex", 90.0));
			fail("Expected REG_SDK_QUALITY_TIMEOUT to propagate");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-001", e.getErrorCode());
			assertEquals("Biometric quality evaluation timed out. Please re-capture.", e.getErrorText());
		}
	}

	/** SDK Exception: block, no fallback, distinct message from Invalid Score. */
	@Test
	public void err_sdkException_blocksWithDistinctMessage() throws Exception {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI, SDK");
		when(mockSdkProvider.getModalityQuality(any(), any()))
				.thenThrow(new RuntimeException("simulated SDK integration failure"));

		try {
			orchestrator.orchestrate(buildDto("leftIndex", 90.0));
			fail("Expected REG_SDK_EVALUATION_EXCEPTION to propagate");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-004", e.getErrorCode());
			assertEquals("Error occurred during SDK-based quality evaluation. Please retry capture.", e.getErrorText());
		}
	}

	/** Missing SBI Score: block registration. */
	@Test
	public void err_missingSbiScore_blocks() {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI");
		try {
			orchestrator.orchestrate(buildDto("leftIndex", -1.0));
			fail("Expected REG_SBI_SCORE_UNAVAILABLE to propagate");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-005", e.getErrorCode());
			assertEquals("Default biometric quality score unavailable.", e.getErrorText());
		}
	}

	/** No Quality Source: neither SDK nor SBI configured -> block. */
	@Test
	public void err_noQualitySource_blocks() {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "NON_EXISTENT_SOURCE");
		try {
			orchestrator.orchestrate(buildDto("leftIndex", 90.0));
			fail("Expected REG_NO_QUALITY_SOURCE to propagate");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-003", e.getErrorCode());
			assertEquals("Biometric quality evaluation not configured. Registration cannot proceed.", e.getErrorText());
		}
	}

	/** Quality Below Threshold: enforce re-capture (duplicated as its own row for clarity). */
	@Test
	public void err_qualityBelowThreshold_enforcesRecapture() {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "Y");
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI");
		try {
			runCaptureModality(10.0, "FINGERPRINT_SLAB_LEFT", "leftIndex");
			fail("Expected below-threshold capture to be blocked");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-006", e.getErrorCode());
			assertEquals("Biometric quality below acceptable threshold. Please re-capture.", e.getErrorText());
		} catch (Exception e) {
			fail("Expected RegBaseCheckedException, got: " + e);
		}
	}

	/**
	 * Audit Log Failure: allow processing to continue, only warn, when the audit
	 * subsystem itself throws.
	 */
	@Test
	public void err_auditLogFailure_doesNotBreakEvaluation() throws Exception {
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SDK");
		stubSdkScore(BiometricType.FINGER, 95.0f);
		Mockito.doThrow(new RuntimeException("audit subsystem down"))
				.when(auditFactory).audit(any(), any(), anyString(), anyString());

		// Must NOT throw despite the audit call failing internally.
		BiometricQualityOrchestrator.OrchestrationResult result =
				orchestrator.orchestrate(buildDto("leftIndex", 95.0));

		assertEquals(95.0, result.getEvaluatorScores().get("SDK"), 0.0);
	}

	/** Corrupt Data: a captured segment with no readable biometric payload is rejected. */
	@Test
	public void err_corruptData_blocks() {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "N");
		try {
			runCaptureModalityWithCorruptData("FINGERPRINT_SLAB_LEFT", "leftIndex");
			fail("Expected corrupt biometric data to be rejected");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-008", e.getErrorCode());
			assertEquals("Captured biometric data is invalid. Please re-capture.", e.getErrorText());
		} catch (Exception e) {
			fail("Expected RegBaseCheckedException, got: " + e);
		}
	}

	/** Configuration Error: a missing/non-positive threshold blocks rather than silently passing. */
	@Test
	public void err_configurationError_blocks() {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "Y");
		ApplicationContext.map().put("mosip.registration.quality.evaluators.default", "SBI");
		ApplicationContext.map().remove(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
		try {
			runCaptureModality(90.0, "FINGERPRINT_SLAB_LEFT", "leftIndex");
			fail("Expected missing threshold config to block");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-009", e.getErrorCode());
			assertEquals("Biometric quality configuration error. Please contact administrator.", e.getErrorText());
		} catch (Exception e) {
			fail("Expected RegBaseCheckedException, got: " + e);
		} finally {
			ApplicationContext.map().put(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD, "40");
		}
	}

	/** Partial Capture: fewer segments returned than the request required. */
	@Test
	public void err_partialCapture_blocks() {
		ApplicationContext.map().put(RegistrationConstants.QUALITY_CHECK_WITH_SDK, "N");
		try {
			runCaptureModalityExpectingMore("FINGERPRINT_SLAB_LEFT", "leftIndex", 4);
			fail("Expected partial capture to be rejected");
		} catch (RegBaseCheckedException e) {
			assertEquals("REG-SDK-007", e.getErrorCode());
			assertEquals("Incomplete biometric capture. Please capture all required biometrics.", e.getErrorText());
		} catch (Exception e) {
			fail("Expected RegBaseCheckedException, got: " + e);
		}
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	private void stubSdkScore(BiometricType type, float score) {
		Map<BiometricType, Float> qualityMap = new HashMap<>();
		qualityMap.put(type, score);
		when(mockSdkProvider.getModalityQuality(any(), any())).thenReturn(qualityMap);
	}

	private BiometricsDto buildDto(String bioAttribute, double sbiQualityScore) {
		BiometricsDto dto = new BiometricsDto();
		dto.setBioAttribute(bioAttribute);
		dto.setQualityScore(sbiQualityScore);
		dto.setAttributeISO(new byte[] { 1, 2, 3 });
		dto.setModalityName(Modality.FINGERPRINT_SLAB_LEFT.name());
		return dto;
	}

	private List<BiometricsDto> runCaptureModality(double sbiQualityScore, String modality, String bioAttribute)
			throws RegBaseCheckedException, java.io.IOException {
		MdmBioDevice bioDevice = mock(MdmBioDevice.class);
		when(bioDevice.getSpecVersion()).thenReturn("0.9.5");
		try {
			when(deviceSpecificationFactory.getDeviceInfoByModality(anyString())).thenReturn(bioDevice);
		} catch (RegBaseCheckedException e) {
			throw new RuntimeException(e);
		}

		MosipDeviceSpecificationProvider provider = mock(MosipDeviceSpecificationProvider.class);
		try {
			when(deviceSpecificationFactory.getMdsProvider(anyString())).thenReturn(provider);
		} catch (RegBaseCheckedException e) {
			throw new RuntimeException(e);
		}

		List<BiometricsDto> captured = new ArrayList<>();
		captured.add(buildDto(bioAttribute, sbiQualityScore));
		when(provider.rCapture(any(MdmBioDevice.class), any(MDMRequestDto.class))).thenReturn(captured);

		MDMRequestDto request = mock(MDMRequestDto.class);
		when(request.getModality()).thenReturn(modality);

		return bioService.captureModality(request);
	}

	private List<BiometricsDto> runCaptureModalityWithCorruptData(String modality, String bioAttribute)
			throws RegBaseCheckedException, java.io.IOException {
		MdmBioDevice bioDevice = mock(MdmBioDevice.class);
		when(bioDevice.getSpecVersion()).thenReturn("0.9.5");
		try {
			when(deviceSpecificationFactory.getDeviceInfoByModality(anyString())).thenReturn(bioDevice);
			MosipDeviceSpecificationProvider provider = mock(MosipDeviceSpecificationProvider.class);
			when(deviceSpecificationFactory.getMdsProvider(anyString())).thenReturn(provider);

			BiometricsDto dto = buildDto(bioAttribute, 90.0);
			dto.setAttributeISO(null); // corrupt/unreadable
			List<BiometricsDto> captured = new ArrayList<>();
			captured.add(dto);
			when(provider.rCapture(any(MdmBioDevice.class), any(MDMRequestDto.class))).thenReturn(captured);
		} catch (RegBaseCheckedException e) {
			throw new RuntimeException(e);
		}

		MDMRequestDto request = mock(MDMRequestDto.class);
		when(request.getModality()).thenReturn(modality);

		return bioService.captureModality(request);
	}

	private List<BiometricsDto> runCaptureModalityExpectingMore(String modality, String bioAttribute,
			int expectedCount) throws RegBaseCheckedException, java.io.IOException {
		MdmBioDevice bioDevice = mock(MdmBioDevice.class);
		when(bioDevice.getSpecVersion()).thenReturn("0.9.5");
		try {
			when(deviceSpecificationFactory.getDeviceInfoByModality(anyString())).thenReturn(bioDevice);
			MosipDeviceSpecificationProvider provider = mock(MosipDeviceSpecificationProvider.class);
			when(deviceSpecificationFactory.getMdsProvider(anyString())).thenReturn(provider);

			List<BiometricsDto> captured = new ArrayList<>();
			captured.add(buildDto(bioAttribute, 90.0)); // only 1, fewer than expectedCount
			when(provider.rCapture(any(MdmBioDevice.class), any(MDMRequestDto.class))).thenReturn(captured);
		} catch (RegBaseCheckedException e) {
			throw new RuntimeException(e);
		}

		MDMRequestDto request = mock(MDMRequestDto.class);
		when(request.getModality()).thenReturn(modality);
		when(request.getCount()).thenReturn(expectedCount);

		return bioService.captureModality(request);
	}
}
