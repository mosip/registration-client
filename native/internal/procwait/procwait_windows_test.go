// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//go:build windows

package procwait

import (
	"os/exec"
	"testing"
	"time"
)

// startPing starts a child that runs for roughly n-1 seconds.
func startPing(t *testing.T, n string) *exec.Cmd {
	t.Helper()
	cmd := exec.Command("ping", "-n", n, "127.0.0.1")
	if err := cmd.Start(); err != nil {
		t.Fatalf("start child: %v", err)
	}
	return cmd
}

func TestWaitForExitReturnsOnceProcessExits(t *testing.T) {
	cmd := startPing(t, "2")
	defer func() { _ = cmd.Wait() }()

	start := time.Now()
	if err := WaitForExit(cmd.Process.Pid, 30*time.Second); err != nil {
		t.Fatalf("WaitForExit: %v", err)
	}
	if time.Since(start) < 500*time.Millisecond {
		t.Fatalf("returned after %s, before the child could have exited", time.Since(start))
	}
}

func TestWaitForExitTimesOutWhileProcessRuns(t *testing.T) {
	cmd := startPing(t, "30")
	defer func() { _ = cmd.Process.Kill(); _ = cmd.Wait() }()

	if err := WaitForExit(cmd.Process.Pid, 200*time.Millisecond); err == nil {
		t.Fatal("expected a timeout error while the child is still running")
	}
}

func TestWaitForExitTreatsGoneProcessAsExited(t *testing.T) {
	cmd := exec.Command("cmd", "/c", "exit", "0")
	if err := cmd.Run(); err != nil {
		t.Fatalf("run child: %v", err)
	}
	if err := WaitForExit(cmd.Process.Pid, time.Second); err != nil {
		t.Fatalf("WaitForExit on an exited process: %v", err)
	}
}
