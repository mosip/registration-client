package io.mosip.registration.dto.schema;

import lombok.Data;

import java.util.List;

@Data
public class ConditionalBioAttributes {

    private String ageGroup;
    private String process;
    private String validationExpr;
    private List<String> bioAttributes;
}
