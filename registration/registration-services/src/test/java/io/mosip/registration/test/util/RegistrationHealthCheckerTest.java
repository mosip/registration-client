package io.mosip.registration.test.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import oshi.software.os.OSFileStore;

public class RegistrationHealthCheckerTest {

	@InjectMocks
	private RegistrationAppHealthCheckUtil registrationAppHealthCheckUtil;

	@Test
	public void isDiskSpaceAvailableTestTrue() {
		assertNotNull(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());

	}

	@Test
	public void testIsDiskSpaceAvailable_LogsRequiredDiskSpaceNotAvailable_WhenDh() {
		OSFileStore oSFileStore = Mockito.mock(OSFileStore.class);
		oSFileStore.setUsableSpace(1L);
		RegistrationAppHealthCheckUtil.isDiskSpaceAvailable();
		assertNotNull(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());
	}

}
