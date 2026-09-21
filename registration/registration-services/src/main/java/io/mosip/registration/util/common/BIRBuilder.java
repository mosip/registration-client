package io.mosip.registration.util.common;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import io.mosip.kernel.biometrics.entities.*;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.kernel.biometrics.constant.OtherKey;
import io.mosip.kernel.biometrics.model.SDKInfo;
import io.mosip.registration.service.bio.quality.SdkInfoProvider;

@Component
public class BIRBuilder {

	private static final Logger LOGGER = AppConfig.getLogger(BIRBuilder.class);

	@org.springframework.beans.factory.annotation.Autowired
	private SdkInfoProvider sdkInfoProvider;

	public BIR buildBIR(BiometricsDto bioDto) {
		LOGGER.debug("started building BIR for for bioAttribute : {}", bioDto.getBioAttribute());
		BiometricType biometricType = Biometric.getSingleTypeByAttribute(bioDto.getBioAttribute());
		// Format
		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(biometricType)));

		LOGGER.debug("started building BIR algorithm for for bioAttribute : {}", bioDto.getBioAttribute());

		// Algorithm
		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		LOGGER.debug("started building Quality type for for bioAttribute : {}", bioDto.getBioAttribute());

		// Quality Type
		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) bioDto.getQualityScore());

		VersionType versionType = new VersionType(1, 1);

		String payLoad = null;
		if(bioDto.getAttributeISO()!=null) {
			int bioValueKeyIndex = bioDto.getPayLoad().indexOf(RegistrationConstants.BIOVALUE_KEY) + (RegistrationConstants.BIOVALUE_KEY.length() + 1);
			int bioValueStartIndex = bioDto.getPayLoad().indexOf('"', bioValueKeyIndex);
			int bioValueEndIndex = bioDto.getPayLoad().indexOf('"', (bioValueStartIndex + 1));
			String bioValue = bioDto.getPayLoad().substring(bioValueStartIndex, (bioValueEndIndex + 1));
			payLoad = bioDto.getPayLoad().replace(bioValue, RegistrationConstants.BIOVALUE_PLACEHOLDER);
		}

		/*List<Entry> otherEntries = new ArrayList<>();
		otherEntries.add(new Entry(OtherKey.EXCEPTION, bioDto.getAttributeISO()==null ? "true" : "false"));
		otherEntries.add(new Entry(OtherKey.RETRIES, bioDto.getNumOfRetries()+RegistrationConstants.EMPTY));
		otherEntries.add(new Entry(OtherKey.SDK_SCORE, bioDto.getSdkScore()+RegistrationConstants.EMPTY));
		otherEntries.add(new Entry(OtherKey.FORCE_CAPTURED, bioDto.isForceCaptured()+RegistrationConstants.EMPTY));
		otherEntries.add(new Entry(OtherKey.PAYLOAD, payLoad == null ? RegistrationConstants.EMPTY : payLoad));
		otherEntries.add(new Entry(OtherKey.SPEC_VERSION, bioDto.getSpecVersion() == null ? RegistrationConstants.EMPTY : bioDto.getSpecVersion()));*/

		// SDK owner/version details, mirroring the Organization already carried
		// inside the primary Quality tag's Algorithm - but sourced from the SDK's
		// own self-reported SDKInfo (productOwner, sdkVersion, apiVersion) rather
		// than a fixed constant, since this describes whichever vendor SDK produced
		// bioDto.getSdkScore(). Only meaningful when an SDK score was produced.
		String sdkOrganization = RegistrationConstants.EMPTY;
		String sdkOrganizationType = RegistrationConstants.EMPTY;
		String sdkVersion = RegistrationConstants.EMPTY;
		String sdkApiVersion = RegistrationConstants.EMPTY;
		if (bioDto.getSdkScore() >= 0 && sdkInfoProvider != null) {
			SDKInfo sdkInfo = sdkInfoProvider.getSdkInfo(biometricType);
			if (sdkInfo != null) {
				if (sdkInfo.getProductOwner() != null) {
					sdkOrganization = sdkInfo.getProductOwner().getOrganization() == null
							? RegistrationConstants.EMPTY : sdkInfo.getProductOwner().getOrganization();
					sdkOrganizationType = sdkInfo.getProductOwner().getType() == null
							? RegistrationConstants.EMPTY : sdkInfo.getProductOwner().getType();
				}
				sdkVersion = sdkInfo.getSdkVersion() == null ? RegistrationConstants.EMPTY : sdkInfo.getSdkVersion();
				sdkApiVersion = sdkInfo.getApiVersion() == null ? RegistrationConstants.EMPTY : sdkInfo.getApiVersion();
			}
		}

		// SDK_SCORE and AGGREGATED_SCORE are existing packet fields (SDK_SCORE
		// already written on develop); their "not evaluated" sentinel (-1.0) is
		// internal to BiometricsDto only (see its field comments) and must not
		// change what these two fields mean on the wire - develop wrote 0.0 when
		// a source didn't run, so that's what's emitted here too, changing an
		// existing packet field's semantics would need downstream consumer
		// sign-off, unlike AGGREGATED_STRATEGY below, which is a brand-new field.
		double sdkScoreForPacket = bioDto.getSdkScore() < 0 ? 0.0 : bioDto.getSdkScore();
		double aggregatedScoreForPacket = bioDto.getAggregatedScore() < 0 ? 0.0 : bioDto.getAggregatedScore();

		return new BIR.BIRBuilder().withBdb(bioDto.getAttributeISO() == null ? new byte[0] : bioDto.getAttributeISO())
				.withVersion(versionType)
				.withCbeffversion(versionType)
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, bioDto.getBioAttribute()))
						.withPurpose(PurposeType.ENROLL).withLevel(ProcessedLevelType.RAW)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.withSb(bioDto.getSignature() == null ? new byte[0] : bioDto.getSignature().getBytes(StandardCharsets.UTF_8))
				.withOthers(OtherKey.EXCEPTION, bioDto.getAttributeISO()==null ? "true" : "false")
				.withOthers(OtherKey.RETRIES, bioDto.getNumOfRetries()+RegistrationConstants.EMPTY)
				.withOthers(OtherKey.SDK_SCORE, sdkScoreForPacket+RegistrationConstants.EMPTY)
				// withOthers accepts a plain String key, not just OtherKey's constants -
				// AGGREGATED_SCORE and the SDK_SCORE_* owner details below are new
				// "others" entries, not repurposing SDK_SCORE.
				.withOthers(RegistrationConstants.AGGREGATED_SCORE, aggregatedScoreForPacket+RegistrationConstants.EMPTY)
				.withOthers(RegistrationConstants.AGGREGATED_STRATEGY, bioDto.getAggregationStrategy() == null
						? RegistrationConstants.EMPTY : bioDto.getAggregationStrategy())
				.withOthers(RegistrationConstants.SDK_SCORE_ORGANIZATION, sdkOrganization)
				.withOthers(RegistrationConstants.SDK_SCORE_ORGANIZATION_TYPE, sdkOrganizationType)
				.withOthers(RegistrationConstants.SDK_SCORE_SDK_VERSION, sdkVersion)
				.withOthers(RegistrationConstants.SDK_SCORE_API_VERSION, sdkApiVersion)
				.withOthers(OtherKey.FORCE_CAPTURED, bioDto.isForceCaptured()+RegistrationConstants.EMPTY)
				.withOthers(OtherKey.PAYLOAD, payLoad == null ? RegistrationConstants.EMPTY : payLoad)
				.withOthers(OtherKey.SPEC_VERSION, bioDto.getSpecVersion() == null ? RegistrationConstants.EMPTY : bioDto.getSpecVersion())
				.build();
	}
	
	public BIR buildBir(String bioAttribute, long qualityScore, byte[] iso, ProcessedLevelType processedLevelType) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Building BIR for captured biometrics to pass them for quality check with SDK");

		BiometricType biometricType = Biometric.getSingleTypeByAttribute(bioAttribute);
		
		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(biometricType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore(qualityScore);

		return new BIR.BIRBuilder().withBdb(iso)
				.withVersion(new VersionType(1, 1))
				.withCbeffversion(new VersionType(1, 1))
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, bioAttribute))
						.withPurpose(PurposeType.IDENTIFY).withLevel(processedLevelType)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();

	}

	public BIR buildBir(BiometricsDto biometricsDto, ProcessedLevelType processedLevelType) {
		LOGGER.info("Building BIR for captured biometrics to pass them for quality check with SDK");

		BiometricType biometricType = Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute());

		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(biometricType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) biometricsDto.getQualityScore());

		return new BIR.BIRBuilder().withBdb(biometricsDto.getAttributeISO())
				.withVersion(new VersionType(1, 1))
				.withCbeffversion(new VersionType(1, 1))
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, biometricsDto.getBioAttribute()))
						.withPurpose(PurposeType.IDENTIFY).withLevel(processedLevelType)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();
	}

	@SuppressWarnings("incomplete-switch")
	public List<String> getSubTypes(BiometricType biometricType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();
		switch (biometricType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			if (bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger"))
						.value());
			}
			break;
		case IRIS:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			break;

		case EXCEPTION_PHOTO:
		case FACE:
			break;
		}
		return subtypes;
	}

}
