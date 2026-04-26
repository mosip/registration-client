# MOSIP Registration Client: Revamp of full & patch upgrades including Java 11 to Java 21 Migration approach

## Problem Statement

The MOSIP Registration Client is currently running on Java Runtime Environment (JRE) 11 and must migrate to JRE 21 while operating under significant constraints:

1. **Immutable Launcher:** The existing `run.bat` file cannot be modified by the application and is hardcoded to use `jre\bin\javaw` with `-cp lib/*`.

2. **Limited Deployment Capability:** The client can only download and place files in the `lib/` (during full upgrade) or `.TEMP/` (during partial/patch upgrade) directories. It cannot modify files outside these directories, including batch files or the JRE folder itself.

3. **Operational Environment:** The application must continue functioning during the migration process, and operators need a clear upgrade path without manual intervention for JRE swapping.

4. **Integrity & Security:** The migration must maintain file integrity checks and ensure secure verification of new artifacts before execution.

5. **Zero-Downtime Requirement:** The migration should be transparent to the operator with minimal disruption. Failed migrations must be rollback-able immediately or at any point thereafter.

**Core Challenge:** How can an application running on Java 11 orchestrate a complete JRE swap to Java 21 when it cannot modify `run.bat` and is confined to lib/.TEMP directories?

### How the update works in our previous versions (<1.3.0)

1. Client detects version change - Polls maven_metadata.xml on the upgrade server and compares version numbers
2. Backup existing state - Backs up bin/, lib/, db/ and ./MANIFEST.MF into the configured backup folder
3. Download new files - Fetches server manifest, replaces local manifest, downloads all new/changed files into lib/ (On full upgrade) or .TEMP/ (on patch upgrades)
4. On failure → During full upgrade, automatic rollback. Existing logic restores from backup automatically if any step fails
5. Prompt operator to restart - run.bat picks up new JARs from lib/ on next launch

**The Constraint — Why Normal Update Cannot Do This Migration**
Switching from JRE 11 to JRE 21 requires changing run.bat to point to a new JRE folder. But the update mechanism can only write files to lib/ and.TEMP/ — it cannot touch run.bat, run.exe, or anything in the app root directly. The migration requires a different approach.

---

## Proposed Solution

### Changes to Existing `softwareUpdateHandler`

The existing `softwareUpdateHandler` detects a version mismatch by comparing the local `./MANIFEST.MF` version against the server's `maven-metadata.xml`. From 1.3.0 onwards, its behaviour changes as follows:

| Step | Existing behaviour (<1.3.0) | New behaviour (1.3.0+) |
|------|-----------------------------|------------------------|
| Version check | Compare local `./MANIFEST.MF` vs `maven-metadata.xml` | Compare local `./MANIFEST.MF` vs `maven-metadata.xml` |
| Manifest download | Download server manifest → replace `./MANIFEST.MF` | Download server manifest → replace `./MANIFEST.MF` and lib.zip brings in `lib/MANIFEST.MF` |
| Artifact download | Download each manifest entry → `lib/` (blocking, foreground) | Download each entry in `./MANIFEST.MF` → `.artifacts/` (resumable, background), Download each entry in `lib/MANIFEST.MF` → `.TEMP/` (resumable, background) |
| Prompt | Prompt operator to restart after all downloads complete | Prompt operator to restart after all downloads complete |
| On restart | `run.bat` picks up new JARs from `lib/` or from `.TEMP/` | `_launcher.jar` takes over — detects version diff, determines JRE path, orchestrates migration or lib update |

**Key invariant:** `softwareUpdateHandler` only downloads artifacts and updates `./MANIFEST.MF`. It never unzips, never copies to `lib/`, and never touches `run.bat`. All decisions about what to do with downloaded artifacts are delegated to `_launcher.jar` on the next restart.

---

### Architecture Overview

The solution employs a **two-phase launcher strategy** with manifest-driven orchestration:

#### Phase 1: Download & Prepare (softwareUpdateHandler, running on Java 11)
- `softwareUpdateHandler` detects version mismatch between `./MANIFEST.MF` and `maven-metadata.xml` on the upgrade server.
- Downloads new `./MANIFEST.MF` from server → replaces existing `./MANIFEST.MF`.
- In version `< 1.3.0` → Downloads each entry in the new `./MANIFEST.MF` → `lib/`
- In version >= 1.3.0 -> Downloads each entry in the new `./MANIFEST.MF` → `.artifacts/` (resumable, background)
  - `jre21.zip` (Compressed Java 21 JRE)
  - `migration.exe` (JRE swap logic, runs as a separate process)
  - `rollback.exe` (recovery logic, runs as a separate process)
  - `run.bat` (Java 21 compatible launcher script)
  - `_launcher.jar` (compiled Java 21, target 11 — contains new `Initialization` entry point)
- Once all downloads complete, prompts operator to restart the application.
- Note: `lib.zip` is **not** in `./MANIFEST.MF` — it is downloaded on-demand by `_launcher.jar` after restart when a version diff is detected.

#### Phase 2: JRE Swap & Launch (Coordinated Transition)
1. `run.bat` launches application, JVM loads `_launcher.jar` first (guaranteed by alphabetical sorting: `_launcher.jar` = ASCII 95 < 'a' = ASCII 97). This ordering guarantee is critical only for the one-time `<1.3.0 → 1.3.0` transition, where the old `registration-client.jar` still contains an `Initialization` class. From 1.3.0 onwards, `registration-client.jar` has no `main()` entry point, so `_launcher.jar` is unambiguously the sole entry point regardless of classpath order.

2. `Initialization` class in `_launcher.jar` is the entry point. On every startup:
   - **Verify external `./MANIFEST.MF` signature:**
     - If `./MANIFEST.MF.sig` is missing → download from upgrade server, then verify.
     - If signature is invalid → show error dialog, exit JVM (Case B).
   - **Compare version in `./MANIFEST.MF` vs `lib/MANIFEST.MF`:**
     - If versions match → nothing to migrate, proceed to normal startup (step 6).
     - If versions differ → determine migration path based on current JRE version (detected via `System.getProperty("java.version")`; since `run.bat` hardcodes `jre\bin\javaw`, this always reflects the JRE in the app root).

3. **If versions differ and JRE is 11 (JRE migration path):**
   - Ensure migration artifacts are in `.artifacts/`:
     - **`<1.3.0 → 1.3.0` transition only:** `softwareUpdateHandler` (old behaviour) downloaded artifacts into `lib/`. Copy `_launcher.jar`, `jre21.zip`, `migration.exe`, `rollback.exe`, `run.bat` from `lib/` → `.artifacts/`. This protects them from subsequent `lib/` cleanup.
     - **1.3.0+ onwards:** artifacts already downloaded directly to `.artifacts/` by `softwareUpdateHandler`. No copy needed.
   - Download `lib.zip` from upgrade server → `.artifacts/lib.zip` (resumable download).
   - Unzip `.artifacts/lib.zip` → `.TEMP/` (contains new JARs, `logback.xml`, updated `lib/MANIFEST.MF`).
   - If `jre21_temp/` does not exist → unzip `.artifacts/jre21.zip` → `jre21_temp/`.
   - If `migration.exe` is not in app root → copy from `.artifacts/`.
   - If `rollback.exe` is not in app root → copy from `.artifacts/`.
   - If `run.bat_jre11` backup does not exist → backup current `run.bat` → `run.bat_jre11`.
   - Start `migration.exe` and exit JVM.

4. **`migration.exe` (running as a separate process — idempotent, resumable):**
   - If JRE version is 11 and `jre11/` backup does not exist → backup `jre/` → `jre11/`.
   - If `jre21_temp/` exists → delete `jre/`, rename `jre21_temp/` → `jre/`.
   - If `jre21_temp/` does not exist → unzip `.artifacts/jre21.zip` → `jre21_temp/`, then delete `jre/`, rename `jre21_temp/` → `jre/`.
   - Delete all files under `lib/`.
   - Copy `_launcher.jar` from `.artifacts/` → `lib/`.
   - Copy `run.bat` from `.artifacts/` → application root.
   - Show dialog: *"JRE migration complete. Please start the application using run.bat."* Exit on OK.
   - On next operator-initiated start, `run.bat` copies `.TEMP/*` → `lib/` (existing run.bat behaviour), then launches the JVM. `_launcher.jar` detects JRE 21, versions now match, proceeds to normal startup.

4a. **`rollback.exe` (idempotent — safe to run multiple times):**

   **When it is invoked:**
   | Trigger | Invoked By | Condition |
   |---------|-----------|-----------|
   | `migration.exe` fails mid-execution | `migration.exe` itself | `jre11/` backup exists AND `jre/` is missing or partially populated |
   | `jre21_temp/` unzip incomplete | `migration.exe` | `jre21_temp/` exists but JRE rename cannot complete |
   | Disk space exhaustion during `migration.exe` | `migration.exe` | Any file operation throws an out-of-space exception |
   | Inconsistent JRE state on startup | `_launcher.jar` (step 3) | `System.getProperty("java.version")` returns an unknown/unexpected version |
   | Operator force-kills during `migration.exe` and JRE is unrecognisable on next restart | `_launcher.jar` (step 3) | JRE version is neither 11 nor 21 |
   | Operator manually runs `rollback.exe` | Operator | Any time post-migration failure; remains available until step 6 cleanup |

   **What it does:**
   - If `jre11/` exists → delete `jre/` (if present), rename `jre11/` → `jre/`.
   - If `jre21_temp/` exists → delete it.
   - If `run.bat_jre11` exists → delete current `run.bat`, rename `run.bat_jre11` → `run.bat`.
   - Delete `.TEMP/` contents.
   - Show dialog: *"Rollback complete. Application restored to previous state. Please start using run.bat."* Exit on OK.

   **What it does NOT do:**
   - Does not touch `./MANIFEST.MF` — the version mismatch will remain, so `softwareUpdateHandler` will re-download artifacts on the next version check and the operator can retry migration later.
   - Does not attempt to re-download anything.

5. **If versions differ and JRE is 21 or greater (For future upgrades):**
   - Download new `lib/MANIFEST.MF` → `.TEMP/MANIFEST.MF` and `lib/MANIFEST.MF.sig` → `.TEMP/MANIFEST.MF.sig`.
   - Verify `.TEMP/MANIFEST.MF` signature. If invalid → show error dialog, exit JVM (Case B).
   - Start resumable download of `lib.zip` → `.TEMP/lib.zip`. Verify hash against entry in `.TEMP/MANIFEST.MF`.
   - Unzip `.TEMP/lib.zip` → `.TEMP/` (contains new JARs, `logback.xml`, updated `lib/MANIFEST.MF`).
   - Show dialog: *"Update ready. Please restart the application."* Exit JVM on OK.
   - On next operator-initiated start, `run.bat` copies `.TEMP/*` → `lib/`, then launches the JVM. `_launcher.jar` detects JRE 21, versions now match, proceeds to normal startup.

6. **If versions match (normal startup):**
   - Clean up migration artefacts if present (indicates a completed prior migration):
     - Remove `jre21_temp/` if exists.
     - Remove `run.bat_jre11` if exists.
     - Remove `migration.exe` if exists.
     - Remove `rollback.exe` if exists.
     - Remove `.artifacts/` if exists.
   - Proceed to normal startup (`LauncherImpl.launchApplication`). `ClientPreLoader` handles per-file hash verification against `lib/MANIFEST.MF`.

#### Post-Migration (Java 21 Context)
- `Initialization` class in `_launcher.jar` executes standard application startup. The `main()` entry point is removed from `registration-client.jar` in 1.3.0 and all subsequent versions — `_launcher.jar` is the sole entry point.
- **Java bytecode compatibility constraint:** The JRE 11 branch in `_launcher.jar` must never reference or load any class from `registration-client.jar` or other Java 21 JARs. All migration logic must rely only on JDK classes and `_launcher.jar`-internal classes. `LauncherImpl.launchApplication()` is only invoked in the normal startup path (step 6), where the JVM is Java 21 and can safely load Java 21 bytecode. Accidentally importing a Java 21 class into the migration code path will cause `UnsupportedClassVersionError` on Java 11.
- `./MANIFEST.MF` drives all upgrades from 1.3.0 onwards. It does **not** contain a `lib.zip` entry — `lib.zip` is always downloaded on-demand when a version difference is detected.
- `lib/MANIFEST.MF` drives `ClientPreLoader`'s per-file integrity checks and patch upgrade tracking.
- All server downloads go to `.TEMP/` (JRE 21 path) or `.artifacts/` (JRE 11 path). `_launcher.jar` is the sole decision point for file placement. Nothing is downloaded directly to `lib/` from 1.3.0 onwards.
- All downloads must use resumable file download. Operator is prompted to restart after downloads complete — no auto-restart.

### Dual MANIFEST.MF Strategy

| Aspect | MANIFEST.MF (lib/) | MANIFEST.MF (root/) |
|--------|-------|---------|
| **Purpose** | Per-file integrity tracking for lib/ | Version tracking & upgrade orchestration |
| **Update Trigger** | Delivered inside `lib.zip`, applied when `run.bat` copies `.TEMP/ → lib/` | Downloaded by `_launcher.jar` on each startup |
| **Used By** | `ClientPreLoader` (hash verification), `_launcher.jar` (version comparison) | `_launcher.jar` (signature check, version comparison, upgrade trigger) |
| **Does NOT contain** | `lib.zip` entry | Per-file hashes of JARs |
| **Initial Creation** | Inside `lib.zip`, first applied during 1.3.0 migration | First time during 1.3.0, written to root if migration succeeds |

### Signature Verification Flow (Applies for both MANIFEST.MF (lib/) and MANIFEST.MF (root/))

Server MANIFEST.MF download 
         ↓ 
Download MANIFEST.MF.sig from server
         ↓
Verify signature using public key in _launcher.jar (1.3.0)
         ↓
   [Signature Valid?] 
         ├─ YES → Proceed to checksum verification of entries
         └─ NO → Log security alert, abort, display operator alert

**Case A: lib/MANIFEST.MF.sig valid, but any of the file hash fails**
This means the MANIFEST.MF is trustworthy (server is not compromised) but the specific file(s) was tampered locally. Recovery is safe:

- Alert operator: `"<filename> integrity check failed. Restoring from server..."`
- Re-download `<filename>` from upgrade server → `.TEMP/<filename>`
- Verify hash of `.TEMP/<filename>` against `MANIFEST.MF`
- **For `lib/` files:** Inform operator: `"Integrity restored. Please exit & restart the application manually."` On restart, `run.bat` copies `.TEMP/<filename>` → `lib/<filename>`.
- **For root-level files (`run.bat`, `logback.xml`):** `_launcher.jar` copies `.TEMP/<filename>` directly to the app root. Inform operator: `"Integrity restored. Please exit & restart the application manually."`
- Exit JVM on OK.

**Case B: MANIFEST.MF.sig itself is invalid**
This suggests server compromise or a MITM attack. Re-downloading anything is unsafe:

Alert operator: "Security alert: MANIFEST.MF signature invalid. Startup aborted."
Do not re-download anything
Exit JVM

**Case C: MANIFEST.MF.sig is missing**
Download MANIFEST.MF.sig from upgrade server and check the integrity. If the integrity fails, continues with `Case B` steps.

**Case D: ./MANIFEST.MF.sig valid, but any of the file hash fails**
This means the MANIFEST.MF is trustworthy (server is not compromised) but the specific file(s) was tampered locally. Recovery is safe:

_launcher.jar has the list of migration files on which checksum can be ignored.
For other files if hash validation fails, its redownloaded and placed in the application root directory.
Inform operator: "Integrity restored. Please exit & restart the application manually."
Exit JVM

---

## Edge Cases & Scenarios

### Scenario 1: Partial Migration Failure During migration.exe
**Context:** `migration.exe` is executing but fails mid-way — e.g., `jre/` has been deleted but `jre21_temp/` rename failed, or a file copy error occurred.

**Handling:**
- `migration.exe` detects the failure condition: `jre11/` backup exists AND `jre/` is missing or not yet the new JRE.
- `migration.exe` auto-invokes `rollback.exe`.
- `rollback.exe` restores application to previous state (see step 4a for full details).
- Operator is shown rollback completion dialog and starts `run.bat` manually to return to the pre-migration state.

### Scenario 2: Migration Succeeds, But Operator Has Not Restarted Yet
**Context:** migration.exe completes successfully (`jre/` is now Java 21, `lib/` has only `_launcher.jar`, `.TEMP/` has new JARs), but operator hasn't started the application yet.

**Handling:**
- On next operator-initiated start via `run.bat`:
  - `run.bat` copies `.TEMP/*` → `lib/`
  - `_launcher.jar` detects JRE 21, versions match → normal startup
  - Step 6 cleanup runs (removes `jre21_temp/`, `run.bat_jre11`, `migration.exe`, `rollback.exe`, `.artifacts/`)

### Scenario 3: Operator Force-Kills During migration.exe Execution
**Context:** Operator kills the process while migration.exe is mid-execution. State may be inconsistent (e.g., `jre11/` backed up but `jre/` partially populated).

**Handling:**
- On next operator-initiated start:
  - `_launcher.jar` detects JRE version via `System.getProperty("java.version")`
  - If JRE is still 11 (rename of `jre21_temp/` → `jre/` did not complete):
    - `jre21_temp/` likely still exists → step 3 skips re-unzip, proceeds to copy exes and run `migration.exe` again (idempotent)
  - If JRE reports an inconsistent/unknown version:
    - Show error dialog: *"Critical error detected. Attempting recovery..."*
    - Auto-invoke `rollback.exe`
    - Show info dialog confirming recovery steps taken


### Scenario 4: Upgrading from 1.3.0 (already on Java 21) to 1.4.0
**Context:** Already on 1.3.0 (Java 21) — upgrading to 1.4.0.

**Handling:**
- `./MANIFEST.MF` version is 1.4.0, `lib/MANIFEST.MF` version is 1.3.0 → versions differ
- JRE is 21 → takes lib-only update path (step 5)
- Downloads `lib/MANIFEST.MF` + `.sig` → `.TEMP/`, verifies signature
- Starts resumable download of `lib.zip` → `.TEMP/lib.zip`
- Unzips `.TEMP/lib.zip` → `.TEMP/`
- Prompts operator to restart
- On restart: `run.bat` copies `.TEMP/*` → `lib/`, versions match, normal startup, `ClientPreLoader` verifies hashes

### Scenario 5: Disk Space Exhaustion During Unzip
**Context:** `jre21.zip` unzips to `jre21_temp/` but disk space runs out mid-way.

**Handling:**
- Unzip throws an exception in `_launcher.jar` (step 3 is where `jre21.zip` is unzipped, not `migration.exe`).
- `_launcher.jar` shows an error dialog: *"Insufficient disk space during JRE extraction. Please free up space and restart."* and exits JVM.
- On next restart, `_launcher.jar` detects JRE is still 11:
  - `jre21_temp/` exists but is partially populated — step 3 detects this, deletes the partial folder, and re-attempts unzip.
  - If disk space still insufficient → same error dialog, exit.
  - If unzip succeeds → continues with `migration.exe` as normal.
- If `migration.exe` was already running when disk exhaustion hit: `migration.exe` detects incomplete `jre21_temp/` and auto-invokes `rollback.exe`.
- `rollback.exe` restores `jre11/` → `jre/`, removes `jre21_temp/`, restores `run.bat_jre11` → `run.bat`, cleans `.TEMP/` and `.artifacts/`.

### Scenario 6: .UNKNOWN_JARS Collision with Migration Files
**Context:** An old .UNKNOWN_JARS file lists jre21.zip as unknown

**Handling:**
- ClientPreLoader processes .UNKNOWN_JARS entries before _launcher.jar Initialization
- Only files from old MANIFEST.MF (not in new 1.3.0 MANIFEST.MF) are marked unknown
- jre21.zip, _launcher.jar are NEW entries → not marked unknown
- registration-client.jar OLD entry → may be marked unknown if not in new manifest
- Worst-case: if registration-client.jar is deleted early by ClientPreLoader, the next restart simply goes straight to `_launcher.jar`.

---

## Tasks

### T1: `softwareUpdateHandler` Changes
- [ ] Change artifact download target from `lib/` to `.artifacts/` with resumable download support
- [ ] Implement background download with progress indication
- [ ] Prompt operator to restart only after all `.artifacts/` downloads complete

### T2: `_launcher.jar` — New Module
- [ ] Create new Maven module `registration-launcher` compiled with `--release 11` and `--target 11`
- [ ] Implement `Initialization` class with `main()` as entry point (remove `main()` from `registration-client.jar`)
- [ ] Implement `./MANIFEST.MF` signature verification using embedded public key
- [ ] Implement `./MANIFEST.MF` vs `lib/MANIFEST.MF` version comparison
- [ ] Implement JRE version detection via `System.getProperty("java.version")`
- [ ] Implement JRE 11 migration path (step 3): artifact copy/verify, `lib.zip` download, unzip, exe copy, `run.bat` backup, launch `migration.exe`
- [ ] Implement JRE 21 update path (step 5): `lib/MANIFEST.MF` download & verify, `lib.zip` resumable download, unzip to `.TEMP/`
- [ ] Implement step 6 cleanup logic (post-migration artefact removal)
- [ ] Implement `JOptionPane`-based operator dialogs for all error, info, and prompt states
- [ ] Implement `GraphicsEnvironment.isHeadless()` guard for headless environments
- [ ] Enforce: no imports from `registration-client.jar` or any Java 21 JAR in migration code paths
- [ ] Add unit tests for all branching logic (JRE version detection, version comparison, file state checks)

### T3: `migration.exe` — New Native Executable
- [ ] Implement idempotent JRE backup: `jre/` → `jre11/` (skip if already exists)
- [ ] Implement idempotent JRE promotion: `jre21_temp/` → `jre/` (unzip from `.artifacts/jre21.zip` if `jre21_temp/` missing)
- [ ] Implement `lib/` cleanup and `_launcher.jar` + `run.bat` restore from `.artifacts/`
- [ ] Implement failure detection and auto-invocation of `rollback.exe`
- [ ] Implement operator dialog on success: *"JRE migration complete. Please start the application using run.bat."*

### T4: `rollback.exe` — New Native Executable
- [ ] Implement idempotent JRE restore: `jre11/` → `jre/`
- [ ] Implement `jre21_temp/` cleanup
- [ ] Implement `run.bat` restore from `run.bat_jre11`
- [ ] Implement `.TEMP/` cleanup
- [ ] Implement operator dialog on completion
- [ ] Verify `./MANIFEST.MF` is untouched (version mismatch preserved for retry)

### T5: Server-Side / Build Pipeline
- [ ] Update `configure.sh` to sign `./MANIFEST.MF` and produce `./MANIFEST.MF.sig`
- [ ] Update `configure.sh` to sign `lib/MANIFEST.MF` (bundled inside `lib.zip`) and produce `lib/MANIFEST.MF.sig`
- [ ] Update upgrade server manifest structure: `./MANIFEST.MF` lists `jre21.zip`, `migration.exe`, `rollback.exe`, `run.bat`, `_launcher.jar` only — no `lib.zip`
- [ ] Bundle `lib/MANIFEST.MF` inside `lib.zip`
- [ ] Zip all files under `lib/**` as `lib.zip` and should be hosted from upgrade server.
- [ ] Build `migration.exe` and `rollback.exe` as part of the 1.3.0 and should be hosted from upgrade server.

### T6: `ClientPreLoader` Changes
- [ ] Remove `main()` entry point from `registration-client.jar`
- [ ] Verify hash verification loop still works correctly against `lib/MANIFEST.MF` post-migration

---

## Test Scenarios

### Positive Scenarios

| # | Scenario | Expected Outcome |
|---|----------|-----------------|
| P1 | Fresh `<1.3.0` client detects 1.3.0 on upgrade server | `softwareUpdateHandler` downloads artifacts to `.artifacts/`, prompts restart |
| P2 | Operator restarts after P1 | `_launcher.jar` detects JRE 11, downloads `lib.zip`, unzips to `.TEMP/`, launches `migration.exe` |
| P3 | `migration.exe` runs successfully end-to-end | `jre/` is Java 21, `lib/` has `_launcher.jar`, `.TEMP/` has new JARs, operator prompted to start |
| P4 | Operator starts application after P3 | `run.bat` copies `.TEMP/* → lib/`, `_launcher.jar` detects JRE 21, versions match, cleanup runs, `ClientPreLoader` starts normally |
| P5 | Client already on 1.3.0 (Java 21), upgrade server has 1.4.0 | `softwareUpdateHandler` downloads new `./MANIFEST.MF` and artifacts to `.artifacts/`, prompts restart |
| P6 | Operator restarts after P5 | `_launcher.jar` detects JRE 21, version diff triggers step 5, `lib.zip` downloaded and unzipped to `.TEMP/`, operator prompted to restart |
| P7 | Operator restarts after P6 | `run.bat` copies `.TEMP/* → lib/`, versions match, normal startup, `ClientPreLoader` verifies all hashes |
| P8 | Operator manually runs `rollback.exe` after successful migration but before first restart | JRE restored to Java 11, `run.bat` restored, `.TEMP/` and `.artifacts/` cleaned, version mismatch preserved for retry |
| P9 | `./MANIFEST.MF.sig` is missing on startup | `_launcher.jar` downloads `.sig` from server, verifies successfully, proceeds normally |
| P10 | `lib.zip` download interrupted mid-way, operator restarts | Resumable download resumes from last byte, completes successfully |

### Negative Scenarios

| # | Scenario | Expected Outcome |
|---|----------|-----------------|
| N1 | `./MANIFEST.MF` signature is invalid (Case B) | Error dialog shown, JVM exits, nothing re-downloaded |
| N2 | `lib/MANIFEST.MF` signature is invalid during step 5 (Case B) | Error dialog shown, JVM exits, no `lib.zip` download attempted |
| N3 | `run.bat` hash fails but `./MANIFEST.MF.sig` is valid (Case A) | `_launcher.jar` re-downloads `run.bat` to `.TEMP/`, verifies hash, copies to app root, prompts restart |
| N4 | `lib/` JAR hash fails on startup (Case A) | `_launcher.jar` re-downloads JAR to `.TEMP/`, informs operator to restart; `run.bat` copies to `lib/` on restart |
| N5 | `migration.exe` fails mid-execution (e.g. disk full during `jre/` rename) | `migration.exe` auto-invokes `rollback.exe`; JRE restored to Java 11; operator prompted |
| N6 | Operator force-kills process during `migration.exe`; JRE still reads as 11 on next start | `_launcher.jar` re-enters step 3, `jre21_temp/` still present so unzip skipped, `migration.exe` re-invoked (idempotent) |
| N7 | Operator force-kills process during `migration.exe`; JRE reads as unknown version on next start | `_launcher.jar` shows critical error dialog, auto-invokes `rollback.exe` |
| N8 | Disk space exhaustion during `jre21.zip` unzip in `_launcher.jar` | Error dialog shown, JVM exits; on next restart partial `jre21_temp/` deleted and unzip retried |
| N9 | Disk space exhaustion during `jre21.zip` unzip inside `migration.exe` | `migration.exe` auto-invokes `rollback.exe`; state restored |
| N10 | `.artifacts/` deleted by operator between download and restart | `_launcher.jar` detects missing artifacts in step 3, re-downloads `lib.zip` and re-copies migration exes from `lib/` (1.3.0 transition) or treats as fresh start |
| N11 | `registration-client.jar` marked in `.UNKNOWN_JARS` and deleted by `ClientPreLoader` before `_launcher.jar` runs | On next restart `_launcher.jar` is sole entry point, proceeds with migration as normal |
| N12 | `rollback.exe` invoked when `jre11/` backup does not exist | `rollback.exe` skips JRE restore step, still cleans `.TEMP/` and `.artifacts/`, restores `run.bat` if backup exists, shows completion dialog |
| N13 | Network lost during `lib.zip` resumable download | Download pauses; on next restart (or reconnect) resumable download continues from last byte |
| N14 | Upgrade server returns invalid signature for `./MANIFEST.MF.sig` (MITM scenario) | Signature verification fails (Case B), startup aborted, no files downloaded or overwritten |

