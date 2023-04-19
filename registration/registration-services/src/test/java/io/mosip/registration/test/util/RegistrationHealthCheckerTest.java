package io.mosip.registration.test.util;


import static org.junit.Assert.assertFalse;

import org.junit.Test;

import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

public class RegistrationHealthCheckerTest {

	@Test 
	public void isDiskSpaceAvailableTest() {
		assertFalse(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());		
	}
	
}
	

