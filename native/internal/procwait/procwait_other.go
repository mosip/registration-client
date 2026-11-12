// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//go:build !windows

package procwait

import "time"

// WaitForExit is a no-op off Windows: the executables only ship for Windows,
// and only Windows refuses to rename files a running process has open.
func WaitForExit(pid int, timeout time.Duration) error {
	return nil
}
