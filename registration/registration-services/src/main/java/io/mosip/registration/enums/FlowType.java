package io.mosip.registration.enums;

import io.mosip.registration.constants.AuditEvent;

public enum FlowType {

    NEW("New", AuditEvent.NAV_NEW_REG, "newRegistrationRoot", "N"),
    UPDATE("Update", AuditEvent.NAV_UIN_UPDATE, "uinUpdateRoot", "U"),
    LOST("Lost", AuditEvent.NAV_LOST_UIN, "lostUINRoot", "L"),
    RENEWAL("Renewal", AuditEvent.NAV_RENEW_UIN, "renewUINRoot", "R"),
    CORRECTION("Correction", AuditEvent.NAV_CORRECTION, "correctionRoot", "C");

    FlowType(String category, AuditEvent auditEvent, String screenId, String registrationTypeCode) {
        this.category = category;
        this.auditEvent = auditEvent;
        this.screenId = screenId;
        this.registrationTypeCode = registrationTypeCode;
    }

    private String category;
    private AuditEvent auditEvent;
    private String screenId;
    private String registrationTypeCode;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public AuditEvent getAuditEvent() {
        return auditEvent;
    }

    public void setAuditEvent(AuditEvent auditEvent) {
        this.auditEvent = auditEvent;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getRegistrationTypeCode() {
        return registrationTypeCode;
    }

    public void setRegistrationTypeCode(String registrationTypeCode) {
        this.registrationTypeCode = registrationTypeCode;
    }
}
