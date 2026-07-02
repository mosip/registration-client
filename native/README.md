# Native migration tooling (Go) — `migration.exe` / `rollback.exe`

Native Windows executables for the Java 11 → 21 JRE migration (the upgrade-revamp
feature). They perform the JRE swap **outside the JVM**, because the swap
deletes/replaces the very `jre/` a running JVM would hold open (Windows locks
in-use files). `_launcher.jar` starts them and exits the JVM first.

| Output | Role | Status |
|--------|------|--------|
| `rollback.exe`  | Restore the pre-migration state after a failed/aborted migration | **Logic complete** (`cmd/rollback`, 4 tests) |
| `migration.exe` | Perform the Java 11 → 21 JRE swap | **Logic complete** (`cmd/migration`, 6 tests; auto-invokes `rollback.exe` on failure) |

## Why Go (this scaffold)
- Single **static `.exe`, no runtime dependency** (the hard requirement — these run when the JRE is mid-swap/absent).
- File ops are stdlib (`os`, `path/filepath`); the operator dialog is a one-line `user32!MessageBoxW` syscall — no GUI toolkit.
- Cross-compiles to Windows from any OS; tiny binary.
- (Comparison candidate vs GraalVM `native-image`, which keeps the language Java but adds the GraalVM + MSVC toolchain. Same logic either way.)

## Layout
Two thin `cmd/` entrypoints build the two exes; **both migration and rollback
logic live together in a single file**, `internal/upgrade/upgrade.go`.
```
native/
  go.mod
  cmd/
    rollback/        rollback.exe  - thin main(): dialog/exit/log -> upgrade.Rollback
      main.go
    migration/       migration.exe - thin main(): dialog/exit/log + invokeRollback -> upgrade.Migrate
      main.go
  internal/
    upgrade/         Migrate(base) + Rollback(base) - BOTH flows in upgrade.go
      upgrade.go
      migrate_test.go / rollback_test.go / helpers_test.go   (6 + 4 tests)
    appenv/          resolve app root from the exe location (not CWD)
    fsops/           idempotent move/remove/clean/restore (+ CopyFile, Unzip)
    jre/             detect JRE feature version from <jre>/release
    ui/              MessageBoxW (windows) + stdout stub (dev)
```

## `migration.exe` behaviour (design step 4)
Idempotent/resumable. Against the app root:
1. extract `.artifacts/jre21.zip → jre21_temp/` (only if missing — done first so a failed extract never destroys `jre/`)
2. back up `jre/ → jre11/` (once, only when on JRE 11)
3. promote `jre21_temp/ → jre/` (delete old `jre/`, rename)
4. reset `lib/` to just `_launcher.jar` (new jars arrive from `.TEMP/` via `run.bat` on restart)
5. restore `run.bat` from `.artifacts/`
6. success dialog — **on any failure, auto-invoke `rollback.exe` and exit non-zero** (design Scenarios 1 & 5)

## `rollback.exe` behaviour (design step 4a)
Idempotent, safe to re-run. Against the app root:
1. `jre11/ → jre/` (skip if no backup — never deletes `jre/` without a replacement)
2. remove `jre21_temp/`
3. `run.bat_jre11 → run.bat` (skip if no backup)
4. empty `.TEMP/`, remove `.artifacts/` (re-downloaded on retry — design P8 / N12 / Scenario 5)
5. completion dialog
6. **never touches `./MANIFEST.MF`** (preserves the version mismatch for retry) — asserted via a before/after fingerprint

## Build + test (one step)
```powershell
./build.ps1            # go vet + go test, then build both .exe into ./dist
./build.ps1 -TestOnly  # vet + test only
```

Or by hand:
```bash
go vet ./...
go test ./...          # runs on any OS (ui has a non-windows stub)

# build ON or FOR Windows; -H=windowsgui = no console window
go build -ldflags "-H=windowsgui" -o dist/rollback.exe  ./cmd/rollback
go build -ldflags "-H=windowsgui" -o dist/migration.exe ./cmd/migration

# cross-compile from Linux/macOS:
GOOS=windows GOARCH=amd64 go build -ldflags "-H=windowsgui" -o dist/rollback.exe ./cmd/rollback
```

