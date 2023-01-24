package io.mosip.registration.test.util;


//import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.UnknownHostException;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

public class RegistrationSystemPropertiesCheckerTest {

	@Mock
	private RegistrationSystemPropertiesChecker registrationSystemPropertiesChecker;

	@Test
	public void testGetMachineId() throws Exception  {
		String machineId = RegistrationSystemPropertiesChecker.getMachineId();
		Assertions.assertNotNull(machineId);
		
	}

	@Test
    public void testGetMachineIdException() throws UnknownHostException{
		
		String machineId = RegistrationSystemPropertiesChecker.getMachineId();
		
		machineId = "";
//		assertThrows(UnknownHostException.class, () -> RegistrationSystemPropertiesChecker.getMachineId());

	}

}