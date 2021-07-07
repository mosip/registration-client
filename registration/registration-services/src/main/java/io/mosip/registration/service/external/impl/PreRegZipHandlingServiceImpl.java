package io.mosip.registration.service.external.impl;


import static io.mosip.registration.constants.RegistrationConstants.ZIP_FILE_EXTENSION;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_IO_EXCEPTION;
import static java.io.File.separator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.DocumentTypeDAO;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.PreRegZipHandlingService;

/**
 * This implementation class to handle the pre-registration data
 * 
 * @author balamurugan ramamoorthy
 * @since 1.0.0
 *
 */
@Service
public class PreRegZipHandlingServiceImpl implements PreRegZipHandlingService {

	private static final String DOBSubType = "dateOfBirth";

	@Autowired
	private DocumentTypeDAO documentTypeDAO;

	@Autowired
	private MasterSyncDao masterSyncDao;
	
	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private KeyGenerator keyGenerator;

	@Autowired
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;


	private static final Logger LOGGER = AppConfig.getLogger(PreRegZipHandlingServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * extractPreRegZipFile(byte[])
	 */
	@Override
	public RegistrationDTO extractPreRegZipFile(byte[] preRegZipFile) throws RegBaseCheckedException {
		LOGGER.debug("extractPreRegZipFile invoked");
		try{
			BufferedReader bufferedReader = null;
			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (zipEntry.getName().equalsIgnoreCase("ID.json")) {
						bufferedReader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
						parseDemographicJson(bufferedReader, zipEntry);						
					}	
				}
			}finally {
				if(bufferedReader != null) {
					bufferedReader.close();
					bufferedReader = null;
				}				
			}
			
			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					String fileName = zipEntry.getName();
					//if (zipEntry.getName().contains("_")) {
					LOGGER.debug("extractPreRegZipFile zipEntry >>>> {}", fileName);
						Optional<Map.Entry<String, DocumentDto>> result = getRegistrationDtoContent().getDocuments().entrySet().stream()
								.filter(e -> fileName.equals(e.getValue().getValue().concat(".").concat(e.getValue().getFormat()))).findFirst();
						if(result.isPresent()) {
							DocumentDto documentDto = result.get().getValue();
							documentDto.setDocument(IOUtils.toByteArray(zipInputStream));
							
							List<DocumentType> documentTypes = documentTypeDAO.getDocTypeByName(documentDto.getType());
							if(Objects.nonNull(documentTypes) && !documentTypes.isEmpty()) {
								LOGGER.debug("{} >>>> documentTypes.get(0).getCode() >>>> {}", documentDto.getType(),
										documentTypes.get(0).getCode());
								documentDto.setType(documentTypes.get(0).getCode());
								documentDto.setValue(documentDto.getCategory().concat("_").concat(documentDto.getType()));
							}							
							getRegistrationDtoContent().addDocument(result.get().getKey(), result.get().getValue());
							LOGGER.debug("Added zip entry as document for field >>>> {}", result.get().getKey());
						}
					//}	
				}
			}
			
			Set<Entry<String, DocumentDto>> entries = getRegistrationDtoContent().getDocuments().entrySet();
			entries.stream()
				.filter(e -> e.getValue().getDocument() == null || e.getValue().getDocument().length == 0)
				.forEach(e -> { getRegistrationDtoContent().removeDocument(e.getKey()); });
		
		} catch (IOException exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(), exception.getCause().getMessage());
		} catch (RuntimeException exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_ZIP_CREATION, exception.getMessage());
		}
		return getRegistrationDtoContent();
	}


	/**
	 * This method is used to parse the demographic json and converts it into
	 * RegistrationDto
	 * 
	 * @param bufferedReader
	 *            - reader for text file
	 * @param zipEntry
	 *            - a file entry in zip
	 * @throws RegBaseCheckedException
	 *             - holds the cheked exceptions
	 */
	@SuppressWarnings("unchecked")
	private void parseDemographicJson(BufferedReader bufferedReader, ZipEntry zipEntry) throws RegBaseCheckedException {

		try {
			
			String value;
			StringBuilder jsonString = new StringBuilder();
			while ((value = bufferedReader.readLine()) != null) {
				jsonString.append(value);
			}
			
			if (!StringUtils.isEmpty(jsonString) && validateDemographicInfoObject()) {
				JSONObject jsonObject = (JSONObject) new JSONObject(jsonString.toString()).get("identity");
				//Always use latest schema, ignoring missing / removed fields
				List<UiSchemaDTO> fieldList = identitySchemaService.getLatestEffectiveUISchema();

				for(UiSchemaDTO field : fieldList) {
					if(field.getId().equalsIgnoreCase("IDSchemaVersion"))
						continue;
					
					switch (field.getType()) {
					case "documentType":
						DocumentDto documentDto = new DocumentDto();
						if(jsonObject.has(field.getId()) && jsonObject.get(field.getId()) != null) {
							JSONObject fieldValue = jsonObject.getJSONObject(field.getId());							
							documentDto.setCategory(field.getSubType());
							documentDto.setOwner("Applicant");
							documentDto.setFormat(fieldValue.getString("format"));
							documentDto.setType(fieldValue.getString("type"));
							documentDto.setValue(fieldValue.getString("value"));
							try {
							    documentDto.setRefNumber((fieldValue.getString("docRefId")));
							} catch(JSONException jsonException) {
								LOGGER.error("Unable to find Document Refernce Number for Pre-Reg-Sync : ", jsonException);
							}
							getRegistrationDtoContent().addDocument(field.getId(), documentDto);
						}
						break;
						
					case "biometricsType":						
						break;

					default:
						Object fieldValue = getValueFromJson(field.getId(), field.getType(), jsonObject);
						if(fieldValue != null) {
							switch (field.getControlType().toLowerCase()) {
								case "agedate":
								case "date":
									getRegistrationDtoContent().setDateField(field.getId(), (String)fieldValue,
											DOBSubType.equalsIgnoreCase(field.getSubType()));
									break;
								default:
									getRegistrationDtoContent().getDemographics().put(field.getId(), fieldValue);
							}
						}
						break;
					}
				}
			}
		} catch (JSONException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(), e.getMessage());
		}
	}
	
	
	private Object getValueFromJson(String key, String fieldType, JSONObject jsonObject) throws IOException, JSONException {
		if(!jsonObject.has(key))
			return null;

		try {
			switch (fieldType) {
				case "string":	return jsonObject.getString(key);
				case "integer":	return jsonObject.getInt(key);
				case "number": return jsonObject.getLong(key);
				case "simpleType":
					List<SimpleDto> list = new ArrayList<SimpleDto>();
					for(int i=0;i<jsonObject.getJSONArray(key).length();i++) {
						JSONObject object = jsonObject.getJSONArray(key).getJSONObject(i);
						list.add(new SimpleDto(object.getString("language"), object.getString("value")));
					}
					return list;
			}
		} catch (Throwable t) {
			LOGGER.error("Failed to parse the pre-reg packet field {}", key, t);
		}
		return null;
	}

	private boolean validateDemographicInfoObject() {
		return null != getRegistrationDtoContent() && getRegistrationDtoContent().getDemographics() != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * encryptAndSavePreRegPacket(java.lang.String, byte[])
	 */
	@Override
	public PreRegistrationDTO encryptAndSavePreRegPacket(String preRegistrationId, byte[] preRegPacket)
			throws RegBaseCheckedException {
		
		// Decrypt the preRegPacket data
		byte[] decryptedPacketData = clientCryptoFacade.decrypt(preRegPacket);

		//KeyGenerator keyGenerator = KeyGeneratorUtils.getKeyGenerator("AES", 256);
		// Generate AES Session Key
		final SecretKey symmetricKey = keyGenerator.getSymmetricKey();

		final byte[] encryptedData = cryptoCore.symmetricEncrypt(symmetricKey, decryptedPacketData, null);

		// Encrypt the Pre reg packet data using AES
		/*final byte[] encryptedData = MosipEncryptor.symmetricEncrypt(symmetricKey.getEncoded(), preRegPacket,
				MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING);*/

		LOGGER.info("Pre Registration packet Encrypted");

		String filePath = storePreRegPacketToDisk(preRegistrationId, encryptedData);

		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath(filePath);
		preRegistrationDTO.setSymmetricKey(Base64.getEncoder().encodeToString(symmetricKey.getEncoded()));
		preRegistrationDTO.setEncryptedPacket(encryptedData);
		preRegistrationDTO.setPreRegId(preRegistrationId);
		return preRegistrationDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * storePreRegPacketToDisk(java.lang.String, byte[])
	 */
	@Override
	public String storePreRegPacketToDisk(String preRegistrationId, byte[] encryptedPacket)
			throws RegBaseCheckedException {
		try {
			// Generate the file path for storing the Encrypted Packet
			String filePath = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.PRE_REG_PACKET_LOCATION))
					.concat(separator).concat(preRegistrationId).concat(ZIP_FILE_EXTENSION);
			// Storing the Encrypted Registration Packet as zip
			FileUtils.copyToFile(new ByteArrayInputStream(encryptedPacket),
					FileUtils.getFile(FilenameUtils.getFullPath(filePath) + FilenameUtils.getName(filePath)));

			LOGGER.info( "Pre Registration Encrypted packet saved");

			return filePath;
		} catch (io.mosip.kernel.core.exception.IOException exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(),
					REG_IO_EXCEPTION.getErrorMessage() + ExceptionUtils.getStackTrace(exception));
		} catch (RuntimeException runtimeException) {
			LOGGER.error(runtimeException.getMessage(), runtimeException);
			throw new RegBaseUncheckedException(RegistrationConstants.ENCRYPTED_PACKET_STORAGE,
					runtimeException.toString(), runtimeException);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * decryptPreRegPacket(java.lang.String, byte[])
	 */
	@Override
	public byte[] decryptPreRegPacket(String symmetricKey, byte[] encryptedPacket) {
		byte[] secret = Base64.getDecoder().decode(symmetricKey);
		/*return MosipDecryptor.symmetricDecrypt(Base64.getDecoder().decode(symmetricKey), encryptedPacket,
				MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING);*/
		SecretKey secretKey = new SecretKeySpec(secret, 0 , secret.length, "AES");
		return cryptoCore.symmetricDecrypt(secretKey, encryptedPacket, null);
	}

	private RegistrationDTO getRegistrationDtoContent() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}
}
