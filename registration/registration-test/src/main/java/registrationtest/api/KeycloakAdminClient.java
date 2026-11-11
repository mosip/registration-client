package registrationtest.api;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Creates Registration Client operator users in Keycloak and assigns realm roles, mirroring
 * android-registration-client ui-test's KeycloakUserManager. Uses the Keycloak admin REST API
 * directly so the test jar does not need the keycloak-admin-client / RESTEasy stack.
 *
 * The configured client must use client_credentials and have the realm-management
 * "manage-users" and "view-realm" roles (e.g. mosip-testrig-client / mosip-admin-client).
 */
public class KeycloakAdminClient {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KeycloakAdminClient.class);

    private static final int TIMEOUT_MS = 30_000;
    private static final RestAssuredConfig HTTP_CONFIG = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", TIMEOUT_MS)
                    .setParam("http.socket.timeout", TIMEOUT_MS)
                    .setParam("http.connection-manager.timeout", (long) TIMEOUT_MS));

    private final String adminRealmUrl;
    private final String accessToken;

    private KeycloakAdminClient(String adminRealmUrl, String accessToken) {
        this.adminRealmUrl = adminRealmUrl;
        this.accessToken = accessToken;
    }

    /**
     * @param keycloakUrl base Keycloak URL, including the "/auth" context path when the server uses it
     */
    public static KeycloakAdminClient login(String keycloakUrl, String realm, String clientId, String clientSecret) {
        String baseUrl = keycloakUrl.endsWith("/") ? keycloakUrl.substring(0, keycloakUrl.length() - 1) : keycloakUrl;
        Response response = RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .post(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token");
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Keycloak token request failed with HTTP status "
                    + response.getStatusCode() + " for client '" + clientId + "'");
        }
        String token = new JSONObject(response.getBody().asString()).getString("access_token");
        return new KeycloakAdminClient(baseUrl + "/admin/realms/" + realm, token);
    }

    /**
     * Creates the user with the given password and realm roles. An already existing user is used as
     * it is: its password and roles are not touched.
     *
     * @return true when the user was created, false when it already existed
     */
    public boolean createUser(String userName, String password, List<String> roleNames) {
        JSONObject credential = new JSONObject()
                .put("type", "password")
                .put("value", password)
                .put("temporary", false);
        JSONObject user = new JSONObject()
                .put("username", userName)
                .put("enabled", true)
                .put("firstName", userName)
                .put("lastName", userName)
                .put("email", "automation" + userName + "@automationlabs.com")
                .put("emailVerified", true)
                .put("credentials", new JSONArray().put(credential));

        Response response = request().body(user.toString()).post(adminRealmUrl + "/users");
        if (response.getStatusCode() == 409) {
            logger.info("Keycloak user {} already exists; using existing user", userName);
            return false;
        }
        if (response.getStatusCode() != 201) {
            throw new IllegalStateException("Creating Keycloak user '" + userName + "' failed with HTTP status "
                    + response.getStatusCode() + ": " + response.getBody().asString());
        }

        logger.info("Created Keycloak user {}", userName);
        assignRealmRoles(findUserId(userName), userName, roleNames);
        return true;
    }

    private String findUserId(String userName) {
        Response response = request()
                .queryParam("username", userName)
                .queryParam("exact", true)
                .get(adminRealmUrl + "/users");
        expectOk(response, "Looking up Keycloak user '" + userName + "'");
        JSONArray users = new JSONArray(response.getBody().asString());
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (userName.equalsIgnoreCase(user.getString("username"))) {
                return user.getString("id");
            }
        }
        throw new IllegalStateException("Keycloak user '" + userName + "' not found after creation");
    }

    private void assignRealmRoles(String userId, String userName, List<String> roleNames) {
        Response response = request().get(adminRealmUrl + "/roles");
        expectOk(response, "Listing Keycloak realm roles");
        JSONArray realmRoles = new JSONArray(response.getBody().asString());

        JSONArray toAssign = new JSONArray();
        List<String> missing = new ArrayList<>();
        for (String roleName : roleNames) {
            JSONObject match = null;
            for (int i = 0; i < realmRoles.length(); i++) {
                JSONObject role = realmRoles.getJSONObject(i);
                if (roleName.equalsIgnoreCase(role.getString("name"))) {
                    match = role;
                    break;
                }
            }
            if (match == null) {
                missing.add(roleName);
            } else {
                toAssign.put(new JSONObject().put("id", match.getString("id")).put("name", match.getString("name")));
            }
        }
        if (!missing.isEmpty()) {
            logger.warn("Roles not present in Keycloak realm, skipped for {}: {}", userName, missing);
        }
        if (toAssign.isEmpty()) {
            throw new IllegalStateException("None of the configured roles " + roleNames + " exist in the Keycloak realm");
        }

        Response assign = request().body(toAssign.toString())
                .post(adminRealmUrl + "/users/" + userId + "/role-mappings/realm");
        expectOk(assign, "Assigning roles to Keycloak user '" + userName + "'");
        logger.info("Assigned roles {} to {}", toAssign, userName);
    }

    private io.restassured.specification.RequestSpecification request() {
        return RestAssured.given()
                .config(HTTP_CONFIG)
                .relaxedHTTPSValidation()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken);
    }

    private static void expectOk(Response response, String context) {
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new IllegalStateException(context + " failed with HTTP status " + response.getStatusCode()
                    + ": " + response.getBody().asString());
        }
    }
}
