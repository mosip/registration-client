package io.mosip.registration.test.util;

import static org.junit.Assert.assertTrue;

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
		assertTrue(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());

	}

	@Test
	public void testIsDiskSpaceAvailable_LogsRequiredDiskSpaceNotAvailable_WhenDh() {
		Long diskSpaceThreshold = 1L;

		OSFileStore oSFileStore = Mockito.mock(OSFileStore.class);
		oSFileStore.setUsableSpace(1L);
		RegistrationAppHealthCheckUtil.isDiskSpaceAvailable();
		assertTrue(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable());
	}

}
