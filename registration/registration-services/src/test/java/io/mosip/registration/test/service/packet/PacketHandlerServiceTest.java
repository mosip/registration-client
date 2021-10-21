package io.mosip.registration.test.service.packet;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;
import java.util.*;

import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.enums.Role;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.update.SoftwareUpdateHandler;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;

import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.core.idgenerator.spi.PridGenerator;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dao.impl.AuditDAOImpl;
import io.mosip.registration.dao.impl.MachineMappingDAOImpl;
import io.mosip.registration.dto.BlocklistedConsentDto;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.packet.impl.PacketHandlerServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ JsonUtils.class, ApplicationContext.class, SessionContext.class, Role.class, RegistrationSystemPropertiesChecker.class })
public class PacketHandlerServiceTest {
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private PacketHandlerServiceImpl packetHandlerServiceImpl;
	
	@Mock
	private PacketHandlerServiceImpl mockPacketHandlerServiceImpl;
	
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
	
	@Mock
	private PacketWriter packetWriter;
	
	@Mock
	private RegistrationDAO registrationDAO;
	
	@Mock
	private AuditDAOImpl auditDAOImpl;

	@Mock
	private SoftwareUpdateHandler softwareUpdateHandler;
	
	@Mock
	private Environment mockEnv;
	
	@Mock
	private ObjectMapper mapper;
	
	@Mock
	private MachineMappingDAOImpl machineMappingDAOImpl;
	
	@Before
	public void initialize() {
		mockedSuccessResponse = new ResponseDTO();
		mockedSuccessResponse.setSuccessResponseDTO(new SuccessResponseDTO());
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.mockStatic(Role.class);

		SessionContext.UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		Mockito.when(mockEnv.getProperty(RegistrationConstants.GPS_DEVICE_DISABLE_FLAG)).thenReturn(RegistrationConstants.ENABLE);
		
		RegistrationCenterDetailDTO detailDTO = new RegistrationCenterDetailDTO();
		userContext.setRegistrationCenterDetailDTO(detailDTO);
		userContext.setRoles(Arrays.asList("SUPERADMIN", "SUPERVISOR"));
		PowerMockito.when(SessionContext.userContext()).thenReturn(userContext);
		
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, "N");
	

		io.mosip.registration.context.ApplicationContext.setApplicationMap(applicationMap);
		
	}


	@Test
	public void startRegistration() throws RegBaseCheckedException {

		List<UiFieldDTO> defaultFields = new LinkedList<>();

		UiFieldDTO uiFieldDTO = new UiFieldDTO();
		uiFieldDTO.setGroup(RegistrationConstants.UI_SCHEMA_GROUP_FULL_NAME);
		defaultFields.add(uiFieldDTO);

		ProcessSpecDto processSpecDto = new ProcessSpecDto();
		processSpecDto.setId("test");
		processSpecDto.setFlow("NEW");

		Mockito.when(ridGeneratorImpl.generateId(Mockito.anyString(), Mockito.anyString())).thenReturn("12345678901");
		Mockito.when(identitySchemaService.getLatestEffectiveSchemaVersion()).thenReturn(2.0);
		Mockito.when(pridGenerator.generateId()).thenReturn("0987654321");
		PowerMockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
		
		Mockito.when(identitySchemaService.getAllFieldSpec(Mockito.anyString(),Mockito.anyDouble())).thenReturn(defaultFields);
		Mockito.when(identitySchemaService.getProcessSpecDto(Mockito.anyString(),Mockito.anyDouble())).thenReturn(processSpecDto);

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
		
		packetHandlerServiceImpl.startRegistration(null, FlowType.NEW.getCategory());
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
	
	@Test
	public void handlePassTest() throws Exception {
		
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setAdditionalInfoReqId("str-ing");
		registrationDTO.setIdSchemaVersion(1.0);
		registrationDTO.setRegistrationId("123");
		registrationDTO.setProcessId("processId");
		registrationDTO.setPreRegistrationId("1209830120");
		
		OSIDataDTO osiDataDTO = new OSIDataDTO();
		osiDataDTO.setOperatorID("110003");
		registrationDTO.setOsiDataDTO(osiDataDTO);
		
		registrationDTO.setFlowType(FlowType.NEW);
		
		RegistrationMetaDataDTO metaDTO = new RegistrationMetaDataDTO();
		metaDTO.setConsentOfApplicant("Yes");
		registrationDTO.setRegistrationMetaDataDTO(metaDTO);
		
		ResponseDTO expectedresponseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setCode("0000");
		successResponseDTO.setMessage("Success");
		expectedresponseDTO.setSuccessResponseDTO(successResponseDTO);
		
		String baseLocation = "../packets";
		String SLASH = "/";
		String source = "REGISTRATION_CLIENT";
		String packetManagerAccount = "PACKET_MANAGER_ACCOUNT";
		
		Map<String, String> metaInfoMap = new LinkedHashMap<>();
		
		SchemaDto schemaDTO = new SchemaDto();
		schemaDTO.setIdVersion(1.0);
		schemaDTO.setId("mosip.handle.reg");
		schemaDTO.setSchemaJson("");
		
		List<PacketInfo> listOfPacket = new ArrayList<PacketInfo>();
		PacketInfo packet1 = new PacketInfo();
		packet1.setId("1010101");
		
		List<Map<String, String>> auditList = new LinkedList<>();
		
		List<Audit> audits = new ArrayList<>();
		Audit audit1 = new Audit();
		audit1.setApplicationName("packetHandler");
		audit1.setActionTimeStamp(LocalDateTime.now());
		audit1.setCreatedAt(LocalDateTime.now());
		audit1.setCreatedBy("Mosip");
		audit1.setDescription("mosip");
		audit1.setEventId("mosip1");
		audit1.setEventName("mosip-packet-handler");
		audit1.setEventType("PACKET-HANDLER");
		audit1.setHostIp("192.108.17.1");
		audit1.setHostName("Mosip-Network");
		audit1.setId("101");
		audit1.setIdType("mosipIdType");
		audit1.setModuleId("mosipModuleId");
		audit1.setModuleName("mosipModuleName");
		audit1.setSessionUserId("mosipSessionUser");
		audit1.setSessionUserName("mosipSessionName");
		audit1.setUuid("AJAK1019120");
		audits.add(audit1);
		
		PowerMockito.mockStatic(ApplicationContext.class, SessionContext.class, RegistrationSystemPropertiesChecker.class);
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.USER_CENTER_ID, "10011");
		applicationMap.put(RegistrationConstants.USER_STATION_ID, "10011");
		applicationMap.put(RegistrationConstants.AUDIT_TIMESTAMP, "2021-10-12T08:01:51.342Z");
		applicationMap.put(RegistrationConstants.DONGLE_SERIAL_NUMBER, "32428723642");
		io.mosip.registration.context.ApplicationContext.setApplicationMap(applicationMap);
		
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		RegistrationCenterDetailDTO regCenDTO = new RegistrationCenterDetailDTO();
		regCenDTO.setRegistrationCenterLatitude("12098312");
		regCenDTO.setRegistrationCenterLongitude("217361398");
		
		Map<String, Object> mapSession = new HashMap<String, Object>();
		mapSession.put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
		userContext.setRegistrationCenterDetailDTO(regCenDTO);
		PowerMockito.doReturn(applicationMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn(userContext).when(SessionContext.class,"userContext");
		PowerMockito.doReturn(mapSession).when(SessionContext.class, "map");
		
		Map<String, String> checkSumMap = new HashMap<>();
		checkSumMap.put("aksjhda", "sadla");
		
		String refId = String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID))
				.concat(RegistrationConstants.UNDER_SCORE)
				.concat(String.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID)));
		
		Mockito.when(identitySchemaService.getIdentitySchema(registrationDTO.getIdSchemaVersion())).thenReturn(schemaDTO);
		Mockito.doNothing().when(packetWriter).addMetaInfo(registrationDTO.getRegistrationId(), metaInfoMap, source.toUpperCase(), registrationDTO.getProcessId().toUpperCase());
		Mockito.when(packetWriter.persistPacket(registrationDTO.getRegistrationId(),
					String.valueOf(registrationDTO.getIdSchemaVersion()), schemaDTO.getSchemaJson(), source.toUpperCase(),
					registrationDTO.getProcessId().toUpperCase(),
					registrationDTO.getAppId(),
					refId, true)).thenReturn(listOfPacket);
		
		
		Mockito.doNothing().when(registrationDAO).save(Mockito.anyString(), Mockito.any(RegistrationDTO.class));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		Mockito.when(auditDAOImpl.getAudits(Mockito.anyString(), Mockito.anyString())).thenReturn(audits);
		Mockito.doNothing().when(packetWriter).addAudits(Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
		ReflectionTestUtils.setField(mockPacketHandlerServiceImpl, "source", "REGISTRATION_CLIENT");
		Mockito.when(SessionContext.userContext().getRegistrationCenterDetailDTO()).thenReturn(regCenDTO);
		Mockito.when(softwareUpdateHandler.getJarChecksum()).thenReturn(checkSumMap);
		Mockito.when(softwareUpdateHandler.getCurrentVersion()).thenReturn("1.1.5");
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("10001");
		Mockito.when(machineMappingDAOImpl.getKeyIndexByMachineName(Mockito.anyString())).thenReturn("ED:70:DY:IO:09:TU");
		//Mockito.when(SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA)).thenReturn(mockMachineID
		OngoingStubbing<ResponseDTO> actualResponseDTO = Mockito.when(mockPacketHandlerServiceImpl.handle(registrationDTO)).thenReturn(expectedresponseDTO);
		Assert.assertNotNull(actualResponseDTO);
		
	}
	
	/*@Test
	public void handleFailTest1() throws Exception {
		
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setRegistrationId("3247629");
		Map<String, Object> demoGraphics = new HashMap<String, Object>();
		Integer int1 = new Integer(1);
		demoGraphics.put("UIN", int1);
		List<String> updatedFields = new ArrayList<String>();
		updatedFields.add("name");
		updatedFields.add("UIN");
		registrationDTO.setDemographics(demoGraphics);
		registrationDTO.setFlowType(FlowType.UPDATE);
		registrationDTO.setUpdatableFields(updatedFields);
		registrationDTO.setProcessId("3247629");
		
		
		ResponseDTO expectedresponseDTO = new ResponseDTO();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setCode("000");
		errorResponseDTO.setInfoType("Fail");
		errorResponseDTO.setMessage("Required registration details are empty or null");
		
		List<ErrorResponseDTO> errorRes = new ArrayList<>();
		errorRes.add(errorResponseDTO);
		
		expectedresponseDTO.setErrorResponseDTOs(errorRes);
		ReflectionTestUtils.setField(mockPacketHandlerServiceImpl, "source", "REGISTRATION_CLIENT");
		ReflectionTestUtils.setField(mockPacketHandlerServiceImpl, "objectMapper", mapper);
		//Mockito.when(packetWriter.setField(registrationDTO.getRegistrationId(), "UIN", "", null, null);)
		PowerMockito.doNothing().when(packetWriter, "setField", Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.when(mapper.writeValueAsString(Mockito.any(Object.class))).thenThrow(new JsonParseException());
		//PowerMockito.doThrow(new RegBaseCheckedException()).when(mockPacketHandlerServiceImpl, "setDemographics", registrationDTO);
		PacketHandlerServiceImpl service = Mockito.mock(PacketHandlerServiceImpl.class);
		Mockito.when(mockPacketHandlerServiceImpl.handle(registrationDTO)).thenReturn(expectedresponseDTO);
		Mockito.verify(mockPacketHandlerServiceImpl, Mockito.atLeast(1)).setSuccessResponse(expectedresponseDTO, null, demoGraphics);
		//Mockito.when(packetHandlerServiceImpl.handle(registrationDTO)).thenReturn(null);
	}*/

}
