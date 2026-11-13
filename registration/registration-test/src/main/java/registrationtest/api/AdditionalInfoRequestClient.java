package registrationtest.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import registrationtest.utility.PropertiesUtil;

/**
 * Reads the additional info request (e.g. BIOMETRIC_CORRECTION) that registration processor creates when it
 * pauses a packet. Read-only access to the regprc database.
 */
public final class AdditionalInfoRequestClient {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdditionalInfoRequestClient.class);

    private AdditionalInfoRequestClient() {
    }

    public static String waitForRequestId(String regId, String process, long timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (true) {
            String requestId = findRequestId(regId, process);
            if (requestId != null) {
                return requestId;
            }
            String status = findRegistrationStatus(regId);
            if (isFinishedWithoutRequest(status)) {
                throw new IllegalStateException("No " + process + " request for " + regId
                        + ". Registration is already " + status);
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("No " + process + " request created for " + regId + " within "
                        + timeoutSeconds + " s. Last registration status: " + status);
            }
            logger.info("Waiting for {} request for {}. Registration status: {}", process, regId, status);
            Thread.sleep(15_000);
        }
    }

    /** A terminal registration status means the processor will not pause this packet later. */
    private static boolean isFinishedWithoutRequest(String status) {
        String code = status == null ? "" : status;
        int split = code.indexOf(' ');
        if (split > 0) {
            code = code.substring(0, split);
        }
        return "PROCESSED".equals(code) || "REJECTED".equals(code) || "FAILED".equals(code);
    }

    private static String findRequestId(String regId, String process) throws Exception {
        String sql = "select additional_info_req_id from regprc.additional_info_request"
                + " where reg_id = ? and additional_info_process = ? order by timestamp desc limit 1";
        try (Connection c = DriverManager.getConnection(PropertiesUtil.getKeyValue("regprc.db.url"),
                PropertiesUtil.getKeyValue("regprc.db.user"), PropertiesUtil.getKeyValue("postgres-password"))) {
            c.setReadOnly(true);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, regId);
                ps.setString(2, process);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }
    }

    private static String findRegistrationStatus(String regId) {
        String sql = "select status_code, reg_stage_name from regprc.registration where reg_id = ?";
        try (Connection c = DriverManager.getConnection(PropertiesUtil.getKeyValue("regprc.db.url"),
                PropertiesUtil.getKeyValue("regprc.db.user"), PropertiesUtil.getKeyValue("postgres-password"))) {
            c.setReadOnly(true);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, regId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return "NOT_FOUND";
                    }
                    String stage = rs.getString(2);
                    return rs.getString(1) + (stage == null || stage.isBlank() ? "" : " at " + stage);
                }
            }
        } catch (SQLException | java.io.IOException e) {
            logger.info("Could not read registration status for {}: {}", regId, e.getMessage());
            return "UNKNOWN";
        }
    }
}
