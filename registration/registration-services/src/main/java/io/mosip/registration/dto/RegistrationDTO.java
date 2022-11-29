package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ValueRange;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONObject;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.dto.packet.AuditDto;
import io.mosip.commons.packet.dto.packet.BiometricsException;
import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.enums.Modality;
import lombok.Data;
import lombok.NonNull;

/**
 * This DTO class contains the Registration details.
 *
 * @author Dinesh Asokan
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Data
public class RegistrationDTO {

	private static final String APPLICANT_DOB_SUBTYPE = "dateOfBirth";
	protected ApplicationContext applicationContext = ApplicationContext.getInstance();

	private double idSchemaVersion;
	private String registrationId;
	private String preRegistrationId;
	private String appId;
	private String packetId;
	private String additionalInfoReqId;
	private String processId;
	private FlowType flowType;
	private RegistrationMetaDataDTO registrationMetaDataDTO;
	private OSIDataDTO osiDataDTO;
	private List<String> selectedLanguagesByApplicant = new ArrayList<>();

	private boolean isBiometricMarkedForUpdate;
	private List<String> updatableFields;
	private List<String> updatableFieldGroups;
	private boolean isUpdateUINNonBiometric;
	private boolean isNameNotUpdated;
	private List<String> defaultUpdatableFieldGroups;
	private Integer selectedFaceAttempt;

	private Map<String, Object> demographics = new HashMap<>();
	private Map<String, Object> defaultDemographics = new LinkedHashMap<>();
	private Map<String, DocumentDto> documents = new HashMap<>();
	private Map<String, BiometricsDto> biometrics = new HashMap<>();
	private Map<String, BiometricsException> biometricExceptions = new HashMap<>();
	private Map<String, BiometricsDto> faceBiometrics = new HashMap<>();

	private List<BiometricsDto> supervisorBiometrics = new ArrayList<>();
	private List<BiometricsDto> officerBiometrics = new ArrayList<>();
	private Map<String, BiometricsException> osBioExceptions = new HashMap<>();

	private List<AuditDto> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;

	// Caches
	public Map<String, byte[]> BIO_CAPTURES = new HashMap<>();
	public Map<String, Double> BIO_SCORES = new HashMap<>();
	public Map<String, Object> AGE_GROUPS = new HashMap<>();
	public Map<String, Integer> ATTEMPTS = new HashMap<>();
	public Map<String, List<String>> CONFIGURED_BIOATTRIBUTES = new HashMap<>();
	public Map<String, String> SELECTED_CODES = new HashMap<>();
	public Map<String, BlocklistedConsentDto> BLOCKLISTED_CHECK = new HashMap<>();

	private List<String> configuredBlockListedWords = new ArrayList<>();

	public void clearRegistrationDto() {
		this.AGE_GROUPS.clear();
		this.biometrics.clear();
		this.biometricExceptions.clear();
		this.faceBiometrics.clear();
		this.BIO_CAPTURES.clear();
		this.BIO_SCORES.clear();
		this.ATTEMPTS.clear();
		this.SELECTED_CODES.clear();

		List<String> allKeys = new ArrayList<>();
		allKeys.addAll(demographics.keySet());
		allKeys.addAll(defaultDemographics.keySet());
		allKeys.addAll(documents.keySet());

		String config = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.FIELDS_TO_RETAIN_ON_PRID_FETCH);
		List<String> keysToRetain = config == null ? Collections.EMPTY_LIST : List.of(config.split(RegistrationConstants.COMMA));

		allKeys.forEach( k -> {
			if(!keysToRetain.contains(k)) {
				this.demographics.remove(k);
				this.defaultDemographics.remove(k);
				this.documents.remove(k);
				this.BLOCKLISTED_CHECK.remove(k);
			}
		});
	}

	public void addDemographicField(@NonNull String fieldId, String value) {
		if(value != null && !value.trim().isEmpty())
			this.demographics.put(fieldId, value);
	}

	public void addDemographicField(@NonNull String fieldId, List<SimpleDto> values) {
		if (fieldId != null && values != null && !values.isEmpty()) {
			this.demographics.put(fieldId, values);
		}
	}

	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}

	public void setDateField(@NonNull String fieldId, String day, String month, String year, String subType) {
		if (isValidValue(day) && isValidValue(month) && isValidValue(year)) {
			LocalDate date = LocalDate.of(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day));

			this.demographics.put(fieldId,
					date.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat())));

			JSONObject ageGroupConfig = new JSONObject((String) ApplicationContext.map().get(RegistrationConstants.AGE_GROUP_CONFIG));
			ageGroupConfig.keySet().forEach( group -> {
				String[] range = ageGroupConfig.getString(group).split("-");
				int ageInYears = Period.between(date, LocalDate.now(ZoneId.of("UTC"))).getYears();
				if(ValueRange.of(Long.valueOf(range[0]), Long.valueOf(range[1])).isValidIntValue(ageInYears)) {
					AGE_GROUPS.put(String.format("%s_%s", fieldId, "ageGroup"), group);
					AGE_GROUPS.put(String.format("%s_%s", fieldId, "age"), ageInYears);

					if(APPLICANT_DOB_SUBTYPE.equals(subType)) {
						AGE_GROUPS.put("ageGroup", group);
						AGE_GROUPS.put("age", ageInYears);
					}
				}
			});
		}
	}

	public String getAgeGroup() {
		return (String)AGE_GROUPS.getOrDefault("ageGroup", null);
	}
	public int getAge() {
		return (int) AGE_GROUPS.getOrDefault("age", null);
	}

	public void setDateField(String fieldId, String dateString, String subType) {
		if (isValidValue(dateString)) {
			LocalDate date = LocalDate.parse(dateString,
					DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat()));
			setDateField(fieldId, String.valueOf(date.getDayOfMonth()), String.valueOf(date.getMonthValue()),
					String.valueOf(date.getYear()), subType);
		}
	}

	public void addDocument(String fieldId, DocumentDto value) {
		this.documents.put(fieldId, value);
	}

	public void removeDocument(String fieldId) {
		this.documents.remove(fieldId);
	}

	public void removeAllDocuments() {
		this.documents.clear();
	}

	public List<BiometricsDto> getBiometric(String fieldId, List<String> bioAttributes) {
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		for (String bioAttribute : bioAttributes) {
			String key = String.format("%s_%s", fieldId, bioAttribute);
			if (this.biometrics.containsKey(key))
				list.add(this.biometrics.get(key));
		}
		return list;
	}

	public BiometricsDto addBiometric(String fieldId, String bioAttribute, BiometricsDto value) {
		String key = String.format("%s_%s", fieldId, bioAttribute);
		value.setNumOfRetries(value.getNumOfRetries());
		//value.setSubType(fieldId);
		this.biometrics.put(key, value);
		this.biometricExceptions.remove(key);
		return value;
	}

	public void addBiometricException(String fieldId, String uiSchemaAttribute, String bioAttribute, String reason,
									  String exceptionType, String subType) {
		String key = String.format("%s_%s", fieldId, uiSchemaAttribute);
		SingleType type = io.mosip.registration.mdm.dto.Biometric.getSingleTypeBySpecConstant(uiSchemaAttribute);
		this.biometricExceptions.put(key, new BiometricsException(type == null ? null : type.value(), bioAttribute,
				reason, exceptionType, subType));
		this.biometrics.remove(key);
	}

	public boolean isBiometricExceptionAvailable(String fieldId, String bioAttribute) {
		return biometricExceptions.containsKey(String.format("%s_%s", fieldId, bioAttribute));
	}

	public boolean isBiometricExceptionAvailable(String fieldId) {
		return biometricExceptions.keySet().stream().anyMatch(k -> k.startsWith(String.format("%s_", fieldId)));
	}

	public List<String> getBiometricExceptions(String fieldId) {
		return biometricExceptions.keySet().stream()
				.filter(k -> k.startsWith(String.format("%s_", fieldId)))
				.collect(Collectors.toList());
	}

	public BiometricsDto getBiometric(String fieldId, String bioAttribute) {
		String key = String.format("%s_%s", fieldId, bioAttribute);
		return this.biometrics.get(key);
	}

	public void removeExceptionPhoto(String fieldId) {
		String key = String.format("%s_%s", fieldId, RegistrationConstants.notAvailableAttribute);
		this.biometrics.remove(key);
		key = String.format("%s_%s", fieldId, Modality.EXCEPTION_PHOTO.name());
		this.ATTEMPTS.remove(key);
		Set<String> captureKeys = this.BIO_CAPTURES.keySet();
		captureKeys.stream()
				.filter( k -> k.startsWith(String.format("%s_%s_", fieldId, RegistrationConstants.notAvailableAttribute)))
				.forEach( k -> {
					this.BIO_CAPTURES.remove(k);
				});

		Set<String> scoreKeys = BIO_SCORES.keySet();
		scoreKeys.stream()
				.filter( k -> k.startsWith(String.format("%s_%s_", fieldId, Modality.EXCEPTION_PHOTO.name())))
				.forEach( k -> {
					this.BIO_SCORES.remove(k);
				});
	}

	public void clearBIOCache(String fieldId, String bioAttribute) {
		Modality modality = Modality.getModality(bioAttribute);
		List<String> keys = new ArrayList<>();
		keys.addAll(this.BIO_CAPTURES.keySet());
		keys.addAll(this.biometrics.keySet());
		keys.addAll(this.biometricExceptions.keySet());

		for(String attr : modality.getAttributes()) {
			keys.stream()
					.filter( k -> k.startsWith(String.format("%s_%s", fieldId, attr)))
					.forEach( k -> {
						this.BIO_SCORES.remove(k);
						this.BIO_CAPTURES.remove(k);
						this.biometrics.remove(k);
						this.biometricExceptions.remove(k);
					});
		}

		String key = String.format("%s_%s", fieldId, modality.name());
		this.ATTEMPTS.remove(key);

		keys.clear();
		keys.addAll(this.BIO_SCORES.keySet());
		keys.stream()
				.filter( k -> k.startsWith(key))
				.forEach( k -> {
					this.BIO_SCORES.remove(k);
				});
	}

	public void addSupervisorBiometrics(List<BiometricsDto> biometrics) {
		this.supervisorBiometrics.addAll(biometrics);
	}

	public void addOfficerBiometrics(List<BiometricsDto> biometrics) {
		this.officerBiometrics.addAll(biometrics);
	}

	public Map<String, Object> getMVELDataContext() {
		Map<String, Object> allIdentityDetails = new LinkedHashMap<String, Object>();
		allIdentityDetails.put("IDSchemaVersion", idSchemaVersion);
		allIdentityDetails.put("_flow", this.flowType.getCategory());
		allIdentityDetails.put("_process", this.processId);
		allIdentityDetails.put("langCodes", this.selectedLanguagesByApplicant);
		allIdentityDetails.put("updatableFields",
				this.updatableFields == null ? Collections.EMPTY_LIST : this.updatableFields);
		allIdentityDetails.put("updatableFieldGroups",
				this.updatableFieldGroups == null ? Collections.EMPTY_LIST : this.updatableFieldGroups);
		allIdentityDetails.putAll(this.demographics);
		allIdentityDetails.putAll(this.documents);
		allIdentityDetails.putAll(this.biometrics);
		allIdentityDetails.putAll(this.AGE_GROUPS);
		allIdentityDetails.putAll(this.SELECTED_CODES);
		allIdentityDetails.put("isBioException", this.biometricExceptions.size() > 0);
		return allIdentityDetails;
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty();
	}

	public void addAllBiometrics(String fieldId, Map<String, BiometricsDto> biometricsDTOMap,
								 double thresholdScore, int maxRetryAttempt) {

		if (fieldId != null && biometricsDTOMap != null && !biometricsDTOMap.isEmpty()) {
			boolean isQualityCheckPassed = false, isForceCaptured = false;

			if (!biometricsDTOMap.isEmpty()) {
				thresholdScore = thresholdScore * biometricsDTOMap.size();
			}

			/** Find force capture or not */
			if (getQualityScore(biometricsDTOMap.values().stream().collect(Collectors.toList())) < thresholdScore) {

				if (maxRetryAttempt == 1) {
					isForceCaptured = true;
				} else {
					Collection<BiometricsDto> values = biometricsDTOMap.values();
					List<BiometricsDto> biometricsList = new ArrayList<>(values);
					BiometricsDto biometricsDto = getBiometric(fieldId, Biometric
							.getBiometricByAttribute(biometricsList.get(0).getBioAttribute()).getAttributeName());

					if (biometricsDto == null && biometricsList.get(0).getNumOfRetries() >= maxRetryAttempt) {
						isForceCaptured = true;
					}
				}
			}
			else
				isQualityCheckPassed = true;

			/** Modify the Biometrics DTO and save */
			for (Entry<String, BiometricsDto> entry : biometricsDTOMap.entrySet()) {
				BiometricsDto savedRegistrationBiometric = getBiometric(fieldId, entry.getKey());
				BiometricsDto value = entry.getValue();
				value.setForceCaptured(isForceCaptured);
				//value.setSubType(fieldId);
				if( (savedRegistrationBiometric == null && (isQualityCheckPassed || isForceCaptured)) ||
						(savedRegistrationBiometric != null &&
								value.getQualityScore() >= savedRegistrationBiometric.getQualityScore())) {
					addBiometric(fieldId, entry.getKey(), value);
					//savedBiometrics.add(addBiometric(fieldId, entry.getKey(), value));
				}

				if(entry.getValue().getBioAttribute().equalsIgnoreCase(Modality.FACE.name())) {
					String key = String.format("%s_%s", entry.getKey(), value.getNumOfRetries());
					this.faceBiometrics.put(key, value);
				}
			}
		}
		//return savedBiometrics;
	}

	private double getQualityScore(List<BiometricsDto> biometrics) {
		double qualityScore = 0.0;

		for (BiometricsDto biometricsDto : biometrics) {
			qualityScore += biometricsDto.getQualityScore();
		}

		return qualityScore;
	}

	public List<String> getSelectedLanguagesByApplicant() {
		return selectedLanguagesByApplicant;
	}

	public void setSelectedLanguagesByApplicant(List<String> selectedLanguagesByApplicant) {
		this.selectedLanguagesByApplicant = selectedLanguagesByApplicant;
	}

}
