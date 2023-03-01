package io.mosip.registration.test.service.packet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.PreRegistrationDataSyncDAO;
import io.mosip.registration.dao.impl.RegistrationCenterDAOImpl;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.PreRegistrationExceptionJSONInfoDTO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.PreRegistrationList;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.external.PreRegZipHandlingService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.impl.PreRegistrationDataSyncServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, SessionContext.class, ApplicationContext.class, RegistrationSystemPropertiesChecker.class, FileUtils.class })
public class PreRegistrationDataSyncServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private PreRegistrationDataSyncServiceImpl preRegistrationDataSyncServiceImpl;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private SyncManager syncManager;

	@Mock
	private PreRegistrationDataSyncDAO preRegistrationDAO;

	@Mock
	SyncTransaction syncTransaction;

	@Mock
	PreRegistrationList preRegistrationList;

	@Mock
	PreRegZipHandlingService preRegZipHandlingService;
	
	@Mock
	private UserDetailService userDetailService;
	
	@Mock
	private CenterMachineReMapService centerMachineReMapService;
	
	@Mock
	private MachineMasterRepository machineMasterRepository;
	
	@Mock
	private RegistrationCenterDAOImpl registrationCenterDAO;

	static byte[] preRegPacket;

	static Map<String, Object> preRegData = new HashMap<>();
	
	private ExecutorService executorServiceForPreReg;
	
	@Before
	public void dtoInitalization() {
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");
	}

	@BeforeClass
	public static void initialize() throws IOException, UnsupportedEncodingException {
		URL url = PreRegistrationDataSyncServiceImpl.class.getResource("/preRegSample.zip");
		File packetZipFile = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
		preRegPacket = FileUtils.readFileToByteArray(packetZipFile);

		preRegData.put(RegistrationConstants.PRE_REG_FILE_NAME, "filename_2018-12-12 09:39:08.272.zip");
		preRegData.put(RegistrationConstants.PRE_REG_FILE_CONTENT, preRegPacket);
	}

	@Before
	public void initiate() throws Exception {
		executorServiceForPreReg = Executors.newFixedThreadPool(2);
		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.PRE_REG_DELETION_CONFIGURED_DAYS, "45");
		applicationMap.put(RegistrationConstants.PRE_REG_DAYS_LIMIT, "5");
		applicationMap.put(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.DISABLE);

		PowerMockito.mockStatic(SessionContext.class, ApplicationContext.class, RegistrationSystemPropertiesChecker.class);
		Mockito.when(ApplicationContext.map()).thenReturn(applicationMap);

		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		RegistrationCenterDetailDTO registrationCenterDetailDTO = new RegistrationCenterDetailDTO();
		registrationCenterDetailDTO.setRegistrationCenterId("10031");
		PowerMockito.when(SessionContext.userContext().getRegistrationCenterDetailDTO())
				.thenReturn(registrationCenterDetailDTO);
		PowerMockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		PowerMockito.when(SessionContext.userId()).thenReturn("mosip");
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
		
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");
		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setRegCenterId("10011");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
	}

	@AfterClass
	public static void destroySession() {
		SessionContext.destroySession();
	}

	@After
	public void destroy() {
		preRegistrationDataSyncServiceImpl.destroy();
	}
	
	@Test
	public void getPreRegistrationsTest() throws RegBaseCheckedException, ConnectionException {
		LinkedHashMap<String, Object> postResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseData = new LinkedHashMap<>();

		HashMap<String, String> map = new HashMap<>();
		map.put("70694681371453", "2019-01-17T05:42:35.747Z");
		responseData.put("preRegistrationIds", map);

		postResponse.put("response", responseData);
		mockPreRegServices(postResponse);

		mockEncryptedPacket();
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");
		Mockito.when(preRegZipHandlingService.encryptAndSavePreRegPacket(Mockito.anyString(), Mockito.any())).thenReturn(preRegistrationDTO);
		RegistrationDTO reg = new RegistrationDTO();
		Mockito.when(preRegZipHandlingService.extractPreRegZipFile(Mockito.any())).thenReturn(reg);
		
		PreRegistrationList preRegList = new PreRegistrationList();
		preRegList.setPacketPath("/preRegSample.zip");
		preRegList.setLastUpdatedPreRegTimeStamp(Timestamp.from(Instant.now()));
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(preRegList);
		Mockito.when(preRegistrationDAO.update(Mockito.any())).thenReturn(preRegList);

		preRegistrationDataSyncServiceImpl.getPreRegistrationIds("System");
	}

	protected void mockPreRegServices(LinkedHashMap<String, Object> postResponse) throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(postResponse);
		// Mockito.when(preRegistrationResponseDTO.getResponse()).thenReturn(list);

		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("pre-registration-id", "70694681371453");
		valuesMap.put("registration-client-id", "10003");
		valuesMap.put("appointment-date", "2019-06-16");
		valuesMap.put("from-time-slot", "09:00");
		valuesMap.put("to-time-slot", "09:15");
		valuesMap.put("zip-filename", "70694681371453");
		valuesMap.put("zip-bytes", RegistrationConstants.FACE_STUB);
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(new PreRegistrationList());
		Mockito.when(
				serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(syncManager.createSyncTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(syncTransaction);
		Mockito.when(preRegistrationDAO.save(preRegistrationList)).thenReturn(preRegistrationList);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
	}

	@Test
	public void getPreRegistrationsAlternateFlowTest() throws RegBaseCheckedException, ConnectionException {
		LinkedHashMap<String, Object> postResponse = new LinkedHashMap<>();

		mockPreRegServices(postResponse);
		mockEncryptedPacket();

		preRegistrationDataSyncServiceImpl.getPreRegistrationIds("System");
	}
	
	@Test
	public void getPreRegistrationsAlternateFlowTest2() throws RegBaseCheckedException, ConnectionException {
		LinkedHashMap<String, Object> postResponse = new LinkedHashMap<>();
		List<PreRegistrationExceptionJSONInfoDTO> errorList = new ArrayList<>();
		PreRegistrationExceptionJSONInfoDTO exception = new PreRegistrationExceptionJSONInfoDTO();
		exception.setErrorCode("PRG_BOOK_RCI_032");
		exception.setMessage("test error message");
		errorList.add(exception);
		postResponse.put("errors", errorList);

		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(postResponse);

		Assert.assertNotNull(preRegistrationDataSyncServiceImpl.getPreRegistrationIds("System").getSuccessResponseDTO());
	}

	@Test
	public void getPreRegistrationTest() throws RegBaseCheckedException, ConnectionException {
		mockData();

		mockEncryptedPacket();
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");
		Mockito.when(preRegZipHandlingService
		.encryptAndSavePreRegPacket(Mockito.anyString(), Mockito.any())).thenReturn(preRegistrationDTO);
		RegistrationDTO reg = new RegistrationDTO();
		Mockito.when(preRegZipHandlingService.extractPreRegZipFile(Mockito.any())).thenReturn(reg);
		
		PreRegistrationList preRegList = new PreRegistrationList();
		preRegList.setPacketPath("/preRegSample.zip");
		preRegList.setLastUpdatedPreRegTimeStamp(Timestamp.valueOf(LocalDateTime.of(2022, 1, 17, 12, 12)));
		
		PowerMockito.mockStatic(FileUtils.class);
		File file = Mockito.mock(File.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(file);
		Mockito.when(file.exists()).thenReturn(true);
		
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(preRegList);
		Mockito.when(preRegistrationDAO.update(Mockito.any())).thenReturn(preRegList);

		ResponseDTO responseDTO = preRegistrationDataSyncServiceImpl.getPreRegistration("70694681371453", true);
		assertNotNull(responseDTO);
	}
	
	@Test
	public void getPreRegistrationPacketResponseExceptionTest() throws RegBaseCheckedException, ConnectionException {
		mockData();

		mockEncryptedPacket();
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");
		Mockito.when(preRegZipHandlingService
		.encryptAndSavePreRegPacket(Mockito.anyString(), Mockito.any())).thenReturn(preRegistrationDTO);
		RegistrationDTO reg = new RegistrationDTO();
		Mockito.when(preRegZipHandlingService.extractPreRegZipFile(Mockito.any())).thenThrow(RegBaseCheckedException.class);
		
		PreRegistrationList preRegList = new PreRegistrationList();
		preRegList.setPacketPath("/preRegSample.zip");
		preRegList.setLastUpdatedPreRegTimeStamp(Timestamp.valueOf(LocalDateTime.of(2022, 1, 17, 12, 12)));
		
		PowerMockito.mockStatic(FileUtils.class);
		File file = Mockito.mock(File.class);
		Mockito.when(FileUtils.getFile(Mockito.anyString())).thenReturn(file);
		Mockito.when(file.exists()).thenReturn(true);
		
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(preRegList);
		Mockito.when(preRegistrationDAO.update(Mockito.any())).thenReturn(preRegList);

		ResponseDTO responseDTO = preRegistrationDataSyncServiceImpl.getPreRegistration("70694681371453", true);
		assertNotNull(responseDTO.getErrorResponseDTOs());
	}

	protected void mockData() throws RegBaseCheckedException, ConnectionException {
		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("pre-registration-id", "70694681371453");
		valuesMap.put("registration-client-id", "10003");
		valuesMap.put("appointment-date", "2019-06-16");
		valuesMap.put("from-time-slot", "09:00");
		valuesMap.put("to-time-slot", "09:15");
		valuesMap.put("zip-filename", "70694681371453");
		valuesMap.put("zip-bytes", RegistrationConstants.FACE_STUB);
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(
				serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(syncManager.createSyncTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(syncTransaction);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
	}

	@Test
	public void getPreRegistrationAlternateTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(
				serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenReturn(223233223);
		Mockito.when(syncManager.createSyncTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(syncTransaction);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);

		// Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(new
		// PreRegistrationList());

		ResponseDTO responseDTO = preRegistrationDataSyncServiceImpl.getPreRegistration("70694681371453", false);
		assertNotNull(responseDTO);
	}

	@Test
	public void getPreRegistrationNegativeTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(
				serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenThrow(HttpClientErrorException.class);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		preRegistrationDataSyncServiceImpl.getPreRegistration("70694681371453", false);
	}

	@Test
	public void getPreRegistrationExceptionTest() throws RegBaseCheckedException, ConnectionException {
		mockData();

		mockEncryptedPacket();
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");
		Mockito.when(preRegZipHandlingService
		.encryptAndSavePreRegPacket(Mockito.anyString(), Mockito.any())).thenReturn(preRegistrationDTO);
	
		doThrow(new RegBaseCheckedException()).when(preRegZipHandlingService).extractPreRegZipFile(Mockito.any());

		preRegistrationDataSyncServiceImpl.getPreRegistration("70694681371453", false);
	}

	@Test
	public void getPreRegistrationsTestNegative()
			throws RegBaseCheckedException, ConnectionException { // Test-2
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString()))
				.thenThrow(HttpClientErrorException.class);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		preRegistrationDataSyncServiceImpl.getPreRegistrationIds("System");
	}

	private void mockEncryptedPacket() throws RegBaseCheckedException {
		mockEncryptedData();
		RegistrationDTO reg = new RegistrationDTO();
		Mockito.when(preRegZipHandlingService.extractPreRegZipFile(preRegPacket)).thenReturn(reg);
	}

	protected void mockEncryptedData() throws RegBaseCheckedException {
		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath("path");
		preRegistrationDTO.setSymmetricKey("0E8BAAEB3CED73CBC9BF4964F321824A");
		preRegistrationDTO.setEncryptedPacket(preRegPacket);
		preRegistrationDTO.setPreRegId("70694681371453");

		Mockito.when(preRegZipHandlingService.encryptAndSavePreRegPacket("70694681371453", preRegPacket))
				.thenReturn(preRegistrationDTO);

		Mockito.when(preRegZipHandlingService.decryptPreRegPacket("0E8BAAEB3CED73CBC9BF4964F321824A", preRegPacket))
				.thenReturn(preRegPacket);
	}

	@Test
	public void fetchAndDeleteRecordsTest() throws java.io.IOException {
		File file = mockDeleteMethodFiles();
		preRegistrationDataSyncServiceImpl.fetchAndDeleteRecords();

		if (file.exists()) {
			file.delete();
		}
	}

	@Test
	public void fetchAndDeleteRecordsRuntimeExceptionTest() throws java.io.IOException {
		File file = mockDeleteMethodFiles();
		doThrow(new RuntimeException()).when(preRegistrationDAO).deleteAll(Mockito.anyList());
		preRegistrationDataSyncServiceImpl.fetchAndDeleteRecords();

		if (file.exists()) {
			file.delete();
		}
	}

	protected File mockDeleteMethodFiles() throws java.io.IOException {
		File file = new File("testDeletePacket.txt");
		file.createNewFile();
		List<PreRegistrationList> preRegList = new ArrayList<>();
		PreRegistrationList preRegistrationList = new PreRegistrationList();
		preRegistrationList.setPacketPath(file.getAbsolutePath());
		preRegList.add(preRegistrationList);
		Mockito.when(preRegistrationDAO.fetchRecordsToBeDeleted(Mockito.any())).thenReturn(preRegList);
		Mockito.when(preRegistrationDAO.update(Mockito.any())).thenReturn(preRegistrationList);
		return file;
	}

	@Test
	public void fetchAndDeleteRecordsAlternateTest() throws java.io.IOException {
		File file = new File("testDeletePac.txt");
		file.createNewFile();
		List<PreRegistrationList> preRegList = new ArrayList<>();
		PreRegistrationList preRegistrationList = new PreRegistrationList();
		preRegistrationList.setPacketPath(file.getAbsolutePath());
		preRegList.add(preRegistrationList);
		Mockito.when(preRegistrationDAO.fetchRecordsToBeDeleted(Mockito.any())).thenReturn(null);
		Mockito.when(preRegistrationDAO.update(Mockito.any())).thenReturn(preRegistrationList);
		preRegistrationDataSyncServiceImpl.fetchAndDeleteRecords();

		if (file.exists()) {
			file.delete();
		}
	}

	@Test
	public void getPreRegistrationRecordForDeletionTest() throws java.io.IOException {
		PreRegistrationList preRegistrationList = new PreRegistrationList();
		preRegistrationList.setId("123456789");
		preRegistrationList.setPreRegId("987654321");
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(preRegistrationList);
		//Mockito.when(StringUtils.isEmpty(null)).thenReturn(true);
		PreRegistrationList preRegistration = preRegistrationDataSyncServiceImpl
				.getPreRegistrationRecordForDeletion("987654321");

		assertTrue(preRegistration.getId().equals("123456789"));
		assertTrue(preRegistration.getPreRegId().equals("987654321"));
	}
	
	@Test
	public void getPreRegistrationRecordForDeletionFailureTest() throws java.io.IOException {
		PreRegistrationList preRegistrationList = new PreRegistrationList();
		preRegistrationList.setId("123456789");
		preRegistrationList.setPreRegId(null);
		Mockito.when(preRegistrationDAO.get(Mockito.anyString())).thenReturn(preRegistrationList);
		PreRegistrationList preRegistration = preRegistrationDataSyncServiceImpl
				.getPreRegistrationRecordForDeletion(null);
	}
	
	@Test
	public void getLastUpdatedTime() {
		Mockito.when(preRegistrationDAO.getLastPreRegPacketDownloadedTime()).thenReturn(new Timestamp(System.currentTimeMillis()));
		assertNotNull(preRegistrationDataSyncServiceImpl.getLastPreRegPacketDownloadedTime());
	}
}
