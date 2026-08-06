# MOSIP-43431 — Automated Registration Client Test Cases

This document lists the 45 registration client test scenarios automated as part of MOSIP-43431.
Each scenario is driven by test data JSON files under `src/main/resources/repository_eng/` and
executed via the registration-test automation framework.

## Test Categories

| Category | Count | Description |
|----------|-------|-------------|
| Preview screen validations | 15 | Verify preview page content before packet submission |
| Acknowledgement slip validations | 15 | Verify acknowledgement slip content after registration |
| Biometric retention flows | 8 | Verify biometrics retained after POE skip/delete navigation |
| Introducer biometrics options | 6 | Verify introducer modality options on biometric screen |
| Document exclusion on preview | 1 | Verify POE not shown on preview when not uploaded |
| **Total** | **45** | |

---

## Preview Screen Validations (15)

| # | Test Case ID | Description | Test Data File |
|---|-------------|-------------|----------------|
| 1 | PREV-001 | Verify QR code displayed on preview | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand, NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 2 | PREV-002 | Verify application ID displayed on preview | All exception test data files |
| 3 | PREV-003 | Verify date/time displayed on preview | All exception test data files |
| 4 | PREV-004 | Verify demographics (name, gender, city) on preview | All exception test data files |
| 5 | PREV-005 | Verify uploaded documents on preview | NewAdultExceptionLeftEye, NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 6 | PREV-006 | Verify applicant biometrics on preview | NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 7 | PREV-007 | Verify biometric exceptions on preview | All exception test data files |
| 8 | PREV-008 | Verify introducer details on preview (infant/minor) | NewChildInfantExceptionEyeParent (via previewTests ALL) |
| 9 | PREV-009 | Verify UIN on preview (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 10 | PREV-010 | Verify authentication biometrics on preview (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 11 | PREV-011 | Verify preview edit restriction (non-editable fields) | NewAdultExceptionLeftEye |
| 12 | PREV-012 | Verify back navigation allows edit from preview | NewAdultExceptionLeftEye |
| 13 | PREV-013 | Verify biometrics on preview (lost flow) | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand |
| 14 | PREV-014 | Verify demographics on preview (lost flow) | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand |
| 15 | PREV-015 | Verify POE document excluded from preview when not uploaded | NewAdultExceptionLefthand, LostAdultExceptionRighthand |

---

## Acknowledgement Slip Validations (15)

| # | Test Case ID | Description | Test Data File |
|---|-------------|-------------|----------------|
| 16 | ACK-001 | Verify QR code on acknowledgement slip | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand, NewAdultExceptionLeftEye, NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 17 | ACK-002 | Verify application ID on acknowledgement slip | All exception test data files |
| 18 | ACK-003 | Verify date/time on acknowledgement slip | All exception test data files |
| 19 | ACK-004 | Verify demographics on acknowledgement slip | All exception test data files |
| 20 | ACK-005 | Verify documents on acknowledgement slip | NewAdultExceptionLeftEye, NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 21 | ACK-006 | Verify biometrics on acknowledgement slip | NewAdultExceptionLeftEye, NewAdultExceptionLefthand, NewChildInfantExceptionEyeParent, NewChildMinorExceptionLeftHand |
| 22 | ACK-007 | Verify biometric exceptions on acknowledgement slip | All exception test data files |
| 23 | ACK-008 | Verify UIN on acknowledgement slip (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 24 | ACK-009 | Verify auth biometrics on acknowledgement slip (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 25 | ACK-010 | Verify demographics on acknowledgement slip (lost flow) | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand |
| 26 | ACK-011 | Verify biometrics on acknowledgement slip (lost flow) | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand |
| 27 | ACK-012 | Verify QR code on acknowledgement slip (lost flow) | LostAdultExceptionLeftEye, LostAdultExceptionRighthand, LostChildMinorExceptionRightHand |
| 28 | ACK-013 | Verify application ID on acknowledgement slip (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 29 | ACK-014 | Verify date/time on acknowledgement slip (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |
| 30 | ACK-015 | Verify demographics on acknowledgement slip (update flow) | UpdateAdultBioExceptionEye, UpdateAdultBioExceptionLeftHand |

---

## Biometric Retention Flows (8)

| # | Test Case ID | Description | Test Data File |
|---|-------------|-------------|----------------|
| 31 | BIO-001 | Verify biometrics retained after skipping POE upload | NewAdultExceptionLefthand, LostAdultExceptionRighthand |
| 32 | BIO-002 | Verify iris biometrics retained after document navigation | NewAdultExceptionLefthand, LostAdultExceptionRighthand |
| 33 | BIO-003 | Verify right hand fingerprints retained after document navigation | NewAdultExceptionLefthand, LostAdultExceptionRighthand |
| 34 | BIO-004 | Verify left hand fingerprints retained after document navigation | NewAdultExceptionLefthand |
| 35 | BIO-005 | Verify thumb fingerprints retained after document navigation | NewAdultExceptionLefthand |
| 36 | BIO-006 | Verify face biometrics retained after document navigation | NewAdultExceptionLefthand, LostAdultExceptionRighthand |
| 37 | BIO-007 | Verify exception photo retained after document navigation | NewAdultExceptionLefthand, LostAdultExceptionRighthand |
| 38 | BIO-008 | Verify POE upload and delete flow on document page | Enabled via `verifyPoeDeleteFlow:Y` in test data |

---

## Introducer Biometrics Options (6)

| # | Test Case ID | Description | Test Data File |
|---|-------------|-------------|----------------|
| 39 | INTRO-001 | Verify introducer iris option is visible | NewChildInfantExceptionEyeParent (via `verifyIntroducerBioOptions:Y`) |
| 40 | INTRO-002 | Verify introducer right hand fingerprint option is visible | NewChildInfantExceptionEyeParent |
| 41 | INTRO-003 | Verify introducer left hand fingerprint option is visible | NewChildInfantExceptionEyeParent |
| 42 | INTRO-004 | Verify introducer thumb fingerprint option is visible | NewChildInfantExceptionEyeParent |
| 43 | INTRO-005 | Verify introducer face capture option is visible | NewChildInfantExceptionEyeParent |
| 44 | INTRO-006 | Verify exception photo option is NOT shown for introducer | NewChildInfantExceptionEyeParent |

---

## Document Exclusion on Preview (1)

| # | Test Case ID | Description | Test Data File |
|---|-------------|-------------|----------------|
| 45 | DOC-001 | Verify POE document not displayed on preview when skipped | NewAdultExceptionLefthand, LostAdultExceptionRighthand |

---

## How to Run Locally

1. Configure `src/main/resources/config.properties` with operator credentials and environment settings.
2. Ensure MockMDS is running on port `4501`.
3. Build: `cd registration && mvn clean install -Dgpg.skip -DskipTests -pl registration-test`
4. Run the automation JAR as described in [README.md](README.md).
5. Check results in `report/extentReport-<timestamp>.html` and `logs/AutomationLogs.log`.

## Test Data Configuration Flags

| JSON Flag | Purpose |
|-----------|---------|
| `previewTests` | List of preview screen checks to run (or `ALL` for full validation) |
| `ackTests` | List of acknowledgement slip checks to run |
| `verifyBioAfterSkipPOE` | Run biometric retention test after skipping POE upload |
| `verifyPoeDeleteFlow` | Run biometric retention test after POE upload and delete |
| `verifyIntroducerBioOptions` | Verify introducer biometric modality options |
| `verifyPreviewEdit` | Verify preview screen edit restrictions |
