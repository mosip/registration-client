// Package fsops provides the small set of idempotent filesystem operations the
// migration/rollback executables need. Each operation is safe to run repeatedly
// (e.g. after an operator force-kill) and treats "source already absent" as a
// no-op rather than an error.
package fsops

import (
	"os"
	"path/filepath"
)

// Exists reports whether path exists (file or directory).
func Exists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

// MoveDir renames src to dst. On Windows os.Rename fails if dst exists, so any
// existing dst (e.g. a partially-populated jre/ from a failed migration) is
// removed first. src and dst are expected to be on the same volume (both under
// the app root), so the rename is atomic.
func MoveDir(src, dst string) error {
	if Exists(dst) {
		if err := os.RemoveAll(dst); err != nil {
			return err
		}
	}
	return os.Rename(src, dst)
}

// RemoveAll deletes path and any children. Absent path is not an error.
func RemoveAll(path string) error {
	return os.RemoveAll(path)
}

// CleanContents removes everything inside dir but keeps dir itself. A missing
// dir is not an error.
func CleanContents(dir string) error {
	entries, err := os.ReadDir(dir)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	for _, e := range entries {
		if err := os.RemoveAll(filepath.Join(dir, e.Name())); err != nil {
			return err
		}
	}
	return nil
}

// RestoreFile replaces target with backup (target removed, backup renamed to
// target). If backup is absent it is a no-op.
func RestoreFile(backup, target string) error {
	if !Exists(backup) {
		return nil
	}
	if Exists(target) {
		if err := os.Remove(target); err != nil {
			return err
		}
	}
	return os.Rename(backup, target)
}
