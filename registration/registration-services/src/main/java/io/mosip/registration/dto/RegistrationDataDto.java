package io.mosip.registration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationDataDto {
	
	private String name;
	private String email;
	private String phone;
	private String langCode;
}
