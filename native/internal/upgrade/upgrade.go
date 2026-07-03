// Package upgrade holds the shared logic for BOTH native upgrade executables in
// a single file: the Java 11 -> 21 migration (T3 #809, built as migration.exe)
// and its inverse rollback (T4 #811, built as rollback.exe). Keeping the swap
// and its undo side by side makes them easy to read and keep in sync; the two
// cmd/ entrypoints are thin wrappers that call Migrate / Rollback.
//
// Both flows run outside the JVM (the swap deletes the very jre/ a JVM would
// hold open) and are idempotent/resumable.
package upgrade

import (
	"fmt"
	"log"
	"os"
	"path/filepath"

	"github.com/mosip/registration-client/native/internal/fsops"
	"github.com/mosip/registration-client/native/internal/jre"
)

// Migrate performs the idempotent Java 11 -> 21 JRE migration against the app
// root (design registration-upgrade.md step 4 / issue #809).
//
// Steps:
//  1. extract .artifacts/jre21.zip -> jre21_temp/ (partial-safe; only if missing)
//  2. back up jre/ -> jre11/ (once, unless already on 21)
//  3. promote jre21_temp/ -> jre/ (remove old jre/, rename)
//  4. reset lib/ to just _launcher.jar
//  5. restore run.bat from .artifacts/
func Migrate(base string) error {
	jrePath := filepath.Join(base, "jre")
	jre11 := filepath.Join(base, "jre11")
	jre21Temp := filepath.Join(base, "jre21_temp")
	jre21Zip := filepath.Join(base, ".artifacts", "jre21.zip")
	lib := filepath.Join(base, "lib")
	launcherSrc := filepath.Join(base, ".artifacts", "_launcher.jar")
	launcherDst := filepath.Join(lib, "_launcher.jar")
	runBatSrc := filepath.Join(base, ".artifacts", "run.bat")
	runBatDst := filepath.Join(base, "run.bat")

	major := jre.DetectMajor(jrePath)
	log.Printf("current jre major=%d", major)

	// Extract the new JRE FIRST, before touching jre/, so a failed extraction
	// never leaves the machine without a JRE. (Normally _launcher.jar already
	// extracted jre21_temp/ in step 3; this is the resume/fallback path.)
	//
	// Extract into a .partial sibling and only rename to jre21_temp/ on success.
	// jre21_temp/ existence therefore means a *complete* extraction: a hard
	// kill or power-loss mid-unzip leaves only jre21_temp.partial/ (discarded
	// and re-extracted on the next run) instead of a half-populated jre21_temp/
	// that the promote step would mistake for a finished JRE.
	if !fsops.Exists(jre21Temp) && major != 21 {
		log.Printf("extracting jre21.zip -> jre21_temp/")
		partial := jre21Temp + ".partial"
		if err := fsops.RemoveAll(partial); err != nil {
			return fmt.Errorf("clear stale jre21_temp.partial/: %w", err)
		}
		if err := fsops.Unzip(jre21Zip, partial); err != nil {
			return fmt.Errorf("extract jre21.zip: %w", err)
		}
		if err := os.Rename(partial, jre21Temp); err != nil {
			return fmt.Errorf("finalize jre21_temp/: %w", err)
		}
	}

	// Back up the old JRE once (rename jre/ -> jre11/) before it is replaced.
	// Guard on "not already on 21" rather than "== 11" so that an undetectable
	// release (major 0) is still backed up instead of being deleted at promote
	// with no jre11/ to roll back to; and so a 21 JRE is never mislabelled as
	// the jre11 backup. Skipped once jre11/ exists (idempotent).
	if major != 21 && fsops.Exists(jrePath) && !fsops.Exists(jre11) {
		log.Printf("backing up jre/ -> jre11/ (detected major=%d)", major)
		if err := os.Rename(jrePath, jre11); err != nil {
			return fmt.Errorf("back up jre/: %w", err)
		}
	}

	// Promote the new JRE into jre/. MoveDir removes any existing jre/ (e.g. a
	// partially-populated one from a prior failed attempt) before the rename.
	if fsops.Exists(jre21Temp) {
		log.Printf("promoting jre21_temp/ -> jre/")
		if err := fsops.MoveDir(jre21Temp, jrePath); err != nil {
			return fmt.Errorf("promote jre21_temp/: %w", err)
		}
	}

	// lib/ must contain only _launcher.jar after migration; the new jars arrive
	// from .TEMP/ via run.bat on the next restart.
	log.Printf("resetting lib/ to _launcher.jar")
	if err := fsops.CleanContents(lib); err != nil {
		return fmt.Errorf("clean lib/: %w", err)
	}
	if err := fsops.CopyFile(launcherSrc, launcherDst); err != nil {
		return fmt.Errorf("copy _launcher.jar: %w", err)
	}

	// Restore the Java 21 run.bat into the app root.
	if err := fsops.CopyFile(runBatSrc, runBatDst); err != nil {
		return fmt.Errorf("copy run.bat: %w", err)
	}
	return nil
}

// Rollback restores the Registration Client to its pre-migration state after a
// failed or aborted migration (design registration-upgrade.md step 4a / issue
// #811). It is fully idempotent (safe to run multiple times) and never touches
// ./MANIFEST.MF, so the version mismatch is preserved and the upgrade can be
// retried later.
//
// Steps:
//  1. restore jre11/ -> jre/ (skip if no backup)
//  2. remove jre21_temp/
//  3. restore run.bat_jre11 -> run.bat (skip if no backup)
//  4. empty .TEMP/, remove .artifacts/
//  5. invariant: ./MANIFEST.MF must be untouched
func Rollback(base string) error {
	var (
		jre         = filepath.Join(base, "jre")
		jre11       = filepath.Join(base, "jre11")
		jre21Temp   = filepath.Join(base, "jre21_temp")
		runBat      = filepath.Join(base, "run.bat")
		runBatJre11 = filepath.Join(base, "run.bat_jre11")
		tempDir     = filepath.Join(base, ".TEMP")
		artifacts   = filepath.Join(base, ".artifacts")
		manifest    = filepath.Join(base, "MANIFEST.MF")
	)

	// Invariant guard: snapshot ./MANIFEST.MF so we can prove it is untouched.
	manifestBefore, manifestPresentBefore := snapshot(manifest)

	// 1) Restore the Java 11 JRE backup. Skip when absent (already rolled back,
	//    or migration never started) - never delete jre/ without a backup to
	//    replace it with.
	if fsops.Exists(jre11) {
		log.Printf("restoring jre/ from jre11/")
		if err := fsops.MoveDir(jre11, jre); err != nil {
			return fmt.Errorf("restore jre/ from jre11/: %w", err)
		}
	} else {
		log.Printf("jre11/ backup absent - skipping JRE restore")
	}

	// 2) Remove any partial jre21_temp/.
	if fsops.Exists(jre21Temp) {
		log.Printf("removing jre21_temp/")
		if err := fsops.RemoveAll(jre21Temp); err != nil {
			return fmt.Errorf("remove jre21_temp/: %w", err)
		}
	}

	// 3) Restore the Java 11 run.bat backup.
	if fsops.Exists(runBatJre11) {
		log.Printf("restoring run.bat from run.bat_jre11")
		if err := fsops.RestoreFile(runBatJre11, runBat); err != nil {
			return fmt.Errorf("restore run.bat: %w", err)
		}
	}

	// 4) Empty the .TEMP/ staging directory (keep the directory).
	if fsops.Exists(tempDir) {
		log.Printf("cleaning .TEMP/")
		if err := fsops.CleanContents(tempDir); err != nil {
			return fmt.Errorf("clean .TEMP/: %w", err)
		}
	}

	// 4b) Remove the downloaded .artifacts/ (jre21.zip, run.bat, _launcher.jar).
	//     The version mismatch is preserved (MANIFEST.MF untouched), so
	//     softwareUpdateHandler re-downloads them if the operator retries the
	//     upgrade (design P8 / N12 / Scenario 5; mirrors step-6 cleanup). The
	//     rollback.exe binary lives in the app root (design step 6 lists it
	//     separately from .artifacts/), not inside .artifacts/, so removing
	//     .artifacts/ does not delete the executing binary.
	if fsops.Exists(artifacts) {
		log.Printf("removing .artifacts/")
		if err := fsops.RemoveAll(artifacts); err != nil {
			return fmt.Errorf("remove .artifacts/: %w", err)
		}
	}

	// 5) Verify ./MANIFEST.MF was not touched.
	manifestAfter, manifestPresentAfter := snapshot(manifest)
	if manifestPresentBefore != manifestPresentAfter || manifestBefore != manifestAfter {
		return fmt.Errorf("invariant violated: ./MANIFEST.MF changed during rollback")
	}
	return nil
}

// snapshot returns a stable size+modtime fingerprint of path and whether it
// exists, used to assert ./MANIFEST.MF is untouched.
func snapshot(path string) (fingerprint string, present bool) {
	fi, err := os.Stat(path)
	if err != nil {
		return "", false
	}
	return fmt.Sprintf("%d:%d", fi.Size(), fi.ModTime().UnixNano()), true
}

