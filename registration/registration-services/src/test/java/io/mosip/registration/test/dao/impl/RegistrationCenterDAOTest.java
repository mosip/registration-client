package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;

import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.impl.RegistrationCenterDAOImpl;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

public class RegistrationCenterDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private RegistrationCenterDAOImpl registrationCenterDAOImpl;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;
	
	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Before
	public void initialize() throws Exception {
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");

	}	
	@Test
	public void getRegistrationCenterDetailsSuccessTest() {

		RegistrationCenter registrationCenter = new RegistrationCenter();
		RegistartionCenterId registartionCenterId = new RegistartionCenterId();
		registartionCenterId.setId("10011");
		registrationCenter.setRegistartionCenterId(registartionCenterId);
		
		Optional<RegistrationCenter> registrationCenterList = Optional.of(registrationCenter);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode("mosip","eng"))
				.thenReturn(registrationCenterList);
		assertTrue(registrationCenterList.isPresent());
		assertNotNull(registrationCenterDAOImpl.getRegistrationCenterDetails("mosip","eng"));
	}

	@Test
	public void isMachineCenterActiveFalseTest() {

		MachineMaster machineMaster = null;
		RegistrationCenter registrationCenter = new RegistrationCenter();
		RegistartionCenterId registartionCenterId = new RegistartionCenterId();
		registartionCenterId.setId("10011");
		registrationCenter.setRegistartionCenterId(registartionCenterId);		
		Optional<RegistrationCenter> registrationCenterList = Optional.of(registrationCenter);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
		Mockito.when(machineMasterRepository.findByNameIgnoreCase("machineName")).thenReturn(machineMaster);
		assertTrue(registrationCenterList.isPresent());
		assertNotNull(registrationCenterDAOImpl.isMachineCenterActive());
	}
	
	@Test
	public void isMachineCenterActiveTrueTest() {
		MachineMaster machineMaster = new MachineMaster() ;
		machineMaster.setName("name");
		RegistrationCenter registrationCenter = new RegistrationCenter();
		Optional<RegistrationCenter> registrationCenterList = Optional.of(registrationCenter);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.any())).thenReturn(machineMaster);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode("mosip","eng")).thenReturn(registrationCenterList);
		assertTrue(registrationCenterList.isPresent());
		assertNotNull(registrationCenterDAOImpl.isMachineCenterActive());
	}
}
