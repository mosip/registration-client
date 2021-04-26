package io.mosip.registration.mdm.sbi.spec_1_0.dto.request;

import lombok.Data;

@Data
public class SbiRCaptureRequestBioDTO {

	private String type;
	private String count;
	private String[] exception;
	private String requestedScore;
	private String serialNo;
	private String bioSubType;
	private String deviceSubId;
	private String previousHash;

	public SbiRCaptureRequestBioDTO(String type, String count, String[] exception, String requestedScore, String serialNo, String deviceId,
			String bioSubType, String deviceSubId, String previousHash) {
		super();
		this.type = type;
		this.count = count;
		this.exception = exception;
		this.requestedScore = requestedScore;
		this.serialNo = serialNo;
		this.bioSubType = bioSubType;
		this.deviceSubId = deviceSubId;
		this.previousHash = previousHash;
	}

}
