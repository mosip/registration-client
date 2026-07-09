// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package jre

import (
	"os"
	"path/filepath"
	"testing"
)

func TestDetectMajor(t *testing.T) {
	cases := []struct {
		name    string
		release string
		want    int
	}{
		{"java11", `JAVA_VERSION="11.0.20"`, 11},
		{"java21", `JAVA_VERSION="21.0.1"`, 21},
		{"legacy8", `JAVA_VERSION="1.8.0_392"`, 8},
		// A UTF-8 BOM on the first line must not defeat detection (regression:
		// release files re-saved by PowerShell/editors gain a leading U+FEFF,
		// which previously degraded detection to 0).
		{"bom", "\uFEFF" + `JAVA_VERSION="11.0.20"`, 11},
		{"noVersionLine", "IMPLEMENTOR=\"Eclipse\"\n", 0},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			dir := t.TempDir()
			if err := os.WriteFile(filepath.Join(dir, "release"), []byte(tc.release+"\n"), 0o644); err != nil {
				t.Fatalf("write release: %v", err)
			}
			if got := DetectMajor(dir); got != tc.want {
				t.Errorf("DetectMajor = %d, want %d", got, tc.want)
			}
		})
	}
}

func TestDetectMajor_MissingReleaseFile(t *testing.T) {
	if got := DetectMajor(t.TempDir()); got != 0 {
		t.Errorf("DetectMajor with no release file = %d, want 0", got)
	}
}
