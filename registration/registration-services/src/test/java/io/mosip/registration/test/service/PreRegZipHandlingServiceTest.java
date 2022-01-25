package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.security.constants.MosipSecurityMethod;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.kernel.keygenerator.bouncycastle.util.KeyGeneratorUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.DocumentTypeDAO;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.impl.PreRegZipHandlingServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ FileUtils.class, SessionContext.class, KeyGeneratorUtils.class, javax.crypto.KeyGenerator.class, ApplicationContext.class })
public class PreRegZipHandlingServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private DocumentTypeDAO documentTypeDAO;

	@InjectMocks
	private PreRegZipHandlingServiceImpl preRegZipHandlingServiceImpl;
	
	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private KeyGenerator keyGenerator;

	static byte[] preRegPacket;

	static byte[] preRegPacketEncrypted;

	static MosipSecurityMethod mosipSecurityMethod;
	
	@Mock
	private IdentitySchemaService identitySchemaService;

	@Mock
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;
	
	List<UiFieldDTO> schemaFields = new ArrayList<UiFieldDTO>();

	@Before
	public void init() throws Exception {
		createRegistrationDTOObject();
		
		UiFieldDTO field1 = new UiFieldDTO();
		field1.setId("fullName");
		field1.setType("simpleType");
		field1.setControlType("textbox");
		field1.setSubType("name");
		schemaFields.add(field1);
		
		UiFieldDTO field2 = new UiFieldDTO();
		field2.setId("gender");
		field2.setType("simpleType");
		field2.setControlType("textbox");
		field2.setSubType("gender");
		schemaFields.add(field2);		
		
		UiFieldDTO field3 = new UiFieldDTO();
		field3.setId("postalCode");
		field3.setType("string");
		field3.setControlType("dropdown");
		field3.setSubType("postalCode");
		schemaFields.add(field3);
		
		UiFieldDTO field8 = new UiFieldDTO();
		field8.setId("dateOfBirth");
		field8.setType("string");
		field8.setControlType("date");
		field8.setSubType("date");
		schemaFields.add(field8);
		
		UiFieldDTO field4 = new UiFieldDTO();
		field4.setId("IDSchemaVersion");
		schemaFields.add(field4);
		
		UiFieldDTO field5 = new UiFieldDTO();
		field5.setId("proofOfAddress");
		field5.setType("documentType");
		field5.setSubType("POA");
		schemaFields.add(field5);
		
		UiFieldDTO field7 = new UiFieldDTO();
		field7.setId("POI");
		field7.setType("documentType");
		field7.setSubType("POI");
		schemaFields.add(field7);
		
		UiFieldDTO field6 = new UiFieldDTO();
		field6.setId("individualBiometrics");
		field6.setType("biometricsType");
		schemaFields.add(field6);	
	}
	
	
	@BeforeClass
	public static void initialize() throws IOException, java.io.IOException {
		URL url = PreRegZipHandlingServiceTest.class.getResource("/preRegSample.zip");
		File packetZipFile = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
		preRegPacket = FileUtils.readFileToByteArray(packetZipFile);

		mosipSecurityMethod = MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING;

		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put("mosip.registration.registration_pre_reg_packet_location", "..//PreRegPacketStore");
		ApplicationContext.getInstance();
		ApplicationContext.setApplicationMap(applicationMap);
	}

	@Test
	public void extractPreRegZipFileTest() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class);
		Map<String, Object> appMap = new LinkedHashMap<>();
		appMap.put("mosip.registration.registration_pre_reg_packet_location", "..//PreRegPacketStore");
		appMap.put(RegistrationConstants.AGE_GROUP_CONFIG, "{'INFANT':'0-5','MINOR':'6-17','ADULT':'18-200'}");
		appMap.put("mosip.default.date.format", "yyyy/MM/dd");
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(ApplicationContext.getDateFormat()).thenReturn("yyyy/MM/dd");
		/*Mockito.doAnswer((idObject) -> {
			return "Success";
		}).when(idObjectValidator).validateIdObject(Mockito.any(), Mockito.any());*/
		Mockito.when(identitySchemaService.getAllFieldSpec(Mockito.anyString(), Mockito.anyDouble())).thenReturn(schemaFields);
		
		List<DocumentType> documentTypes = new ArrayList<>();
		DocumentType docType = new DocumentType();
		docType.setCode("POI");
		documentTypes.add(docType);
		Mockito.when(documentTypeDAO.getDocTypeByName(Mockito.anyString())).thenReturn(documentTypes);
		
		RegistrationDTO registrationDTO = preRegZipHandlingServiceImpl.extractPreRegZipFile(preRegPacket);

		assertNotNull(registrationDTO);
	}

	@Test(expected = RegBaseCheckedException.class)
	public void extractPreRegZipFileTestNegative() throws Exception {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
			ZipEntry zipEntry = new ZipEntry("id.json");
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(
					"\"identity\" : {    \"CNIENumber\" : 6789545678909,    \"gender\" : [ {      \"language\" : \"eng\",      \"value\" : \"male\"    }, {      \"language\" : \"ara\",      \"value\" : \"male\"    } ],    \"city\" : [ {      \"language\" : \"eng\",      \"value\" : \"Bangalore\"    }, {      \"language\" : \"ara\",      \"value\" : \"BLR\"    } ],    \"postalCode\" : \"570000\",    \"localAdministrativeAuthority\" : [ {      \"language\" : \"eng\",      \"value\" : \"Bangalore\"    }, {      \"language\" : \"ara\",      \"value\" : \"BLR\"    } ]"
							.getBytes());
			zipOutputStream.flush();
			zipOutputStream.closeEntry();

			/*Mockito.doAnswer((idObject) -> {
				return "Success";
			}).when(idObjectValidator).validateIdObject(Mockito.any(), Mockito.any());*/
			Mockito.when(documentTypeDAO.getDocTypeByName(Mockito.anyString())).thenReturn(new ArrayList<>());
			//Mockito.when(identitySchemaService.getAllFieldSpec(Mockito.anyString(), Mockito.any())).thenReturn(schemaFields);
			preRegZipHandlingServiceImpl.extractPreRegZipFile(byteArrayOutputStream.toByteArray());
		}
	}

	@Test
	public void encryptAndSavePreRegPacketTest() throws RegBaseCheckedException, IOException {
		PreRegistrationDTO preRegistrationDTO = encryptPacket();
		assertNotNull(preRegistrationDTO);
	}

	private PreRegistrationDTO encryptPacket() throws RegBaseCheckedException, IOException {
		mockSecretKey();

		PreRegistrationDTO preRegistrationDTO = preRegZipHandlingServiceImpl
				.encryptAndSavePreRegPacket("89149679063970", preRegPacket);
		return preRegistrationDTO;
	}

	@Test(expected = RegBaseCheckedException.class)
	public void encryptAndSavePreRegPacketIoExceptionTest() throws RegBaseCheckedException, IOException {
		mockExceptions();
		encryptPacket();
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void encryptAndSavePreRegPacketRuntimeExceptionTest() throws RegBaseCheckedException, IOException {
		mockRuntimeExceptions();
		encryptPacket();
	}
	
	protected void mockExceptions() throws IOException {
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.doThrow(new io.mosip.kernel.core.exception.IOException("", "")).when(FileUtils.class);
		FileUtils.copyToFile(Mockito.any(), Mockito.any());
	}
	
	protected void mockRuntimeExceptions() throws IOException {
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.doThrow(new RuntimeException()).when(FileUtils.class);
		FileUtils.copyToFile(Mockito.any(), Mockito.any());
	}

	@Test
	public void decryptPreRegPacketTest() throws RegBaseCheckedException, IOException {

		final byte[] decrypted = preRegZipHandlingServiceImpl.decryptPreRegPacket("0E8BAAEB3CED73CBC9BF4964F321824A",
				encryptPacket().getEncryptedPacket());
		assertNotNull(decrypted);
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void extractPreRegZipFileNegative() throws RegBaseCheckedException {
		mockSecretKey();
		preRegZipHandlingServiceImpl.extractPreRegZipFile(null);
	}

	private void mockSecretKey() {
		byte[] decodedKey = ClientCryptoUtils.decodeBase64Data("0E8BAAEB3CED73CBC9BF4964F321824A");
		SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		Mockito.when(keyGenerator.getSymmetricKey()).thenReturn(secretKey);
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(new byte[0]);
		Mockito.when(cryptoCore.symmetricEncrypt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new byte[0]);
		Mockito.when(cryptoCore.symmetricDecrypt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new byte[0]);
	}

	private static void createRegistrationDTOObject() {
		RegistrationDTO registrationDTO = new RegistrationDTO();

		// Set the RID
		registrationDTO.setRegistrationId("10011100110016320190307151917");
		registrationDTO.setProcessId("NEW");
		registrationDTO.setIdSchemaVersion(0.5);

		// Create object for OSIData DTO
		registrationDTO.setOsiDataDTO(new OSIDataDTO());

		// Create object for RegistrationMetaData DTO
		RegistrationMetaDataDTO registrationMetaDataDTO = new RegistrationMetaDataDTO();
		//registrationMetaDataDTO.setRegistrationCategory("New");
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);

		// Put the RegistrationDTO object to SessionContext Map
		PowerMockito.mockStatic(SessionContext.class);
		Map<String,Object> regMap = new LinkedHashMap<>();
		regMap.put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
		PowerMockito.when(SessionContext.map()).thenReturn(regMap);
	}
}
