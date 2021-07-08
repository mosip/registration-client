package io.mosip.registration.test.service.packet;

import static org.mockito.Mockito.when;

import java.util.*;

import io.mosip.registration.enums.Role;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.sync.PolicySyncService;
import org.junit.*;
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

import io.mosip.kernel.core.idgenerator.spi.PridGenerator;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.packet.impl.PacketHandlerServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ JsonUtils.class, ApplicationContext.class, SessionContext.class, Role.class })
public class PacketHandlerServiceTest {
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private PacketHandlerServiceImpl packetHandlerServiceImpl;
	
	@Mock
	private BaseService baseService;
	
	@Mock
	private RegistrationCenterDAO registrationCenterDAO;
	
	@Mock
	private GlobalParamService globalParamService;
	
	@Mock
	private AuditManagerSerivceImpl auditFactory;
	private ResponseDTO mockedSuccessResponse;

	@Mock
	private IdentitySchemaService identitySchemaService;

	@Mock
	private RidGenerator<String> ridGeneratorImpl;

	@Mock
	private PridGenerator<String> pridGenerator;

	@Mock
	private UserDetailService userDetailService;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private LocalConfigService localConfigService;

	@Mock
	private PolicySyncService policySyncService;

	@Before
	public void initialize() {
		mockedSuccessResponse = new ResponseDTO();
		mockedSuccessResponse.setSuccessResponseDTO(new SuccessResponseDTO());
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.mockStatic(Role.class);

		SessionContext.UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		userContext.setRoles(Arrays.asList("SUPERADMIN", "SUPERVISOR"));
		PowerMockito.when(SessionContext.userContext()).thenReturn(userContext);
		
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, "N");
	

		io.mosip.registration.context.ApplicationContext.setApplicationMap(applicationMap);
		
	}


	@Test
	public void startRegistration() throws RegBaseCheckedException {

		List<UiSchemaDTO> defaultFields = new LinkedList<>();

		UiSchemaDTO uiSchemaDTO = new UiSchemaDTO();
		uiSchemaDTO.setGroup(RegistrationConstants.UI_SCHEMA_GROUP_FULL_NAME);
		defaultFields.add(uiSchemaDTO);
		Mockito.when(ridGeneratorImpl.generateId(Mockito.anyString(), Mockito.anyString())).thenReturn("12345678901");
		Mockito.when(identitySchemaService.getLatestEffectiveSchemaVersion()).thenReturn(2.0);
		Mockito.when(pridGenerator.generateId()).thenReturn("0987654321");
		PowerMockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
		
		Mockito.when(identitySchemaService.getUISchema(2.0)).thenReturn(defaultFields);

		PowerMockito.when(SessionContext.userId()).thenReturn("12345");
		Mockito.when(userDetailService.isValidUser("12345")).thenReturn(true);

//		PowerMockito.when(SessionContext.userId()).thenReturn("12345");
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);

		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setIsActive(true);

		machineMaster.setId("123");

		Mockito.when(machineMasterRepository.findByNameIgnoreCase(machineName.toLowerCase())).thenReturn(machineMaster);

		when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);

		
		when(registrationCenterDAO.isMachineCenterActive("123")).thenReturn(true);

		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setMessage(RegistrationConstants.VALID_KEY);
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		when(policySyncService.checkKeyValidation()).thenReturn(responseDTO);
		
		packetHandlerServiceImpl.startRegistration(null, RegistrationConstants.PACKET_TYPE_NEW);
	}

	@Test
	public void testCreationException() throws RegBaseCheckedException {

		ResponseDTO actualResponse = packetHandlerServiceImpl.handle(new RegistrationDTO());

		Assert.assertEquals(RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE.getErrorCode(),
				actualResponse.getErrorResponseDTOs().get(0).getCode());
	}

	@Test
	public void testHandlerChkException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException("errorCode", "errorMsg");
		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

	@Test
	public void testHandlerAuthenticationException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException(
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode(),
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorMessage());

		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

}
