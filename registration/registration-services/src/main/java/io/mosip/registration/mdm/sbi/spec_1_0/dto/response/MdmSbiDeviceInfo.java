package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import lombok.Data;

@Data
public class MdmSbiDeviceInfo {

	public MdmSbiDeviceInfoSubType deviceInfo;
	public Error error;
	
}
