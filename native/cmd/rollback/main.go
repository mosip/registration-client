// Command rollback is the T4 (#811) native executable: rollback.exe.
//
// Thin entrypoint over the shared upgrade logic. It restores the Registration
// Client to its pre-migration state after a failed or aborted Java 11 -> 21
// migration. It is invoked by migration.exe on failure, by _launcher.jar on an
// inconsistent-JRE startup, or manually by an operator. It is fully idempotent
// and never touches ./MANIFEST.MF, so the version mismatch is preserved and the
// upgrade can be retried later.
//
// The migration + rollback logic both live in ../../internal/upgrade.
package main

import (
	"log"
	"os"
	"path/filepath"

	"github.com/mosip/registration-client/native/internal/appenv"
	"github.com/mosip/registration-client/native/internal/ui"
	"github.com/mosip/registration-client/native/internal/upgrade"
)

const (
	dialogTitle    = "Registration Client - Rollback"
	successMessage = "Rollback complete. Application restored to previous state. Please start using run.bat."
)

func main() { os.Exit(run()) }

// run performs the rollback and returns the process exit code. Keeping os.Exit
// in main (not here) lets the deferred log-file close actually run.
func run() int {
	base, err := appenv.AppRoot()
	if err != nil {
		ui.ShowError(dialogTitle, "Could not determine the application directory: "+err.Error())
		return 1
	}

	// Best-effort file log in the app root (the windowsgui build has no console).
	if f, e := os.OpenFile(filepath.Join(base, "rollback.log"),
		os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644); e == nil {
		defer func() { _ = f.Close() }()
		log.SetOutput(f)
	}
	log.Printf("rollback started, base=%s", base)

	if err := upgrade.Rollback(base); err != nil {
		log.Printf("rollback FAILED: %v", err)
		ui.ShowError(dialogTitle,
			"Rollback failed: "+err.Error()+"\n\nYou can retry by running rollback.exe again.")
		return 1
	}

	log.Printf("rollback completed successfully")
	ui.ShowInfo(dialogTitle, successMessage)
	return 0
}

