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
- **Crypto keys are externalized:** the AES key (`AESEncryptionService`, `encryption.secret.key`)
  and JWT secret (`JwtUtil`, `jwt.secret`) are read from config (Base64). The committed H2 profile
  ships throwaway dev defaults; real secrets live in the gitignored
  `application-{local,docker}.properties`. Rotating the AES key makes existing ciphertext
  undecryptable — there is no in-place re-encryption (DECISIONS.md §6).
- **Rate limiting** is implemented: tiered Bucket4j token buckets in a `RateLimitFilter`
  (auth/generation keyed by client IP, authenticated CRUD by username), plus an account-level
  login throttle in `AuthServiceImpl` (keyed by submitted username → 429 via
  `TooManyRequestsException`). Configurable via `ratelimit.*`. `ClientIpResolver` supports
  trusted-proxy `X-Forwarded-For` (opt-in via `ratelimit.trusted-proxies`; default trusts none
  and uses `getRemoteAddr()`). Still **single-instance / in-memory** (`ConcurrentHashMap`); a
  shared store (e.g. Redis) is needed before horizontal scaling.
- **Registration enumeration:** `register` reveals "username/email already exists"; the planned
  fix is an email-verification flow (DECISIONS.md §7).
- **Android client hardening:** body logging is gated to debug builds, cleartext HTTP is blocked
  except for local dev hosts (`network_security_config.xml`), and `allowBackup=false`
  (DECISIONS.md §11). The H2 console ships **disabled** (`spring.h2.console.enabled=false`).
  The on-device JWT is now **Keystore-encrypted** (`TokenCrypto`). The frontend Gradle wrapper +
  version catalog have been reconstructed, committed, and verified by an Android Studio sync
  (Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10), and a **`frontend-ci.yml`** workflow builds the app on
  JDK 21 (`testDebugUnitTest assembleDebug`).

## Docs
- `README.md` — overview & quick start
- `DECISIONS.md` — architecture & design rationale (numbered sections)
- `API.md` — HTTP API reference (routes, request/response shapes, status codes)
- `SETUP.md` — environment / database / secrets setup
