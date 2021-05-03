package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class SbiRCaptureResponseDTO {

	List<SbiRCaptureResponseBiometricsDTO> biometrics;
}
