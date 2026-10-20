// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//go:build windows

package procwait

import (
	"errors"
	"fmt"
	"syscall"
	"time"
)

// errorInvalidParameter is ERROR_INVALID_PARAMETER, which package syscall does not define.
const errorInvalidParameter = syscall.Errno(87)

// WaitForExit blocks until the process with the given PID has terminated, or
// timeout elapses. A PID that no longer exists counts as already exited.
func WaitForExit(pid int, timeout time.Duration) error {
	h, err := syscall.OpenProcess(syscall.SYNCHRONIZE, false, uint32(pid))
	if err != nil {
		// OpenProcess rejects the PID of a process that has already gone.
		if errors.Is(err, errorInvalidParameter) {
			return nil
		}
		return fmt.Errorf("open process %d: %w", pid, err)
	}
	defer func() { _ = syscall.CloseHandle(h) }()

	event, err := syscall.WaitForSingleObject(h, uint32(timeout.Milliseconds()))
	switch event {
	case syscall.WAIT_OBJECT_0:
		return nil
	case syscall.WAIT_TIMEOUT:
		return fmt.Errorf("process %d still running after %s", pid, timeout)
	default:
		return fmt.Errorf("wait for process %d: %v", pid, err)
	}
}
