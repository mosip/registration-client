package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import io.mosip.registration.dao.impl.UserDetailDAOImpl;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.dto.UserDetailResponseDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserPassword;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.repositories.UserBiometricRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserPwdRepository;
import io.mosip.registration.repositories.UserRoleRepository;
import io.mosip.registration.repositories.UserTokenRepository;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class UserDetailDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private UserDetailDAOImpl userDetailDAOImpl;

	@Mock
	private UserDetailRepository userDetailRepository;

	@Mock
	private UserBiometricRepository userBiometricRepository;
	

	/** The userDetail repository. */
	@Mock
	private UserPwdRepository userPwdRepository;

	/** The userDetail repository. */
	@Mock
	private UserRoleRepository userRoleRepository;
	
	@Mock
	private UserTokenRepository userTokenRepository;
	
	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
	}

	@Test
	public void getUserDetailSuccessTest() {

		UserDetail userDetail = new UserDetail();
		userDetail.setName("Sravya");
		userDetail.setIsActive(true);

		Mockito.when(userDetailRepository.findByIdIgnoreCase("mosip"))
				.thenReturn(userDetail);
		assertNotNull(userDetail);
		assertNotNull(userDetailDAOImpl.getUserDetail("mosip"));
	}

	@Test
	public void getUserDetailFailureTest() {

		UserDetail userDetail = new UserDetail();
		userDetail.setIsActive(true);

		Mockito.when(userDetailRepository.findByIdIgnoreCase("mosip"))
				.thenReturn(userDetail);
		assertNotNull(userDetail);
		assertNotNull(userDetailDAOImpl.getUserDetail("mosip"));
	}

	@Test
	public void testUpdateLoginParams() {
		UserDetail userDetail = new UserDetail();
		userDetail.setId("mosip");
		userDetail.setUnsuccessfulLoginCount(0);
		userDetail.setUserlockTillDtimes(new Timestamp(new Date().getTime()));
		Mockito.when(userDetailRepository.save(userDetail)).thenReturn(userDetail);
		userDetailDAOImpl.updateLoginParams(userDetail);
	}

	@Test
	public void getAllActiveUsersTest() {
		List<UserBiometric> bioList = new ArrayList<>();
		Mockito.when(userBiometricRepository.findByUserBiometricIdBioAttributeCodeAndIsActiveTrue(Mockito.anyString()))
				.thenReturn(bioList);
		assertEquals(bioList, userDetailDAOImpl.getAllActiveUsers("leftThumb"));
	}

	@Test
	public void getUserSpecificFingerprintDetailsTest() {

		List<UserBiometric> bioList = new ArrayList<>();
		Mockito.when(userBiometricRepository
				.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeIgnoreCase(Mockito.anyString(),
						Mockito.anyString()))
				.thenReturn(bioList);
		assertEquals(bioList, userDetailDAOImpl.getUserSpecificBioDetails("abcd", "Fingerprint"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void userDetlsDao() {
		UserDetailResponseDto userDetailsResponse = new UserDetailResponseDto();
		List<UserDetailDto> userDetails = new ArrayList<>();

		UserDetailDto user = new UserDetailDto();
		user.setUserId("110011");
		//user.setUserPassword("test".getBytes());
		//user.setRoles(Arrays.asList("SUPERADMIN"));
		//user.setMobile("9894589435");
		user.setLangCode("eng");
		UserDetailDto user1 = new UserDetailDto();
		user1.setUserId("110011");
		//user1.setUserPassword("test".getBytes());
		//user1.setRoles(Arrays.asList("SUPERADMIN"));
		//user1.setMobile("9894589435");
		user1.setLangCode("eng");
		userDetails.add(user);
		userDetails.add(user1);
		userDetailsResponse.setUserDetails(userDetails);
		Mockito.when(userDetailRepository.save(Mockito.any())).thenReturn(new UserDetail());
		Mockito.when(userPwdRepository.save(Mockito.any())).thenReturn(new UserPassword());
		Mockito.when(userRoleRepository.saveAll(Mockito.anyCollection())).thenReturn(new ArrayList<>());
		//doNothing().when(userRoleRepository).delete(Mockito.anyString());
		userDetailDAOImpl.save(user);
		userDetailDAOImpl.save(user1);
	}

	
	@Test
	public void updateAuthTokensTest() {
		UserDetail userDetail = new UserDetail();
		Mockito.when( userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(Mockito.anyString())).thenReturn(userDetail);
		userDetailDAOImpl.updateAuthTokens("userId", "authToken", "refreshToken", 2, 1);
	}
	
	@Test
	public void getUserSpecificBioDetailTest() {
		
		UserBiometric userBiometric = new UserBiometric();
	
		Mockito.when(userBiometricRepository.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeAndUserBiometricIdBioAttributeCodeIgnoreCase("mosip","bio","sub")).thenReturn(userBiometric);
		assertEquals(userBiometric, userDetailDAOImpl.getUserSpecificBioDetail("mosip","bio","sub"));
	}
	@Test
	public void updateUserPwdTest() {
		UserDetail userDetail = new UserDetail();
		try {
		Mockito.when(userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(Mockito.anyString())).thenReturn(userDetail);
		userDetailDAOImpl.updateUserPwd("s1","s2");
		}catch(Exception e) {
			
		}
	}
	
	@Test
	public void findAllActiveUsersExceptCurrentUserTest() {
		
		List<UserBiometric> userBioMetrics = new ArrayList<UserBiometric>();
		userBioMetrics.add(new UserBiometric());
	
		Mockito.when(userBiometricRepository.findByUserBiometricIdUsrIdNotAndUserBiometricIdBioTypeCodeAndIsActiveTrue(Mockito.anyString(), Mockito.anyString())).thenReturn(userBioMetrics);
		assertEquals(userBioMetrics.size(), userDetailDAOImpl.findAllActiveUsersExceptCurrentUser("s1","s2").size());
	}

	@Test
	public void getUserRoleByUserIdTest() {
		Mockito.when(userRoleRepository.findByUserRoleIdUsrId(Mockito.anyString())).thenReturn(new ArrayList<>());
		assertNotNull(userDetailDAOImpl.getUserRoleByUserId("userId"));		
	}
	
	@Test
	public void findAllActiveUsersTest() {
		List<UserBiometric> bioMetrics = new ArrayList<UserBiometric>();
		UserBiometric bio1 = new UserBiometric();
		bioMetrics.add(bio1);
		Mockito.when(userBiometricRepository.findByUserBiometricIdBioTypeCodeAndIsActiveTrue(Mockito.anyString())).thenReturn(bioMetrics);
		assertEquals(bioMetrics.size(),userDetailDAOImpl.findAllActiveUsers("bioType").size());
	}	
	
	@Test
	public void updateTest() {		
		UserDetail userDtl = new UserDetail();
		userDetailDAOImpl.update(userDtl);
	}
	
	@Test
	public void getAllUsersTest() {		
		List<UserDetail> userDtlList = new ArrayList<UserDetail>();	
		userDtlList.add(new UserDetail());
		Mockito.when(userDetailRepository.findAll()).thenReturn(userDtlList);
		assertEquals(userDtlList.size(),userDetailDAOImpl.getAllUsers().size());
	}
	
	@Test
	public void deleteUserRoleTest() {		
		List<UserRole> userRoleList = new ArrayList<UserRole>();		
		Mockito.when(userRoleRepository.findByUserRoleIdUsrId("userName")).thenReturn(userRoleList);
		userDetailDAOImpl.deleteUserRole("userName");
	}
	
	@Test
	public void deleteUserTest() {		
		UserDetail userDtl = new UserDetail();
		userDetailDAOImpl.deleteUser(userDtl);
	}
	
	@Test
	public void getUserDetailFailTest() {
		UserDetail userDetail = null;
		Mockito.when(userDetailRepository.findByIdIgnoreCase("mosip")).thenReturn(userDetail);
		userDetailDAOImpl.getUserDetail("mosip");
	}
}
