# AGENTS.md

This file provides guidance to AI agent when working with code in this repository.

## Build Commands

All commands must be run from inside the `registration/` directory (the Maven parent project).

```bash
# Full build (skip tests and GPG signing)
cd registration && mvn clean install -Dgpg.skip -DskipTests

# Build with tests
cd registration && mvn clean install -Dgpg.skip

# Build a single module
cd registration && mvn clean install -Dgpg.skip -DskipTests -pl registration-services

# Run tests in a specific module
cd registration && mvn test -pl registration-services

# Run a single test class
cd registration && mvn test -pl registration-services -Dtest=PacketHandlerServiceTest

# Sonar analysis
cd registration && mvn verify sonar:sonar -Psonar -Dgpg.skip -DskipTests
```

**Requirements:** JDK 21.0.3, Maven 3.9.6

## Module Architecture

The project is a Maven multi-module build under `registration/`:

| Module | Role |
|---|---|
| `registration-api` | SPI interfaces for pluggable hardware (document scanner, geo-position) |
| `registration-api-stub-impl` | No-op stub implementations of those SPIs for development/testing |
| `registration-services` | All business logic, data access, sync, packet handling — no UI dependencies |
| `registration-client` | JavaFX desktop UI; depends on `registration-services` |
| `registration-test` | End-to-end automation tests using a real running client |
| `ref-impl/` | Reference hardware implementations (commented out of parent pom by default) |

## Key Architectural Patterns

### Spring Context Bootstrap
`ClientApplication` (JavaFX `Application` subclass) bootstraps Spring via `AnnotationConfigApplicationContext(AppConfig.class)` during JavaFX `init()`. Spring beans are not available before this point. `SessionContext.setApplicationContext()` stores the context for static access throughout the app.

### Configuration Loading Order
`DaoConfig` runs first — it initializes the embedded Apache Derby database (encrypted with a machine-specific key via TPM or software fallback), then loads all `GlobalParam` rows from Derby into `ApplicationContext.applicationMap`. Properties in `mosip-application.properties` provide defaults; the DB values override at runtime after sync.

### Offline-First with Sync Jobs
The client operates fully offline using local Derby. Quartz-scheduled jobs (defined in `jobs/impl/`) periodically sync with the MOSIP server when online:
- `MasterSyncJob` — pulls master data (locations, document types, etc.)
- `PublicKeySyncJob` / `KeyPolicySyncJob` — key material sync
- `RegistrationPacketSyncJob` / `RegistrationPacketUploadJob` — packet status and upload
- `UserDetailServiceJob` — operator details
- `SynchConfigDataJob` — global parameters

### Registration Packet Flow
1. UI controllers in `registration-client` collect demographic/biometric data into DTOs
2. `PacketHandlerServiceImpl` assembles everything using `commons-packet` library's `PacketWriter`
3. Packet is encrypted client-side via `ClientCryptoFacade` (TPM-backed if available)
4. `PacketSynchServiceImpl` syncs packet status; `PacketUploadServiceImpl` uploads to server

### JavaFX UI Structure
- `ClientPreLoader` shows a splash screen while `ClientApplication.init()` runs the heavy Spring boot + initial sync
- `Initialization` is the true `main()` entry point (configured as jar manifest `Main-Class`)
- FXML files under `registration-client/src/main/resources/fxml/` are paired with `@Component` controller classes
- `BaseController` provides common navigation helpers; all screen controllers extend it
- `GenericController` drives the dynamic registration form, rendering fields from `IdentitySchema` (downloaded from server and stored in Derby)

### Biometric Device Integration (MDM)
`MosipDeviceSpecificationFactory` discovers SBI-compliant biometric devices by scanning localhost ports at startup. Device interaction goes through `BioServiceImpl` → MDM spec DTOs in `mdm/`. The SPI in `registration-api` allows injecting alternative scanner/geo-position implementations.

### AOP Security
`AuthenticationAdvice` and `ResponseSignatureAdvice` in `util/advice/` intercept service calls annotated with `@PreAuthorizeUserId` to enforce that the logged-in operator has the required role before executing sensitive operations.

### Local Database
Apache Derby embedded DB. Schema lives in `registration-services/src/main/resources/sql/` with versioned migration scripts. `SoftwareUpdateHandler.updateDerbyDB()` runs migrations on startup by comparing the installed version against applied scripts.

## Configuration

- Application properties: `registration-services/src/main/resources/props/mosip-application.properties` (defaults only; runtime values come from DB after sync)
- External runtime config is pulled from `mosip-config` repo on the MOSIP server (URL configured via `mosip.hostname`)
- `mosip.hostname` and related server URLs must be set in the DB / properties before the client can sync

## End-to-End Automation Tests (`registration-test`)

These are UI-level tests that drive a running Registration Client instance. They are not standard unit tests — they require a live client and a configured environment.

Configure `registration-test/src/main/resources/config.properties` with operator credentials and environment details, then run via the packaged JAR per the `README.txt` in that directory.

## Sonar Coverage Exclusions

DTOs, entities, repositories, FXML controllers, enums, and UI utility classes are excluded from coverage analysis (see `sonar.coverage.exclusions` in the parent `pom.xml`). Focus unit test efforts on `service/`, `jobs/`, `validator/`, and `util/` packages.
