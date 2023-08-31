package io.mosip.registration.mdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MdmDeviceInfo extends DeviceInfo {
	private String[] specVersion;
	private String deviceStatus;
	private String deviceId;
	private String firmware;
	private String certification;
	private String serviceVersion;
	private String[] deviceSubId;
	private String callbackId;
	private String digitalId;
	private String deviceCode;
	private String purpose;

}
