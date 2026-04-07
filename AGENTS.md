# CLAUDE.md

This file provides guidance to AI agents (claude.ai/code) when working with code in this repository.

## Build Commands

All Maven commands are run from the `registration/` subdirectory (not the repo root):

```bash
# Build without tests (fast)
cd registration && mvn clean install -Dgpg.skip -DskipTests

# Build with tests
cd registration && mvn clean install -Dgpg.skip

# Build a single module
cd registration/registration-services && mvn clean install

# Run tests only
cd registration && mvn test

# SonarQube analysis
cd registration && mvn clean verify -Psonar
```

Requires **JDK 21** and **Maven 3.9.6+**.

## Running a Single Test

```bash
cd registration/registration-services && mvn test -Dtest=ClassName#methodName
```

## Project Structure

This is a multi-module Maven project. The `registration/` subdirectory is the Maven root (parent `pom.xml` lives there). Modules:

- **registration-api** — Interface contracts for external service integrations (no implementations)
- **registration-api-stub-impl** — Stub implementations for offline/test scenarios
- **registration-services** — Core business logic: authentication, biometric capture, packet handling, sync, Quartz jobs, Derby DB access via Hibernate/JPA
- **registration-client** — JavaFX desktop UI: FXML views, CSS, controllers, i18n resources
- **registration-test** — Automation test suite using JUnit 5 + TestFX, with CSV-driven test data for registration flows

## Architecture Overview

**Runtime flow**: User authenticates → `SessionContext` (singleton) holds roles/tokens → UI controllers in `registration-client` invoke services in `registration-services` → services write to embedded **Derby** database and package data for upload → Quartz jobs periodically sync with the MOSIP backend over REST.

**Layered architecture** (all in `registration-services`):
- `service/` — business logic interfaces + implementations (e.g., `BioService`, `PacketHandlerService`, `LoginService`)
- `dao/` — data access via Hibernate; entities in `entity/`
- `dto/` — data transfer objects; mapped using Orika (1.4.6)
- `jobs/` — Quartz scheduler jobs for background sync, config refresh, status polling
- `config/` — Spring `AppConfig`, `DaoConfig` — the main Spring application context setup
- `context/SessionContext.java` — central session state; created at login, cleared at logout

**UI module** (`registration-client`):
- `controller/` — JavaFX MVC controllers
- `resources/fxml/` — FXML view definitions
- `resources/labels_[eng|fra|ara].properties` — i18n strings (English, French, Arabic)
- Entry point: `controller/Initialization.java`

**Configuration**: loaded from a remote MOSIP Spring Cloud Config Server on startup; fallback to local Derby. Primary config file: `registration-services/src/main/resources/spring.properties`.

**Biometric device integration**: via MDM (Mobile Device Manager) REST endpoints, managed by `MosipBioDeviceManager`. Default test port: 4501. Supports fingerprint, iris, face capture.

**Packet lifecycle**: demographic + biometric data collected into `RegistrationDTO` → serialized/encrypted into a MOSIP packet → packet metadata stored in Derby, packet is stored in configured folder → uploaded to Registration Processor by Quartz job → status polled asynchronously.

## Key Technologies

| Concern | Technology |
|---|---|
| UI | JavaFX 21 with FXML |
| DI / IoC | Spring 6.1.4 |
| ORM | Hibernate 6.4.4 + Derby 10.17.1 (embedded) |
| Scheduling | Quartz 2.2.1 |
| Object mapping | Orika 1.4.6 |
| Biometrics matching | SourceAFIS 3.4 |
| JSON | Jackson 2.15.4 |
| Testing | JUnit 4/5, Mockito 3, PowerMock 2, TestFX 4 |
| Coverage | JaCoCo 0.8.11 |
| CI/CD | GitHub Actions → Docker → Sonatype/DockerHub |

## Test Coverage Notes

JaCoCo excludes DTOs, entities, repositories, controllers, constants, exceptions, enums, builders, and stub implementations from coverage metrics. Focus test effort on service implementations.
