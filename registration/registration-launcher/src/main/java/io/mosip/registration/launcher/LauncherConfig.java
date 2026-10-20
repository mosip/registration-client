/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
 * it reads the same {@code mosip-application.properties} the rest of the client reads:
 * <ul>
 *   <li>{@code mosip.client.upgrade.server.url} — the upgrade-server base (e.g. {@code https://dev.mosip.net})</li>
 *   <li>{@code mosip.reg.client.url} — the registration-client path template (e.g. {@code %s/registration-client/})</li>
 * </ul>
 * Per-version artifacts live under {@code {base}/{version}/...}, matching how {@code configure.sh}
 * publishes them.
 * <p>
 * <b>Where that file lives:</b> {@code configure.sh} does <i>not</i> ship it at the app root — it
 * {@code jar uf}s it as {@code props/mosip-application.properties} into {@code registration-services-*.jar}
 * (where {@code AppConfig}, {@code DaoConfig} and the validators read it) and into {@code _launcher.jar}
 * itself. The launcher's own copy is the one that matters: across the {@code <1.3.0 -> 1.3.0} transition
 * the services jar has been deleted and {@code _launcher.jar} is alone on the classpath. An app-root file
 * still takes precedence, as an operator/test override.
 */
public final class LauncherConfig {

    /** Where the build puts it: inside {@code _launcher.jar} (and the services jar), via configure.sh {@code jar uf}. */
    private static final String CLASSPATH_PROPERTIES = "props/mosip-application.properties";

    private static final String UPGRADE_SERVER_URL = "mosip.client.upgrade.server.url";
    private static final String REG_CLIENT_URL = "mosip.reg.client.url";
    private static final String DEFAULT_REG_CLIENT_TEMPLATE = "%s/registration-client/";

    private final String regClientBaseUrl;

    private LauncherConfig(String regClientBaseUrl) {
        this.regClientBaseUrl = regClientBaseUrl;
    }

    /**
     * Loads configuration from {@code propertiesFile} when that file exists, otherwise from the
     * {@code props/mosip-application.properties} classpath resource the build packages into
     * {@code _launcher.jar} — the one location present on every real install, transition included.
     *
     * @param propertiesFile optional app-root override; may be {@code null} or nonexistent
     * @throws IOException if neither source can be read
     */
    public static LauncherConfig load(File propertiesFile) throws IOException {
        if (propertiesFile != null && propertiesFile.isFile()) {
            try (InputStream in = Files.newInputStream(propertiesFile.toPath())) {
                return read(in);
            }
        }
        try (InputStream in = LauncherConfig.class.getClassLoader().getResourceAsStream(CLASSPATH_PROPERTIES)) {
            if (in == null) {
                throw new IOException("mosip-application.properties not found — neither at "
                        + propertiesFile + " nor on the classpath as " + CLASSPATH_PROPERTIES);
            }
            return read(in);
        }
    }

    private static LauncherConfig read(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        return fromProperties(props);
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
        // A query/fragment would be silently corrupted: versionBase appends "<version>/" as plain path
        // text, turning ".../registration-client?x=1" into ".../registration-client?x=11.4.0/".
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    propertyName + " must not contain a query or fragment: " + value);
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

    /**
     * Base URL (with trailing {@code /}) the root-level artifacts are served from for the given version:
     * {@code jre21.zip}, {@code migration.exe}, {@code rollback.exe}, {@code _launcher.jar} and
     * {@code run.bat}. They are hosted under {@code <version>/lib/} — the same location
     * {@code softwareUpdateHandler} downloads them from — even though they are consumed from
     * {@code .artifacts/} and the application root, never from {@code lib/}.
     * <p>
     * Used by the Case A / Case D recovery to re-fetch a root artifact whose local copy fails its hash.
     */
    public String rootArtifactBaseUrl(String version) {
        return versionBase(version) + "lib/";
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
