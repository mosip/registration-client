package io.mosip.registration.test.util;


import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.mockito.Mock;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import oshi.software.os.OSFileStore;

public class RegistrationHealthCheckerTest {


@Mock
private RegistrationAppHealthCheckUtil registrationAppHealthCheckUtil;

	@Test
	public void isDiskSpaceAvailableTestTrue() {
		assertTrue(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());
			
	}
	
	@Test
	public void isDiskSpaceAvailableTestFalse() {
		OSFileStore fs = new OSFileStore();
		fs.setUsableSpace(0);
		
	}

}
	

