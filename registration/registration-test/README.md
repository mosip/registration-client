# Registration Client Automation

## Overview
Registration-test automation covers flows [New, Update, Correction, Lost flows](https://docs.mosip.io/1.2.0/id-lifecycle-management)

## Prerequisite
1. Mock MDS on port `4501`. The automation starts it from the `io.mosip.mock.mds:mock-mds` dependency (`mvn package` stages it under `target/mockmds`). A separate `mosip-mock-services` checkout is not required. If something is already listening on 4501, that process is left as it is.
2. Download the utility from target environment
2. Testdata files to be set in advance based on schema id's. For default schema testdata is present inside resources folder `repository_eng`

## Build
The project requires JDK 21.0.3 and mvn version 3.9.6<br>
1. First build jar `mvn clean install`

2. Using jar
```
jre\bin\java -Dpath.config=/config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -cp "registration-test.jar;lib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1
```
3. Using IDE set vm args
```
-Dpath.config=\\src\\main\\resources\\config.properties  -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --module-path "D:\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
```
3. Place jar in one folder along with `src/main/resources` files and folder and then run jar with above mentioned arguments

## Configurations
Update the following:

* operatorId:
* operatorPwd:
* supervisorUserid:
* supervisorUserpwd:
* reviewerUserid:
* reviewerpwd:
* appLanguage=français
* langcode=eng@@fra@@ara
* makeUniqueEntry:fullName
* sync:Y or N
* upload:Y or N
* multilang:Y or N
* datadir=/repository_eng/
* manual:Y or N

## Environment setup
`run.bat` prepares the target environment, then runs the initial launch and the full suite:
1. Creates `operatorId`, `supervisorUserid` and `reviewerUserid` in Keycloak with their passwords and `setup.user.roles`
   (existing users are used as they are)
2. Maps those users to `setup.zoneCode` and `setup.regCenterId` (existing mappings are kept)
3. Creates and activates this machine in masterdata. An existing machine with the same name is reused; it is only
   activated if inactive, and its keys are updated only if they no longer match this machine

4. Creates a server-trusted device certificate for Mock MDS (`setup.mockMds=Y`), following the DSL device partner flow:
   registers a `Device_Provider` partner, uploads a generated CA and intermediate certificate to the `DEVICE` domain,
   uploads the partner certificate (PMS re-signs it) and creates a device specific key signed by the partner key.
   The keys are kept in `mds-certs/` and reused while valid. `device-dsk-partner.p12` is copied into the Mock MDS
   run folder and the Registration keystore entries of its `application.properties` are pointed at it. Set
   `setup.mockmds.dir` only to use a different run folder than the one staged from the Maven dependency.

The zone and center must be inside the zone of `setup.admin.userName`, otherwise masterdata rejects the mapping.
`mosip.hostname` must be the host that serves the MOSIP APIs (for example `api-internal.<env>.mosip.net`).

`run.bat` starts Mock MDS before the client and stops it when the run ends. The dependency
is `mock-mds` 1.2.0.2, which accepts the capture requests this Registration Client sends.
By default UI automation drives the real mouse and keyboard, so keep the machine unlocked while it runs.
To run without a visible window (machine can be locked or in use), set `headless:Y` in `config.properties`
(`Y`, `yes` or `true`). `run.bat` reads that key. `headless:N` keeps the visible client.
`-Dheadless=true` or `-Dheadless=false` on the command line overrides the file.
Headless mode uses JavaFX Monocle (`openjfx-monocle` 21.x, matching the client's JavaFX 21); failure snapshots are taken
from the top JavaFX window instead of the screen. `openjfx-monocle-21*.jar` must be in `lib` or `testlib` (remove the old
`openjfx-monocle-jdk-11+26.jar`); `run.bat` patches it into the bundled JRE's `javafx.graphics` module.

Fill the `setup.*` keys and `mosip.hostname` in `config.properties`, then run from the same folder as `run.bat`.
`run.bat` does the whole sequence: setup, then initial launch, then the full suite.
When setup creates a new user or machine it waits `setup.propagationWaitMinutes` (default 15) before the initial launch.
When the user and machine already exist, that wait is skipped.
```
run.bat
```
Machine details are read the same way the Registration Client reads them: machine name is the lowercase hostname, and the
public key / sign public key come from the TPM, or from `.mosipkeys/reg.pub` when no TPM is available.

## Execution result and logs
1. Verify the failure in the logs file `\logs\AutomationLogs.log`
1. Execution results present under report folder file `extentReport-datetimestamp.html`

## Automated Test Cases

See [AUTOMATED_TEST_CASES.md](AUTOMATED_TEST_CASES.md) for the full list of 45 automated test scenarios (MOSIP-43431).

## License
This project is licensed under the terms of [Mozilla Public License 2.0](../../LICENSE)

