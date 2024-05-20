package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.registration.dao.AppAuthenticationDetails;
import io.mosip.registration.dao.AppRolePriorityDetails;
import io.mosip.registration.dao.impl.AppAuthenticationDAOImpl;
import io.mosip.registration.entity.id.AppRolePriorityId;
import io.mosip.registration.enums.Role;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.AppAuthenticationRepository;
import io.mosip.registration.repositories.AppRolePriorityRepository;

public class AppAuthenticationDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private AppAuthenticationDAOImpl appAuthenticationDAOImpl;

	@Mock
	private AppAuthenticationRepository appAuthenticationRepository;
	
	@Mock
	private AppRolePriorityRepository appRolePriorityRepository;

	@Test
	public void getModesOfLoginSuccessTest() throws RegBaseCheckedException {

		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		
		Set<String> roleSet = new HashSet<>();
		roleSet.add("OFFICER");
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		List<String> modes = new ArrayList<>();
		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertEquals(modes, appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
	@Test
	public void getModesOfLoginSuccess1Test() throws RegBaseCheckedException {

		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();

		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();

		Set<String> roleSet = new HashSet<>();
		roleSet.add("DEFAULT");
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		List<String> modes = new ArrayList<>();
		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());

		assertEquals(modes, appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
	@Test
	public void getModesOfLoginMultipleRoleTest() throws RegBaseCheckedException {

		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		
		Set<String> roleSet = new HashSet<>();
		roleSet.add("OFFICER");
		roleSet.add("SUPERVISOR");
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		List<String> modes = new ArrayList<>();
		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertEquals(modes, appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
	@Test
	public void getModesOfLoginMultipleRoleFailureTest() throws RegBaseCheckedException {

		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		
		Set<String> roleSet = new HashSet<>();
		roleSet.add("OFFICER");
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		List<String> modes = new ArrayList<>();
		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertEquals(modes, appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}

	@Test
	public void getModesOfRoleListEmptyTest() throws RegBaseCheckedException {
		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		
		Set<String> roleSet = null;
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(null);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertNotNull(appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
	@Test
	public void getModesOfRoleListNotEmptyTest() throws RegBaseCheckedException {
		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		AppRolePriorityId appRolePriorityId = new AppRolePriorityId();
		appRolePriorityId.setRoleCode("roleCode");

		Set<String> roleSet = new HashSet<>();
		roleSet.add(Role.REGISTRATION_OFFICER.name());
		roleSet.add(Role.REGISTRATION_SUPERVISOR.name());
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertNotNull(appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
	
	@Test
	public void getModesOfLoginDefaultRoleTest() throws RegBaseCheckedException {
		
		List<AppAuthenticationDetails> loginList = new ArrayList<AppAuthenticationDetails>();
		List<AppRolePriorityDetails> roleList = new ArrayList<AppRolePriorityDetails>();
		AppRolePriorityId appRolePriorityId = new AppRolePriorityId();
		appRolePriorityId.setRoleCode("roleCode");

		Set<String> roleSet = new HashSet<>();
		roleSet.add(Role.Default.name());
		Mockito.when(appRolePriorityRepository.findByAppRolePriorityIdProcessIdAndAppRolePriorityIdRoleCodeInOrderByPriority("login", roleSet)).thenReturn(roleList);
		Mockito.when(appAuthenticationRepository.findByIsActiveTrueAndAppAuthenticationMethodIdProcessIdAndAppAuthenticationMethodIdRoleCodeInOrderByMethodSequence("login",roleSet)).thenReturn(loginList);

		loginList.stream().map(loginMethod -> loginMethod.getAppAuthenticationMethodId().getAuthMethodCode()).collect(Collectors.toList());
		
		assertNotNull(appAuthenticationDAOImpl.getModesOfLogin("login", roleSet));
	}
	
}
