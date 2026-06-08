# CLAUDE.md

Guidance for AI agents and contributors working in this repository. Read this first.

## Project
**Passwordy** — a multi-user password manager.
- `backend/` — Spring Boot 3.5.7 REST API (Java 25, Maven)
- `frontend/` — Android app (Kotlin, Jetpack Compose, Gradle)

The Android app is a thin client; all security-critical logic (auth, hashing, encryption,
authorization) lives in the backend.

## Git workflow
- **Develop on `dev`.** All ongoing work and commits land here.
- **Integrate into `main` with a no-fast-forward merge:** `git merge --no-ff dev` (always
  create a merge commit — never fast-forward into `main`).
- `main` is the stable branch; `dev` is the integration branch.
- Remote is `origin` (GitHub `adhamamr01/Passwordy`); push both branches.

## Build & test

### Backend (`backend/`) — use the Maven wrapper (there is no system `mvn`)
- Run (default in-memory H2, zero setup): `./mvnw spring-boot:run`
- Run against PostgreSQL: `docker compose up -d`, then
  `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker`
- Test: `./mvnw clean test` (runs on H2 — no database or Docker required)
- On Windows use `mvnw.cmd`.

### Frontend (`frontend/`)
- Open in Android Studio, or use the Gradle wrapper:
  `./gradlew assembleDebug` / `installDebug` / `test`.

## Architecture
- **Backend is layered:** `controller` (HTTP only) → `service` (business logic + ownership
  checks) → `repository` (Spring Data JPA). Controllers stay thin and delegate everything.
- **Auth:** stateless JWT (24h). `JwtAuthenticationFilter` populates the security context;
  `SecurityConfig` permits `/api/auth/**` and `/api/password/generate(-pin)` and authenticates
  the rest. Master passwords are BCrypt-hashed; stored passwords are AES-256-GCM encrypted
  (fresh IV per entry) and only decrypted on the explicit `/api/passwords/{id}/decrypt` route.
- **Frontend is MVVM:** Compose screens observe `StateFlow`s from ViewModels → repositories →
  Retrofit `ApiService`; the JWT is held in DataStore.

## Conventions
- **Secrets** live in gitignored `backend/src/main/resources/application-{local,docker}.properties`.
  Copy from the committed `*.example` templates; never commit real secrets.
- **DTOs are Java records** (in `dto/`), except `PasswordGenerationRequest` /
  `PinGenerationRequest`, which stay classes because they rely on field defaults. Request DTOs
  carry `jakarta.validation` constraints and controllers use `@Valid`.
- **Errors:** throw typed exceptions; `GlobalExceptionHandler` maps them to HTTP status —
  400 (`BadRequestException` / Bean Validation), 401 (`InvalidCredentialsException`),
  403 (`UnauthorizedException`), 404 (`ResourceNotFoundException`), 500 (anything else; logged
  with a stack trace and returned as a generic message). All errors are `{"error": "<message>"}`.

## Environment notes (these bit us — save yourself the trouble)
- **JDK 25 required.** If `./mvnw` can't launch a JVM, your `JAVA_HOME` is likely broken; point
  it at a working JDK 25 (on this machine: `C:\Program Files\Java\jdk-25.0.2`).
- **Tests run on H2, not Testcontainers.** A Testcontainers/PostgreSQL test was reverted because
  the docker-java client in current Testcontainers (≤1.21.4) cannot talk to Docker Engine 29.x.
  Don't reintroduce it until Testcontainers supports Docker 29 (see DECISIONS.md §10).
- Prefer the wrappers (`./mvnw`, `./gradlew`) over IDE-launched Maven — the IDE Maven
  integration has been flaky here.

## Known limitations / follow-ups
- **Hardcoded crypto keys:** the AES key (`AESEncryptionService`) and JWT secret (`JwtUtil`) are
  in-source constants — externalize them to config/secrets (DECISIONS.md §6).
- **Rate limiting** on `/api/auth/**` is not yet implemented (brute-force / enumeration).
- **Registration enumeration:** `register` reveals "username/email already exists"; the planned
  fix is an email-verification flow (DECISIONS.md §7).

## Docs
- `README.md` — overview & quick start
- `DECISIONS.md` — architecture & design rationale (numbered sections)
- `API.md` — HTTP API reference (routes, request/response shapes, status codes)
- `SETUP.md` — environment / database / secrets setup
