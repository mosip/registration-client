package io.mosip.registration.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
@RunWith(MockitoJUnitRunner.class)
public class RegistrationSystemPropertiesCheckerTest {

	@Test
	public void testGetMachineId() throws UnknownHostException, RegBaseCheckedException {
		String expected = InetAddress.getLocalHost().getHostName().toLowerCase();
		String actual = RegistrationSystemPropertiesChecker.getMachineId();
		assertEquals(expected, actual);
		assertNotNull(actual);
	}
}