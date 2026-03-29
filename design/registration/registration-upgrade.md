## 1. Introduction
This document outlines the strategy for migrating the MOSIP Registration Client from JRE 11 to JRE 21. The migration must be fully automated and executed within a constrained environment where the existing run.bat is immutable.

## 2. Existing System Overview

**Current Runtime:** Java 11.

**Launcher:** run.bat (hardcoded to jre\bin\javaw and with -cp lib/*).

**Full Update Flow:** Existing versions of the client will notice the version change in the maven_metatdata.xml, prompting the update process. When the operator initiates the update, Below existing logic executes:

1. BacksUp bin, lib, db and manifest in the configured backup folder.
2. Fetch server manifest && replace local manifest with Server manifest.
3. Clears temp directory
4. Downloads each new file in the manifest into lib directory 
5. if any existing file in the lib is not matching the hash in the manifest is also downloaded into lib directory.

On Failure in any of the above steps, existing logic will rollback the backup.
On Success, Prompts the operator to restart application.

**Partial Update Flow:** Downloads files to .TEMP → Restarts → run.bat copies .TEMP to lib → Launches Initialization.class.

**Limitation:** Client only knows how to download the files into lib or .TEMP directory. It cannot change run.bat or in that case any files outside lib directory.

**Integrity:** Single MANIFEST.MF for checksum validation. on start of the application (ClientPreLoader) if any file fails the integrity check, 
1. Attempts to delete the file, if failed makes an entry in .UNKNOWN_JARS file. And exits the application startup.
2. run.bat file first clears are the files under "lib" listed in .UNKNOWN_JARS file to starting the JVM.

## 3. Proposed Migration Strategy:

The new 1.3.0 MANIFEST.MF contains below entries:

**jre21.zip:** Compressed Java 21 JRE.

**lib21.zip:** All Java 21 compatible JARs (all client jars and dependencies & MANIFEST.MF).

**logback.xml:** Updated logging configuration compatible with Java 21.

**_launcher.jar:** Compiled with Java 21 with compiler target 11 to ensure it can run on Java 11. 
This class will contain the logic to swap the JRE and rewrite run.bat.

**Assumption: OpenJDK (on all platforms) sorts JARs alphabetically when expanding lib/* — this is consistent behavior across Windows/Linux/Mac. "_launcher.jar" (underscore = ASCII 95, before all letters) guarantees it loads first.**

With this logic, the Initialization class in the _launcher.jar is loaded on restart of the client application, then it will execute the logic to swap the JRE and then launch the new run.bat with Java 21 parameters.

**Worst case:** If the Initialization class in the registration-client.jar is loaded. Existing logic will try to remove unknown files in the lib directory as part of ClientPreLoader based on the entries in the MANIFEST.MF and prompts the operator to restart the client application. UNKNOWN file names are written into .UNKNOWN_JARS file if the deletion failed. On restart, run.bat clears all the files listed in the .UNKNOWN_JARS file and then launches the Initialization class. Note this time the Initialization class in the _launcher.jar will be loaded as the registration-client.jar is removed from the lib directory.

Our existing Initialization class is as below in registration-client.jar:

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

In the new _launcher.jar, the Initialization class will be as below:

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
        //String javaVersion = fetch version based on the jre being used by the application.
        if (javaVersion.startsWith("11")) {
            // Verifies the hash of the jre21.zip, lib21.zip, _launcher.jar in the MANIFEST.MF to ensure integrity before proceeding with the migration.
            // Unzips jre21.zip to jre21_temp
            // Unzips lib21.zip to .TEMP
            // Backup existing run.bat to run.bat_11_bkp.
            // Write new run.bat with Java 21 parameters.
            // Write new rollback.bat
            //    -> renames jre11 to jre
            //    -> Deletes .TEMP
            //    -> Deletes jre21_temp
            //    -> Rename run.bat_11_bkp to run.bat
            //    -> Identifies the latest backup folder based on the folder name
            //    -> Replaces lib, bin, db, Manifest file
            //    -> Asks operator to start run.bat
            // Write new migration.bat 
            //    -> backup existing jre to jre11, if not exists
            //    -> Rename jre21_temp to jre
            //    -> copy all files in .TEMP to lib
            //    -> and launches the new run.bat with Java 21 parameters.
            // Executes migrator.bat and terminates the JRE 11 process.
            // Note: this is just a high-level overview of the steps involved.
            // Note: All the new batch files are written in the application root directory.
        } else {
            // If the JRE is already 21, launch the application as usual.
            LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
        }
    }
}
```

**We will now have two MANIFEST.MF files, one in the lib directory and the other in the application root directory.** 

* The MANIFEST.MF in the lib directory will be used for integrity checks and to identify the unknown files to be deleted during every client startup. Also identifies the files to be downloaded during the **patch upgrade** process.

* The MANIFEST.MF in the application root directory will be used to identify the files to be downloaded during the **full upgrade** process hereafter.

Both the MANIFEST.MF files will have hashes of the files to ensure integrity.

*Note:* For all future upgrades (1.3.0 onwards), _launcher.jar or the new registration-client.jar holds the public key used to sign the MANIFEST.MF file. Upgrade server, serves both MANIFEST.MF and MANIFEST.MF.sig

1. Logic should be added in the configure.sh to sign the manifest.mf and update the jar with the upgrade server public key.
2. Verification logic to added in _launcher.jar and ClientPreloader. This check MUST happen before any file is downloaded or migration step begins.
3. On verification failure, we MUST log and abort the migration process. if verification fails on partial upgrades or on application startup, alert message MUST be displayed in the preloader screen or as an alert to operator.


## Future enhancements:

1. Resumable file download: In case of large files, we can implement resumable file download to handle network interruptions.
2. Downloads in background: To improve user experience, we can implement background downloads with progress indication. Once the download is complete, we can prompt the user to restart the application to apply the update.