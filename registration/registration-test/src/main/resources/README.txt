Using JAR
jre\bin\java -Dpath.config=\\config.properties -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED -cp "registration-test.jar;lib\*" registrationtest.runapplication.NewRegistrationAdultTest
And Add mock sdk jar in lib folder as run time dependency.

USING IDE
-Dpath.config=\\src\\main\\resources\\config.properties  -Dfile.encoding=UTF-8 -Djdbc.drivers=org.apache.derby.jdbc.EmbeddedDriver --module-path "D:\openjfx-11.0.2_windows-x64_bin-sdk\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED