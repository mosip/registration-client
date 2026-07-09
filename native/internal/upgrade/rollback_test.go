// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package upgrade

import (
	"os"
	"path/filepath"
	"testing"
)

// stageAppRoot builds a fake app root in a temp dir representing a machine that
// has migrated (or partially migrated) and now needs rolling back:
//   - jre11/        : the Java 11 backup (marker "jre11")
//   - jre/          : the partially-populated new JRE (marker "partial")
//   - jre21_temp/   : leftover extraction dir
//   - run.bat       : the (Java 21) run.bat
//   - run.bat_jre11 : the Java 11 run.bat backup
//   - .TEMP/        : staged files
//   - .artifacts/   : downloaded upgrade artifacts (must be removed)
//   - MANIFEST.MF   : must be untouched
func stageAppRoot(t *testing.T) string {
	t.Helper()
	base := t.TempDir()
	mustDirWith(t, filepath.Join(base, "jre11"), "marker.txt", "jre11")
	mustDirWith(t, filepath.Join(base, "jre"), "marker.txt", "partial")
	mustDirWith(t, filepath.Join(base, "jre21_temp"), "x.txt", "tmp")
	mustWrite(t, filepath.Join(base, "run.bat"), "jre21 runbat")
	mustWrite(t, filepath.Join(base, "run.bat_jre11"), "jre11 runbat")
	mustDirWith(t, filepath.Join(base, ".TEMP"), "patch.jar", "data")
	mustDirWith(t, filepath.Join(base, ".artifacts"), "jre21.zip", "zipdata")
	mustWrite(t, filepath.Join(base, "MANIFEST.MF"), "Manifest-Version: 1.0\n")
	return base
}

func TestRollback_RestoresPreMigrationState(t *testing.T) {
	base := stageAppRoot(t)
	manifestBefore := mustRead(t, filepath.Join(base, "MANIFEST.MF"))

	if err := Rollback(base); err != nil {
		t.Fatalf("Rollback returned error: %v", err)
	}

	// jre/ restored from jre11/ (marker == "jre11"); jre11/ consumed.
	if got := mustRead(t, filepath.Join(base, "jre", "marker.txt")); got != "jre11" {
		t.Errorf("jre/ not restored from jre11/: marker=%q", got)
	}
	assertAbsent(t, filepath.Join(base, "jre11"))
	// jre21_temp/ removed.
	assertAbsent(t, filepath.Join(base, "jre21_temp"))
	// run.bat restored from backup; backup consumed.
	if got := mustRead(t, filepath.Join(base, "run.bat")); got != "jre11 runbat" {
		t.Errorf("run.bat not restored: %q", got)
	}
	assertAbsent(t, filepath.Join(base, "run.bat_jre11"))
	// .TEMP/ emptied but kept.
	if entries, _ := os.ReadDir(filepath.Join(base, ".TEMP")); len(entries) != 0 {
		t.Errorf(".TEMP/ not emptied: %d entries", len(entries))
	}
	// .artifacts/ removed (P8 / Scenario 5).
	assertAbsent(t, filepath.Join(base, ".artifacts"))
	// MANIFEST.MF untouched.
	if got := mustRead(t, filepath.Join(base, "MANIFEST.MF")); got != manifestBefore {
		t.Errorf("MANIFEST.MF changed: %q", got)
	}
}

func TestRollback_Idempotent(t *testing.T) {
	base := stageAppRoot(t)
	if err := Rollback(base); err != nil {
		t.Fatalf("first Rollback: %v", err)
	}
	// Second run must be a clean no-op success (backups already consumed).
	if err := Rollback(base); err != nil {
		t.Fatalf("second Rollback (idempotency) returned error: %v", err)
	}
	if got := mustRead(t, filepath.Join(base, "jre", "marker.txt")); got != "jre11" {
		t.Errorf("jre/ corrupted by second run: marker=%q", got)
	}
}

// N12: rollback invoked when jre11/ backup does not exist -> skip JRE restore,
// still clean the rest, succeed.
func TestRollback_NoJreBackup_SkipsRestore(t *testing.T) {
	base := stageAppRoot(t)
	if err := os.RemoveAll(filepath.Join(base, "jre11")); err != nil {
		t.Fatal(err)
	}
	jreBefore := mustRead(t, filepath.Join(base, "jre", "marker.txt")) // "partial"

	if err := Rollback(base); err != nil {
		t.Fatalf("Rollback returned error: %v", err)
	}
	// jre/ left as-is (no backup to restore from - must NOT be deleted).
	if got := mustRead(t, filepath.Join(base, "jre", "marker.txt")); got != jreBefore {
		t.Errorf("jre/ altered without a backup: %q", got)
	}
	// Other cleanups still ran (N12: clean .TEMP/ and .artifacts/ even with no backup).
	assertAbsent(t, filepath.Join(base, "jre21_temp"))
	if entries, _ := os.ReadDir(filepath.Join(base, ".TEMP")); len(entries) != 0 {
		t.Errorf(".TEMP/ not emptied")
	}
	assertAbsent(t, filepath.Join(base, ".artifacts"))
}

// Scenario 1: migration failed after deleting jre/ but before promoting the new
// one. jre/ is absent, jre11/ backup present -> rollback restores it.
func TestRollback_JreDeleted_RestoresFromBackup(t *testing.T) {
	base := stageAppRoot(t)
	if err := os.RemoveAll(filepath.Join(base, "jre")); err != nil {
		t.Fatal(err)
	}

	if err := Rollback(base); err != nil {
		t.Fatalf("Rollback returned error: %v", err)
	}

	if got := mustRead(t, filepath.Join(base, "jre", "marker.txt")); got != "jre11" {
		t.Errorf("jre/ not restored from jre11/: marker=%q", got)
	}
	assertAbsent(t, filepath.Join(base, "jre11"))
}
