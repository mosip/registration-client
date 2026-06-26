package io.mosip.registration.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

/**
 * Resolves the upgrade-server URLs the launcher needs (the missing root {@code MANIFEST.MF.sig} for
 * Case C, and the {@code lib/MANIFEST.MF}(+sig) / {@code lib.zip} for step 5).
 * <p>
 * The launcher runs under Java 11 with no access to the services {@code ApplicationContext} / DB, so
 * it reads the same {@code mosip-application.properties} the build writes to the app root:
 * <ul>
 *   <li>{@code mosip.client.upgrade.server.url} — the upgrade-server base (e.g. {@code https://dev.mosip.net})</li>
 *   <li>{@code mosip.reg.client.url} — the registration-client path template (e.g. {@code %s/registration-client/})</li>
 * </ul>
 * Per-version artifacts live under {@code {base}/{version}/...}, matching how {@code configure.sh}
 * publishes them.
 */
public final class LauncherConfig {

    private static final String UPGRADE_SERVER_URL = "mosip.client.upgrade.server.url";
    private static final String REG_CLIENT_URL = "mosip.reg.client.url";
    private static final String DEFAULT_REG_CLIENT_TEMPLATE = "%s/registration-client/";

    private final String regClientBaseUrl;

    private LauncherConfig(String regClientBaseUrl) {
        this.regClientBaseUrl = regClientBaseUrl;
    }

    /** Loads configuration from a properties file (typically {@code mosip-application.properties} in the app root). */
    public static LauncherConfig load(File propertiesFile) throws IOException {
        try (InputStream in = Files.newInputStream(propertiesFile.toPath())) {
            Properties props = new Properties();
            props.load(in);
            return fromProperties(props);
        }
    }

    public static LauncherConfig fromProperties(Properties props) {
        String upgradeServer = props.getProperty(UPGRADE_SERVER_URL);
        Objects.requireNonNull(upgradeServer, UPGRADE_SERVER_URL + " is not set");
        upgradeServer = stripTrailingSlash(upgradeServer.trim());

        String template = props.getProperty(REG_CLIENT_URL, DEFAULT_REG_CLIENT_TEMPLATE);
        String base = String.format(template, upgradeServer);
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        // Validate the RENDERED base as a real URI, not a string prefix. A crafted reg.client.url
        // template such as "%s@evil.example/..." would pass a startsWith("https://") check yet parse to a
        // different authority, redirecting every artifact download off the configured host. Require https,
        // forbid embedded user-info, and pin the host/port to the configured upgrade server.
        URI serverUri = parseHttpsUri(upgradeServer, UPGRADE_SERVER_URL);
        URI baseUri = parseHttpsUri(base, REG_CLIENT_URL);
        if (baseUri.getUserInfo() != null
                || !serverUri.getHost().equalsIgnoreCase(baseUri.getHost())
                || serverUri.getPort() != baseUri.getPort()) {
            throw new IllegalArgumentException("Resolved upgrade URL '" + base
                    + "' does not stay on the configured upgrade server '" + upgradeServer + "'");
        }
        return new LauncherConfig(base);
    }

    /** Parses {@code value} and enforces an https URL with a host; used for both the server URL and the rendered base. */
    private static URI parseHttpsUri(String value, String propertyName) {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(propertyName + " is not a valid URL: " + value, e);
        }
        if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(propertyName + " must use https:// (got: " + value + ")");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException(propertyName + " has no host: " + value);
        }
        return uri;
    }

    /** URL of the detached signature of the root {@code ./MANIFEST.MF} for the given version (Case C). */
    public String rootManifestSigUrl(String version) {
        return versionBase(version) + "MANIFEST.MF.sig";
    }

    /** URL of {@code lib/MANIFEST.MF} for the given version (step 5). */
    public String libManifestUrl(String version) {
        return versionBase(version) + "lib/MANIFEST.MF";
    }

    /** URL of {@code lib/MANIFEST.MF.sig} for the given version (step 5). */
    public String libManifestSigUrl(String version) {
        return versionBase(version) + "lib/MANIFEST.MF.sig";
    }

    /** URL of {@code lib.zip} for the given version (step 5). */
    public String libZipUrl(String version) {
        return versionBase(version) + "lib.zip";
    }

    public String getRegClientBaseUrl() {
        return regClientBaseUrl;
    }

    private String versionBase(String version) {
        Objects.requireNonNull(version, "version is null");
        return regClientBaseUrl + version + "/";
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
