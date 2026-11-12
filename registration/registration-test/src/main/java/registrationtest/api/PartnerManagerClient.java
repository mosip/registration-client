package registrationtest.api;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Partner management calls used to make device (SBI) certificates trusted by the server, mirroring the DSL
 * apitest-commons PartnerRegistration device flow: register a Device_Provider partner, upload its CA and
 * intermediate certificates to the DEVICE domain and upload the partner certificate to get it signed.
 */
public class PartnerManagerClient {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PartnerManagerClient.class);

    private static final int TIMEOUT_MS = 30_000;
    private static final RestAssuredConfig HTTP_CONFIG = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", TIMEOUT_MS)
                    .setParam("http.socket.timeout", TIMEOUT_MS)
                    .setParam("http.connection-manager.timeout", (long) TIMEOUT_MS));
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final String baseUrl;
    private final String token;

    private PartnerManagerClient(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    /**
     * Logs in through Keycloak with the PMS client (password grant), as the DSL does for its partner user.
     * The user needs the PARTNER_ADMIN role.
     */
    public static PartnerManagerClient login(String baseUrl, String keycloakUrl, String realm, String clientId,
            String clientSecret, String userName, String password) {
        String kcUrl = keycloakUrl.endsWith("/") ? keycloakUrl.substring(0, keycloakUrl.length() - 1) : keycloakUrl;
        Response response = RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "password")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("username", userName)
                .formParam("password", password)
                .post(kcUrl + "/realms/" + realm + "/protocol/openid-connect/token");
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("PMS login for '" + userName + "' failed with HTTP status "
                    + response.getStatusCode());
        }
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new PartnerManagerClient(url, new JSONObject(response.getBody().asString()).getString("access_token"));
    }

    /** Registers the Device_Provider partner; an already registered partner is reused. */
    public void registerDeviceProvider(String partnerId, String emailId) {
        JSONObject request = new JSONObject()
                .put("partnerId", partnerId)
                .put("organizationName", partnerId)
                .put("partnerType", "Device_Provider")
                .put("emailId", emailId)
                .put("contactNumber", "9876543210")
                .put("address", "Registration client automation");
        JSONObject json = parse(authorized().body(wrap(request, "LTS").toString())
                .post(baseUrl + "/v1/partnermanager/partners"), "Device provider registration");
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null || errors.isEmpty()) {
            logger.info("Registered device provider partner {}", partnerId);
            return;
        }
        String message = errors.toString().toLowerCase();
        if (message.contains("exist") || message.contains("duplicate")) {
            logger.info("Device provider partner {} already registered: {}", partnerId, errors);
            return;
        }
        throw new IllegalStateException("Registering device provider " + partnerId + " failed: " + errors);
    }

    /** Uploads a root or intermediate CA certificate; one that is already uploaded is accepted. */
    public void uploadCaCertificate(String certificatePem, String partnerDomain) {
        JSONObject request = new JSONObject()
                .put("certificateData", certificatePem)
                .put("partnerDomain", partnerDomain);
        JSONObject json = parse(authorized().body(wrap(request, "1.0").toString())
                .post(baseUrl + "/v1/partnermanager/partners/certificate/ca/upload"), "CA certificate upload");
        JSONArray errors = json.optJSONArray("errors");
        if (errors != null && !errors.isEmpty()) {
            if (errors.toString().toLowerCase().contains("exist")) {
                logger.info("CA certificate already uploaded to {}: {}", partnerDomain, errors);
                return;
            }
            throw new IllegalStateException("Uploading CA certificate to " + partnerDomain + " failed: " + errors);
        }
        logger.info("Uploaded CA certificate to domain {}", partnerDomain);
    }

    /**
     * Uploads the partner certificate and returns the certificates PMS sends back: the partner certificate
     * re-signed by the server plus its trust chain (PEM certificate or PKCS7 bundle, depending on version).
     */
    public List<X509Certificate> uploadPartnerCertificate(String certificatePem, String partnerDomain,
            String partnerId) {
        JSONObject request = new JSONObject()
                .put("certificateData", certificatePem)
                .put("partnerDomain", partnerDomain)
                .put("partnerId", partnerId);
        JSONObject json = parse(authorized().body(wrap(request, "1.0").toString())
                .post(baseUrl + "/v1/partnermanager/partners/certificate/upload"), "Partner certificate upload");
        JSONObject body = json.optJSONObject("response");
        if (body == null || !body.has("signedCertificateData")) {
            throw new IllegalStateException("Uploading partner certificate for " + partnerId + " failed: "
                    + json.opt("errors"));
        }
        logger.info("Uploaded partner certificate for {} to domain {}", partnerId, partnerDomain);
        return parseCertificates(body.getString("signedCertificateData"));
    }

    private static List<X509Certificate> parseCertificates(String data) {
        try {
            List<X509Certificate> certificates = new ArrayList<>();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (java.security.cert.Certificate certificate : factory
                    .generateCertificates(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))) {
                certificates.add((X509Certificate) certificate);
            }
            return certificates;
        } catch (CertificateException e) {
            throw new IllegalStateException("Signed partner certificate returned by PMS could not be parsed", e);
        }
    }

    private RequestSpecification authorized() {
        return RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.JSON)
                .cookie("Authorization", token);
    }

    private static JSONObject wrap(JSONObject request, String version) {
        return new JSONObject()
                .put("id", "string")
                .put("version", version)
                .put("requesttime", OffsetDateTime.now(ZoneOffset.UTC).format(TIMESTAMP))
                .put("metadata", new JSONObject())
                .put("request", request);
    }

    private static JSONObject parse(Response response, String context) {
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            String body = response.getBody() == null ? "" : response.getBody().asString().replaceAll("\\s+", " ").trim();
            if (body.length() > 500) {
                body = body.substring(0, 500);
            }
            throw new IllegalStateException(context + " failed with HTTP status " + response.getStatusCode()
                    + (body.isEmpty() ? "" : ": " + body));
        }
        try {
            return new JSONObject(response.getBody().asString());
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(context + " returned a non-JSON response (status "
                    + response.getStatusCode() + ")", e);
        }
    }
}
