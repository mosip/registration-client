package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import java.util.HashMap;

import lombok.Data;

@Data
public class SbiRCaptureResponseBiometricsDTO {

	private String specVersion;
	private String data;
	private String hash;
	private Error error;
	
	private HashMap<String, String> additionalInfo;
}
