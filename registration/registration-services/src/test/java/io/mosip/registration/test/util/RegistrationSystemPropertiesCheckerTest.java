package io.mosip.registration.test.util;

import org.junit.Assert;
import org.junit.Test;

import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

public class RegistrationSystemPropertiesCheckerTest {

	@Test
	public void testGetMachineId() {
		String machineId = RegistrationSystemPropertiesChecker.getMachineId();
		Assert.assertNotNull(machineId);
	}


}
