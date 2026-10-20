// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// Package procwait lets migration.exe wait for the _launcher.jar JVM that
// started it to exit before the JRE swap.
//
// The launcher starts migration.exe and then exits, but until that JVM process
// has fully terminated Windows keeps jre/bin/*.dll and lib/_launcher.jar mapped,
// so renaming jre/ fails with "Access is denied". The launcher therefore passes
// its own PID as "--wait-pid <pid>" and migration.exe blocks on it first.
package procwait

import "strconv"

// WaitPIDFlag is the argument that carries the PID to wait for.
const WaitPIDFlag = "--wait-pid"

// ParseWaitPID returns the PID following WaitPIDFlag in args, and whether a
// valid (positive integer) one was given. A missing or malformed value yields
// false, so an operator starting the exe by hand is not blocked.
func ParseWaitPID(args []string) (int, bool) {
	for i := 0; i+1 < len(args); i++ {
		if args[i] != WaitPIDFlag {
			continue
		}
		// Parse as 32 bits: a Windows PID is a DWORD, and anything wider would
		// wrap when WaitForExit converts it, so it is rejected like a malformed one.
		pid, err := strconv.ParseUint(args[i+1], 10, 32)
		if err != nil || pid == 0 {
			return 0, false
		}
		return int(pid), true
	}
	return 0, false
}
