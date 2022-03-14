package io.mosip.registration.test.service.packet;

import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.idgenerator.spi.PridGenerator;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.enums.Role;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.packet.impl.PacketHandlerServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.test.util.datastub.DataProvider;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ JsonUtils.class, ApplicationContext.class, SessionContext.class, Role.class, FileUtils.class, Paths.class, CryptoUtil.class, ClientCryptoUtils.class,  MosipDeviceSpecificationFactory.class })
public class PacketHandlerServiceTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private PacketHandlerServiceImpl packetHandlerServiceImpl;
	
	@Mock
	private BIRBuilder birBuilder;
	
	@Mock
	private BaseService baseService;
	
	@Mock
	private RegistrationCenterDAO registrationCenterDAO;
	
	@Mock
	private RegistrationDAO registrationDAO;
	
	@Mock
	private PacketWriter packetWriter;
	
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
	private AuditDAO auditDAO;
	
	@Mock
	private Environment environment;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@Mock
	private SoftwareUpdateHandler softwareUpdateHandler;	
	
	@Mock
	private ClientCryptoFacade clientCryptoFacade;
	
	@Mock
	private ClientCryptoService clientCryptoService;
	
	@Mock
	private MachineMappingDAO machineMappingDAO;
	
	@Mock
	private Path pMock;
	
	@Mock
	private File file;

	@Mock
	private MasterSyncService masterSyncService;
	
	private RegistrationDTO registrationDTO;

	@Before
	public void initialize() throws Exception {
		mockedSuccessResponse = new ResponseDTO();
		mockedSuccessResponse.setSuccessResponseDTO(new SuccessResponseDTO());
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.mockStatic(Role.class);

		SessionContext.UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		userContext.setRoles(Arrays.asList("SUPERADMIN", "SUPERVISOR"));
		
		RegistrationCenterDetailDTO registrationCenterDetailDTO = new RegistrationCenterDetailDTO();
		registrationCenterDetailDTO.setRegistrationCenterLatitude("101.11");
		registrationCenterDetailDTO.setRegistrationCenterLongitude("102.11");
		userContext.setRegistrationCenterDetailDTO(registrationCenterDetailDTO);
		
		PowerMockito.when(SessionContext.userContext()).thenReturn(userContext);
		
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, "N");
		applicationMap.put(RegistrationConstants.USER_CENTER_ID, "10011");
		applicationMap.put(RegistrationConstants.USER_STATION_ID, "10011");
		applicationMap.put(RegistrationConstants.AUDIT_TIMESTAMP, "2021-12-13 16:09:14");
		
		PowerMockito.doReturn(applicationMap).when(ApplicationContext.class, "map");

		io.mosip.registration.context.ApplicationContext.setApplicationMap(applicationMap);
		ReflectionTestUtils.setField(packetHandlerServiceImpl, "source", "REGISTRATION_CLIENT");
		Mockito.when(environment.getProperty(RegistrationConstants.GPS_DEVICE_DISABLE_FLAG)).thenReturn("N");
		when(masterSyncService.getAllBlockListedWords()).thenReturn(Collections.emptyList());
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

		
		when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		successResponseDTO.setMessage(RegistrationConstants.VALID_KEY);
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		when(policySyncService.checkKeyValidation()).thenReturn(responseDTO);
		packetHandlerServiceImpl.startRegistration(null, FlowType.NEW.getCategory());
	}

	@Test
	public void testPacketHandle() throws RegBaseCheckedException, JsonProcessingException {
		registrationDTO = DataProvider.getFilledPacketDTO(Arrays.asList("eng", "fra"));
		
		List<PacketInfo> packetInfo = new ArrayList<>();
		PacketInfo pInfo = new PacketInfo();
		pInfo.setId("test-packet-id");
		packetInfo.add(pInfo);
		
		SchemaDto schema = new SchemaDto();
		schema.setSchemaJson("test-json");
		Mockito.when(identitySchemaService.getIdentitySchema(Mockito.anyDouble())).thenReturn(schema);
		Mockito.doNothing().when(packetWriter).setField(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		
		List<Audit> audits = new ArrayList<>();
		Audit audit = new Audit();
		audit.setId("TEST");
		audit.setCreatedAt(LocalDateTime.now());
		audit.setActionTimeStamp(LocalDateTime.now());
		audits.add(audit);
		Mockito.when(auditDAO.getAudits(Mockito.anyString(), Mockito.anyString())).thenReturn(audits);
		
		Map<String, String> checkSumMap = new HashMap<>();
		checkSumMap.put("test key", "test val");
		Mockito.when(softwareUpdateHandler.getJarChecksum()).thenReturn(checkSumMap);
		Mockito.when(objectMapper.writeValueAsString(checkSumMap)).thenReturn("checksum");
		
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setId("10011");
		machineMaster.setRegCenterId("10012");
		Mockito.when(machineMappingDAO.getMachine()).thenReturn(machineMaster);
		
		Mockito.doNothing().when(packetWriter).addAudits(Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(packetWriter).addMetaInfo(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString(), Mockito.anyString());
		when(packetWriter.persistPacket(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
				Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(packetInfo);
		Mockito.doNothing().when(registrationDAO).save(Mockito.anyString(), Mockito.any(RegistrationDTO.class));
		Mockito.doNothing().when(globalParamService).update(Mockito.anyString(), Mockito.anyString());
		BIR bir = new BIR();
		BDBInfo bdbInfo = new BDBInfo();
		bdbInfo.setIndex("1");
		bir.setBdbInfo(bdbInfo );
		Mockito.when(birBuilder.buildBIR(Mockito.any(BiometricsDto.class))).thenReturn(bir);
		
		PowerMockito.mockStatic(MosipDeviceSpecificationFactory.class);
		Map<String, MdmBioDevice> deviceMap = new HashMap<>();
		MdmBioDevice device = new MdmBioDevice();
		device.setSerialNumber("1234");
		device.setSerialVersion("test_version");
		device.setDeviceCode("test_code");
		device.setDeviceMake("test_make");
		deviceMap.put("FINGERPRINT_DEVICE", device);
		Mockito.when(MosipDeviceSpecificationFactory.getDeviceRegistryInfo()).thenReturn(deviceMap);
		ResponseDTO response = packetHandlerServiceImpl.handle(registrationDTO);
		Assert.assertNotNull(response.getSuccessResponseDTO());
	}
	
	@Test
	public void testPacketHandleException() throws RegBaseCheckedException {
		registrationDTO = DataProvider.getFilledPacketDTO(Arrays.asList("eng", "fra"));
		RegBaseCheckedException exception = new RegBaseCheckedException("REG-PAC-001", "Exception while packet creation");
		Mockito.when(identitySchemaService.getIdentitySchema(Mockito.anyDouble())).thenThrow(exception);
		Assert.assertNotNull(packetHandlerServiceImpl.handle(registrationDTO).getErrorResponseDTOs());
	}
	
	@Test
	public void testCreationException() throws RegBaseCheckedException {
		ResponseDTO actualResponse = packetHandlerServiceImpl.handle(new RegistrationDTO());
		Assert.assertEquals(RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE.getErrorCode(),
				actualResponse.getErrorResponseDTOs().get(0).getCode());
	}

	@Test
	public void testHandlerChkException() throws RegBaseCheckedException {
		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

	@Test
	public void testHandlerAuthenticationException() throws RegBaseCheckedException {
		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}
	
	@Test
	public void getAcknowledgementReceiptTest() throws Exception {
		Registration registration = new Registration();
		registration.setPacketId("1234");
		registration.setId("1234");
		//registration.setAckSignature("test");
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		
		byte[] testBytes = "test_byte".getBytes();
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn(testBytes);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn(testBytes);
		Mockito.when(clientCryptoFacade.encrypt(Mockito.any(), Mockito.any())).thenReturn(testBytes);
		
		ReflectionTestUtils.setField(packetHandlerServiceImpl, "baseLocation", "//logs");
		ReflectionTestUtils.setField(packetHandlerServiceImpl, "packetManagerAccount", "//registration");
		PowerMockito.mockStatic(Paths.class);
		Mockito.when(Paths.get(Mockito.any(String.class))).thenReturn(pMock);
		Mockito.when(pMock.toFile()).thenReturn(file);
		
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.doNothing().when(FileUtils.class, "copyToFile", Mockito.any(), Mockito.any());
		
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		Mockito.when(registrationDAO.updateAckReceiptSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(registration);
		
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(testBytes);
		Mockito.when(FileUtils.readFileToByteArray(Mockito.any())).thenReturn(testBytes);
		
		PowerMockito.mockStatic(ClientCryptoUtils.class);
		Mockito.when(ClientCryptoUtils.decodeBase64Data(Mockito.anyString())).thenReturn(testBytes);
		Mockito.when(clientCryptoService.validateSignature(Mockito.any(), Mockito.any())).thenReturn(true);
		
		Assert.assertNotNull(packetHandlerServiceImpl.getAcknowledgmentReceipt("1234", "../packets"));
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getAcknowledgementReceiptExceptionTest() throws Exception {
		Registration registration = new Registration();
		registration.setPacketId("1234");
		registration.setId("1234");
		//registration.setAckSignature("test");
		Mockito.when(registrationDAO.getRegistrationByPacketId(Mockito.anyString())).thenReturn(registration);
		
		byte[] testBytes = "test_byte".getBytes();
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn(testBytes);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn(testBytes);
		Mockito.when(clientCryptoFacade.encrypt(Mockito.any(), Mockito.any())).thenReturn(testBytes);
		
		ReflectionTestUtils.setField(packetHandlerServiceImpl, "baseLocation", "//logs");
		ReflectionTestUtils.setField(packetHandlerServiceImpl, "packetManagerAccount", "//registration");
		
		PowerMockito.mockStatic(FileUtils.class);
		IOException exception = new IOException("EXP-001", "Failed to sign and encrypt existing ack receipt");
		PowerMockito.doThrow(exception).when(FileUtils.class, "copyToFile", Mockito.any(), Mockito.any());
		
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		Mockito.when(registrationDAO.updateAckReceiptSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(registration);
		
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(testBytes);
		Mockito.when(FileUtils.readFileToByteArray(Mockito.any())).thenReturn(testBytes);
		
		PowerMockito.mockStatic(ClientCryptoUtils.class);
		Mockito.when(ClientCryptoUtils.decodeBase64Data(Mockito.anyString())).thenReturn(testBytes);
		Mockito.when(clientCryptoService.validateSignature(Mockito.any(), Mockito.any())).thenReturn(false);
		
		packetHandlerServiceImpl.getAcknowledgmentReceipt("1234", "../packets");
	}
	
	@Test
	public void getAllPacketsTest() {
		List<Registration> registeredPackets = new ArrayList<>();
		Registration reg = new Registration();
		reg.setClientStatusCode(RegistrationClientStatusCode.CREATED.getCode());
		registeredPackets.add(reg);
		Mockito.when(registrationDAO.getAllRegistrations()).thenReturn(registeredPackets);
		Assert.assertNotNull(packetHandlerServiceImpl.getAllPackets());
	}
	
	@Test
	public void getAllRegistrationsTest() {
		List<Registration> registeredPackets = new ArrayList<>();
		Registration reg = new Registration();
		reg.setClientStatusCode(RegistrationClientStatusCode.CREATED.getCode());
		registeredPackets.add(reg);
		Mockito.when(registrationDAO.getAllRegistrations()).thenReturn(registeredPackets);
		Assert.assertNotNull(packetHandlerServiceImpl.getAllRegistrations());
	}

}
