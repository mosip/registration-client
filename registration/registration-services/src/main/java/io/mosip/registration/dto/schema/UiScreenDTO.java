package io.mosip.registration.dto.schema;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiScreenDTO {

	private int order;
	private String name;
	private HashMap<String, String> label;
	private HashMap<String, String> caption;
	//TODO - need to change to List<UiSchemaDto>, values of attributes mentioned here takes priority over the general attribute values
	private List<String> fields;
	private String layoutTemplate;
	private boolean isActive;
	private boolean preRegFetchRequired;
	
}
