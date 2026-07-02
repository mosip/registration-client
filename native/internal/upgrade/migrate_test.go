package upgrade

import (
	"path/filepath"
	"testing"
)

// P3-style: _launcher.jar already extracted jre21_temp/; migration promotes it.
func TestMigrate_PromotesExtractedTemp(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")
	writeJre(t, filepath.Join(base, "jre21_temp"), "21.0.1", "new21")
	artifacts := filepath.Join(base, ".artifacts")
	mustWrite(t, filepath.Join(artifacts, "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(artifacts, "run.bat"), "jre21 runbat")
	mustWrite(t, filepath.Join(base, "lib", "old.jar"), "old")

	if err := Migrate(base); err != nil {
		t.Fatalf("Migrate: %v", err)
	}

	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "new21")   // promoted
	assertAbsent(t, filepath.Join(base, "jre21_temp"))                    // consumed
	assertContent(t, filepath.Join(base, "jre11", "marker.txt"), "old11") // backed up
	assertAbsent(t, filepath.Join(base, "lib", "old.jar"))                // lib cleared
	assertContent(t, filepath.Join(base, "lib", "_launcher.jar"), "launcher")
	assertContent(t, filepath.Join(base, "run.bat"), "jre21 runbat")
}

// Fallback path: no jre21_temp/ -> migration extracts jre21.zip itself.
func TestMigrate_ExtractsWhenNoTemp(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")
	artifacts := filepath.Join(base, ".artifacts")
	makeZip(t, filepath.Join(artifacts, "jre21.zip"), map[string]string{
		"release":    "JAVA_VERSION=\"21.0.1\"\n",
		"marker.txt": "new21",
	})
	mustWrite(t, filepath.Join(artifacts, "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(artifacts, "run.bat"), "jre21 runbat")

	if err := Migrate(base); err != nil {
		t.Fatalf("Migrate: %v", err)
	}

	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "new21")
	assertContent(t, filepath.Join(base, "jre11", "marker.txt"), "old11")
	assertAbsent(t, filepath.Join(base, "jre21_temp"))
}

// A previous run was hard-killed mid-unzip, leaving a partial jre21_temp.partial/
// (and no jre21_temp/, since the finalize rename never happened). The re-run must
// discard the stale .partial, re-extract cleanly, and never promote the garbage.
func TestMigrate_StalePartialExtract_DiscardedAndReextracted(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")
	// Leftover from an interrupted extraction: a partial dir with garbage and no
	// release file. It must not survive or be promoted.
	mustWrite(t, filepath.Join(base, "jre21_temp.partial", "garbage.txt"), "half-written")
	artifacts := filepath.Join(base, ".artifacts")
	makeZip(t, filepath.Join(artifacts, "jre21.zip"), map[string]string{
		"release":    "JAVA_VERSION=\"21.0.1\"\n",
		"marker.txt": "new21",
	})
	mustWrite(t, filepath.Join(artifacts, "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(artifacts, "run.bat"), "jre21 runbat")

	if err := Migrate(base); err != nil {
		t.Fatalf("Migrate: %v", err)
	}

	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "new21") // clean extract promoted
	assertAbsent(t, filepath.Join(base, "jre", "garbage.txt"))          // stale partial not promoted
	assertAbsent(t, filepath.Join(base, "jre21_temp"))                  // consumed
	assertAbsent(t, filepath.Join(base, "jre21_temp.partial"))          // discarded
	assertContent(t, filepath.Join(base, "jre11", "marker.txt"), "old11")
}

func TestMigrate_Idempotent(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")
	writeJre(t, filepath.Join(base, "jre21_temp"), "21.0.1", "new21")
	artifacts := filepath.Join(base, ".artifacts")
	mustWrite(t, filepath.Join(artifacts, "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(artifacts, "run.bat"), "jre21 runbat")
	mustWrite(t, filepath.Join(base, "lib", "old.jar"), "old")

	if err := Migrate(base); err != nil {
		t.Fatalf("first Migrate: %v", err)
	}
	if err := Migrate(base); err != nil {
		t.Fatalf("second Migrate (idempotency): %v", err)
	}
	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "new21")
	assertContent(t, filepath.Join(base, "jre11", "marker.txt"), "old11")
}

// N5/N9-style failure: jre21.zip missing and no jre21_temp -> Migrate errors
// (migration.exe would then invoke rollback.exe). Extract-first means jre/ is
// left intact.
func TestMigrate_MissingJre21_FailsWithoutDestroyingJre(t *testing.T) {
	base := t.TempDir()
	writeJre(t, filepath.Join(base, "jre"), "11.0.20", "old11")
	mustWrite(t, filepath.Join(base, ".artifacts", "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(base, ".artifacts", "run.bat"), "jre21 runbat")

	if err := Migrate(base); err == nil {
		t.Fatal("expected error when jre21.zip is missing")
	}
	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "old11") // untouched
	assertAbsent(t, filepath.Join(base, "jre11"))
}

// Safety: jre/ has no readable release file (DetectMajor -> 0). The old JRE
// must still be backed up to jre11/ before promotion, never deleted without a
// rollback target.
func TestMigrate_UndetectableJre_StillBacksUp(t *testing.T) {
	base := t.TempDir()
	// jre/ with a marker but NO release file -> major detected as 0.
	mustWrite(t, filepath.Join(base, "jre", "marker.txt"), "old11")
	writeJre(t, filepath.Join(base, "jre21_temp"), "21.0.1", "new21")
	artifacts := filepath.Join(base, ".artifacts")
	mustWrite(t, filepath.Join(artifacts, "_launcher.jar"), "launcher")
	mustWrite(t, filepath.Join(artifacts, "run.bat"), "jre21 runbat")

	if err := Migrate(base); err != nil {
		t.Fatalf("Migrate: %v", err)
	}

	assertContent(t, filepath.Join(base, "jre11", "marker.txt"), "old11") // backed up, not lost
	assertContent(t, filepath.Join(base, "jre", "marker.txt"), "new21")   // promoted
	assertAbsent(t, filepath.Join(base, "jre21_temp"))
}
