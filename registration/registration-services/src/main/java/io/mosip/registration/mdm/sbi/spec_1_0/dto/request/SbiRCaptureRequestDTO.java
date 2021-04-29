package io.mosip.registration.mdm.sbi.spec_1_0.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SbiRCaptureRequestDTO {

	private String env;
	private String purpose;
	private String specVersion;
	private String timeout;
	private String captureTime;
	private String transactionId;
	private String domainUri;
	private List<SbiRCaptureRequestBioDTO> bio;
	private Object customOpts;

	public SbiRCaptureRequestDTO(String env, String purpose, String specVersion, String timeout, String captureTime,
			String registrationId, List<SbiRCaptureRequestBioDTO> bio, Object customOpts) {
		super();
		this.env = env;
		this.specVersion = specVersion;
		this.timeout = timeout;
		this.captureTime = captureTime;
		this.bio = bio;
		this.customOpts = customOpts;
	}

}
