package io.mosip.registration.test.packetStatusSync;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.registration.dao.impl.RegPacketStatusDAOImpl;
import io.mosip.registration.entity.Registration;
//github.com/mosip/registration-client
import io.mosip.registration.repositories.RegistrationRepository;

public class RegPacketStatusDaoImplTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@Mock
	RegistrationRepository registrationRepository;

	@InjectMocks
	RegPacketStatusDAOImpl packetStatusDao;

	@Test
	public void updateTest() {
		Registration registration = new Registration();
		when(registrationRepository.update(Mockito.any())).thenReturn(registration);

		packetStatusDao.update(registration);
	}

	@Test
	public void findByClientStatusCodeTest() {
		List<Registration> registrations = null;
		when(registrationRepository.findByClientStatusCodeOrClientStatusCommentsOrderByCrDtime(Mockito.any(), Mockito.any())).thenReturn(registrations);

		packetStatusDao.getPacketIdsByStatusUploadedOrExported();
	}

	@Test
	public void deleteTest() {
		Registration registration = new Registration();

		registration.setId("REG12345");

		Mockito.doNothing().when(registrationRepository).deleteById(Mockito.anyString());

		packetStatusDao.delete(registration);

	}

}
