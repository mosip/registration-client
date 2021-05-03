package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import lombok.Data;

@Data
public class MdmSbiDeviceInfo {

	private String env;
	private String purpose;
	private String[] deviceSubId;
	private String deviceStatus;
	private String digitalId;
	private String certification;
	private String serviceVersion;
	private String[] specVersion;
	private String callbackId;
	private String firmware;
	private String serialNo;
}
