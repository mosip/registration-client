package io.mosip.registration.dto.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessSpecDto {

    private String id;
    private int order;
    private String flow;
    @JsonProperty(value = "isActive")
    private boolean isActive;
    private HashMap<String, String> label;
    private List<UiScreenDTO> screens;
    private HashMap<String, String> caption;
    private String icon;
    private List<String> autoSelectedGroups;

}
