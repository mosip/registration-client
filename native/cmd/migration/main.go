// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// Command migration is the native executable: migration.exe.
//
// Thin entrypoint over the shared upgrade logic. It performs the Java 11 -> 21
// JRE swap from outside the JVM (the swap deletes the very jre/ a JVM would hold
// open). It is launched by _launcher.jar (which then exits the JVM) and is
// idempotent/resumable. On failure it auto-invokes rollback.exe (T4).
//
// The migration + rollback logic both live in ../../internal/upgrade.
package main

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/mosip/registration-client/native/internal/appenv"
	"github.com/mosip/registration-client/native/internal/fsops"
	"github.com/mosip/registration-client/native/internal/ui"
	"github.com/mosip/registration-client/native/internal/upgrade"
)

const (
	dialogTitle    = "Registration Client - JRE Migration"
	successMessage = "JRE migration complete. Please start the application using run.bat."
)

func main() { os.Exit(run()) }

// run performs the migration and returns the process exit code. Keeping os.Exit
// in main (not here) lets the deferred log-file close actually run.
func run() int {
	base, err := appenv.AppRoot()
	if err != nil {
		ui.ShowError(dialogTitle, "Could not determine the application directory: "+err.Error())
		return 1
	}

	if f, e := os.OpenFile(filepath.Join(base, "migration.log"),
		os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644); e == nil {
		defer func() { _ = f.Close() }()
		log.SetOutput(f)
	}
	log.Printf("migration started, base=%s", base)

	if err := upgrade.Migrate(base); err != nil {
		log.Printf("migration FAILED: %v", err)
		// Design step 4a: auto-invoke rollback.exe on failure, EXCEPT once the
		// JRE swap has already completed (jre/ is a valid Java 21). A late-stage
		// failure after the swap (lib/ reset or run.bat copy) must NOT roll back
		// — that would downgrade a good swap; the idempotent migration.exe is
		// simply re-run on the next launch to retry the failed tail steps.
		if !upgrade.ShouldRollback(base) {
			log.Printf("skipping rollback: JRE swap intact, migration is re-runnable")
			ui.ShowError(dialogTitle,
				"Migration could not be completed: "+err.Error()+
					"\n\nThe JRE is already in place and nothing was rolled back. "+
					"Please restart the application to finish the migration.")
			return 1
		}
		// Scenarios 1 & 5: restore the previous state; rollback.exe shows its own dialog.
		if rbErr := invokeRollback(base); rbErr != nil {
			log.Printf("could not launch rollback.exe: %v", rbErr)
			ui.ShowError(dialogTitle,
				"Migration failed: "+err.Error()+"\n\nRollback could not be started: "+rbErr.Error())
		}
		return 1
	}

	log.Printf("migration completed successfully")
	ui.ShowInfo(dialogTitle, successMessage)
	return 0
}

// invokeRollback fire-and-forget launches rollback.exe from the app root.
func invokeRollback(base string) error {
	rb := filepath.Join(base, "rollback.exe")
	if !fsops.Exists(rb) {
		return fmt.Errorf("rollback.exe not found at %s", rb)
	}
	log.Printf("auto-invoking rollback.exe")
	return exec.Command(rb).Start()
}

