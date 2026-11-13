@echo off
REM Creates users and this machine when needed, waits only if one of them was just created,
REM then runs the initial launch and the full suite.
REM Run from this folder. Extra arguments are passed to the client
REM (for example run.bat -Dheadless=true).
cd /d "%~dp0"

set HEADLESS_ARGS=
set HEADLESS_DEFINE=
set HEADLESS=
set HEADLESS_FROM_ARGS=

echo %* | findstr /i /c:"headless=true" >nul && set HEADLESS_FROM_ARGS=true
echo %* | findstr /i /c:"headless=false" >nul && set HEADLESS_FROM_ARGS=false

if "%HEADLESS_FROM_ARGS%"=="true" set HEADLESS=true
if "%HEADLESS_FROM_ARGS%"=="false" goto configured
if "%HEADLESS%"=="true" goto apply

if exist config.properties (
    for /f "usebackq eol=# tokens=1* delims==:" %%a in ("config.properties") do if /i "%%a"=="headless" call :checkvalue "%%b"
)

:apply
if "%HEADLESS%"=="true" (
    for %%f in (lib\openjfx-monocle-21*.jar testlib\openjfx-monocle-21*.jar) do set HEADLESS_ARGS=--patch-module javafx.graphics=%%f
    if not "%HEADLESS_FROM_ARGS%"=="true" set HEADLESS_DEFINE=-Dheadless=true
)

:configured
echo === Environment setup ===
jre\bin\java -Dpath.config=/config.properties -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED -cp "registration-test.jar;lib/*;testlib/*" registrationtest.runapplication.EnvironmentSetup > setup.log 2>&1
set SETUP_EXIT=%ERRORLEVEL%
type setup.log
if not "%SETUP_EXIT%"=="0" (
    echo Environment setup failed. Initial launch was not started.
    exit /b %SETUP_EXIT%
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
jre\bin\java %HEADLESS_DEFINE% %* %HEADLESS_ARGS% -Dpath.config=/config.properties -Dmodule.run=initiallaunch -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -cp "registration-test.jar;lib/*;testlib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1
set LAUNCH_EXIT=%ERRORLEVEL%
if exist startup.log copy /y startup.log startup-initial.log >nul
if not "%LAUNCH_EXIT%"=="0" (
    echo Initial launch failed. Full suite was not started. See startup-initial.log
    exit /b %LAUNCH_EXIT%
)

echo === Full suite ===
jre\bin\java %HEADLESS_DEFINE% %* %HEADLESS_ARGS% -Dpath.config=/config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -cp "registration-test.jar;lib/*;testlib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1
exit /b %ERRORLEVEL%

:checkvalue
set "VAL=%~1"
set "VAL=%VAL: =%"
if /i "%VAL%"=="Y" set HEADLESS=true
if /i "%VAL%"=="YES" set HEADLESS=true
if /i "%VAL%"=="TRUE" set HEADLESS=true
goto :eof

:setwait
set "VAL=%~1"
set "VAL=%VAL: =%"
if not "%VAL%"=="" set WAIT_MINUTES=%VAL%
goto :eof
