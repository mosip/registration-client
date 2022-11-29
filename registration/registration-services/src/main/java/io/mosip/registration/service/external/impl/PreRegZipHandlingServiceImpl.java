package io.mosip.registration.service.external.impl;


import static io.mosip.registration.constants.RegistrationConstants.ZIP_FILE_EXTENSION;
import static io.mosip.registration.exception.RegistrationExceptionConstants.*;
import static java.io.File.separator;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.mosip.registration.context.SessionContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.DocumentTypeDAO;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.BaseService;
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
public class PreRegZipHandlingServiceImpl extends BaseService implements PreRegZipHandlingService {

	private static final Logger LOGGER = AppConfig.getLogger(PreRegZipHandlingServiceImpl.class);
	private static final String CONTROLTYPE_DOB = "date";
	private static final String CONTROLTYPE_DOB_AGE = "ageDate";

	@Autowired
	private DocumentTypeDAO documentTypeDAO;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private KeyGenerator keyGenerator;

	@Autowired
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	@Value("${mosip.registration.prereg.packet.entires.limit:15}")
	private int THRESHOLD_ENTRIES;

	@Value("${mosip.registration.prereg.packet.size.limit:200000}")
	private long THRESHOLD_SIZE;

	@Value("${mosip.registration.prereg.packet.threshold.ratio:10}")
	private int THRESHOLD_RATIO;

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
			int totalEntries = 0;
			long totalReadArchiveSize = 0;

			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					totalEntries++;
					if (zipEntry.getName().equalsIgnoreCase("ID.json")) {
						byte[] idjson = IOUtils.toByteArray(zipInputStream);
						double compressionRatio = (double)idjson.length / zipEntry.getCompressedSize();
						if(compressionRatio > THRESHOLD_RATIO) {
							LOGGER.error("compression ratio is more than the threshold");
							throw new RegBaseCheckedException(PRE_REG_PACKET_ZIP_COMPRESSED_RATIO_EXCEEDED.getErrorCode(),
									PRE_REG_PACKET_ZIP_COMPRESSED_RATIO_EXCEEDED.getErrorMessage());
						}
						totalReadArchiveSize = totalReadArchiveSize + idjson.length;
						parseDemographicJson(new String(idjson));
						break;
					}

					if(totalEntries > THRESHOLD_ENTRIES) {
						LOGGER.error("Number of entries in the packet is more than the threshold");
						throw new RegBaseCheckedException(PRE_REG_PACKET_ENTRIES_THRESHOLD_CROSSED.getErrorCode(),
								PRE_REG_PACKET_ENTRIES_THRESHOLD_CROSSED.getErrorMessage());
					}

					if(totalReadArchiveSize > THRESHOLD_SIZE) {
						LOGGER.error("Archive size read in the packet is more than the threshold");
						throw new RegBaseCheckedException(PRE_REG_PACKET_ZIP_SIZE_THRESHOLD_CROSSED.getErrorCode(),
								PRE_REG_PACKET_ZIP_SIZE_THRESHOLD_CROSSED.getErrorMessage());
					}
				}
			}

			totalEntries = 0;
			totalReadArchiveSize = 0;

			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					totalEntries++;

					String fileName = zipEntry.getName();
					validateFilename(fileName, ".");
					//if (zipEntry.getName().contains("_")) {
					LOGGER.debug("extractPreRegZipFile zipEntry >>>> {}", fileName);
					Optional<Map.Entry<String, DocumentDto>> result = getRegistrationDTOFromSession().getDocuments().entrySet().stream()
							.filter(e -> fileName.equals(e.getValue().getValue().concat(".").concat(e.getValue().getFormat()))).findFirst();
					if(result.isPresent()) {
						DocumentDto documentDto = result.get().getValue();
						documentDto.setDocument(IOUtils.toByteArray(zipInputStream));
						totalReadArchiveSize = totalReadArchiveSize + documentDto.getDocument().length;

						double compressionRatio = (double)documentDto.getDocument().length / zipEntry.getCompressedSize();
						if(compressionRatio > THRESHOLD_RATIO) {
							LOGGER.error("compression ratio is more than the threshold");
							throw new RegBaseCheckedException(PRE_REG_PACKET_ZIP_COMPRESSED_RATIO_EXCEEDED.getErrorCode(),
									PRE_REG_PACKET_ZIP_COMPRESSED_RATIO_EXCEEDED.getErrorMessage());
						}

						List<DocumentType> documentTypes = documentTypeDAO.getDocTypeByName(documentDto.getType());
						if(Objects.nonNull(documentTypes) && !documentTypes.isEmpty()) {
							LOGGER.debug("{} >>>> documentTypes.get(0).getCode() >>>> {}", documentDto.getType(),
									documentTypes.get(0).getCode());
							documentDto.setType(documentTypes.get(0).getCode());
							documentDto.setValue(documentDto.getCategory().concat("_").concat(documentDto.getType()));
						}
						getRegistrationDTOFromSession().addDocument(result.get().getKey(), result.get().getValue());
						LOGGER.debug("Added zip entry as document for field >>>> {}", result.get().getKey());
					}
					//}

					if(totalEntries > THRESHOLD_ENTRIES) {
						LOGGER.error("Number of entries in the packet is more than the threshold", totalEntries);
						throw new RegBaseCheckedException(PRE_REG_PACKET_ENTRIES_THRESHOLD_CROSSED.getErrorCode(),
								PRE_REG_PACKET_ENTRIES_THRESHOLD_CROSSED.getErrorMessage());
					}

					if(totalReadArchiveSize > THRESHOLD_SIZE) {
						LOGGER.error("Archive size read in the packet is more than the threshold", totalReadArchiveSize);
						throw new RegBaseCheckedException(PRE_REG_PACKET_ZIP_SIZE_THRESHOLD_CROSSED.getErrorCode(),
								PRE_REG_PACKET_ZIP_SIZE_THRESHOLD_CROSSED.getErrorMessage());
					}
				}
			}

			Iterator<Entry<String, DocumentDto>> entries = getRegistrationDTOFromSession().getDocuments().entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, DocumentDto> entry = entries.next();
				if (entry.getValue().getDocument() == null || entry.getValue().getDocument().length == 0) {
					entries.remove();
				}
			}
		} catch (IOException exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(), exception.getCause().getMessage());
		} catch (RuntimeException exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_ZIP_CREATION, exception.getMessage());
		}
		return getRegistrationDTOFromSession();
	}


	/**
	 * This method is used to parse the demographic json and converts it into
	 * RegistrationDto
	 *
	 * @param jsonString
	 *            - reader for text file
	 * @throws RegBaseCheckedException
	 *             - holds the cheked exceptions
	 */
	private void parseDemographicJson(String jsonString) throws RegBaseCheckedException {
		try {
			if (!StringUtils.isEmpty(jsonString) && validateDemographicInfoObject()) {
				JSONObject jsonObject = (JSONObject) new JSONObject(jsonString).get("identity");
				JSONArray array=new JSONArray();
				array.put(new JSONObject().put("language", "eng").put("value","10"));
				jsonObject.put("homeless", array);
				SessionContext.map().put(RegistrationConstants.REGISTRATION_DATA_DEMO, new HashMap<String,Object>());
				//Always use latest schema, ignoring missing / removed fields
				RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
				List<UiFieldDTO> fieldList = identitySchemaService.getAllFieldSpec(registrationDTO.getProcessId(), registrationDTO.getIdSchemaVersion());
				getRegistrationDTOFromSession().clearRegistrationDto();

				for(UiFieldDTO field : fieldList) {
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
									documentDto.setRefNumber(fieldValue.has("refNumber") ? fieldValue.getString("refNumber") :
											(fieldValue.has("docRefId") ? fieldValue.getString("docRefId") : null));
								} catch(JSONException jsonException) {
									LOGGER.error("Unable to find Document Refernce Number for Pre-Reg-Sync : ", jsonException);
								}
								getRegistrationDTOFromSession().addDocument(field.getId(), documentDto);
							}
							break;

						case "biometricsType":
							break;

						default:
							Object fieldValue = getValueFromJson(field.getId(), field.getType(), jsonObject);
							if(fieldValue != null) {
								switch (field.getControlType()) {
									case CONTROLTYPE_DOB_AGE:
									case CONTROLTYPE_DOB:
										getRegistrationDTOFromSession().setDateField(field.getId(), (String)fieldValue, field.getSubType());
										break;
									default:
										getRegistrationDTOFromSession().getDemographics().put(field.getId(), fieldValue);
										getRegistrationDTODemographics().put(field.getId(), fieldValue);
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
		return null != getRegistrationDTOFromSession() && getRegistrationDTOFromSession().getDemographics() != null;
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

		// Generate AES Session Key
		final SecretKey symmetricKey = keyGenerator.getSymmetricKey();

		final byte[] encryptedData = cryptoCore.symmetricEncrypt(symmetricKey, decryptedPacketData, null);

		LOGGER.info("Pre Registration packet Encrypted");

		String filePath = storePreRegPacketToDisk(preRegistrationId, encryptedData);

		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath(filePath);
		preRegistrationDTO.setSymmetricKey(CryptoUtil.encodeToURLSafeBase64(symmetricKey.getEncoded()));
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
		byte[] secret = ClientCryptoUtils.decodeBase64Data(symmetricKey);
		SecretKey secretKey = new SecretKeySpec(secret, 0 , secret.length, "AES");
		return cryptoCore.symmetricDecrypt(secretKey, encryptedPacket, null);
	}

	private String validateFilename(String filename, String intendedDir) throws IOException {
		File f = new File(filename);
		String canonicalPath = f.getCanonicalPath();

		File iD = new File(intendedDir);
		String canonicalID = iD.getCanonicalPath();

		if (canonicalPath.startsWith(canonicalID)) {
			return canonicalPath;
		} else {
			throw new IllegalStateException("File is outside extraction target directory.");
		}
	}

}
