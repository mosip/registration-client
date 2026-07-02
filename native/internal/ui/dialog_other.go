//go:build !windows

// Non-Windows stub so the module builds and tests on developer machines
// (Linux/macOS). The shipped artifact is always the windows build.
package ui

import "fmt"

// ShowInfo prints the dialog to stdout on non-Windows platforms.
func ShowInfo(title, message string) { fmt.Printf("[INFO]  %s :: %s\n", title, message) }

// ShowError prints the dialog to stderr-style stdout on non-Windows platforms.
func ShowError(title, message string) { fmt.Printf("[ERROR] %s :: %s\n", title, message) }
