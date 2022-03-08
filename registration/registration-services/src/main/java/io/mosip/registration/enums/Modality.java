package io.mosip.registration.enums;

import io.mosip.registration.constants.RegistrationConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Modality {
    FINGERPRINT_SLAB_LEFT(RegistrationConstants.leftHandUiAttributes),
    FINGERPRINT_SLAB_RIGHT(RegistrationConstants.rightHandUiAttributes),
    FINGERPRINT_SLAB_THUMBS(RegistrationConstants.twoThumbsUiAttributes),
    IRIS_DOUBLE(RegistrationConstants.eyesUiAttributes),
    FACE(RegistrationConstants.faceUiAttributes),
    EXCEPTION_PHOTO(RegistrationConstants.exceptionPhotoAttributes);

    public List<String> getAttributes() {
        return attributes;
    }

    private List<String> attributes;

    Modality(List<String> attributes) {
        this.attributes = attributes;
    }

    public static List<String> getAllBioAttributes() {
        List<String> allAttributes = new ArrayList<>();
        allAttributes.addAll(FINGERPRINT_SLAB_LEFT.attributes);
        allAttributes.addAll(FINGERPRINT_SLAB_RIGHT.attributes);
        allAttributes.addAll(FINGERPRINT_SLAB_THUMBS.attributes);
        allAttributes.addAll(IRIS_DOUBLE.attributes);
        allAttributes.addAll(FACE.attributes);
        return allAttributes;
    }

    public static List<String> getAllBioAttributes(Modality modality) {
        switch (modality) {
            case FINGERPRINT_SLAB_THUMBS:
                return FINGERPRINT_SLAB_THUMBS.attributes;
            case FINGERPRINT_SLAB_RIGHT:
                return FINGERPRINT_SLAB_RIGHT.attributes;
            case FINGERPRINT_SLAB_LEFT:
                return FINGERPRINT_SLAB_LEFT.attributes;
            case IRIS_DOUBLE:
                return IRIS_DOUBLE.attributes;
            case FACE:
                return FACE.attributes;
            case EXCEPTION_PHOTO:
                return EXCEPTION_PHOTO.attributes;
        }
        return Collections.EMPTY_LIST;
    }

    public static Modality getModality(String bioAttribute) {
        switch (bioAttribute) {
            case RegistrationConstants.rightIndexUiAttribute:
            case RegistrationConstants.rightLittleUiAttribute:
            case RegistrationConstants.rightMiddleUiAttribute:
            case RegistrationConstants.rightRingUiAttribute:
                return Modality.FINGERPRINT_SLAB_RIGHT;

            case RegistrationConstants.leftIndexUiAttribute:
            case RegistrationConstants.leftLittleUiAttribute:
            case RegistrationConstants.leftMiddleUiAttribute:
            case RegistrationConstants.leftRingUiAttribute:
                return Modality.FINGERPRINT_SLAB_LEFT;

            case RegistrationConstants.rightThumbUiAttribute:
            case RegistrationConstants.leftThumbUiAttribute:
                return Modality.FINGERPRINT_SLAB_THUMBS;

            case RegistrationConstants.rightEyeUiAttribute:
            case RegistrationConstants.leftEyeUiAttribute:
                return Modality.IRIS_DOUBLE;

            case "face": return Modality.FACE;
            case RegistrationConstants.notAvailableAttribute: return Modality.EXCEPTION_PHOTO;
        }
        return null;
    }

}
