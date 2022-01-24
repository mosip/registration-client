package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.impl.MasterSyncDaoImpl;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.BiometricAttribute;
import io.mosip.registration.entity.BlocklistedWords;
import io.mosip.registration.entity.DocumentCategory;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Language;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.RegistrationCenterType;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.BiometricAttributeRepository;
import io.mosip.registration.repositories.BlocklistedWordsRepository;
import io.mosip.registration.repositories.DocumentCategoryRepository;
import io.mosip.registration.repositories.DocumentTypeRepository;
import io.mosip.registration.repositories.LanguageRepository;
import io.mosip.registration.repositories.LocationHierarchyRepository;
import io.mosip.registration.repositories.LocationRepository;
import io.mosip.registration.repositories.ReasonCategoryRepository;
import io.mosip.registration.repositories.ReasonListRepository;
import io.mosip.registration.repositories.SyncJobControlRepository;
import io.mosip.registration.repositories.SyncJobDefRepository;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.mastersync.ClientSettingSyncHelper;
import io.mosip.registration.util.mastersync.MetaDataUtils;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@SpringBootTest
@PrepareForTest({ MetaDataUtils.class, RegBaseUncheckedException.class, SessionContext.class,
		BiometricAttributeRepository.class, RegistrationAppHealthCheckUtil.class })
public class MasterSyncDaoImplTest {

	// private MapperFacade mapperFacade = CustomObjectMapper.MAPPER_FACADE;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	/** Object for Sync Status Repository. */
	@Mock
	private SyncJobControlRepository syncStatusRepository;

	/** Object for Sync Biometric Attribute Repository. */
	@Mock
	private BiometricAttributeRepository biometricAttributeRepository;

	/** Object for Sync Blocklisted Words Repository. */
	@Mock
	private BlocklistedWordsRepository blocklistedWordsRepository;

	/** Object for Sync Document Category Repository. */
	@Mock
	private DocumentCategoryRepository documentCategoryRepository;

	/** Object for Sync Document Type Repository. */
	@Mock
	private DocumentTypeRepository documentTypeRepository;

	/** Object for Sync Location Repository. */
	@Mock
	private LocationRepository locationRepository;

	/** Object for Sync Reason Category Repository. */
	@Mock
	private ReasonCategoryRepository reasonCategoryRepository;

	/** Object for Sync Reason List Repository. */
	@Mock
	private ReasonListRepository reasonListRepository;

	/** Object for Sync language Repository. */
	@Mock
	private LanguageRepository languageRepository;

	/** Object for Sync screen auth Repository. */
	@Mock
	private SyncJobDefRepository syncJobDefRepository;

	@Mock
	private ClientSettingSyncHelper clientSettingSyncHelper;

	@Mock
	private LocationHierarchyRepository locationHierarchyRepository;

	/*@Mock
	private MasterSyncDao masterSyncDao;*/
	
	/*@Mock
	private MetaDataUtils metaDataUtils;
	
	@Mock
	private MapperUtils mapperUtils;
	
	@InjectMocks
	private MasterSyncServiceImpl masterSyncServiceImpl;*/
	
	@InjectMocks
	private MasterSyncDaoImpl masterSyncDaoImpl;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	
	@Before
	public void initialize() throws Exception {
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		//Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		//Mockito.when(clientCryptoService.asymmetricDecrypt(Mockito.any())).thenReturn("[]".getBytes(StandardCharsets.UTF_8));

		/*Map<String, Object> response = new HashMap<>();
		response.put("response", null);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.any(),
				Mockito.anyString())).thenReturn(response);*/
	}

	@BeforeClass
	public static void beforeClass() {

		List<RegistrationCenterType> registrationCenterType = new ArrayList<>();
		RegistrationCenterType MasterRegistrationCenterType = new RegistrationCenterType();
		MasterRegistrationCenterType.setCode("T1011");
		MasterRegistrationCenterType.setName("ENG");
		MasterRegistrationCenterType.setLangCode("Main");
		registrationCenterType.add(MasterRegistrationCenterType);
	}

	@Test
	public void testMasterSyncDaoSucess() throws RegBaseCheckedException {

		SyncControl masterSyncDetails = new SyncControl();

		masterSyncDetails.setSyncJobId("MDS_J00001");
		masterSyncDetails.setLastSyncDtimes(new Timestamp(System.currentTimeMillis()));
		masterSyncDetails.setCrBy("mosip");
		masterSyncDetails.setIsActive(true);
		masterSyncDetails.setLangCode("eng");
		masterSyncDetails.setCrDtime(new Timestamp(System.currentTimeMillis()));

		Mockito.when(syncStatusRepository.findBySyncJobId(Mockito.anyString())).thenReturn(masterSyncDetails);

		masterSyncDaoImpl.syncJobDetails("MDS_J00001");

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMasterSyncExceptionThrown() throws RegBaseUncheckedException {

		try {
			Mockito.when(masterSyncDaoImpl.syncJobDetails(Mockito.anyString()))
					.thenThrow(RegBaseUncheckedException.class);
			masterSyncDaoImpl.syncJobDetails("MDS_J00001");
		} catch (Exception exception) {

		}
	}

	@Test
	public void findLocationByLangCode() throws RegBaseCheckedException {

		List<Location> locations = new ArrayList<>();
		Location locattion = new Location();
		locattion.setCode("LOC01");
		locattion.setName("english");
		locattion.setLangCode("ENG");
		locattion.setHierarchyLevel(1);
		locattion.setHierarchyName("english");
		locattion.setParentLocCode("english");
		locations.add(locattion);

		Mockito.when(locationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(locations);

		masterSyncDaoImpl.findLocationByLangCode(1, "ENG");

		assertTrue(locations != null);
	}

	@Test
	public void findLocationByParentLocCode() throws RegBaseCheckedException {

		List<Location> locations = new ArrayList<>();
		Location locattion = new Location();
		locattion.setCode("LOC01");
		locattion.setName("english");
		locattion.setLangCode("ENG");
		locattion.setHierarchyLevel(1);
		locattion.setHierarchyName("english");
		locattion.setParentLocCode("english");
		locations.add(locattion);

		Mockito.when(locationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(locations);

		masterSyncDaoImpl.findLocationByParentLocCode("TPT", "eng");

		assertTrue(locations != null);
	}

	@Test
	public void findAllReason() throws RegBaseCheckedException {

		List<ReasonCategory> allReason = new ArrayList<>();
		ReasonCategory reasons = new ReasonCategory();
		reasons.setCode("DEMO");
		reasons.setName("InvalidData");
		reasons.setLangCode("FRE");
		allReason.add(reasons);

		Mockito.when(reasonCategoryRepository.findByIsActiveTrueAndLangCode(Mockito.anyString())).thenReturn(allReason);

		masterSyncDaoImpl.getAllReasonCatogery(Mockito.anyString());

		assertTrue(allReason != null);
	}

	@Test
	public void findAllReasonList() throws RegBaseCheckedException {

		List<String> reasonCat = new ArrayList<>();
		List<ReasonList> allReason = new ArrayList<>();
		ReasonList reasons = new ReasonList();
		reasons.setCode("DEMO");
		reasons.setName("InvalidData");
		reasons.setLangCode("FRE");
		allReason.add(reasons);

		Mockito.when(reasonListRepository
				.findByIsActiveTrueAndLangCodeAndReasonCategoryCodeIn(Mockito.anyString(), Mockito.anyList()))
				.thenReturn(allReason);

		masterSyncDaoImpl.getReasonList("FRE", reasonCat);

		assertTrue(allReason != null);
	}

	@Test
	public void findBlockWords() throws RegBaseCheckedException {

		List<BlocklistedWords> allBlockWords = new ArrayList<>();
		BlocklistedWords blockWord = new BlocklistedWords();
		blockWord.setWord("asdfg");
		blockWord.setDescription("asdfg");
		blockWord.setLangCode("ENG");
		allBlockWords.add(blockWord);
		allBlockWords.add(blockWord);

		Mockito.when(
				blocklistedWordsRepository.findBlockListedWordsByIsActiveTrue())
				.thenReturn(allBlockWords);

		masterSyncDaoImpl.getBlockListedWords();

		assertTrue(allBlockWords != null);
	}

	@Test
	public void findDocumentCategories() throws RegBaseCheckedException {

		List<DocumentType> documents = new ArrayList<>();
		DocumentType document = new DocumentType();
		document.setName("Aadhar");
		document.setDescription("Aadhar card");
		document.setLangCode("ENG");
		documents.add(document);
		documents.add(document);
		List<String> validDocuments = new ArrayList<>();
		validDocuments.add("MNA");
		validDocuments.add("CLR");
		Mockito.when(masterSyncDaoImpl.getDocumentTypes(Mockito.anyList(), Mockito.anyString())).thenReturn(documents);

		masterSyncDaoImpl.getDocumentTypes(validDocuments, "test");

		assertTrue(documents != null);

	}


	/*@Test
	public void findValidDoc() {

		List<ValidDocument> docList = new ArrayList<>();
		ValidDocument docs = new ValidDocument();
		ValidDocumentID validDocumentId = new ValidDocumentID();
		validDocumentId.setDocCategoryCode("D101");
		validDocumentId.setDocTypeCode("DC101");
		docs.setLangCode("eng");
		docList.add(docs);

		Mockito.when(masterSyncDaoImpl.getValidDocumets(Mockito.anyString())).thenReturn(docList);

		masterSyncDaoImpl.getValidDocumets("POA");

		assertTrue(docList != null);

	}*/

	
	@Test
	public void getBiometricType() {
		
		List<String> biometricType = new LinkedList<>(Arrays.asList(RegistrationConstants.FNR, RegistrationConstants.IRS));
		List<BiometricAttribute> biometricAttributes = new ArrayList<>();
		BiometricAttribute biometricAttribute = new BiometricAttribute();
		biometricAttribute.setCode("RS");
		biometricAttribute.setBiometricTypeCode("FNR");
		biometricAttribute.setName("Right Slap");
		biometricAttribute.setLangCode("eng");
		biometricAttributes.add(biometricAttribute);
		
		Mockito.when(biometricAttributeRepository.findByLangCodeAndBiometricTypeCodeIn("eng",biometricType)).thenReturn(biometricAttributes);
		assertNotNull(masterSyncDaoImpl.getBiometricType("eng", biometricType));
		
	}
	
	@Test
	public void getLocationDetailsTest() {
		List<Location> locations = new ArrayList<Location>();
		locations.add(new Location());
		Mockito.when(locationRepository.findAllByIsActiveTrue()).thenReturn(locations);
		assertEquals(locations.size(), masterSyncDaoImpl.getLocationDetails().size());
	}
	
	@Test
	public void getLocationDetailsBasedOnLangCodeTest() {
		List<Location> locations = new ArrayList<Location>();
		locations.add(new Location());
		Mockito.when(locationRepository.findByIsActiveTrueAndLangCode(Mockito.anyString())).thenReturn(locations);
		assertEquals(locations.size(), masterSyncDaoImpl.getLocationDetails("lanCode").size());
	}

	@Test
	public void getLocationDetailsBasedOnLangCodeandHierarchyTest() {
		List<Location> locations = new ArrayList<Location>();
		locations.add(new Location());
		Mockito.when(locationRepository.findByIsActiveTrueAndHierarchyNameAndLangCode(Mockito.anyString(), Mockito.anyString())).thenReturn(locations);
		assertEquals(locations.size(), masterSyncDaoImpl.getLocationDetails("hierarchyName", "langCode").size());
	}
	
	@Test
	public void getLocationDetailsBasedOnLangCodeandCodeTest() {
		Location location = new Location();
		Mockito.when(locationRepository.findByCodeAndLangCode("code", "langCode")).thenReturn(location);
		assertNotNull(masterSyncDaoImpl.getLocation("code", "langCode"));
	}

	@Test
	public void getDocumentCategoryTest() {
		List<DocumentCategory> docCategories = new ArrayList<DocumentCategory>();
		docCategories.add(new DocumentCategory());
		Mockito.when(documentCategoryRepository.findAllByIsActiveTrue()).thenReturn(docCategories);
		assertEquals(docCategories.size(), masterSyncDaoImpl.getDocumentCategory().size());
	}

	@Test
	public void getActiveLanguagesTest() {
		List<Language> languages = new ArrayList<Language>();
		languages.add(new Language());
		Mockito.when(languageRepository.findAllByIsActiveTrue()).thenReturn(languages);
		assertEquals(languages.size(), masterSyncDaoImpl.getActiveLanguages().size());
	}
	
	@Test
	public void getAllLocationHierarchyTest() {
		List<LocationHierarchy> locationHierarcies = new ArrayList<LocationHierarchy>();
		locationHierarcies.add(new LocationHierarchy());
		Mockito.when(locationHierarchyRepository.findAllByIsActiveTrueAndLangCode("langCode")).thenReturn(locationHierarcies);
		assertEquals(locationHierarcies.size(), masterSyncDaoImpl.getAllLocationHierarchy("langCode").size());
	}

	@Test
	public void getSyncJobsTest() {
		List<SyncJobDef> syncJobs = new ArrayList<SyncJobDef>();
		syncJobs.add(new SyncJobDef());
		Mockito.when(syncJobDefRepository.findAllByIsActiveTrue()).thenReturn(syncJobs);
		assertEquals(syncJobs.size(), masterSyncDaoImpl.getSyncJobs().size());
	}
	@Test
	public void getDocumentTypeTest() {
		DocumentType docType = new DocumentType();
		Mockito.when(documentTypeRepository.findByIsActiveTrueAndLangCodeAndCode("langCode","docCode")).thenReturn(docType);
		assertNotNull(masterSyncDaoImpl.getDocumentType("docCode", "langCode"));
	}
	
	/*@Test()
	public void testSingleEntity() {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("biometricJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenReturn(RegistrationConstants.SUCCESS);
		response= masterSyncDaoImpl.saveSyncData(syncDataResponseDto);		
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testClientSettingsSyncForJson() {
		String response=null;
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("responseJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenReturn(RegistrationConstants.SUCCESS);
		response= masterSyncDaoImpl.saveSyncData(syncDataResponseDto);		
		assertEquals(RegistrationConstants.SUCCESS, response);
	}
		

	@Test
	public void testInvalidJsonSyntaxJsonSyntaxException() {
		masterSyncDaoImpl.saveSyncData(null);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RegBaseUncheckedException.class)
	public void testEmptyJsonRegBaseUncheckedException() {		
		SyncDataResponseDto syncDataResponseDto = getSyncDataResponseDto("emptyJson.json");
		Mockito.when(clientSettingSyncHelper.saveClientSettings(Mockito.any(SyncDataResponseDto.class)))
		.thenThrow(RegBaseUncheckedException.class);
		masterSyncDaoImpl.saveSyncData(syncDataResponseDto);			
	}*/
	
	
	private SyncDataResponseDto getSyncDataResponseDto(String fileName) {
		
		ObjectMapper mapper = new ObjectMapper();
        SyncDataResponseDto syncDataResponseDto = null;
		
			try {
				syncDataResponseDto = mapper.readValue(
						new File(getClass().getClassLoader().getResource(fileName).getFile()),SyncDataResponseDto.class);
			} catch (Exception e) {
				//it could throw exception for invalid json which is part of negative test case
			} 
		
		return syncDataResponseDto;
	}
	
	
	

}
