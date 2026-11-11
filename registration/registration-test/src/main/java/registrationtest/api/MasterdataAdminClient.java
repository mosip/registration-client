package registrationtest.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import registrationtest.utility.LocalMachineDetails;

/**
 * Admin/masterdata calls needed before the Registration Client can be used against an environment:
 * user-zone mapping, user-center mapping and machine creation/activation. Mirrors
 * android-registration-client ui-test's AdminTestUtil, talking to the APIs directly with RestAssured.
 */
public class MasterdataAdminClient {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MasterdataAdminClient.class);

    private static final int TIMEOUT_MS = 30_000;
    private static final RestAssuredConfig HTTP_CONFIG = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", TIMEOUT_MS)
                    .setParam("http.socket.timeout", TIMEOUT_MS)
                    .setParam("http.connection-manager.timeout", (long) TIMEOUT_MS));
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final int MACHINE_VALIDITY_DAYS = 365;

    private final String baseUrl;
    private final String token;
    private final String langCode;

    private MasterdataAdminClient(String baseUrl, String token, String langCode) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.langCode = langCode;
    }

    /**
     * Authenticates an admin user (needs GLOBAL_ADMIN / ZONAL_ADMIN) through auth-manager.
     */
    public static MasterdataAdminClient login(String baseUrl, String appId, String clientId, String clientSecret,
            String userName, String password, String langCode) {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        JSONObject request = new JSONObject()
                .put("appId", appId)
                .put("clientId", clientId)
                .put("clientSecret", clientSecret)
                .put("userName", userName)
                .put("password", password);

        Response response = RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.JSON)
                .body(wrap("mosip.internal.authmanager.userpwd", request).toString())
                .post(url + "/v1/authmanager/authenticate/internal/useridPwd");

        JSONObject json = parse(response, "Admin authentication");
        JSONObject body = json.optJSONObject("response");
        if (body == null || !body.has("token")) {
            throw new IllegalStateException("Admin authentication failed for '" + userName + "': " + json.opt("errors"));
        }
        return new MasterdataAdminClient(url, body.getString("token"), langCode);
    }

    public void mapUserToZone(String userId, String zoneCode) {
        JSONObject request = new JSONObject()
                .put("zoneCode", zoneCode)
                .put("userId", userId)
                .put("langCode", langCode)
                .put("isActive", true);
        JSONObject created = parse(authorized().body(wrap("string", request).toString())
                .post(baseUrl + "/v1/masterdata/zoneuser"), "Zone mapping for " + userId);
        if (!failUnlessAlreadyDone(created, "Mapping user " + userId + " to zone " + zoneCode)) {
            return;
        }

        JSONObject activated = parse(authorized().queryParam("isActive", true).queryParam("userId", userId)
                .patch(baseUrl + "/v1/masterdata/zoneuser"), "Zone mapping activation for " + userId);
        failUnlessAlreadyDone(activated, "Activating zone mapping of " + userId);
        logger.info("User {} mapped to zone {}", userId, zoneCode);
    }

    public void mapUserToCenter(String userId, String regCenterId) {
        JSONObject request = new JSONObject()
                .put("id", userId)
                .put("name", userId)
                .put("statusCode", "active")
                .put("regCenterId", regCenterId)
                .put("langCode", langCode)
                .put("isActive", true);
        JSONObject created = parse(authorized().body(wrap("string", request).toString())
                .post(baseUrl + "/v1/masterdata/usercentermapping"), "Center mapping for " + userId);
        if (!failUnlessAlreadyDone(created, "Mapping user " + userId + " to center " + regCenterId)) {
            return;
        }

        JSONObject activated = parse(authorized().queryParam("isActive", true).queryParam("id", userId)
                .patch(baseUrl + "/v1/masterdata/usercentermapping"), "Center mapping activation for " + userId);
        failUnlessAlreadyDone(activated, "Activating center mapping of " + userId);
        logger.info("User {} mapped to center {}", userId, regCenterId);
    }

    public String getFirstMachineSpecId() {
        JSONObject json = parse(authorized().get(baseUrl + "/v1/masterdata/machinespecifications/all"),
                "Fetching machine specifications");
        JSONObject body = json.optJSONObject("response");
        JSONArray data = body == null ? null : body.optJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("No machine specification found on server: " + json.opt("errors"));
        }
        return data.getJSONObject(0).getString("id");
    }

    /**
     * Reuses the machine with this machine's name if it already exists, otherwise creates it. An
     * existing machine is left as it is, except that it is activated when inactive and its keys are
     * replaced when they no longer match this machine (the client could not sync otherwise, e.g. after
     * .mosipkeys was recreated on a machine without TPM).
     *
     * @return the server-side machine id, and whether this call created the machine
     */
    public RegisteredMachine registerMachine(LocalMachineDetails machine, String serialNum, String machineSpecId,
            String regCenterId, String zoneCode) {
        JSONObject existing = findMachineByName(machine.getName());
        if (existing == null) {
            String machineId = createMachine(machine, serialNum, machineSpecId, regCenterId, zoneCode);
            activateMachine(machineId);
            return new RegisteredMachine(machineId, true);
        }

        String machineId = existing.getString("id");
        logger.info("Using existing machine {} with id {} (center {})", machine.getName(), machineId,
                existing.optString("regCenterId"));
        if (!sameKey(machine.getPublicKey(), existing.optString("publicKey"))
                || !sameKey(machine.getSignPublicKey(), existing.optString("signPublicKey"))) {
            updateMachineKeys(existing, machine);
        }
        if (!existing.optBoolean("isActive")) {
            activateMachine(machineId);
        }
        return new RegisteredMachine(machineId, false);
    }

    /** Server-side machine id and whether {@link #registerMachine} created it on this call. */
    public static final class RegisteredMachine {
        private final String id;
        private final boolean created;

        public RegisteredMachine(String id, boolean created) {
            this.id = id;
            this.created = created;
        }

        public String getId() {
            return id;
        }

        public boolean isCreated() {
            return created;
        }
    }

    private String createMachine(LocalMachineDetails machine, String serialNum, String machineSpecId,
            String regCenterId, String zoneCode) {
        JSONObject request = new JSONObject()
                .put("name", machine.getName())
                .put("serialNum", serialNum)
                .put("macAddress", machine.getMacAddress())
                .put("ipAddress", machine.getIpAddress())
                .put("machineSpecId", machineSpecId)
                .put("regCenterId", regCenterId)
                .put("zoneCode", zoneCode)
                .put("langCode", langCode)
                .put("isActive", true)
                .put("validityDateTime", OffsetDateTime.now(ZoneOffset.UTC).plusDays(MACHINE_VALIDITY_DAYS).format(TIMESTAMP))
                .put("publicKey", machine.getPublicKey())
                .put("signPublicKey", machine.getSignPublicKey());

        JSONObject json = parse(authorized().body(wrap("string", request).toString())
                .post(baseUrl + "/v1/masterdata/machines"), "Machine creation");
        failOnErrors(json, "Creating machine " + machine.getName());
        String machineId = json.getJSONObject("response").getString("id");
        logger.info("Created machine {} with id {}", machine.getName(), machineId);
        return machineId;
    }

    private void updateMachineKeys(JSONObject existing, LocalMachineDetails machine) {
        JSONObject request = new JSONObject();
        for (String field : new String[] { "id", "name", "serialNum", "macAddress", "ipAddress", "machineSpecId",
                "regCenterId", "zoneCode", "langCode", "validityDateTime", "isActive" }) {
            if (existing.has(field) && !existing.isNull(field)) {
                request.put(field, existing.get(field));
            }
        }
        request.put("publicKey", machine.getPublicKey());
        request.put("signPublicKey", machine.getSignPublicKey());

        JSONObject json = parse(authorized().body(wrap("string", request).toString())
                .put(baseUrl + "/v1/masterdata/machines"), "Machine key update");
        failOnErrors(json, "Updating keys of machine " + existing.getString("id"));
        logger.warn("Keys of existing machine {} did not match this machine and were updated", existing.getString("id"));
    }

    private void activateMachine(String machineId) {
        JSONObject activated = parse(authorized().queryParam("isActive", true).queryParam("id", machineId)
                .patch(baseUrl + "/v1/masterdata/machines"), "Machine activation");
        failUnlessAlreadyDone(activated, "Activating machine " + machineId);
        logger.info("Machine {} activated", machineId);
    }

    /** Server may return keys in standard or URL-safe Base64, with or without padding. */
    private static boolean sameKey(String local, String server) {
        return normalizeBase64(local).equals(normalizeBase64(server));
    }

    private static String normalizeBase64(String value) {
        return value == null ? "" : value.replace('+', '-').replace('/', '_').replace("=", "").trim();
    }

    private JSONObject findMachineByName(String name) {
        JSONObject filter = new JSONObject()
                .put("columnName", "name")
                .put("type", "equals")
                .put("value", name);
        JSONObject request = new JSONObject()
                .put("filters", new JSONArray().put(filter))
                .put("sort", new JSONArray())
                .put("pagination", new JSONObject().put("pageStart", 0).put("pageFetch", 10))
                .put("languageCode", langCode);

        JSONObject json = parse(authorized().body(wrap("string", request).toString())
                .post(baseUrl + "/v1/masterdata/machines/search"), "Machine search");
        JSONObject body = json.optJSONObject("response");
        JSONArray data = body == null ? null : body.optJSONArray("data");
        if (data == null) {
            logger.info("No existing machine named {} ({})", name, json.opt("errors"));
            return null;
        }
        for (int i = 0; i < data.length(); i++) {
            JSONObject machine = data.getJSONObject(i);
            if (name.equalsIgnoreCase(machine.optString("name"))) {
                return machine;
            }
        }
        return null;
    }

    private RequestSpecification authorized() {
        return RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.JSON)
                .cookie("Authorization", token);
    }

    private static JSONObject wrap(String id, JSONObject request) {
        return new JSONObject()
                .put("id", id)
                .put("version", "1.0")
                .put("requesttime", OffsetDateTime.now(ZoneOffset.UTC).format(TIMESTAMP))
                .put("metadata", new JSONObject())
                .put("request", request);
    }

    private static JSONObject parse(Response response, String context) {
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new IllegalStateException(context + " failed with HTTP status " + response.getStatusCode());
        }
        try {
            return new JSONObject(response.getBody().asString());
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(context + " returned a non-JSON response (status "
                    + response.getStatusCode() + ")", e);
        }
    }

    private static void failOnErrors(JSONObject json, String context) {
        JSONArray errors = json.optJSONArray("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new IllegalStateException(context + " failed: " + errors);
        }
    }

    /**
     * Setup is re-run before every automation cycle, so "already exists"/"duplicate request" errors
     * from previous runs are expected and only logged.
     *
     * @return true if the call succeeded, false if it was skipped because it was already done
     */
    private static boolean failUnlessAlreadyDone(JSONObject json, String context) {
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null || errors.isEmpty()) {
            return true;
        }
        for (int i = 0; i < errors.length(); i++) {
            String message = errors.getJSONObject(i).optString("message").toLowerCase();
            if (!message.contains("exist") && !message.contains("already") && !message.contains("duplicate")) {
                throw new IllegalStateException(context + " failed: " + errors);
            }
        }
        logger.warn("{} skipped: {}", context, errors);
        return false;
    }
}
