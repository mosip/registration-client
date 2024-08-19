package io.mosip.registration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionMappings {

	private String dbVersion;
	private Integer releaseOrder;
	private String fullSyncEntities;
	
}
