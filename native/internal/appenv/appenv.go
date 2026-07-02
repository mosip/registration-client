// Package appenv resolves the Registration Client application root directory.
//
// The migration/rollback executables live in the app root (next to run.bat,
// jre/, lib/, ./MANIFEST.MF). They may be launched by run.bat, by migration.exe,
// or manually by an operator, so the working directory is not reliable. We resolve
// the app root from the executable's own location instead.
package appenv

import (
	"os"
	"path/filepath"
)

// AppRoot returns the directory containing the running executable, with symlinks
// resolved. This is the Registration Client application root.
func AppRoot() (string, error) {
	exe, err := os.Executable()
	if err != nil {
		return "", err
	}
	if resolved, err := filepath.EvalSymlinks(exe); err == nil {
		exe = resolved
	}
	return filepath.Dir(exe), nil
}
