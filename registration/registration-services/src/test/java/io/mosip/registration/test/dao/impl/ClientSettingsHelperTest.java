package io.mosip.registration.test.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dao.impl.MasterSyncDaoImpl;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.*;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
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
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;
import io.mosip.registration.util.mastersync.MetaDataUtils;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@SpringBootTest
@PrepareForTest({ MetaDataUtils.class, RegBaseUncheckedException.class, SessionContext.class, MasterSyncDaoImpl.class,
		ClientSettingSyncHelper.class, BiometricAttributeRepository.class, RegistrationAppHealthCheckUtil.class })
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
	private BlacklistedWordsRepository masterSyncBlacklistedWordsRepository;

	@Mock
	private DocumentCategoryRepository masterSyncDocumentCategoryRepository;

	@Mock
	private DocumentTypeRepository masterSyncDocumentTypeRepository;

	@Mock
	private IdTypeRepository masterSyncIdTypeRepository;

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
	private TemplateFileFormatRepository masterSyncTemplateFileFormatRepository;

	@Mock
	private TemplateRepository masterSyncTemplateRepository;

	@Mock
	private TemplateTypeRepository masterSyncTemplateTypeRepository;

	@Mock
	private ApplicantValidDocumentRepository masterSyncValidDocumentRepository;

	@Mock
	private ValidDocumentRepository validDocumentRepository;

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
	private RegistrationCenterUserRepository registrationCenterUserRepository;

	@Mock
	private CenterMachineRepository centerMachineRepository;

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
	public void testClientSettingsSyncForValidJson() throws RegBaseCheckedException, ConnectionException, IOException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn("[]".getBytes(StandardCharsets.UTF_8));

		Map<String, Object> map = new LinkedHashMap<>();
		SchemaDto schemaDto = new SchemaDto();
		schemaDto.setSchemaJson("");
		schemaDto.setSchema(new ArrayList<>());
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
