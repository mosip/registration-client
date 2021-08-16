package io.mosip.registration.dto;

import java.util.List;

public class BlocklistedConsentDto {

    private String screenName;
    private List<String> words;
    private Boolean operatorConsent;
    private String supervisorId;
    private String supervisorAuthType;
}
