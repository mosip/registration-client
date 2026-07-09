// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

// Package jre detects the feature version of a JRE from its release file, used
// by migration.exe to decide whether to back up the Java 11 JRE.
package jre

import (
	"bufio"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

// DetectMajor reads <jreDir>/release and returns the Java feature version
// (e.g. 11, 21), or 0 if it cannot be determined.
func DetectMajor(jreDir string) int {
	f, err := os.Open(filepath.Join(jreDir, "release"))
	if err != nil {
		return 0
	}
	defer func() { _ = f.Close() }()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		// Tolerate a UTF-8 BOM on the first line: some editors/scripts re-save
		// the release file with a leading U+FEFF, which would otherwise defeat
		// the HasPrefix check and silently degrade detection to 0.
		line := strings.TrimPrefix(scanner.Text(), "\uFEFF")
		if strings.HasPrefix(line, "JAVA_VERSION=") {
			value := strings.Trim(strings.TrimPrefix(line, "JAVA_VERSION="), "\"")
			return parseMajor(value)
		}
	}
	return 0
}

// parseMajor maps a java.version string to its feature number:
// "11.0.20" -> 11, "21" -> 21, "1.8.0_392" -> 8.
func parseMajor(version string) int {
	version = strings.TrimSpace(version)
	if strings.HasPrefix(version, "1.") {
		parts := strings.Split(version, ".")
		if len(parts) >= 2 {
			if n, err := strconv.Atoi(parts[1]); err == nil {
				return n
			}
		}
		return 0
	}
	if idx := strings.IndexAny(version, ".+_-"); idx > 0 {
		version = version[:idx]
	}
	n, err := strconv.Atoi(version)
	if err != nil {
		return 0
	}
	return n
}
