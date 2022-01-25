package io.mosip.registration.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.mosip.registration.dto.mastersync.MasterSyncBaseDto;
import lombok.Data;

/**
 * The DTO Class UserDetailDto.
 *
 * @author Sreekar Chukka
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailDto extends MasterSyncBaseDto {
	private String userId;
	private String langCode;
	private Boolean isActive;
	private String regCenterId;
}
