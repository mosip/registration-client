package io.mosip.registration.dto;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import lombok.Data;

@Data
public class BiometricDeviceInfo {
	
	private String serialNumber;
	private String make;
	private String model;
	private String deviceType;
	
	
	@Override
	public String toString() {
		return new StringBuilder(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("serialNumber"))
						.append(serialNumber).append(RegistrationConstants.NEW_LINE)
						.append(ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("make"))
						.append(make).append(RegistrationConstants.NEW_LINE)
						.append(ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("model"))
						.append(model).toString();
	}	

}
