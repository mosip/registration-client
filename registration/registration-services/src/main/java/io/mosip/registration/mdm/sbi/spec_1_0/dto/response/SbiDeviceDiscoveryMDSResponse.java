package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import lombok.Data;

@Data
public class SbiDeviceDiscoveryMDSResponse {

	private String serialNo;
	private String deviceStatus;
	private String certification;
	private String serviceVersion;
	private String[] deviceSubId;
	private String callbackId;
	private String digitalId;
	private String[] specVersion;
	private String purpose;
	private Error error;
}
