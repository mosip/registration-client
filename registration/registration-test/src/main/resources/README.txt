Download the utility..
--First check config file - user details add user as per your machine regcenter.
operatorId:
operatorPwd:
supervisorUserid:
supervisorUserpwd:
reviewerUserid:
reviewerpwd:

--Second check - language selection
appLanguage=français
langcode=eng@@fra@@ara

--Third check - Unique data for below id's this appends date
makeUniqueEntry:fullName

--Fourth check update this as per env
sync:Y
upload:Y
multilang:Y


--Fifth check - testdata files path
datadir=/repository_eng/

--Sixth check- Run utility manually below options
manual:Y
#datadir=/repository_eng/

--Using JAR

jre\bin\java -Dpath.config=/config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -cp "registration-test.jar;lib/*" registrationtest.runapplication.RegistrationMain > startup.log 2>&1

--USING IDE set vm args
-Dpath.config=\\src\\main\\resources\\config.properties  -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --module-path "D:\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED