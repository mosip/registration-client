package registrationtest.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import registrationtest.utility.PropertiesUtil;

/**
 * Boots apitest-commons ({@link ConfigManager}, {@link BaseTestCase}, staged resource files)
 * so EnvironmentSetup can call KeycloakUserManager, zone mapping and PartnerRegistration.
 * Must run before any commons class that reads {@link BaseTestCase#getGlobalResourcePath()}.
 */
public final class ApitestCommonsBootstrap {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ApitestCommonsBootstrap.class);

    public static final String MODULE = "regclient";

    private static boolean initialized;

    private ApitestCommonsBootstrap() {
    }

    public static synchronized void init() throws IOException {
        if (initialized) {
            return;
        }

        Path resourceRoot = stageCommonsResources();
        // BaseTestCase.getGlobalResourcePath() for JAR mode is parent(markerJar) + "/MosipTestResource".
        // Keep the marker at the working directory so that resolves to ./MosipTestResource once, not twice.
        Path markerJar = Paths.get("").toAbsolutePath().resolve("registration-test-setup.jar");
        if (!Files.isRegularFile(markerJar)) {
            Files.write(markerJar, new byte[0]);
        }
        BaseTestCase.setRunContext("JAR", markerJar.toAbsolutePath().toString());

        Map<String, Object> overrides = buildOverrides();
        // ConfigManager.init reads System properties env.endpoint / env.keycloak and NPEs if unset.
        String endpoint = adminBaseUrl();
        System.setProperty("env.endpoint", endpoint);
        String keycloakUrl = require("setup.keycloak.url");
        System.setProperty("env.keycloak", keycloakUrl);
        ConfigManager.init(overrides);

        System.setProperty("env.endpoint", endpoint);
        System.setProperty("env.keycloak", ConfigManager.getIAMUrl());
        BaseTestCase.currentModule = MODULE;
        BaseTestCase.initialize();

        AdminTestUtil.init();
        seedLanguageList();

        initialized = true;
        logger.info("apitest-commons bootstrapped for {} (endpoint {})", MODULE, BaseTestCase.ApplnURI);
    }

    private static Map<String, Object> buildOverrides() throws IOException {
        Map<String, Object> map = new HashMap<>();
        String keycloakUrl = require("setup.keycloak.url");
        // ConfigManager.getIAMUrl() appends "/auth"
        if (keycloakUrl.endsWith("/auth")) {
            keycloakUrl = keycloakUrl.substring(0, keycloakUrl.length() - "/auth".length());
        } else if (keycloakUrl.endsWith("/auth/")) {
            keycloakUrl = keycloakUrl.substring(0, keycloakUrl.length() - "/auth/".length());
        }
        map.put("keycloak-external-url", keycloakUrl);
        map.put("keycloak-realm-id", optional("setup.keycloak.realm", "mosip"));
        map.put("mosip_testrig_client_id", optional("setup.keycloak.clientId", "mosip-testrig-client"));
        map.put("mosip_testrig_client_secret", require("mosip_testrig_client_secret"));
        map.put("mosip_admin_client_id", require("setup.admin.clientId"));
        map.put("mosip_admin_client_secret", require("mosip_admin_client_secret"));
        map.put("mosip_admin_app_id", optional("setup.admin.appId", "admin"));
        map.put("admin_userName", require("setup.admin.userName"));
        map.put("admin_password", require("setup.admin.password"));
        map.put("admin_zone_userName", require("setup.admin.userName"));
        map.put("admin_zone_password", require("setup.admin.password"));
        map.put("mosip_pms_client_id", optional("setup.pms.clientId", "mosip-pms-client"));
        map.put("mosip_pms_client_secret", require("mosip_pms_client_secret"));
        map.put("roles", require("setup.user.roles"));
        map.put("roles.partner", "PARTNER_ADMIN,PARTNERMANAGER");
        map.put("enableDebug", "no");
        return map;
    }

    /**
     * Stages the JSON/properties files PartnerRegistration and zone mapping read from disk under
     * {@code MosipTestResource/MosipTemporaryTestResource}.
     */
    private static Path stageCommonsResources() throws IOException {
        Path workDir = Paths.get("").toAbsolutePath();
        Path resourceRoot = workDir.resolve("MosipTestResource").resolve("MosipTemporaryTestResource");
        Path configDir = resourceRoot.resolve("config");
        Path authDir = configDir.resolve("Authorization");
        Files.createDirectories(authDir);

        copyClasspathResource("config/application.properties", configDir.resolve("application.properties"));
        copyClasspathResource("config/Authorization/zoneMappingRequest.json",
                authDir.resolve("zoneMappingRequest.json"));
        // KeycloakUserManager / RestClient may also look for Kernel.properties on disk; ship the jar default.
        copyClasspathResource("config/Kernel.properties", configDir.resolve("Kernel.properties"));
        return resourceRoot;
    }

    private static void copyClasspathResource(String classpathLocation, Path target) throws IOException {
        URL url = ApitestCommonsBootstrap.class.getClassLoader().getResource(classpathLocation);
        if (url == null) {
            // Kernel.properties lives inside apitest-commons; fall back to that classloader.
            url = ConfigManager.class.getClassLoader().getResource(classpathLocation);
        }
        if (url == null) {
            throw new IOException("Classpath resource not found: " + classpathLocation);
        }
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void seedLanguageList() throws IOException {
        BaseTestCase.languageList.clear();
        String lang = optional("langcode", "eng").split("@@")[0].trim();
        if (!lang.isEmpty()) {
            BaseTestCase.languageList.add(lang);
        }
    }

    public static String adminBaseUrl() throws IOException {
        String baseUrl = optional("setup.admin.baseUrl", null);
        return baseUrl != null ? baseUrl : "https://" + require("mosip.hostname");
    }

    public static String require(String key) throws IOException {
        String value = optional(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing required key '" + key
                    + "' (config.properties empty and not set in pod env / REG_CFG_" + key + ")");
        }
        return value;
    }

    public static String optional(String key, String defaultValue) throws IOException {
        String value = PropertiesUtil.getKeyValue(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    /** Partner Keycloak user that {@code getAuthForPartner()} will log in as ({@code regclient-111997}). */
    public static String partnerKeycloakUser() {
        return MODULE + "-111997";
    }

    /**
     * Creates/updates the partner admin Keycloak user with PARTNER_ADMIN roles, then restores operator
     * roles for subsequent {@link io.mosip.testrig.apirig.utils.KeycloakUserManager#createUsers} calls.
     */
    public static void ensurePartnerAdminUser(String userName, String password) throws IOException {
        Map<String, Object> partnerRoles = buildOverrides();
        partnerRoles.put("roles", "PARTNER_ADMIN,PARTNERMANAGER");
        ConfigManager.init(partnerRoles);
        io.mosip.testrig.apirig.utils.KeycloakUserManager.createUsers(userName, password, null);
        ConfigManager.init(buildOverrides());
    }
}
