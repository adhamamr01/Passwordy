# CLAUDE.md

Guidance for AI agents and contributors working in this repository.

## Project
**Passwordy** — a multi-user password manager:
- `backend/` — Spring Boot 3.5.7 REST API (Java 25, Maven)
- `frontend/` — Android app (Kotlin, Jetpack Compose, Gradle)

## Git workflow
- **Develop on `dev`.** All ongoing work and commits land here.
- **Integrate into `main` with a no-fast-forward merge:** `git merge --no-ff dev`.
  Always create a merge commit — never fast-forward into `main`.
- `main` is the stable branch; `dev` is the integration branch.

## Build & test

### Backend (`backend/`)
- Run (default in-memory H2, zero setup): `./mvnw spring-boot:run`
- Run against PostgreSQL: `docker compose up -d` then
  `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker`
- Test: `./mvnw clean test` (runs on H2 — no database or Docker required)

### Frontend (`frontend/`)
- Open in Android Studio, or use the Gradle wrapper:
  `./gradlew assembleDebug` / `installDebug` / `test`

## Conventions
- **Secrets** live in gitignored `backend/src/main/resources/application-{local,docker}.properties`.
  Copy from the committed `*.example` templates and fill in real values — never commit real secrets.
- **Use the wrappers** (`./mvnw`, `./gradlew`), not a system `mvn`/`gradle`.

## Docs
- `README.md` — overview & quick start
- `DECISIONS.md` — architecture & design rationale
- `API.md` — HTTP API reference
- `SETUP.md` — environment / database / secrets setup
