# Registration Client Automation

## Overview
Registration-test automation covers flows [New, Update, Correction, Lost flows](https://docs.mosip.io/1.2.0/id-lifecycle-management)

## Prerequisite
1. MockMDS run on port `4501`
2. Download the utility from target environment
2. Testdata files to be set in advance based on schema id's. For default schema testdata is present inside resources folder `repository_eng`

## Build
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

## Execution result and logs
1. Verify the failure in the logs file `\logs\AutomationLogs.log`
1. Execution results present under report folder file `extentReport-datetimestamp.html`

## License
This project is licensed under the terms of [Mozilla Public License 2.0](../../LICENSE)

