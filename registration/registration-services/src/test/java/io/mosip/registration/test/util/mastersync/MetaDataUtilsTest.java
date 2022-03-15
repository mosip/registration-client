package io.mosip.registration.test.util.mastersync;

import static io.mosip.registration.util.mastersync.MetaDataUtils.setCreateMetaData;
import static io.mosip.registration.util.mastersync.MetaDataUtils.setUpdateMetaData;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dto.mastersync.LanguageDto;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.RegistrationCommonFields;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.util.mastersync.MetaDataUtils;


/**
 * 
 * @author M1063027
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ SessionContext.class, ApplicationContext.class })
public class MetaDataUtilsTest {

	@Mock
	UserContext userContext;

	@InjectMocks
	MetaDataUtils metaDataUtils;

	@Mock
	io.mosip.registration.context.ApplicationContext context;

	@Before
	public void setup() throws Exception {
		userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		PowerMockito.mockStatic(ApplicationContext.class);
	}

	@Test
	public void setUpdateMetaDataTest() {
		LanguageDto dto = new LanguageDto();
		Language entity = new Language();

		dto.setCode("ENG");
		dto.setFamily("English");

		entity.setCode("eng");
		entity.setFamily("english");
		entity.setName("english");
		entity.setNativeName("english");
		entity.setIsActive(true);
		entity.setCrDtime(Timestamp.valueOf(LocalDateTime.now()));
		entity.setCrBy("admin");
		entity.setUpdBy("admin");

		setUpdateMetaData(dto, entity, false);

		assertTrue(entity.getCode().equals(dto.getCode()));
		assertTrue(entity.getFamily().equals(dto.getFamily()));
		assertTrue(entity.getName().equals("english"));
		assertTrue(entity.getNativeName().equals("english"));
		// assertTrue(entity.getUpdatedBy() != null);
		assertTrue(entity.getUpdDtimes() != null);
	}

	@Test
	public void setUpdateMetaDataTrueTest() {
		LanguageDto dto = new LanguageDto();
		Language entity = new Language();

		dto.setCode("ENG");
		dto.setFamily("English");

		entity.setCode("eng");
		entity.setFamily("english");
		entity.setName("english");
		entity.setNativeName("english");
		entity.setIsActive(true);
		entity.setCrDtime(Timestamp.valueOf(LocalDateTime.now()));
		entity.setCrBy("admin");
		entity.setUpdBy("admin");

		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		setUpdateMetaData(dto, entity, false);

		assertTrue(entity.getCode().equals(dto.getCode()));
		assertTrue(entity.getFamily().equals(dto.getFamily()));
		assertTrue(entity.getName().equals("english"));
		assertTrue(entity.getNativeName().equals("english"));
		// assertTrue(entity.getUpdatedBy() != null);
		assertTrue(entity.getUpdDtimes() != null);
	}

	@Test
	public void setCreateMetaDataTest() {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<? extends RegistrationCommonFields> userRoleClass = userRoleObj.getClass();
		assertNotNull(setCreateMetaData(new LanguageDto(), userRoleClass));
	}

	@Test
	public void setCreateMetaDataFalseTest() {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(Boolean.FALSE);
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<? extends RegistrationCommonFields> userRoleClass = userRoleObj.getClass();
		assertNotNull(setCreateMetaData(new LanguageDto(), userRoleClass));
	}

	@Test
	public void setCreateMetaDataListDTONotNullTest() {
		List<LanguageDto> languageDtoList = new ArrayList<LanguageDto>();
		LanguageDto lanDto = new LanguageDto();
		lanDto.setCode("ENG");
		lanDto.setFamily("English");
		languageDtoList.add(lanDto);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<? extends RegistrationCommonFields> userRoleClass = userRoleObj.getClass();
		assertNotNull(setCreateMetaData(languageDtoList, userRoleClass));
	}

	@Test
	public void setCreateMetaDataListDTONullTest() {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<? extends RegistrationCommonFields> userRoleClass = userRoleObj.getClass();
		assertNotNull(setCreateMetaData(null, userRoleClass));
	}

	@Test
	public void etCreateMetaDataListDTOFalseTest() {
		List<LanguageDto> languageDtoList = new ArrayList<LanguageDto>();
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(Boolean.FALSE);
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<? extends RegistrationCommonFields> userRoleClass = userRoleObj.getClass();
		assertNotNull(setCreateMetaData(languageDtoList, userRoleClass));
	}

	@Test
	public void setCreateJSONObjectToMetaDataTest() throws Throwable {
		Map<String, Object> applicationMap = new HashMap<>();
		UserRole userRole = new UserRole();
		Class<?> userRoleClass = userRole.getClass();
		applicationMap.put(RegistrationConstants.AGE_GROUP_CONFIG, "{'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}");
		when(context.map()).thenReturn(applicationMap);
		JSONObject ageGroupConfig = new JSONObject(
				(String) ApplicationContext.map().get(RegistrationConstants.AGE_GROUP_CONFIG));
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		RegistrationCommonFields userRoleObj = new UserRole();
		assertNotNull(MetaDataUtils.setCreateJSONObjectToMetaData(ageGroupConfig, userRoleClass));
	}

	@Test
	public void setCreateJSONObjectToMetaDataFalseTest() throws Throwable {
		Map<String, Object> applicationMap = new HashMap<>();
		UserRole userRole = new UserRole();
		Class<?> userRoleClass = userRole.getClass();
		applicationMap.put(RegistrationConstants.AGE_GROUP_CONFIG, "{'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}");
		when(context.map()).thenReturn(applicationMap);
		JSONObject ageGroupConfig = new JSONObject(
				(String) ApplicationContext.map().get(RegistrationConstants.AGE_GROUP_CONFIG));
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(Boolean.FALSE);
		RegistrationCommonFields userRoleObj = new UserRole();
		assertNotNull(MetaDataUtils.setCreateJSONObjectToMetaData(ageGroupConfig, userRoleClass));
	}

}
