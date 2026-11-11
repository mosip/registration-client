package registrationtest.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import registrationtest.utility.PropertiesUtil;

/**
 * Reads the additional info request (e.g. BIOMETRIC_CORRECTION) that registration processor creates when it
 * pauses a packet. Read-only access to the regprc database configured with regprc.db.* keys.
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
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(
                        "No " + process + " request created for " + regId + " within " + timeoutSeconds + " s");
            }
            logger.info("Waiting for {} request for {}", process, regId);
            Thread.sleep(15_000);
        }
    }

    private static String findRequestId(String regId, String process) throws Exception {
        String sql = "select additional_info_req_id from regprc.additional_info_request"
                + " where reg_id = ? and additional_info_process = ? order by timestamp desc limit 1";
        try (Connection c = DriverManager.getConnection(PropertiesUtil.getKeyValue("regprc.db.url"),
                PropertiesUtil.getKeyValue("regprc.db.user"), PropertiesUtil.getKeyValue("regprc.db.password"))) {
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
}
