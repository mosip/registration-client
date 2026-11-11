package registrationtest.runapplication;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import registrationtest.utility.PropertiesUtil;

/**
 * Starts and stops Mock MDS from the {@code io.mosip.mock.mds:mock-mds} dependency staged by the build.
 * Mock MDS reads {@code application.properties}, {@code Biometric Devices} and {@code Profile} from its
 * working directory, so those are copied beside the staged jars. The published jar does not contain the device
 * files, and its embedded application.properties lacks the status messages and admin APIs (admin/score,
 * admin/profile) the tests use, so a complete application.properties is shipped in mockmds-runtime as well.
 */
public final class MockMdsServer {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MockMdsServer.class);

    static final String MAIN_CLASS = "io.mosip.mock.sbi.test.TestMockSBI";
    static final int PORT = 4501;
    private static final String RUNTIME_ROOT = "mockmds-runtime";
    private static final long START_TIMEOUT_MS = 45_000;

    private static volatile Process process;

    private MockMdsServer() {
    }

    /**
     * Working directory Mock MDS is launched from. {@code setup.mockmds.dir} overrides it. Otherwise the
     * directory is {@code mockmds} next to the process, or {@code target/mockmds} after a Maven build.
     */
    public static Path runDirectory() throws IOException {
        String configured = PropertiesUtil.getKeyValue("setup.mockmds.dir");
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured.trim());
        }
        Path beside = Paths.get(System.getProperty("user.dir"), "mockmds");
        if (Files.isDirectory(beside.resolve("lib"))) {
            return beside;
        }
        Path staged = Paths.get(System.getProperty("user.dir"), "target", "mockmds");
        if (Files.isDirectory(staged.resolve("lib"))) {
            return staged;
        }
        throw new IllegalStateException("Mock MDS libraries were not found under " + beside + " or " + staged
                + ". Build registration-test (mvn package) so the mock-mds dependency is staged, and run from that directory.");
    }

    /** Copies device files and application.properties into the run directory. Does not start the process. */
    public static Path prepare() throws IOException {
        Path runDir = runDirectory();
        Files.createDirectories(runDir);
        copyRuntimeResources(runDir);
        extractApplicationProperties(runDir);
        return runDir;
    }

    /** Starts Mock MDS on port 4501 when nothing is already listening there. */
    public static void start() throws IOException {
        if (isListening()) {
            logger.info("Mock MDS is already listening on port {}; not starting another one", PORT);
            return;
        }
        Path runDir = prepare();
        installDeviceKey(runDir);
        Path lib = runDir.resolve("lib");
        String classpath;
        try (Stream<Path> jars = Files.list(lib)) {
            classpath = jars.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .map(path -> path.toAbsolutePath().toString())
                    .collect(Collectors.joining(java.io.File.pathSeparator));
        }
        if (!classpath.contains("mock-mds")) {
            throw new IllegalStateException("mock-mds jar not found in " + lib);
        }

        ProcessBuilder builder = new ProcessBuilder(javaBinary(), "-cp", classpath, MAIN_CLASS,
                "mosip.mock.sbi.device.purpose=Registration",
                "mosip.mock.sbi.biometric.type=Biometric Device");
        builder.directory(runDir.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(runDir.resolve("mockmds.log").toFile()));
        process = builder.start();
        logger.info("Starting Mock MDS from {}", runDir);

        long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Mock MDS exited before opening port " + PORT + ". See " + runDir.resolve("mockmds.log"));
            }
            if (isListening()) {
                logger.info("Mock MDS is listening on port {}", PORT);
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stop();
                throw new IOException("Interrupted while waiting for Mock MDS", e);
            }
        }
        stop();
        throw new IllegalStateException("Mock MDS did not open port " + PORT + " within " + START_TIMEOUT_MS + "ms. See " + runDir.resolve("mockmds.log"));
    }

    /** Stops the Mock MDS process this class started. A Mock MDS that was already running is left alone. */
    public static void stop() {
        Process running = process;
        process = null;
        if (running == null) {
            return;
        }
        running.destroy();
        try {
            if (!running.waitFor(5, TimeUnit.SECONDS)) {
                running.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running.destroyForcibly();
        }
        logger.info("Mock MDS stopped");
    }

    private static void installDeviceKey(Path runDir) {
        try {
            String certsDir = PropertiesUtil.getKeyValue("setup.mds.certsDir");
            Path certs = Paths.get(certsDir == null || certsDir.trim().isEmpty() ? "mds-certs" : certsDir.trim());
            Path dsk = certs.resolve(MockMdsSetup.DSK_FILE_NAME);
            if (!Files.isRegularFile(dsk)) {
                logger.info("No device key at {}; Mock MDS will use the keystore named in its application.properties", dsk);
                return;
            }
            String password = PropertiesUtil.getKeyValue("setup.mds.p12Password");
            new MockMdsSetup(certs, password == null || password.trim().isEmpty() ? "qwerty@123" : password.trim())
                    .installIntoMockMds(dsk, runDir);
        } catch (IOException e) {
            logger.warn("Could not install the device key into Mock MDS: {}", e.getMessage());
        }
    }

    private static void extractApplicationProperties(Path runDir) throws IOException {
        Path properties = runDir.resolve("application.properties");
        if (Files.isRegularFile(properties)) {
            return;
        }
        Path lib = runDir.resolve("lib");
        if (!Files.isDirectory(lib)) {
            throw new IllegalStateException("Mock MDS lib directory not found: " + lib);
        }
        Path mockJar;
        try (Stream<Path> jars = Files.list(lib)) {
            mockJar = jars.filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith("mock-mds-") && name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc");
            }).findFirst().orElseThrow(() -> new IllegalStateException("mock-mds jar not found in " + lib));
        }
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(mockJar.toFile())) {
            java.util.jar.JarEntry entry = jar.getJarEntry("application.properties");
            if (entry == null) {
                throw new IllegalStateException("application.properties is missing from " + mockJar);
            }
            try (InputStream in = jar.getInputStream(entry)) {
                Files.copy(in, properties);
            }
        }
    }

    private static void copyRuntimeResources(Path runDir) throws IOException {
        URL url = MockMdsServer.class.getClassLoader().getResource(RUNTIME_ROOT);
        if (url == null) {
            throw new IllegalStateException("Classpath resource " + RUNTIME_ROOT + " is missing from registration-test");
        }
        try {
            if ("file".equals(url.getProtocol())) {
                copyMissing(Paths.get(url.toURI()), runDir);
                return;
            }
            // jar:file:/.../registration-test.jar!/mockmds-runtime
            String spec = url.toString();
            int separator = spec.indexOf("!/");
            Path jarFile = Paths.get(URI.create(spec.substring(4, separator)));
            try (FileSystem jar = FileSystems.newFileSystem(jarFile, Collections.emptyMap())) {
                copyMissing(jar.getPath(RUNTIME_ROOT), runDir);
            }
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Could not read " + url, e);
        }
    }

    private static void copyMissing(Path sourceRoot, Path runDir) throws IOException {
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            for (Path source : walk.collect(Collectors.toList())) {
                Path relative = sourceRoot.relativize(source);
                if (relative.toString().isEmpty()) {
                    continue;
                }
                Path dest = runDir;
                for (String part : relative.toString().replace('\\', '/').split("/")) {
                    if (!part.isEmpty() && !".".equals(part)) {
                        dest = dest.resolve(part);
                    }
                }
                if (Files.isDirectory(source)) {
                    Files.createDirectories(dest);
                } else if (!Files.exists(dest)) {
                    Files.createDirectories(dest.getParent());
                    Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static String javaBinary() {
        Path windows = Paths.get(System.getProperty("user.dir"), "jre", "bin", "java.exe");
        if (Files.isRegularFile(windows)) {
            return windows.toString();
        }
        Path unix = Paths.get(System.getProperty("user.dir"), "jre", "bin", "java");
        if (Files.isRegularFile(unix)) {
            return unix.toString();
        }
        return "java";
    }

    private static boolean isListening() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
