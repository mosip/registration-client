package io.mosip.registration.dto.schema;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsSchema {
	
	private String name;
	private HashMap<String, String> description;
	private HashMap<String, String> label;
	private String fxml;
	private String icon;
	@JsonProperty("shortcut-icon")
	private String shortcutIcon;
	private String order;
	@JsonProperty("access-control")
	private List<String> accessControl;

}
