package io.mosip.registration.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public enum Role {

    REGISTRATION_SUPERVISOR,
    REGISTRATION_OFFICER,
    Default;

    public static List<String> getValidRoles() {
        return Arrays.asList(REGISTRATION_OFFICER.name(), REGISTRATION_SUPERVISOR.name(), Default.name());
    }

    public static List<String> getSupervisorAuthRoles() {
        return Arrays.asList(REGISTRATION_SUPERVISOR.name());
    }

    public static boolean hasOperatorRole(List<String> userRoles) {
        return userRoles != null && userRoles.contains(REGISTRATION_OFFICER.name());
    }

    public static boolean hasSupervisorRole(List<String> userRoles) {
        return userRoles != null && userRoles.stream().anyMatch(r -> getSupervisorAuthRoles().contains(r));
    }

    public static boolean isDefaultUser(Collection<String> userRoles) {
        return userRoles != null && userRoles.contains(Default.name());
    }

    public static boolean hasAnyRegistrationRoles(Collection<String> userRoles) {
        return userRoles != null &&
                userRoles.stream().anyMatch(r ->  getValidRoles().contains(r));
    }
}
