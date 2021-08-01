package io.mosip.registration.enums;

import io.mosip.registration.constants.AuditEvent;

public enum FlowType {

    NEW("New", AuditEvent.NAV_NEW_REG, "newRegistrationRoot"),
    UPDATE("Update", AuditEvent.NAV_UIN_UPDATE, "uinUpdateRoot"),
    LOST("Lost", AuditEvent.NAV_LOST_UIN, "lostUINRoot"),
    CORRECTION("Correction", AuditEvent.NAV_CORRECTION, "correctionRoot");

    FlowType(String category, AuditEvent auditEvent, String screenId) {
        this.category = category;
        this.auditEvent = auditEvent;
        this.screenId = screenId;
    }

    private String category;
    private AuditEvent auditEvent;
    private String screenId;

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
}
