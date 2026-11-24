package registrationtest.api;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.RestClient;
import io.restassured.response.Response;

/**
 * Parameterized user→center mapping. Commons {@link BaseTestCase#userCenterMapping()} hardcodes
 * center {@code 10005} and a module-prefixed admin user, so registration-test keeps this thin helper
 * that reuses commons auth + RestClient.
 */
public final class MasterdataCenterMapper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MasterdataCenterMapper.class);

    private MasterdataCenterMapper() {
    }

    public static void mapUserToCenter(String userId, String regCenterId) {
        String token = BaseTestCase.kernelAuthLib.getTokenByRole("globalAdmin");
        String url = BaseTestCase.ApplnURI + ConfigManager.getproperty("userCenterMappingUrl");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", userId);
        requestMap.put("name", userId);
        requestMap.put("statusCode", "active");
        requestMap.put("regCenterId", regCenterId);
        requestMap.put(GlobalConstants.ISACTIVE, GlobalConstants.TRUE_STRING);
        requestMap.put("langCode", BaseTestCase.getLanguageList().get(0));

        Map<String, Object> body = new HashMap<>();
        body.put("id", GlobalConstants.STRING);
        body.put(GlobalConstants.VERSION, GlobalConstants.STRING);
        body.put(GlobalConstants.REQUESTTIME, AdminTestUtil.generateCurrentUTCTimeStamp());
        body.put(GlobalConstants.METADATA, new HashMap<>());
        body.put(GlobalConstants.REQUEST, requestMap);

        Response created = RestClient.postRequestWithCookie(url, body, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON, GlobalConstants.AUTHORIZATION, token);
        if (!failUnlessAlreadyDone(created, "Mapping user " + userId + " to center " + regCenterId)) {
            return;
        }

        Map<String, String> activate = new HashMap<>();
        activate.put(GlobalConstants.ISACTIVE, GlobalConstants.TRUE_STRING);
        activate.put("id", userId);
        Response activated = RestClient.patchRequestWithCookieAndQueryParm(url, activate, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON, GlobalConstants.AUTHORIZATION, token);
        failUnlessAlreadyDone(activated, "Activating center mapping of " + userId);
        logger.info("User {} mapped to center {}", userId, regCenterId);
    }

    private static boolean failUnlessAlreadyDone(Response response, String context) {
        JSONObject json = new JSONObject(response.getBody().asString());
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null || errors.length() == 0) {
            return true;
        }
        for (int i = 0; i < errors.length(); i++) {
            JSONObject error = errors.getJSONObject(i);
            String message = error.optString("message").toLowerCase();
            if (!message.contains("exist") && !message.contains("already") && !message.contains("duplicate")) {
                throw new IllegalStateException(context + " failed: " + errors);
            }
        }
        logger.warn("{} skipped: {}", context, errors);
        return false;
    }
}
