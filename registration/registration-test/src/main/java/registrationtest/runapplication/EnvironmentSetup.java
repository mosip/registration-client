package registrationtest.runapplication;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.KeycloakUserManager;
import registrationtest.api.ApitestCommonsBootstrap;
import registrationtest.api.MasterdataAdminClient;
import registrationtest.api.MasterdataCenterMapper;
import registrationtest.utility.LocalMachineDetails;

/**
 * One-time environment preparation run by run.bat before the Registration Client automation:
 * <ol>
 * <li>create the configured operator/supervisor/reviewer users in Keycloak with their roles
 * (via apitest-commons {@link KeycloakUserManager})</li>
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

    /** Password for the commons partner user ({@code regclient-111997}); matches Kernel.properties. */
    private static final String PARTNER_PASSWORD = "mosip123";

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
        ApitestCommonsBootstrap.init();

        Map<String, String> users = operatorUsers();
        String langCode = optional("langcode", "eng").split("@@")[0];
        String regCenterId = require("setup.regCenterId");
        String zoneCode = require("setup.zoneCode");

        boolean createdNewUserOrMachine = false;
        if (isEnabled("setup.createUsers")) {
            for (Map.Entry<String, String> user : users.entrySet()) {
                boolean existed = !KeycloakUserManager.getKeycloakUserID(user.getKey()).isEmpty();
                KeycloakUserManager.createUsers(user.getKey(), user.getValue(), null);
                if (!existed) {
                    createdNewUserOrMachine = true;
                }
            }
            report("Keycloak users ready: " + users.keySet());
        }

        if (isEnabled("setup.mapUsers")) {
            for (String userId : users.keySet()) {
                BaseTestCase.mapUserToZone(userId, zoneCode);
                BaseTestCase.mapZone(userId);
                MasterdataCenterMapper.mapUserToCenter(userId, regCenterId);
            }
            report("Users " + users.keySet() + " mapped to zone " + zoneCode + " and center " + regCenterId);
        }

        if (isEnabled("setup.createMachine")) {
            MasterdataAdminClient admin = MasterdataAdminClient.login(
                    ApitestCommonsBootstrap.adminBaseUrl(),
                    optional("setup.admin.appId", "admin"),
                    require("setup.admin.clientId"),
                    require("mosip_admin_client_secret"),
                    require("setup.admin.userName"),
                    require("setup.admin.password"),
                    langCode);
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
        // run.bat reads this line to decide whether to wait before the initial launch.
        report("SETUP_NEW_RESOURCES=" + createdNewUserOrMachine);
    }

    private static void setupMockMds(LocalMachineDetails machine) throws Exception {
        // Commons KernelAuthentication.getAuthForPartner() logs in as currentModule + "-111997"
        // with password from Kernel.properties (mosip123). Custom setup.pms.userName is not used for
        // PartnerRegistration auth; keep creating the commons-expected user.
        String partnerUser = ApitestCommonsBootstrap.partnerKeycloakUser();
        ApitestCommonsBootstrap.ensurePartnerAdminUser(partnerUser, PARTNER_PASSWORD);
        report("Partner Keycloak user ready: " + partnerUser);

        String partnerId = optional("setup.mds.partnerId",
                "regclient_device_" + machine.getName().replaceAll("[^A-Za-z0-9]", "_"));
        MockMdsSetup mockMds = new MockMdsSetup(Paths.get(optional("setup.mds.certsDir", "mds-certs")),
                optional("setup.mds.p12Password", "qwerty@123"));
        Path dskFile = mockMds.prepareDeviceKeystore(partnerId, require("mosip.hostname"));
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

    private static boolean isEnabled(String key) throws IOException {
        return !"N".equalsIgnoreCase(optional(key, "Y"));
    }

    private static String require(String key) throws IOException {
        return ApitestCommonsBootstrap.require(key);
    }

    private static String optional(String key, String defaultValue) throws IOException {
        return ApitestCommonsBootstrap.optional(key, defaultValue);
    }

    private static void report(String message) {
        logger.info(message);
        System.out.println(message);
    }
}
