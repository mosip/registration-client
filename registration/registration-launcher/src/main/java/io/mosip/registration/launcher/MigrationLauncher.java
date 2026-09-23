/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static io.mosip.registration.launcher.MigrationArtifacts.FILE_MIGRATION_EXE;

/**
 * Launches the native {@code migration.exe} that performs the JRE 11 -> 21 swap.
 * <p>
 * The swap deletes/replaces {@code jre/}, whose {@code javaw.exe}/{@code jvm.dll} the running JVM
 * holds open — so the launcher must start {@code migration.exe} as a <b>detached</b> process and then
 * exit the JVM immediately (see {@code Initialization.handleJreMigration}). {@code migration.exe}
 * shows its own progress/result dialogs and, on failure, invokes {@code rollback.exe}.
 * <p>
 * The binary is placed at the application root and integrity-checked against the signature-verified
 * root manifest by {@link JreMigrationStager} before this runs; this class only starts the
 * already-verified binary, by absolute path, so nothing external can substitute it.
 */
public final class MigrationLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationLauncher.class);

    /** Seam so the process launch can be exercised in tests without spawning a real process. */
    @FunctionalInterface
    interface Starter {
        void start(ProcessBuilder pb) throws IOException;
    }

    private MigrationLauncher() {
        // utility class
    }

    /**
     * Starts {@code migration.exe} from {@code appRoot} as a detached process. The caller MUST
     * {@code System.exit(0)} right after so the JVM releases {@code jre/bin/*} for the swap.
     *
     * @param appRoot the application root (also the process working directory)
     * @throws IOException if {@code migration.exe} is missing or cannot be started
     */
    public static void launch(File appRoot) throws IOException {
        launch(appRoot, ProcessBuilder::start);
    }

    static void launch(File appRoot, Starter starter) throws IOException {
        File exe = new File(appRoot, FILE_MIGRATION_EXE);
        if (!exe.isFile()) {
            throw new IOException(exe.getName() + " not found at the application root ("
                    + exe.getPath() + ") — cannot start the JRE migration");
        }
        // Absolute path (not a bare name resolved against PATH) so nothing on PATH can be run instead.
        ProcessBuilder pb = new ProcessBuilder(exe.getAbsolutePath());
        pb.directory(appRoot);
        // migration.exe is a windowsgui binary that shows its own dialogs; discard any stray stream
        // output rather than piping it into unread buffers we abandon at JVM exit.
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        // Do NOT waitFor: the process must outlive this JVM. On Windows a child started here keeps
        // running after the parent exits, so the migration proceeds once the JVM releases jre/.
        starter.start(pb);
        LOGGER.info("Launched {} — exiting so the JVM releases jre/ for the swap", exe.getName());
    }
}
