package io.mosip.registration.service.bio.quality.mock;

import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.service.bio.quality.IBiometricQualityEvaluator;

/**
 * Standalone mock evaluator simulating Vendor 1 SDK (e.g. Neurotechnology Finger SDK).
 */
public class MockVendor1Evaluator implements IBiometricQualityEvaluator {

    @Override
    public QualityScore evaluate(BiometricsDto dto) {
        QualityScore qs = new QualityScore();
        qs.setScore(60L);
        return qs;
    }

    @Override
    public String getEvaluatorName() {
        return "MOCK_VENDOR_1";
    }
}
