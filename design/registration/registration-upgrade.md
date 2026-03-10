## 1. Introduction
This document outlines the strategy for migrating the MOSIP Registration Client from JRE 11 to JRE 21. The migration must 
be fully automated ("Zero-Touch") and executed within a constrained environment where the existing run.bat is immutable.

## 2. Existing System Overview

**Current Runtime:** Java 11.

**Launcher:** run.bat (hardcoded to jre\bin\javaw and -cp lib/*).

**Update Flow:** Downloads files to .TEMP → Restarts → run.bat copies .TEMP to lib → Launches Initialization.class.

**Integrity:** Single MANIFEST.MF for checksum validation.

**Limitation:** Client only knows how to download the files into .TEMP directory. It cannot change run.bat or in that case 
any files outside lib directory.

## 3. Proposed Migration Strategy:

Existing versions of the client will notice the version change in the maven_metatdata.xml, prompting the update process.
When the operator initiates the update, the client will connect to the server and downloads the MANIFEST.MF for version 1.3.0.

#### Upgrade MANIFEST.MF contains the following entries:

**jre21.zip:** Compressed Java 21 JRE.

**lib21.zip:** All Java 21 compatible JARs (all client jars and dependencies & MANIFEST.MF).

**logback.xml:** Updated logging configuration compatible with Java 21.

**launcher.jar:** Compiled with Java 21 with compiler target 11 to ensure it can run on Java 11. 
This class will contain the logic to swap the JRE and rewrite run.bat.

With this, we will have two Initialization classes in the classpath with the same package. Based on the classpath priority 
logic of the JVM, either Initialization class in the launcher.jar will be loaded or Initialization class in the registration-client.jar is loaded.

* If the Initialization class in the launcher.jar is loaded, then it will execute the logic to swap the JRE and then launch 
the new run.bat with Java 21 parameters.

* If the Initialization class in the registration-client.jar is loaded. Existing logic will try to remove 
UNKNOWN files in the lib directory as part of ClientPreLoader based on the entries in the MANIFEST.MF and prompts the operator to 
restart the client application. UNKNOWN file names are written into .UNKNOWN_JARS file if the deletion failed.
On restart, run.bat clears all the files listed in the .UNKNOWN_JARS file and then launches the Initialization class. 
Note this time the Initialization class in the launcher.jar will be loaded as the registration-client.jar is removed from the lib directory.

Our existing Initialization class is as below:

```java
package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

import java.nio.file.Path;


public class Initialization {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", Path.of("lib", "logback.xml").toFile().getCanonicalPath());   //NOSONAR Setting logger configuration file path here.
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}
```

In the new launcher.jar, the Initialization class will be as below:

```java
package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

import java.nio.file.Path;


public class Initialization {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", Path.of("lib", "logback.xml").toFile().getCanonicalPath());   //NOSONAR Setting logger configuration file path here.
        
        // Logic to detect if the JRE is 11 or 21 and perform the necessary actions to swap the JRE and rewrite run.bat.
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("11")) {
            // Verifies the signed hash of the jre21.zip, lib21.zip, launcher.jar in the MANIFEST.MF to ensure integrity before proceeding with the migration.
            // Note: Launcher.jar holds the certificate to verify the integrity of the files part of the upgrade process.
            // Unzips jre21.zip to jre21_temp.
            // Unzips lib21.zip to .TEMP.
            // Create new run.bat with Java 21 parameters.
            // Write rollback.bat -> Identifies the latest backup folder based on the folder name (date is part of the folder name).
            // Write migration.bat -> copies jre21_temp to jre, copies .TEMP to lib, and launches the new run.bat with Java 21 parameters.
            // Executes migrator.bat and terminates the JRE 11 process.
            // Note: this is just a high-level overview of the steps involved.
        } else {
            // If the JRE is already 21, launch the application as usual.
            LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
        }
    }
}
```


Migrator Script (migration.bat)

```
@echo off
timeout /t 5 /nobreak > nul
:: Backup old environment
if exist jre_old rmdir /s /q jre_old
move jre jre_old
move jre21_temp jre

:: Clean lib to prevent JAR version mixing
if exist lib rmdir /s /q lib
mkdir lib

:: Launch the updated run.bat
start run.bat
del "%~f0"
```

Note: We will now have two MANIFEST.MF files, one in the lib directory and the other in the application root directory. 

* The MANIFEST.MF in the lib directory will be used for integrity checks and to identify the unknown files to be deleted during every client startup.
Also identifies the files to be downloaded during the **patch upgrade** process.

* The MANIFEST.MF in the application root directory will be used to identify the files to be downloaded during the **full upgrade** process hereafter.

Both the MANIFEST.MF files will have signed hashes of the files to ensure integrity. The signature will be generated using 
a private key, and the client will have the corresponding public key to verify the integrity of the files before proceeding with the upgrade.

## Future enhancements:

1. Resumable file download: In case of large files, we can implement resumable file download to handle network interruptions.
2. Downloads in background: To improve user experience, we can implement background downloads with progress indication. Once the download is complete, we can prompt the user to restart the application to apply the update.