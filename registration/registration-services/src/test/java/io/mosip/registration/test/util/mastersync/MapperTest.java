package io.mosip.registration.test.util.mastersync;

import static io.mosip.registration.util.mastersync.MapperUtils.map;
import static io.mosip.registration.util.mastersync.MetaDataUtils.setCreateMetaData;
import static io.mosip.registration.util.mastersync.MetaDataUtils.setUpdateMetaData;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;

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
	public void testMapSourceNull() {
		map(null, new Language());
	}
	
	@Test(expected = NullPointerException.class)
	public void testMapMetaDataDestinationNull() {
		setCreateMetaData(new LanguageDto(), null);
	}

	@Test
	public void testMapSource() {
		LanguageDto dto = getLanguageDto();
		assertNotNull(map(dto, new Language()));

	}

	@Test
	public void testMapSourceDTO() {
		UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setRole("role");
		assertNotNull(map(userRoleDto, new UserRole()));
	}

	@Test
	public void testMapSourceDTOGenerics() {
		UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setRole("role");
		RegistrationCommonFields userRoleObj = new UserRole();
		Class<?> userRoleClass = userRoleObj.getClass();
		assertNotNull(map(userRoleDto, userRoleClass));
	}

	@Test
	public void testMapSourceEmbededId() {
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
	public void testMapJSONObjectToEntity() throws Exception {
		JSONObject jsonObject = getJsonObject();
		AppAuthenticationMethod appAuthenticationMethod = new AppAuthenticationMethod();
		Class<?> appAuthenticationMethodClass = appAuthenticationMethod.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, appAuthenticationMethodClass));
	}

	@Test
	public void testMapJSONObjectToEntityFields() throws Exception {
		JSONObject jsonObject = getJsonObject();
		AppAuthenticationMethod appAuthenticationMethod = new AppAuthenticationMethod();
		Class<?> appAuthenticationMethodClass = appAuthenticationMethod.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, appAuthenticationMethodClass));
	}

	@Test
	public void testMapJSONObjectToEntitySqltimeStamp() throws Exception {
		JSONObject jsonObject = getSqlTimeJsonObject();
		GlobalParam globalParam = new GlobalParam();
		Class<?> globalParamClass = globalParam.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, globalParamClass));
	}

	@Ignore
	@Test
	public void testMapJSONObjectToEntityLocaltimeStamp() throws Exception {
		JSONObject jsonObject = getLocalTimeJsonObject();
		MachineMaster machineMaster = new MachineMaster();
		Class<?> machineMasterClass = machineMaster.getClass();
		assertNotNull(MapperUtils.mapJSONObjectToEntity(jsonObject, machineMasterClass));
	}

	@Test
	public void testSetUpdateMetaData() {
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
	
}
