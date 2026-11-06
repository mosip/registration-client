package registrationtest.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import registrationtest.utility.PropertiesUtil;

/**
 * Creates a Pre-Registration application against a live MOSIP environment and
 * returns its Pre-Registration ID (PRID), for use with DemographicPage's PRID
 * fetch field. Mirrors the intent of android-registration-client's ui-test
 * PRID-creation flow, but talks to the auth-manager/pre-registration APIs
 * directly with RestAssured instead of depending on the (very heavy)
 * apitest-commons test-rig library.
 *
 * The identity payload is derived from repository_eng/NewAdult.json's own
 * "identity" object (this environment's actual, custom ID schema, already
 * proven to work against this environment's registration flow) rather than
 * fetched live, since this environment's schema is deployment-specific and
 * there is no reliably-known public endpoint to introspect it generically.
 * Only uniqueness-sensitive fields are regenerated per call.
 */
public class PreRegistrationClient {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(PreRegistrationClient.class);

    private static final Random RANDOM = new Random();

    private static final int TIMEOUT_MS = 30_000;
    private static final RestAssuredConfig HTTP_CONFIG = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", TIMEOUT_MS)
                    .setParam("http.socket.timeout", TIMEOUT_MS)
                    .setParam("http.connection-manager.timeout", (long) TIMEOUT_MS));

    // Automation/test-data metadata present in repository_eng JSON fixtures that
    // is not part of the actual MOSIP identity schema, plus fields (biometrics,
    // document-proof selections) that pre-registration does not collect.
    private static final Set<String> NON_IDENTITY_KEYS = new HashSet<>(Arrays.asList(
            "BELOW_ARE_STATIC_KEYWORDS", "documentUploadAttributes", "bioAttributes", "ageGroup",
            "process", "previewTests", "verifyPreviewEdit", "preferredLang", "profile",
            "score1", "score2", "score3", "score4", "consent", "individualBiometrics",
            "proofOfConsent", "proofOfAddress", "proofOfIdentity", "proofOfRelationship",
            "proofOfDateOfBirth"));

    private PreRegistrationClient() {
    }

    /**
     * @param ageGroup "infant", "minor" or "adult" (default: adult) - informational only,
     *                 the underlying demographic template already matches an adult profile.
     * @return the created Pre-Registration ID
     */
    public static String createPreRegistration(String ageGroup) {
        try {
            String baseUrl = "https://" + require("mosip.hostname");

            String token = authenticate(baseUrl);
            JSONObject identity = buildIdentity();

            JSONObject requestJson = new JSONObject();
            requestJson.put("id", "mosip.pre-registration.demographic.create");
            requestJson.put("version", "1.0");
            requestJson.put("requesttime", currentUtcTimestamp());

            JSONObject request = new JSONObject();
            request.put("langCode", "eng");
            request.put("requiredFields", new JSONArray(identity.keySet()));
            JSONObject demographicDetails = new JSONObject();
            demographicDetails.put("identity", identity);
            request.put("demographicDetails", demographicDetails);
            requestJson.put("request", request);

            // relaxedHTTPSValidation(): MOSIP QA/sandbox environments (this client's only
            // intended target) commonly present self-signed/internal-CA certificates, matching
            // the convention used across MOSIP's own apitest-commons RestClient for the same reason.
            Response response = RestAssured.given()
                    .config(HTTP_CONFIG)
                    .relaxedHTTPSValidation()
                    .contentType(ContentType.JSON)
                    .cookie("Authorization", token)
                    .body(requestJson.toString())
                    .post(baseUrl + "/preregistration/v1/applications/prereg");

            JSONObject responseJson = parseJsonResponse(response, "Pre-Registration creation");
            if (responseJson.has("response") && !responseJson.isNull("response")
                    && responseJson.getJSONObject("response").has("preRegistrationId")) {
                String preRegId = responseJson.getJSONObject("response").getString("preRegistrationId");
                logger.info("Created preRegistrationId={}", preRegId);
                return preRegId;
            }
            throw new IllegalStateException("Pre-Registration creation failed: status="
                    + response.getStatusCode() + " errors=" + responseJson.opt("errors"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config.properties for pre-registration setup", e);
        }
    }

    private static String authenticate(String baseUrl) throws IOException {
        JSONObject requestJson = new JSONObject();
        requestJson.put("id", "mosip.internal.authmanager.userpwd");
        requestJson.put("version", "1.0");
        requestJson.put("requesttime", currentUtcTimestamp());

        JSONObject request = new JSONObject();
        request.put("appId", require("preregistration.admin.appId"));
        request.put("userName", require("preregistration.admin.userName"));
        request.put("password", require("preregistration.admin.password"));
        request.put("clientId", require("preregistration.admin.clientId"));
        request.put("clientSecret", require("preregistration.admin.clientSecret"));
        requestJson.put("request", request);

        Response response = RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.JSON)
                .body(requestJson.toString())
                .post(baseUrl + "/v1/authmanager/authenticate/internal/useridPwd");

        JSONObject responseJson = parseJsonResponse(response, "Admin authentication");
        if (!responseJson.has("response") || responseJson.isNull("response")
                || !responseJson.getJSONObject("response").has("token")) {
            throw new IllegalStateException("Admin authentication failed: status="
                    + response.getStatusCode() + " errors=" + responseJson.opt("errors"));
        }
        return responseJson.getJSONObject("response").getString("token");
    }

    /**
     * Validates the HTTP status before parsing so a gateway/HTML error page produces a clear
     * failure instead of an opaque JSONException, and avoids echoing the full response body
     * (which can carry submitted PII or auth tokens) into logs/exception messages.
     */
    private static JSONObject parseJsonResponse(Response response, String context) {
        String body = response.getBody().asString();
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new IllegalStateException(
                    context + " failed with HTTP status " + response.getStatusCode());
        }
        try {
            return new JSONObject(body);
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(context + " returned a non-JSON response (status "
                    + response.getStatusCode() + ")", e);
        }
    }

    private static JSONObject buildIdentity() throws IOException {
        String datadir = require("datadir");
        java.nio.file.Path templateFile = Paths.get(System.getProperty("user.dir") + datadir + "NewAdult.json");
        if (!Files.exists(templateFile)) {
            // IDE/dev layout: repository_eng/ lives under src/main/resources rather than
            // alongside the built artifact.
            templateFile = Paths.get(
                    System.getProperty("user.dir") + "/src/main/resources" + datadir + "NewAdult.json");
        }
        String content = new String(Files.readAllBytes(templateFile));
        JSONObject template = new JSONObject(content).getJSONObject("identity");

        JSONObject identity = new JSONObject();
        Iterator<String> keys = template.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!NON_IDENTITY_KEYS.contains(key)) {
                identity.put(key, template.get(key));
            }
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        setLangArrayValue(identity, "fullName", "Test Automation " + suffix);
        identity.put("phone", String.valueOf(9000000000L + RANDOM.nextInt(999999999)));
        identity.put("email", "autotest" + suffix + "@mailinator.com");
        if (identity.has("referenceIdentityNumber")) {
            identity.put("referenceIdentityNumber", randomDigits(20));
        }

        return identity;
    }

    private static void setLangArrayValue(JSONObject identity, String field, String value) {
        JSONArray values = identity.optJSONArray(field);
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.length(); i++) {
            values.getJSONObject(i).put("value", value);
        }
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private static String currentUtcTimestamp() {
        return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    private static String require(String key) throws IOException {
        String value = PropertiesUtil.getKeyValue(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Missing required config.properties key '" + key + "' for pre-registration setup");
        }
        return value;
    }
}
