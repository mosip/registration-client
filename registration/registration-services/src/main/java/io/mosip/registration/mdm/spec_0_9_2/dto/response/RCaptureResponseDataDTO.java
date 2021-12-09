package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import java.util.Base64;

import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

@Data
public class RCaptureResponseDataDTO {

	private String deviceCode;
	private String digitalId;
	private String deviceServiceVersion;
	private String bioSubType;
	private String purpose;
	private String env;
	private String bioValue;
	private String bioExtract;
	private String registrationId;
	private String timestamp;
	private String requestedScore;
	private String qualityScore;

	public byte[] getDecodedBioValue() {
		return CryptoUtil.decodeURLSafeBase64(bioExtract);
	}

}
