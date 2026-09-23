/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MigrationLauncherTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void launch_missingExe_throwsAndDoesNotStart() throws Exception {
        File appRoot = folder.getRoot();
        AtomicReference<ProcessBuilder> started = new AtomicReference<>();
        try {
            MigrationLauncher.launch(appRoot, started::set);
            fail("expected IOException when migration.exe is absent");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("migration.exe"));
        }
        assertNull("no process must be started when the exe is missing", started.get());
    }

    @Test
    public void launch_exePresent_startsFromAppRootByAbsolutePath() throws Exception {
        File appRoot = folder.getRoot();
        File exe = new File(appRoot, "migration.exe");
        Files.write(exe.toPath(), "stub".getBytes(StandardCharsets.UTF_8));

        AtomicReference<ProcessBuilder> started = new AtomicReference<>();
        MigrationLauncher.launch(appRoot, started::set);

        ProcessBuilder pb = started.get();
        assertNotNull("the exe should have been started", pb);
        assertEquals(1, pb.command().size());
        assertEquals(exe.getAbsolutePath(), pb.command().get(0));
        assertEquals(appRoot, pb.directory());
    }
}
