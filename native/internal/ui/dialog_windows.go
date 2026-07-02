//go:build windows

// Package ui shows the operator dialogs required by the migration/rollback flow.
// On Windows it calls user32!MessageBoxW directly via syscall, so the binary has
// no GUI-toolkit dependency and can be built with the windowsgui subsystem.
package ui

import (
	"syscall"
	"unsafe"
)

var (
	user32          = syscall.NewLazyDLL("user32.dll")
	procMessageBoxW = user32.NewProc("MessageBoxW")
)

const (
	mbOK              = 0x00000000
	mbIconError       = 0x00000010
	mbIconInformation = 0x00000040
)

// ShowInfo displays an informational, OK-only modal dialog.
func ShowInfo(title, message string) { messageBox(title, message, mbIconInformation) }

// ShowError displays an error, OK-only modal dialog.
func ShowError(title, message string) { messageBox(title, message, mbIconError) }

func messageBox(title, message string, icon uintptr) {
	titlePtr, _ := syscall.UTF16PtrFromString(title)
	msgPtr, _ := syscall.UTF16PtrFromString(message)
	// MessageBoxW always sets a non-nil syscall error ("operation completed
	// successfully"); the return values are not meaningful for an OK-only dialog.
	_, _, _ = procMessageBoxW.Call(
		0,
		uintptr(unsafe.Pointer(msgPtr)),
		uintptr(unsafe.Pointer(titlePtr)),
		mbOK|icon,
	)
}
