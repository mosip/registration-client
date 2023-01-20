package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

import io.mosip.commons.packet.util.PacketManagerHelper;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.impl.UserOnboardDAOImpl;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.repositories.UserBiometricRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserMachineMappingRepository;
import io.mosip.registration.service.BaseService;

/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class, ApplicationContext.class })
public class UserOnBoardDAOImlpTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private UserMachineMappingRepository userMachineMappingRepository;

	@Mock
	private UserBiometricRepository userBiometricRepository;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@InjectMocks
	private UserOnboardDAOImpl userOnboardDAOImpl;

	@InjectMocks
	private BaseService baseService;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;
	
	@Mock
	private UserDetailRepository userDetailRepository;
	
	@Mock
	private PacketManagerHelper packetManagerHelper;

	@Before
	public void initialize() throws Exception {
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");

		Map<String, Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.USER_STATION_ID, "1947");
		appMap.put(RegistrationConstants.USER_CENTER_ID, "1947");
		PowerMockito.when(ApplicationContext.map()).thenReturn(appMap);

	}

	@Test
	public void UserOnBoardSuccess() throws IOException {

		List<UserBiometric> bioMetricsList = new ArrayList<>();

		BiometricDTO biometricDTO = new BiometricDTO();

		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		File file = new File(URLDecoder.decode(ClassLoader.getSystemResource("ISOTemplate.iso").getFile(), "UTF-8"));
		byte[] data = FileUtils.readFileToByteArray(file);

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint(data);
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		List<IrisDetailsDTO> iriesList = new ArrayList<>();
		IrisDetailsDTO iries = new IrisDetailsDTO();

		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);

		FaceDetailsDTO face = new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);

		BiometricInfoDTO info = new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);

		biometricDTO.setOperatorBiometricDTO(info);

		Mockito.when(userBiometricRepository.saveAll(bioMetricsList)).thenReturn(bioMetricsList);
		doNothing().when(userBiometricRepository).deleteByUserBiometricIdUsrId(Mockito.anyString());

		assertNotNull(userOnboardDAOImpl.insert(biometricDTO));

	}

	@Test
	public void savetest() {
		UserMachineMapping machineMapping = new UserMachineMapping();
		Mockito.when(userMachineMappingRepository.save(Mockito.any(UserMachineMapping.class)))
				.thenReturn(machineMapping);
		userOnboardDAOImpl.save();
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RuntimeException.class)
	public void saveFailuretest() {

		UserMachineMapping machineMapping = new UserMachineMapping();
		Mockito.when(userMachineMappingRepository.save(Mockito.any(UserMachineMapping.class)))
				.thenThrow(RuntimeException.class);
		userMachineMappingRepository.save(machineMapping);
	}

	@SuppressWarnings("serial")
	@Test
	public void UserOnBoardException() {

		BiometricDTO biometricDTO = new BiometricDTO();

		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		List<IrisDetailsDTO> iriesList = new ArrayList<>();
		IrisDetailsDTO iries = new IrisDetailsDTO();

		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);

		FaceDetailsDTO face = new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);

		BiometricInfoDTO info = new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);

		biometricDTO.setOperatorBiometricDTO(info);

		try {
			Mockito.when(userBiometricRepository.saveAll(anyObject())).thenThrow(new RuntimeException("...") {
			});
			assertNotNull(userOnboardDAOImpl.insert(biometricDTO));
		} catch (Exception e) {

		}

	}

	@Test(expected = RegBaseUncheckedException.class)
	public void getStationIDRunException() throws RegBaseCheckedException {
		Mockito.when(machineMasterRepository
				.findByNameIgnoreCase(Mockito.anyString()))
				.thenThrow(new RegBaseUncheckedException());
		baseService.getStationId();
	}

	@Test
	public void getStationID() throws RegBaseCheckedException {
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setName("localhost");
		machineMaster.setIsActive(true);
		machineMaster.setMacAddress("8C-16-45-88-E7-0C");

		machineMaster.setId("100311");
		Mockito.when(machineMasterRepository
				.findByNameIgnoreCase(Mockito.anyString()))
				.thenReturn(machineMaster);
		String stationId = baseService.getStationId();
		Assert.assertSame("100311", stationId);
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void getCenterIDRunExceptionTest() throws RegBaseCheckedException {
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString()))
				.thenThrow(new RegBaseUncheckedException());
		baseService.getCenterId();
	}

	@Test
	public void getCenterID() throws RegBaseCheckedException {
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setId("StationID1947");
		machineMaster.setRegCenterId("CenterID1947");
		machineMaster.setIsActive(true);

		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString()))
				.thenReturn(machineMaster);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		RegistrationCenter registrationCenter = new RegistrationCenter();
		registrationCenter.setRegistartionCenterId(new RegistartionCenterId());
		registrationCenter.getRegistartionCenterId().setId(machineMaster.getRegCenterId());
		registrationCenter.getRegistartionCenterId().setLangCode("eng");
		Optional<RegistrationCenter> mockedCenter = Optional.of(registrationCenter);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(mockedCenter);

		Assert.assertSame("CenterID1947", baseService.getCenterId());
	}

	@Test
	public void getLastUpdatedTime() {
		UserMachineMapping userMachineMapping = new UserMachineMapping();		
		userMachineMapping.setCrDtime(new Timestamp(System.currentTimeMillis()));		
		Mockito.when(userMachineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(Mockito.anyString())).thenReturn(userMachineMapping);
		Assert.assertNotNull(userOnboardDAOImpl.getLastUpdatedTime("Usr123"));
	}
	
	@Test(expected = RuntimeException.class)
	public void getLastUpdatedTimeFailure() {		
		Mockito.when(userMachineMappingRepository.findByUserMachineMappingIdUsrIdIgnoreCase(Mockito.anyString())).thenThrow(RuntimeException.class);
		Assert.assertNotNull(userOnboardDAOImpl.getLastUpdatedTime("Usr123"));
	}
	
	@Test
	public void insertTest() {
		List<FingerprintDetailsDTO> fingerPrints = getFingerPrintDataList();
		BiometricDTO biometricDTO = new BiometricDTO();
		List<UserBiometric> bioMetricsList = getUserBiometrics();
		BiometricInfoDTO biometInfo = new BiometricInfoDTO();
		biometInfo.setFingerprintDetailsDTO(fingerPrints);
		biometricDTO.setOperatorBiometricDTO(biometInfo);
		Mockito.when(userBiometricRepository.saveAll(bioMetricsList)).thenReturn(bioMetricsList);
		assertEquals(RegistrationConstants.SUCCESS,userOnboardDAOImpl.insert(biometricDTO));
	}
	
	
	@Test
	@Ignore
	public void insertListBiometricsTest() {
		List<BiometricsDto> biometrics = getBioMetricsDTOList();		
		assertNotNull(userOnboardDAOImpl.insert(biometrics));
	}
	
	@Test
	public void insertExtractedTemplatesTest() throws Exception {
		List<BIR> templates = getTemplates();
		List<UserBiometric> bioMetricsList = getUserBiometrics();
		UserDetail userDetail = getUserDetails();
		Mockito.when(userBiometricRepository.findByUserBiometricIdUsrId(Mockito.anyString())).thenReturn(bioMetricsList);
		Mockito.when(userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(Mockito.anyString())).thenReturn(userDetail);
		Mockito.when(packetManagerHelper.getXMLData(Mockito.any(BiometricRecord.class), Mockito.anyBoolean())).thenReturn("testXML".getBytes());
		assertEquals(RegistrationConstants.SUCCESS,userOnboardDAOImpl.insertExtractedTemplates(templates));
	};

	@Test
	@Ignore
	public void insertExtractedTemplatesFalseTest() {
		List<BIR> templates = getTemplates();
		List<UserBiometric> existingBiometrics = null;
		UserDetail userDetail = null;
		Mockito.when(userBiometricRepository.findByUserBiometricIdUsrId(Mockito.any())).thenReturn(existingBiometrics);
		Mockito.when(userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(Mockito.anyString())).thenReturn(userDetail);
		assertEquals(RegistrationConstants.SUCCESS,userOnboardDAOImpl.insertExtractedTemplates(templates));
	};

		
	@Test
	public void saveTest() {
		assertEquals(RegistrationConstants.SUCCESS,userOnboardDAOImpl.save());
	}

	@Test
	@Ignore
	public void insertWithBioMetricsListTest() {
		List<BiometricsDto> biometrics = getBioMetricsDTOList();
		List<UserBiometric> existingBiometrics = getUserBiometrics();
		Mockito.when(userBiometricRepository.findByUserBiometricIdUsrId(Mockito.anyString())).thenReturn(existingBiometrics);
		assertEquals(RegistrationConstants.SUCCESS,userOnboardDAOImpl.insert(biometrics));
	}
	
	
	UserDetail getUserDetails() {		
		UserDetail userdtl = new UserDetail();
		Set<UserBiometric> userBiometrics = new HashSet<UserBiometric>();
		UserBiometric userBio = new UserBiometric();
		userBiometrics.add(userBio);
		userdtl.setUserBiometric(userBiometrics);
		return userdtl;
		
	}
	List<UserBiometric> getUserBiometrics(){		
		List<UserBiometric> biometrics = new ArrayList<UserBiometric>();
		UserBiometric ubio = new UserBiometric();
		UserDetail udtl = new UserDetail();
		ubio.setUserDetail(udtl);
		biometrics.add(ubio);
		return biometrics;
		
	}
	
	List<FingerprintDetailsDTO> getFingerPrintDataList() {
		List<FingerprintDetailsDTO> fingerprints = new ArrayList<FingerprintDetailsDTO>();
		FingerprintDetailsDTO fingerPrintData = new FingerprintDetailsDTO();
		byte[] fingerPrintISOImag = {1,2,3,4};
		fingerPrintData.setFingerprintImageName("image");
		fingerPrintData.setFingerPrintISOImage(fingerPrintISOImag);
		fingerPrintData.setNumRetry(12);
		fingerPrintData.setQualityScore(20.0);		
		fingerprints.add(fingerPrintData);
		fingerPrintData.setSegmentedFingerprints(fingerprints);
		return fingerprints;
	}
	
	List<IrisDetailsDTO> getIrisDataList() {		
		List<IrisDetailsDTO> irisDtlsDataList = new ArrayList<IrisDetailsDTO>();
		IrisDetailsDTO iries = new IrisDetailsDTO();
		byte[] irisISOImag = {1,2,3,4};
		iries.setIrisImageName("image");		
		iries.setIrisIso(irisISOImag);
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(2);
		irisDtlsDataList.add(iries);
		return irisDtlsDataList;
	}
	
	List<BiometricDTO> getBioMetricDTOList(){		
		byte[] faceISO = {1,2,3,4};
		List<BiometricDTO> biometrics = new ArrayList<BiometricDTO>();
		BiometricDTO biometricDTO = new BiometricDTO();
		BiometricInfoDTO bometricInfoDTO = new BiometricInfoDTO();
		FaceDetailsDTO faceDtlsDTO = new FaceDetailsDTO();
		faceDtlsDTO.setFaceISO(faceISO);
		faceDtlsDTO.setNumOfRetries(2);
		faceDtlsDTO.setQualityScore(2);
		bometricInfoDTO.setFace(faceDtlsDTO);
		biometricDTO.setOperatorBiometricDTO(bometricInfoDTO);		
		biometricDTO.getOperatorBiometricDTO().getFace().getQualityScore();
		biometrics.add(biometricDTO);
		return biometrics;
	}
	
	List<BiometricsDto> getBioMetricsDTOList(){	
		String str1 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData1 = new byte[str1.length()];
		List<BiometricsDto> biometrics = new ArrayList<BiometricsDto>();
		BiometricsDto dto = new BiometricsDto("rightIndex", byteData1, 80.0);		
		dto.setBioAttribute("LEFT_LITTLE");
		dto.setAttributeISO(byteData1);
		dto.setNumOfRetries(2);
		dto.setQualityScore(new Double(2));
		biometrics.add(dto);
		
		String str2 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData2 = new byte[str2.length()];
		BiometricsDto dto1 = new BiometricsDto("rightIndex", byteData2, 80.0);		
		dto1.setBioAttribute("LEFT_LITTLE");
		dto1.setAttributeISO(byteData2);
		dto1.setNumOfRetries(2);
		dto1.setQualityScore(new Double(2));
		biometrics.add(dto1);
		return biometrics;
	}
	
	/**
	 * return template list data
	 * @return
	 */
	List<BIR> getTemplates(){
		List<BIR> templates = new ArrayList<BIR>();
		List<String> subTypes = new ArrayList<String>();
		byte[] bdbInfoImg = {1,2,3,4};
		subTypes.add("LeftIndexFinger");
		BIR temp1 = new BIR();
		BDBInfo bdbInfo = new BDBInfo();
		bdbInfo.setSubtype(subTypes);
		QualityType quaType = new QualityType();
		quaType.setScore(new Long(2));
		bdbInfo.setQuality(quaType);
		temp1.setBdbInfo(bdbInfo);
		temp1.setBdb(bdbInfoImg);
		templates.add(temp1);
		
		templates.forEach( temp -> System.out.println(temp.getBdbInfo().getSubtype()));
		return templates;
	}

}

