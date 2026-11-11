@echo off
REM Creates users and this machine when needed, waits only if one of them was just created,
REM then runs the initial launch and the full suite.
REM Run from this folder. Extra arguments are passed to runInitialLaunch.bat and run.bat
REM (for example runAll.bat -Dheadless=true).
cd /d "%~dp0"

echo === Environment setup ===
call runSetup.bat
if errorlevel 1 (
    echo Environment setup failed. Initial launch was not started.
    exit /b 1
)

findstr /c:"SETUP_NEW_RESOURCES=true" setup.log >nul
if errorlevel 1 goto initial

set WAIT_MINUTES=15
if exist config.properties (
    for /f "usebackq eol=# tokens=1* delims==:" %%a in ("config.properties") do if /i "%%a"=="setup.propagationWaitMinutes" call :setwait "%%b"
)
set WAIT_SECONDS=900
set /a WAIT_SECONDS=%WAIT_MINUTES%*60
if errorlevel 1 set WAIT_SECONDS=900
if %WAIT_SECONDS% LEQ 0 goto initial

echo New user or machine was created. Waiting %WAIT_MINUTES% minutes before initial launch.
timeout /t %WAIT_SECONDS% /nobreak
if errorlevel 1 exit /b 1

:initial
echo === Initial launch ===
call runInitialLaunch.bat %*
set LAUNCH_EXIT=%ERRORLEVEL%
if exist startup.log copy /y startup.log startup-initial.log >nul
if not "%LAUNCH_EXIT%"=="0" (
    echo Initial launch failed. Full suite was not started. See startup-initial.log
    exit /b %LAUNCH_EXIT%
)

echo === Full suite ===
call run.bat %*
exit /b %ERRORLEVEL%

:setwait
set "VAL=%~1"
set "VAL=%VAL: =%"
if not "%VAL%"=="" set WAIT_MINUTES=%VAL%
goto :eof
