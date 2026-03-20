package io.mosip.registration.test.util.mastersync;

import static io.mosip.registration.util.mastersync.MapperUtils.map;
import static io.mosip.registration.util.mastersync.MetaDataUtils.setCreateMetaData;
import static io.mosip.registration.util.mastersync.MetaDataUtils.setUpdateMetaData;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.core.authmanager.model.UserRoleDto;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dto.mastersync.AppAuthenticationMethodDto;
import io.mosip.registration.dto.mastersync.LanguageDto;
import io.mosip.registration.entity.AppAuthenticationMethod;
import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCommonFields;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.AppAuthenticationMethodId;
import io.mosip.registration.util.mastersync.MapperUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class })
public class MapperTest {
	


	@Before
	public void setup() throws Exception{
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");

	}

	@Test(expected = NullPointerException.class)
	public void map_sourceNull_throwsNullPointerException() {
		map(null, new Language());
	}
	
	@Test(expected = NullPointerException.class)
	public void setCreateMetaData_destinationNull_throwsNullPointerException() {
		setCreateMetaData(new LanguageDto(), null);
	}

	@Test
	public void map_languageDto_returnsEntityNotNull() {
		LanguageDto dto = getLanguageDto();
		assertNotNull(map(dto, new Language()));

	}

	@Test
	public void map_userRoleDto_returnsEntityNotNull() {
		UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setRole("role");
		assertNotNull(map(userRoleDto, new UserRole()));
	}

	@Test
	public void map_userRoleDto_generics_returnsEntityNotNull() {
		UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setRole("role");
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<?> userRoleClass = userRoleObj.getClass();
		assertNotNull(map(userRoleDto, userRoleClass));
	}

	@Test
	public void map_embeddedId_returnsDtoNotNull() {
		AppAuthenticationMethod appAuth = new AppAuthenticationMethod();
		AppAuthenticationMethodId appAuthId = new AppAuthenticationMethodId();
		appAuthId.setAppId("appId");
		appAuthId.setAuthMethodCode("authMethodCode");
		appAuthId.setProcessId("processId");
		appAuth.setAppAuthenticationMethodId(appAuthId);
		appAuth.setLangCode("langCode");
		assertNotNull(map(appAuth, new AppAuthenticationMethodDto()));
	}

	@Test
	public void mapJSONObjectToEntity_validJson_returnsEntityNotNull() throws Exception {
		JSONObject jsonObject = getJsonObject();
		AppAuthenticationMethod appAuthenticationMethod = new AppAuthenticationMethod();
		Class<?> appAuthenticationMethodClass = appAuthenticationMethod.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, appAuthenticationMethodClass));
	}

	@Test
	public void mapJSONObjectToEntity_fields_returnsEntityNotNull() throws Exception {
		JSONObject jsonObject = getJsonObject();
		AppAuthenticationMethod appAuthenticationMethod = new AppAuthenticationMethod();
		Class<?> appAuthenticationMethodClass = appAuthenticationMethod.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, appAuthenticationMethodClass));
	}

	@Test
	public void mapJSONObjectToEntity_sqlTimestamp_returnsEntityNotNull() throws Exception {
		JSONObject jsonObject = getSqlTimeJsonObject();
		GlobalParam globalParam = new GlobalParam();
		Class<?> globalParamClass = globalParam.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, globalParamClass));
	}

	@Ignore
	@Test
	public void mapJSONObjectToEntity_localTimestamp_returnsEntityNotNull() throws Exception {
		JSONObject jsonObject = getLocalTimeJsonObject();
		MachineMaster machineMaster = new MachineMaster();
		Class<?> machineMasterClass = machineMaster.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, machineMasterClass));
	}

	@Test
	public void setUpdateMetaData_withDto_updatesEntityFields() {
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
		//assertTrue(entity.getUpdatedBy() != null);
		assertTrue(entity.getUpdDtimes() != null);
	}

	private JSONObject getJsonObject() {

		JSONObject jsonObject = new JSONObject(
				"{'appAuthenticationMethodId':{'appId':'appId','processId':'processId','authMethodCode':'authMethodCode'},'langCode':'langCode'}");
		return jsonObject;
	}

	private JSONObject getSqlTimeJsonObject(){
		JSONObject jsonObject = new JSONObject(
				"{'globalParamId':{'code':'code','langCode':'langCode'},'isDeleted':true,'delDtimes':'Apr 11, 2022, 3:54:53 PM'}");
		return jsonObject;
	}
	
	private JSONObject getLocalTimeJsonObject(){
		JSONObject jsonObject = new JSONObject("{'validityDateTime':{'date':{'year':2022,'month':4,'day':11},'time':{'hour':12,'minute':35,'second':23,'nano':518264300}}}");
		return jsonObject;
	}
	private LanguageDto getLanguageDto() {		
		LanguageDto dto = new LanguageDto();
		dto.setCode("ENG");
		dto.setFamily("English");
		dto.setIsActive(Boolean.TRUE);
		dto.setCode("code");
		return dto;
	}

	private LocalDateTime invokeMethod(String value) {
		Method method = ReflectionUtils.findMethod(
				MapperUtils.class,
				"getLocalDateTimeValue",
				String.class
			);
		assertNotNull(method);
		method.setAccessible(true);
		return (LocalDateTime) ReflectionUtils.invokeMethod(method, null, value);
	}

	@Test
	public void getLocalDateTimeValue_validInstant_returnsLocalDateTime() {
		String input = "2024-01-15T10:20:30Z";

		LocalDateTime result = invokeMethod(input);

		assertNotNull(result);
		assertEquals(2024, result.getYear());
		assertEquals(Month.JANUARY, result.getMonth());
		assertEquals(15, result.getDayOfMonth());
		assertEquals(10, result.getHour());
		assertEquals(20, result.getMinute());
		assertEquals(30, result.getSecond());
	}

	@Test
	public void getLocalDateTimeValue_invalidFormat_returnsNull() {
		String input = "invalid-date-time";

		LocalDateTime result = invokeMethod(input);

		assertNull(result);
	}

	@Test
	public void getLocalDateTimeValue_emptyString_returnsNull() {
		String input = "";

		LocalDateTime result = invokeMethod(input);

		assertNull(result);
	}

	private LocalDate invokeGetLocalDateValue(String value) {
		Method method = ReflectionUtils.findMethod(
				MapperUtils.class,
				"getLocalDateValue",
				String.class
			);
		assertNotNull(method);
		method.setAccessible(true);
		return (LocalDate) ReflectionUtils.invokeMethod(method, null, value);
	}

	@Test
	public void getLocalDateValue_validDate_returnsLocalDate() {
		String input = "2024-02-10";

		LocalDate result = invokeGetLocalDateValue(input);

		assertNotNull(result);
		assertEquals(2024, result.getYear());
		assertEquals(Month.FEBRUARY, result.getMonth());
		assertEquals(10, result.getDayOfMonth());
	}

	@Test
	public void getLocalDateValue_invalidFormat_returnsNull() {
		String input = "10-02-2024";

		LocalDate result = invokeGetLocalDateValue(input);

		assertNull(result);
	}

	@Test
	public void getLocalDateValue_emptyString_returnsNull() {
		String input = "";

		LocalDate result = invokeGetLocalDateValue(input);

		assertNull(result);
	}

	static class TestDto {
		private String name;
		private int age;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
	}


	@Test
	public void convertJSONStringToDto_validJson_returnsDto() throws IOException {
		String json = "{\"name\":\"User\",\"age\":28}";

		TestDto dto = MapperUtils.convertJSONStringToDto(
				json,
				new TypeReference<>() {}
			);

		assertNotNull(dto);
		assertEquals("User", dto.getName());
		assertEquals(28, dto.getAge());
	}

	@Test(expected = IOException.class)
	public void convertJSONStringToDto_invalidJson_throwsIOException() throws IOException {
		String invalidJson = "{name:User, age:28}";

		MapperUtils.convertJSONStringToDto(
				invalidJson,
				new TypeReference<TestDto>() {}
			);
	}

	@Test
	public void convertObjectToJsonString_validDto_returnsJson() throws IOException {
		TestDto dto = new TestDto();
		dto.setName("MOSIP");
		dto.setAge(10);

		String json = MapperUtils.convertObjectToJsonString(dto);

		assertNotNull(json);
		assertTrue(json.contains("\"name\":\"MOSIP\""));
		assertTrue(json.contains("\"age\":10"));
	}

	@Test(expected = IOException.class)
	public void convertObjectToJsonString_invalidObject_throwsIOException() throws IOException {
		Object invalidObject = new Object() {
			Object self = this;
		};

		MapperUtils.convertObjectToJsonString(invalidObject);
	}

}
