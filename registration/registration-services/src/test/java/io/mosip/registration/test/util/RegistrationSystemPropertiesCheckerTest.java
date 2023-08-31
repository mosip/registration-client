package io.mosip.registration.test.util;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

public class RegistrationSystemPropertiesCheckerTest {
	
	@Mock
	private RegistrationSystemPropertiesChecker registrationSystemPropertiesChecker;

	@Test
	public void testGetMachineId() throws Exception  {
		String machineId = RegistrationSystemPropertiesChecker.getMachineId();
		Assert.assertNotNull(machineId);	
	}

}