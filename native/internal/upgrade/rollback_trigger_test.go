// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package upgrade

import (
	"path/filepath"
	"testing"
)

// The reviewer's scenario: the JRE swap fully succeeded (jre/ is a complete
// Java 21, jre11/ backup present) but a late Migrate step (lib/ reset, run.bat
// copy) failed. ShouldRollback must be false so we do NOT downgrade a good
// Java 21 swap back to Java 11 — the idempotent migration is simply re-run.
func TestShouldRollback_Jre21SwapDone_DoesNotRollBack(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "21.0.1", "new21")
	writeJre(t, filepath.Join(base, "jre11"), "11.0.20", "old11")

	if ShouldRollback(base) {
		t.Fatal("ShouldRollback = true; a complete Java 21 jre/ must NOT be rolled back")
	}
}

// jre11/ backup exists and jre/ is missing (promote failed after backup) ->
// design trigger row 1 holds: roll back.
func TestShouldRollback_JreMissingWithBackup_RollsBack(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre11"), "11.0.20", "old11")
	// no jre/ at all

	if !ShouldRollback(base) {
		t.Fatal("ShouldRollback = false; a missing jre/ with a backup must roll back")
	}
}

// jre11/ backup exists and jre/ is partially populated (no readable release ->
// DetectMajor 0) -> roll back.
func TestShouldRollback_JrePartial_RollsBack(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre11"), "11.0.20", "old11")
	mustWrite(t, filepath.Join(base, "jre", "half.txt"), "partial") // no release file

	if !ShouldRollback(base) {
		t.Fatal("ShouldRollback = false; a partially-populated jre/ with a backup must roll back")
	}
}

// Early failure: extract failed before any backup, so jre/ is still the
// untouched Java 11 and there is no jre11/. The swap has NOT completed, so
// rollback is still invoked — it is a no-op on jre/ (no backup) and performs the
// harmless cleanup the design expects (run.bat_jre11 restore, .artifacts/).
func TestShouldRollback_StillJava11NoBackup_RollsBack(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")

	if !ShouldRollback(base) {
		t.Fatal("ShouldRollback = false; an incomplete swap (jre/ still Java 11) must roll back")
	}
}

// A completed Java 21 swap must never be rolled back even if the jre11/ backup
// was already cleaned up — do not undo real progress.
func TestShouldRollback_Jre21NoBackup_DoesNotRollBack(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "21.0.1", "new21")

	if ShouldRollback(base) {
		t.Fatal("ShouldRollback = true; a completed Java 21 swap must NOT be rolled back")
	}
}