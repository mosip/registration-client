package io.mosip.registration.mdm.sbi.spec_1_0.dto.response;

import io.mosip.registration.mdm.dto.DeviceInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MdmSbiDeviceInfo extends DeviceInfo {

	public MdmSbiDeviceInfoSubType deviceInfo;
	public Error error;
	
}
