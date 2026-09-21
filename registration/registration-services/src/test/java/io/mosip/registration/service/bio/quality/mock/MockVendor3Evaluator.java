package io.mosip.registration.service.bio.quality.mock;

import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.service.bio.quality.IBiometricQualityEvaluator;

/**
 * Standalone mock evaluator simulating Vendor 3 SDK (e.g. Idemia Iris SDK).
 */
public class MockVendor3Evaluator implements IBiometricQualityEvaluator {

    @Override
    public QualityScore evaluate(BiometricsDto dto) {
        QualityScore qs = new QualityScore();
        qs.setScore(90L);
        return qs;
    }

    @Override
    public String getEvaluatorName() {
        return "MOCK_VENDOR_3";
    }
}
