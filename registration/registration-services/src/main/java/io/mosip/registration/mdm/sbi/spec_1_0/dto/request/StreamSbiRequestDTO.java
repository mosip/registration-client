package io.mosip.registration.mdm.sbi.spec_1_0.dto.request;

import lombok.Data;

@Data
public class StreamSbiRequestDTO {

	private String serialNo;
	private String deviceSubId;
	private String timeout;
	private String dimensions;
}
