@echo off
set HEADLESS_ARGS=
set HEADLESS_DEFINE=
set HEADLESS=
set HEADLESS_FROM_ARGS=

echo %* | findstr /i /c:"headless=true" >nul && set HEADLESS_FROM_ARGS=true
echo %* | findstr /i /c:"headless=false" >nul && set HEADLESS_FROM_ARGS=false

if "%HEADLESS_FROM_ARGS%"=="true" set HEADLESS=true
if "%HEADLESS_FROM_ARGS%"=="false" goto launch
if "%HEADLESS%"=="true" goto apply

if exist config.properties (
    for /f "usebackq eol=# tokens=1* delims==:" %%a in ("config.properties") do if /i "%%a"=="headless" call :checkvalue "%%b"
)

:apply
if "%HEADLESS%"=="true" (
    for %%f in (lib\openjfx-monocle-21*.jar testlib\openjfx-monocle-21*.jar) do set HEADLESS_ARGS=--patch-module javafx.graphics=%%f
    if not "%HEADLESS_FROM_ARGS%"=="true" set HEADLESS_DEFINE=-Dheadless=true
)

:launch
jre\bin\java %HEADLESS_DEFINE% %* %HEADLESS_ARGS% -Dpath.config=/config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -cp "registration-test.jar;lib/*;testlib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1
exit /b %ERRORLEVEL%

:checkvalue
set "VAL=%~1"
set "VAL=%VAL: =%"
if /i "%VAL%"=="Y" set HEADLESS=true
if /i "%VAL%"=="YES" set HEADLESS=true
if /i "%VAL%"=="TRUE" set HEADLESS=true
goto :eof