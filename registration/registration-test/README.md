# Regisration Client Automation.

Registration Client automation covers thick client flows NEW, LOST, UPDATE and Bio Correction.
## Prerequisite :
1. MockMDS run on port `4501`
2. Download the utility from target environment.
2. Testdata files to be set in advance based on schema id's. For default schema testdata is present inside resources folder `repository_eng`

## Build : 

1. First Build Jar `mvn clean install`

2. Using JAR
```
jre\bin\java -Dpath.config=/config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -cp "registration-test.jar;lib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1
```
3. USING IDE set vm args
```
-Dpath.config=\\src\\main\\resources\\config.properties  -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --module-path "D:\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
```
3. Place jar in one folder along with src/main/resources files and folder and then run jar with above mentioned arguments.

## Configurations :

1. First check config file - user details add user as per your machine regcenter.
     * operatorId:
     * operatorPwd:
     * supervisorUserid:
     * supervisorUserpwd:
     * reviewerUserid:
     * reviewerpwd:
1. Second check - language selection
     * appLanguage=français
     * langcode=eng@@fra@@ara

1. Third check - Unique data for below id's this appends date
     * makeUniqueEntry:fullName

1. Fourth check update this as per env
     * sync:Y
     * upload:Y
     * multilang:Y

1. Fifth check - testdata files path
     * datadir=/repository_eng/

1. Sixth check- Run utility manually below options
     * manual:Y



## Below scenarios and their tags :
NEW, LOST, UPDATE, Bio Correction
## Execution result and Logs
1. Verify the Failure in the Logs file `\logs\AutomationLogs.log`
1. Execution results present under report folder file `extentReport-datetimestamp.html`


## License
This project is licensed under the terms of [Mozilla Public License 2.0]

