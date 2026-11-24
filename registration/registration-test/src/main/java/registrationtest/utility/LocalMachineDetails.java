package registrationtest.utility;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * Details of the local machine as the Registration Client identifies itself to the server
 * (machine name, encryption public key, signing public key), plus the network details
 * needed to register it through the masterdata machine API.
 *
 * Keys are read through the same {@link ClientCryptoFacade} the client uses: TPM-backed
 * keys when a TPM is available, otherwise the software key pair under
 * ${user.dir}/.mosipkeys. This must therefore run from the same working directory as the
 * Registration Client, otherwise a different key pair is read/generated.
 */
public class LocalMachineDetails {

    private final String name;
    private final String ipAddress;
    private final String macAddress;
    private final String publicKey;
    private final String signPublicKey;
    private final String keyIndex;
    private final boolean tpm;

    private LocalMachineDetails(String name, String ipAddress, String macAddress, String publicKey,
            String signPublicKey, String keyIndex, boolean tpm) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.publicKey = publicKey;
        this.signPublicKey = signPublicKey;
        this.keyIndex = keyIndex;
        this.tpm = tpm;
    }

    public static LocalMachineDetails read() throws Exception {
        ClientCryptoService security = newClientCryptoFacade().getClientSecurity();
        try {
            byte[] encryptionKey = security.getEncryptionPublicPart();
            byte[] signingKey = security.getSigningPublicPart();
            InetAddress address = resolveLocalIPv4Address();
            return new LocalMachineDetails(
                    machineName(),
                    address == null ? "127.0.0.1" : address.getHostAddress(),
                    macAddressOf(address),
                    CryptoUtil.encodeToURLSafeBase64(encryptionKey),
                    CryptoUtil.encodeToURLSafeBase64(signingKey),
                    CryptoUtil.computeFingerPrint(encryptionKey, null).toLowerCase(),
                    security.isTPMInstance());
        } finally {
            security.closeSecurityInstance();
        }
    }

    // ClientCryptoFacade is normally a Spring bean; outside Spring this @Value field stays null
    // and LocalClientCryptoServiceImpl would fail unboxing it when reading the public key.
    private static ClientCryptoFacade newClientCryptoFacade() throws ReflectiveOperationException {
        ClientCryptoFacade facade = new ClientCryptoFacade();
        Field field = ClientCryptoFacade.class.getDeclaredField("useResidentServiceModuleKey");
        field.setAccessible(true);
        field.set(facade, Boolean.FALSE);
        return facade;
    }

    // Must stay identical to RegistrationSystemPropertiesChecker.getMachineId(), which is the
    // machine name the client sends during its public key verification.
    private static String machineName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName().toLowerCase();
    }

    private static InetAddress resolveLocalIPv4Address() throws SocketException {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost instanceof Inet4Address && !localHost.isLoopbackAddress()) {
                return localHost;
            }
        } catch (UnknownHostException e) {
            // fall through to interface scan
        }
        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                continue;
            }
            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    return address;
                }
            }
        }
        return null;
    }

    private static String macAddressOf(InetAddress address) throws SocketException {
        if (address == null) {
            return "";
        }
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
        byte[] hardwareAddress = networkInterface == null ? null : networkInterface.getHardwareAddress();
        if (hardwareAddress == null) {
            return "";
        }
        StringBuilder mac = new StringBuilder();
        for (int i = 0; i < hardwareAddress.length; i++) {
            mac.append(String.format("%02X", hardwareAddress[i]));
            if (i < hardwareAddress.length - 1) {
                mac.append('-');
            }
        }
        return mac.toString();
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSignPublicKey() {
        return signPublicKey;
    }

    public String getKeyIndex() {
        return keyIndex;
    }

    public boolean isTpm() {
        return tpm;
    }

    @Override
    public String toString() {
        return "MachineName: " + name + System.lineSeparator()
                + "KeySource: " + (tpm ? "TPM" : "LOCAL (.mosipkeys)") + System.lineSeparator()
                + "IpAddress: " + ipAddress + System.lineSeparator()
                + "MacAddress: " + macAddress + System.lineSeparator()
                + "KeyIndex: " + keyIndex + System.lineSeparator()
                + "PublicKey: " + publicKey + System.lineSeparator()
                + "SignPublicKey: " + signPublicKey;
    }
}
