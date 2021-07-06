package io.mosip.registration.test.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.entity.CenterMachine;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.id.CenterMachineId;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class, ApplicationContext.class, RegistrationSystemPropertiesChecker.class })
public class BaseServiceTest {

	@Mock
	private MachineMappingDAO machineMappingDAO;
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private BaseService baseService;

	@Mock
	private UserOnboardDAO onboardDAO;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private CenterMachineRepository centerMachineRepository;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Before
	public void init() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn("eng").when(ApplicationContext.class, "applicationLanguage");
		PowerMockito.doReturn("test").when(RegistrationSystemPropertiesChecker.class, "getMachineId");
	}


	@Test
	public void getUserIdTest() {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(SessionContext.userId()).thenReturn("MYUSERID");
		Assert.assertSame(baseService.getUserIdFromSession(), "MYUSERID");
	}

	@Test
	public void isNullTest() {
		Assert.assertSame(baseService.isNull(null), true);

	}

	@Test
	public void isEmptyTest() {
		Assert.assertSame(baseService.isEmpty(new LinkedList<>()), true);
	}

	@Test
	public void getStationIdTest() {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);

		Assert.assertSame("11002", baseService.getStationId());
	}

	@Test
	public void getNegativeStationIdTest() {
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(false);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);

		Assert.assertSame(null, baseService.getStationId());
	}

	@Test
	public void getCenterIdTest() {

		CenterMachine centerMachine = new CenterMachine();
		CenterMachineId centerMachineId = new CenterMachineId();
		centerMachineId.setMachineId("11002");
		centerMachineId.setRegCenterId("10011");
		centerMachine.setCenterMachineId(centerMachineId);
		centerMachine.setIsActive(true);
		Mockito.when(centerMachineRepository.findByCenterMachineIdMachineId(Mockito.anyString())).thenReturn(centerMachine);
		Mockito.when(registrationCenterDAO.isMachineCenterActive(Mockito.anyString())).thenReturn(true);

		Assert.assertSame("10011", baseService.getCenterId("11002"));
	}

	@Test
	public void getNegativeCenterIdTest() {

		CenterMachine centerMachine = new CenterMachine();
		CenterMachineId centerMachineId = new CenterMachineId();
		centerMachineId.setMachineId("11002");
		centerMachineId.setRegCenterId("10011");
		centerMachine.setCenterMachineId(centerMachineId);
		centerMachine.setIsActive(true);
		Mockito.when(centerMachineRepository.findByCenterMachineIdMachineId(Mockito.anyString())).thenReturn(centerMachine);
		Mockito.when(registrationCenterDAO.isMachineCenterActive(Mockito.anyString())).thenReturn(false);

		Assert.assertSame(null, baseService.getCenterId("11002"));
	}

	@Test
	public void getCenterIdTestWithCenterInactive() {

		CenterMachine centerMachine = new CenterMachine();
		CenterMachineId centerMachineId = new CenterMachineId();
		centerMachineId.setMachineId("11002");
		centerMachineId.setRegCenterId("10011");
		centerMachine.setCenterMachineId(centerMachineId);
		centerMachine.setIsActive(false);
		Mockito.when(centerMachineRepository.findByCenterMachineIdMachineId(Mockito.anyString())).thenReturn(centerMachine);
		Mockito.when(registrationCenterDAO.isMachineCenterActive(Mockito.anyString())).thenReturn(true);

		Assert.assertSame("10011", baseService.getCenterId("11002"));
	}
	

}
