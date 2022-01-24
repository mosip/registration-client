package io.mosip.registration.test.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dao.impl.MasterSyncDaoImpl;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.AppAuthenticationRepository;
import io.mosip.registration.repositories.AppRolePriorityRepository;
import io.mosip.registration.repositories.ApplicantValidDocumentRepository;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BiometricTypeRepository;
import io.mosip.registration.repositories.BlocklistedWordsRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.DynamicFieldRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationHierarchyRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.MachineSpecificationRepository;
import io.mosip.registration.repositories.MachineTypeRepository;
import io.mosip.registration.repositories.PermittedLocalConfigRepository;
import io.mosip.registration.repositories.ProcessListRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.repositories.RegistrationCenterTypeRepository;
import io.mosip.registration.repositories.ScreenAuthorizationRepository;
import io.mosip.registration.repositories.ScreenDetailRepository;
import io.mosip.registration.repositories.SyncJobControlRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.repositories.TemplateRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;
import io.mosip.registration.util.mastersync.MetaDataUtils;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ MetaDataUtils.class, RegBaseUncheckedException.class, SessionContext.class, MasterSyncDaoImpl.class,
		BiometricAttributeRepository.class, RegistrationAppHealthCheckUtil.class, Paths.class, CryptoUtil.class, FileUtils.class, ClientCryptoUtils.class })
public class ClientSettingsHelperTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private SyncJobControlRepository syncStatusRepository;

	@Mock
	private BiometricAttributeRepository biometricAttributeRepository;

	@Mock
	private BiometricTypeRepository masterSyncBiometricTypeRepository;

	@Mock
	private BlocklistedWordsRepository masterSyncBlocklistedWordsRepository;

	@Mock
	private DocumentCategoryRepository masterSyncDocumentCategoryRepository;

	@Mock
	private DocumentTypeRepository masterSyncDocumentTypeRepository;


	@Mock
	private LanguageRepository masterSyncLanguageRepository;

	@Mock
	private LocationRepository masterSyncLocationRepository;

	@Mock
	private LocationHierarchyRepository locationHierarchyRepository;


	@Mock
	private MachineMasterRepository masterSyncMachineRepository;

	@Mock
	private MachineSpecificationRepository masterSyncMachineSpecificationRepository;

	@Mock
	private MachineTypeRepository masterSyncMachineTypeRepository;

	@Mock
	private ReasonCategoryRepository reasonCategoryRepository;

	@Mock
	private ReasonListRepository masterSyncReasonListRepository;

	@Mock
	private RegistrationCenterRepository masterSyncRegistrationCenterRepository;

	@Mock
	private RegistrationCenterTypeRepository masterSyncRegistrationCenterTypeRepository;

	@Mock
	private TemplateRepository masterSyncTemplateRepository;

	@Mock
	private ApplicantValidDocumentRepository masterSyncValidDocumentRepository;

	@Mock
	private AppAuthenticationRepository appAuthenticationRepository;

	@Mock
	private AppRolePriorityRepository appRolePriorityRepository;

	@Mock
	private ScreenAuthorizationRepository screenAuthorizationRepository;

	@Mock
	private ProcessListRepository processListRepository;

	@Mock
	private UserMachineMappingRepository userMachineMappingRepository;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;

	@Mock
	private RegistrationCenterTypeRepository registrationCenterTypeRepository;

	@Mock
	private ScreenDetailRepository screenDetailRepository;

	@Mock
	private SyncJobDefRepository syncJobDefRepository;

	@InjectMocks
	private ClientSettingSyncHelper clientSettingSyncHelper;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private ClientCryptoService clientCryptoService;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private IdentitySchemaDao identitySchemaDao;

	@Mock
	private LocalConfigService localConfigService;

	@Mock
	private PermittedLocalConfigRepository permittedLocalConfigRepository;
	
	@Mock
	private DynamicFieldRepository dynamicFieldRepository;
	
	@Mock
	private Path pMock;
	
	@Mock
	private File file;

	@Test(expected = RegBaseUncheckedException.class)
	public void testSingleEntity() {
		String response = null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");
		response = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
		assertEquals(RegistrationConstants.SUCCESS, response);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testEmptyJsonRegBaseUncheckedException() {
		String response = null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("emptyJson.json");
		clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
	}

	@Test
	public void testClientSettingsSyncForValidJson() throws RegBaseCheckedException, ConnectionException, IOException, io.mosip.kernel.core.exception.IOException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(Paths.class);
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.mockStatic(ClientCryptoUtils.class);
		Mockito.when(Paths.get(Mockito.anyString(), Mockito.anyString())).thenReturn(pMock);
		Mockito.when(pMock.toFile()).thenReturn(file);
		
		String data = "[{\n" + 
				"	\"headers\": \"test-headers\",\n" + 
				"	\"auth-required\": true,\n" + 
				"	\"auth-token\": \"test-token\",\n" + 
				"	\"encrypted\": true\n" + 
				"}]";
		
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(data.getBytes(StandardCharsets.UTF_8));
		
		String jsonObjData = "{\n" + 
				"  	\"url\": \"https://dev.mosip.net\",\n" + 
				"	\"headers\": \"test-headers\",\n" + 
				"	\"auth-required\": true,\n" + 
				"	\"auth-token\": \"test-token\",\n" + 
				"	\"encrypted\": false\n" + 
				"}";
		byte[] bytes = "test".getBytes();
		Mockito.when(ClientCryptoUtils.decodeBase64Data("BVlY")).thenReturn(bytes);
		Mockito.when(clientCryptoFacade.decrypt(bytes)).thenReturn(jsonObjData.getBytes(StandardCharsets.UTF_8));
		
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(CryptoUtil.computeFingerPrint(Mockito.any(byte[].class), Mockito.anyString())).thenReturn("test");
		Mockito.doNothing().when(serviceDelegateUtil).download(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean());
		
		Mockito.when(FileUtils.readFileToString(Mockito.any(File.class), Mockito.any(Charset.class))).thenReturn("");
		
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		Map<String, Object> map = new LinkedHashMap<>();
		SchemaDto schemaDto = new SchemaDto();
		schemaDto.setSchemaJson(""); 
		map.put("response", schemaDto);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(),Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(map);

		String response = null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");
		response = clientSettingSyncHelper.saveClientSettings(syncDataResponseDto);
		assertEquals(RegistrationConstants.SUCCESS, response);
	}

	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {

		ObjectMapper mapper = new ObjectMapper();
		SyncDataResponseDto syncDataResponseDto = null;

		try {
			syncDataResponseDto = mapper.readValue(
					new File(getClass().getClassLoader().getResource(fileName).getFile()), SyncDataResponseDto.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return syncDataResponseDto;
	}

}
