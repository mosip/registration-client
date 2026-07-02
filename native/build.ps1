# Build + verify the T3/T4 native upgrade executables (migration.exe / rollback.exe).
#
#   ./build.ps1            # vet + test, then build both .exe into ./dist
#   ./build.ps1 -TestOnly  # vet + test only (no .exe produced)
#
# Requires the Go toolchain on PATH (https://go.dev/dl/ or: winget install GoLang.Go).
# The .exe still needs Authenticode signing before release (see README.md).
[CmdletBinding()]
param(
    [switch]$TestOnly
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

if (-not (Get-Command go -ErrorAction SilentlyContinue)) {
    throw "Go toolchain not found on PATH. Install it (winget install GoLang.Go) and re-run."
}
Write-Host ("Using {0}" -f (go version))

Write-Host "`n== go vet ==" -ForegroundColor Cyan
go vet ./...
if ($LASTEXITCODE -ne 0) { throw "go vet failed" }

Write-Host "`n== go test ==" -ForegroundColor Cyan
go test ./...
if ($LASTEXITCODE -ne 0) { throw "go test failed" }

if ($TestOnly) {
    Write-Host "`nTests passed (TestOnly: no binaries built)." -ForegroundColor Green
    return
}

$dist = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force -Path $dist | Out-Null

# -H=windowsgui => no console window pops up when run.bat / _launcher.jar launches it.
$ldflags = '-H=windowsgui'
$env:GOOS = 'windows'
$env:GOARCH = 'amd64'

foreach ($cmd in @('rollback', 'migration')) {
    $out = Join-Path $dist "$cmd.exe"
    Write-Host ("`n== build {0}.exe ==" -f $cmd) -ForegroundColor Cyan
    go build -ldflags $ldflags -o $out "./cmd/$cmd"
    if ($LASTEXITCODE -ne 0) { throw "build $cmd failed" }
    Write-Host ("  -> {0}" -f $out)
}

Write-Host "`nDone. Sign the binaries in ./dist before release (see README.md)." -ForegroundColor Green
