package io.mosip.registration.dto;

import java.util.List;

import lombok.Data;

@Data
public class BlocklistedConsentDto {

    private String screenName;
    private List<String> words;
    private Boolean operatorConsent;
    private String operatorId;
}
