package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import java.util.Base64;

import io.mosip.kernel.core.util.CryptoUtil;
import lombok.Data;

@Data
public class SbiRCaptureResponseDataDTO {

	private String digitalId;
	private String bioType;
	private String deviceCode;
	private String deviceServiceVersion;
	private String bioSubType;
	private String purpose;
	private String env;
	private String bioValue;
	private String bioExtract;
	private String transactionId;
	private String timestamp;
	private String requestedScore;
	private String qualityScore;

	public byte[] getDecodedBioValue() {
		return CryptoUtil.decodeURLSafeBase64(bioValue);
	}

}
