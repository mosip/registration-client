package registrationtest.runapplication;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import registrationtest.api.KeycloakAdminClient;
import registrationtest.api.MasterdataAdminClient;
import registrationtest.api.PartnerManagerClient;
import registrationtest.utility.LocalMachineDetails;
import registrationtest.utility.PropertiesUtil;

/**
 * One-time environment preparation to run before the Registration Client automation (runSetup.bat):
 * <ol>
 * <li>create the configured operator/supervisor/reviewer users in Keycloak with their roles</li>
 * <li>map those users to the configured zone and registration center</li>
 * <li>create (or reuse) this machine in masterdata with its public and signing public keys, and
 * activate it</li>
 * <li>create a server-trusted device certificate for Mock MDS and install it into the Mock MDS
 * run folder staged from the mock-mds Maven dependency</li>
 * </ol>
 * Run with argument {@code machine-details} to only print this machine's details without calling the
 * server.
 */
public class EnvironmentSetup {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EnvironmentSetup.class);

    /** Created in Keycloak when setup.pms.userName is not set. CA upload requires one of these roles. */
    private static final String PARTNER_ADMIN_USER = "regclientpms";
    private static final String PARTNER_ADMIN_PASSWORD = "Mosip@123";
    private static final List<String> PARTNER_ADMIN_ROLES = List.of("PARTNER_ADMIN", "PARTNERMANAGER");

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            LocalMachineDetails machine = LocalMachineDetails.read();
            report("Local machine details:" + System.lineSeparator() + machine);

            if (args.length == 0 || !"machine-details".equalsIgnoreCase(args[0])) {
                run(machine);
                report("Environment setup completed successfully");
            }
        } catch (Exception e) {
            logger.error("Environment setup failed", e);
            System.err.println("Environment setup failed: " + e.getMessage());
            e.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    private static void run(LocalMachineDetails machine) throws Exception {
        Map<String, String> users = operatorUsers();
        String langCode = optional("langcode", "eng").split("@@")[0];
        String regCenterId = require("setup.regCenterId");
        String zoneCode = require("setup.zoneCode");

        boolean createdNewUserOrMachine = false;
        if (isEnabled("setup.createUsers")) {
            KeycloakAdminClient keycloak = KeycloakAdminClient.login(
                    require("setup.keycloak.url"),
                    optional("setup.keycloak.realm", "mosip"),
                    require("setup.keycloak.clientId"),
                    require("setup.keycloak.clientSecret"));
            List<String> roles = csv(require("setup.user.roles"));
            for (Map.Entry<String, String> user : users.entrySet()) {
                if (keycloak.createUser(user.getKey(), user.getValue(), roles)) {
                    createdNewUserOrMachine = true;
                }
            }
            report("Keycloak users ready: " + users.keySet());
        }

        MasterdataAdminClient admin = MasterdataAdminClient.login(
                adminBaseUrl(),
                optional("setup.admin.appId", "admin"),
                require("setup.admin.clientId"),
                require("setup.admin.clientSecret"),
                require("setup.admin.userName"),
                require("setup.admin.password"),
                langCode);

        if (isEnabled("setup.mapUsers")) {
            for (String userId : users.keySet()) {
                admin.mapUserToZone(userId, zoneCode);
                admin.mapUserToCenter(userId, regCenterId);
            }
            report("Users " + users.keySet() + " mapped to zone " + zoneCode + " and center " + regCenterId);
        }

        if (isEnabled("setup.createMachine")) {
            String machineSpecId = optional("setup.machine.specId", null);
            if (machineSpecId == null) {
                machineSpecId = admin.getFirstMachineSpecId();
            }
            MasterdataAdminClient.RegisteredMachine registered = admin.registerMachine(machine,
                    optional("setup.machine.serialNum", machine.getName()),
                    machineSpecId, regCenterId, zoneCode);
            if (registered.isCreated()) {
                createdNewUserOrMachine = true;
            }
            report("Machine " + machine.getName() + " registered and active with id " + registered.getId());
        }

        if (isEnabled("setup.mockMds")) {
            setupMockMds(machine);
        }
        // runAll.bat reads this line to decide whether to wait before the initial launch.
        report("SETUP_NEW_RESOURCES=" + createdNewUserOrMachine);
    }

    private static void setupMockMds(LocalMachineDetails machine) throws Exception {
        String keycloakUrl = require("setup.keycloak.url");
        String realm = optional("setup.keycloak.realm", "mosip");
        String pmsClientId = optional("setup.pms.clientId", "mosip-pms-client");
        String pmsClientSecret = require("setup.pms.clientSecret");
        String user = optional("setup.pms.userName", null);
        String password = optional("setup.pms.password", null);
        if (user == null) {
            user = PARTNER_ADMIN_USER;
            if (password == null) {
                password = PARTNER_ADMIN_PASSWORD;
            }
            KeycloakAdminClient keycloak = KeycloakAdminClient.login(
                    keycloakUrl, realm, require("setup.keycloak.clientId"), require("setup.keycloak.clientSecret"));
            keycloak.ensureUser(user, password, PARTNER_ADMIN_ROLES);
            report("Partner admin user ready: " + user);
        } else if (password == null) {
            password = require("setup.admin.password");
        }
        final String pmsUser = user;
        final String pmsPassword = password;
        String baseUrl = adminBaseUrl();

        String partnerId = optional("setup.mds.partnerId",
                "regclient_device_" + machine.getName().replaceAll("[^A-Za-z0-9]", "_"));
        MockMdsSetup mockMds = new MockMdsSetup(Paths.get(optional("setup.mds.certsDir", "mds-certs")),
                optional("setup.mds.p12Password", "qwerty@123"));
        Path dskFile = mockMds.prepareDeviceKeystore(partnerId, () -> PartnerManagerClient.login(baseUrl,
                keycloakUrl, realm, pmsClientId, pmsClientSecret, pmsUser, pmsPassword));
        report("Device key for Mock MDS ready: " + dskFile.toAbsolutePath() + " (partner " + partnerId + ")");

        Path mockMdsDir = MockMdsServer.prepare();
        mockMds.installIntoMockMds(dskFile, mockMdsDir);
        report("Mock MDS in " + mockMdsDir + " configured with the device key");
    }

    /** The users the automation logs in with, de-duplicated (operator, supervisor and reviewer may be the same). */
    private static Map<String, String> operatorUsers() throws IOException {
        Map<String, String> users = new LinkedHashMap<>();
        users.putIfAbsent(require("operatorId"), require("operatorPwd"));
        users.putIfAbsent(require("supervisorUserid"), require("supervisorUserpwd"));
        users.putIfAbsent(require("reviewerUserid"), require("reviewerpwd"));
        return users;
    }

    private static String adminBaseUrl() throws IOException {
        String baseUrl = optional("setup.admin.baseUrl", null);
        return baseUrl != null ? baseUrl : "https://" + require("mosip.hostname");
    }

    private static boolean isEnabled(String key) throws IOException {
        return !"N".equalsIgnoreCase(optional(key, "Y"));
    }

    private static List<String> csv(String value) {
        List<String> values = new ArrayList<>();
        for (String item : value.split(",")) {
            if (!item.trim().isEmpty()) {
                values.add(item.trim());
            }
        }
        return values;
    }

    private static String require(String key) throws IOException {
        String value = optional(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing required config.properties key '" + key + "' for environment setup");
        }
        return value;
    }

    private static String optional(String key, String defaultValue) throws IOException {
        String value = PropertiesUtil.getKeyValue(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static void report(String message) {
        logger.info(message);
        System.out.println(message);
    }
}
