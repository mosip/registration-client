@echo off
REM Prepares the environment before running Registration Client automation:
REM creates the operator users in Keycloak, maps them to zone and center, and creates/activates this machine.
REM Run from the same folder as run.bat so the client's .mosipkeys keys are the ones registered.
REM Usage: runSetup.bat                  - full setup
REM        runSetup.bat machine-details  - only print this machine's name, IP, MAC, public key and sign public key

jre\bin\java -Dpath.config=/config.properties -Dfile.encoding=UTF-8 --add-opens java.base/java.lang=ALL-UNNAMED -cp "registration-test.jar;lib/*;testlib/*" registrationtest.runapplication.EnvironmentSetup %* > setup.log 2>&1
set SETUP_EXIT_CODE=%ERRORLEVEL%

type setup.log
if %SETUP_EXIT_CODE% neq 0 (
    echo Environment setup FAILED. Check setup.log and logs\AutomationLogs.log
) else (
    echo Environment setup finished. Details in setup.log
)
exit /b %SETUP_EXIT_CODE%
