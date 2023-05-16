package io.mosip.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionMappings {

	private String dbVersion;
	private Integer releaseOrder;
	
}
