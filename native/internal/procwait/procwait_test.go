// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package procwait

import "testing"

func TestParseWaitPID(t *testing.T) {
	cases := []struct {
		name   string
		args   []string
		want   int
		wantOK bool
	}{
		{"flag with pid", []string{"--wait-pid", "1234"}, 1234, true},
		{"flag among other args", []string{"-x", "--wait-pid", "42", "y"}, 42, true},
		{"no args", nil, 0, false},
		{"flag without value", []string{"--wait-pid"}, 0, false},
		{"non-numeric pid", []string{"--wait-pid", "abc"}, 0, false},
		{"zero pid", []string{"--wait-pid", "0"}, 0, false},
		{"negative pid", []string{"--wait-pid", "-5"}, 0, false},
		{"largest 32-bit pid", []string{"--wait-pid", "4294967295"}, 4294967295, true},
		{"pid wider than 32 bits", []string{"--wait-pid", "4294967296"}, 0, false},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			got, ok := ParseWaitPID(c.args)
			if got != c.want || ok != c.wantOK {
				t.Fatalf("ParseWaitPID(%q) = %d, %v; want %d, %v", c.args, got, ok, c.want, c.wantOK)
			}
		})
	}
}
