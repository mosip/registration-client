package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.dto.packet.AuditDto;
import io.mosip.commons.packet.dto.packet.BiometricsException;
import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.constants.IntroducerType;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.dto.schema.ConditionalBioAttributes;
import io.mosip.registration.enums.Modality;
import lombok.Data;
import lombok.NonNull;
import org.json.JSONObject;

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

	protected ApplicationContext applicationContext = ApplicationContext.getInstance();

	private double idSchemaVersion;
	private String registrationId;
	private String preRegistrationId;
	private String appId;
	private String registrationCategory;
	private RegistrationMetaDataDTO registrationMetaDataDTO;
	private OSIDataDTO osiDataDTO;
	private List<String> selectedLanguagesByApplicant = new ArrayList<>();

	private boolean isBiometricMarkedForUpdate;
	private HashMap<String, Object> selectionListDTO;
	private List<String> updatableFields;
	private List<String> updatableFieldGroups;
	private boolean isUpdateUINNonBiometric;
	private boolean isNameNotUpdated;
	private List<String> defaultUpdatableFields;
	private List<String> defaultUpdatableFieldGroups;

	private Map<String, Object> demographics = new HashMap<>();
	private Map<String, Object> defaultDemographics = new LinkedHashMap<>();
	private Map<String, DocumentDto> documents = new HashMap<>();
	private Map<String, BiometricsDto> biometrics = new HashMap<>();
	private Map<String, BiometricsException> biometricExceptions = new HashMap<>();

	private List<BiometricsDto> supervisorBiometrics = new ArrayList<>();
	private List<BiometricsDto> officerBiometrics = new ArrayList<>();
	private Map<String, BiometricsException> osBioExceptions = new HashMap<>();

	private List<AuditDto> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;

	public Map<String, byte[]> BIO_CAPTURES = new HashMap<>();
	public Map<String, Double> BIO_SCORES = new HashMap<>();
	public Map<String, Object> AGE_GROUPS = new HashMap<>();
	public Map<String, Integer> ATTEMPTS = new HashMap<>();


	public void addDemographicField(String fieldId, String value) {
		this.demographics.put(fieldId, (value != null && !value.isEmpty()) ? value : null);
	}

	public void addDemographicField(String fieldId, List<SimpleDto> values) {
		if (fieldId != null && !values.isEmpty()) {
			this.demographics.put(fieldId, values);
		}
	}

	public void addDefaultDemographicField(String fieldId, String applicationLanguage, String value,
			String localLanguage, String localValue) {
		List<SimpleDto> values = new ArrayList<SimpleDto>();
		if (value != null && !value.isEmpty())
			values.add(new SimpleDto(applicationLanguage, value));

		if (localValue != null && !localValue.isEmpty())
			values.add(new SimpleDto(localLanguage, localValue));

		if (!values.isEmpty())
			this.defaultDemographics.put(fieldId, values);
	}

	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}

	public void setDateField(@NonNull String fieldId, String day, String month, String year, boolean computeAgeGroup) {
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

					if(computeAgeGroup) {
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

	public void setDateField(String fieldId, String dateString, boolean computeAgeGroup) {
		if (isValidValue(dateString)) {
			LocalDate date = LocalDate.parse(dateString,
					DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat()));
			setDateField(fieldId, String.valueOf(date.getDayOfMonth()), String.valueOf(date.getMonthValue()),
					String.valueOf(date.getYear()), computeAgeGroup);
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

	public List<BiometricsDto> getBiometric(String subType, List<String> bioAttributes) {
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		for (String bioAttribute : bioAttributes) {
			String key = String.format("%s_%s", subType, bioAttribute);
			if (this.biometrics.containsKey(key))
				list.add(this.biometrics.get(key));
		}
		return list;
	}

	public BiometricsDto addBiometric(String subType, String bioAttribute, BiometricsDto value) {
		String key = String.format("%s_%s", subType, bioAttribute);
		value.setNumOfRetries(value.getNumOfRetries());
		value.setSubType(subType);
		this.biometrics.put(key, value);
		this.biometricExceptions.remove(key);
		return value;
	}

	public void addBiometricException(String subType, String uiSchemaAttribute, String bioAttribute, String reason,
			String exceptionType) {
		String key = String.format("%s_%s", subType, uiSchemaAttribute);
		SingleType type = io.mosip.registration.mdm.dto.Biometric.getSingleTypeBySpecConstant(uiSchemaAttribute);
		this.biometricExceptions.put(key, new BiometricsException(type == null ? null : type.value(), bioAttribute,
				reason, exceptionType, subType));
		this.biometrics.remove(key);
	}

	public boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		return biometricExceptions.containsKey(String.format("%s_%s", subType, bioAttribute));
	}

	public List<String> getBiometricExceptions(String subType) {
		return biometricExceptions.keySet().stream()
				.filter(k -> k.startsWith(String.format("%s_", subType)))
				.collect(Collectors.toList());
	}

	public BiometricsDto getBiometric(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		return this.biometrics.get(key);
	}

	public void clearBIOCache(String subType, String bioAttribute) {
		Modality modality = Modality.getModality(bioAttribute);
		List<String> keys = new ArrayList<>();
		keys.addAll(this.BIO_CAPTURES.keySet());
		keys.addAll(this.biometrics.keySet());
		keys.addAll(this.biometricExceptions.keySet());

		for(String attr : modality.getAttributes()) {
			keys.stream()
					.filter( k -> k.startsWith(String.format("%s_%s", subType, attr)))
					.forEach( k -> {
						this.BIO_SCORES.remove(k);
						this.BIO_CAPTURES.remove(k);
						this.biometrics.remove(k);
						this.biometricExceptions.remove(k);
					});
		}
		this.ATTEMPTS.remove(String.format("%s_%s", subType, modality.name()));
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
		allIdentityDetails.put("isNew", RegistrationConstants.PACKET_TYPE_NEW.equals(this.registrationCategory));
		allIdentityDetails.put("isUpdate", RegistrationConstants.PACKET_TYPE_UPDATE.equals(this.registrationCategory));
		allIdentityDetails.put("isLost", RegistrationConstants.PACKET_TYPE_LOST.equals(this.registrationCategory));
		allIdentityDetails.put("updatableFields",
				this.updatableFields == null ? Collections.EMPTY_LIST : this.updatableFields);
		allIdentityDetails.put("updatableFieldGroups",
				this.updatableFieldGroups == null ? Collections.EMPTY_LIST : this.updatableFieldGroups);
		allIdentityDetails.putAll(this.demographics);
		allIdentityDetails.putAll(this.documents);
		allIdentityDetails.putAll(this.biometrics);
		allIdentityDetails.putAll(this.AGE_GROUPS);
		return allIdentityDetails;
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty();
	}

	public List<BiometricsDto> addAllBiometrics(String subType, Modality currentModality, Map<String, BiometricsDto> biometricsDTOMap,
			double thresholdScore, int maxRetryAttempt) {
			List<BiometricsDto> savedBiometrics = null;
		if (subType != null && biometricsDTOMap != null && !biometricsDTOMap.isEmpty()) {
			savedBiometrics = new LinkedList<>();
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
					BiometricsDto biometricsDto = getBiometric(subType, Biometric
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
				BiometricsDto savedRegistrationBiometric = getBiometric(subType, entry.getKey());
				BiometricsDto value = entry.getValue();
				value.setForceCaptured(isForceCaptured);
				value.setSubType(subType);
				if( (savedRegistrationBiometric == null && (isQualityCheckPassed || isForceCaptured)) ||
						(savedRegistrationBiometric != null &&
								value.getQualityScore() >= savedRegistrationBiometric.getQualityScore())) {
					savedBiometrics.add(addBiometric(subType, entry.getKey(), value));
				}
			}
		}
		return savedBiometrics;
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
