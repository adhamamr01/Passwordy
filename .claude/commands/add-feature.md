---
description: Run the full add-a-feature workflow — user stories, implementation, tests, docs, pen test, and stress test — end to end.
argument-hint: <feature description, e.g. "password sharing between users">
---

You are running the **Passwordy feature-delivery workflow** for the following feature:

> $ARGUMENTS

Passwordy is a multi-user password manager: a Spring Boot 3.5 / Java 25 backend (`backend/`,
Maven) and a thin Android client (`frontend/`). All security-critical logic lives in the
backend. Read `CLAUDE.md`, `DECISIONS.md`, and `API.md` before starting if not already loaded.

Work through the phases below **in order**. Use a TaskCreate list to track them, marking each
in_progress/completed as you go. Pause for the user only at the explicit checkpoints. Do not
skip the security or stress phases — they are the point of this workflow.

---

## Phase 1 — User stories & acceptance criteria
- Restate the feature in one sentence. If the scope is ambiguous (data model, who can do what,
  edge cases), ask the user with `AskUserQuestion` **before** writing code.
- Write 2–5 user stories in the form *"As a <role>, I want <capability>, so that <benefit>"*,
  each with concrete **acceptance criteria** (happy path + failure/authorization cases).
- List the security & privacy implications up front (new endpoints, new stored data, who owns
  it, what must be encrypted, what must be rate limited).
- **Checkpoint:** show the stories + criteria and get a 👍 before implementing.

## Phase 2 — Design
- Plan against the layered architecture: `controller` (HTTP only) → `service` (business logic +
  ownership checks) → `repository`. Controllers stay thin.
- Follow conventions: request/response DTOs are Java **records** in `dto/` with
  `jakarta.validation` constraints (`@Valid` in controllers); typed exceptions mapped by
  `GlobalExceptionHandler`; stored secrets AES-256-GCM encrypted, never returned in plaintext
  except via an explicit decrypt route; every per-user record goes through an ownership gate.
- Identify the exact files to add/change. Note any new config (`application.properties` +
  `*.example` templates) and whether new endpoints need entries in `SecurityConfig` and a
  rate-limit tier.

## Phase 3 — Implement
- Create a feature branch off `dev` if the change is large; otherwise commit on `dev`. Never
  work directly on `main`.
- Implement the design. Keep diffs reviewable. Reuse existing helpers (encryption service,
  validators, `findOwned…` ownership pattern) rather than duplicating.
- Run `/code-review` and `/simplify` on the diff and address findings.

## Phase 4 — Tests
- Add unit tests (service/util/security) **and** MockMvc integration tests for new endpoints,
  covering: happy path, validation failures (400), auth required (401), ownership violations
  (403), not-found (404), and rate-limit (429) where applicable.
- Run the suite: `cd backend && ./mvnw clean test` (Windows: `mvnw.cmd`; set
  `JAVA_HOME` to the JDK 25 path if the wrapper can't launch a JVM). Tests run on H2 — no DB.
- Do not proceed until green.

## Phase 5 — Documentation
- Update `API.md` (new routes: request/response shapes, status codes), `DECISIONS.md` (a new
  numbered section if a non-obvious design choice was made), and `CLAUDE.md` (known
  limitations / follow-ups) as needed.
- Add Javadoc to new classes and public methods — explain the *why*, not the obvious.

## Phase 6 — Pen test (security review)
- Run the `/security-review` skill on the diff, then manually probe the new surface:
  - **Authorization:** can user A reach user B's new resource? (IDOR / missing ownership check)
  - **Input:** injection, oversized payloads, missing validation, mass-assignment.
  - **Secrets:** is anything sensitive logged, returned, or stored unencrypted? Does any new
    response leak existence (enumeration) or internal errors?
  - **Auth/session:** does the route correctly require a JWT? Is it in the right `SecurityConfig`
    matcher? Any way to bypass the rate limiter?
- For each finding: fix it, add a regression test, and note it. Re-run `/security-review` until clean.

## Phase 7 — Stress test
- Stress the new endpoint(s) against a locally running instance
  (`./mvnw spring-boot:run`, default H2). Prefer an installed tool (`k6`, `hey`, `ab`,
  `wrk`); if none is available, write a short concurrent `curl`/PowerShell loop. Put any script
  under `backend/loadtest/` (gitignored if throwaway).
- Capture: throughput, p50/p95/p99 latency, error rate, and **confirm the rate limiter returns
  429 under flood** (and that legitimate traffic below the limit is unaffected).
- Record results in the PR/commit description. Flag any limit that needs tuning in `ratelimit.*`.

## Phase 8 — Ship
- Confirm `./mvnw clean test` is green and the working tree is clean.
- Commit on `dev` with a descriptive message ending in the required `Co-Authored-By` trailer;
  push `dev`.
- Merge into `main` with **`git merge --no-ff dev`** and push. Watch CI to green:
  `gh run watch <id> --exit-status`.
- Summarize: stories delivered, files changed, test count, security findings fixed, stress-test
  numbers, and any follow-up tasks created.

---

**Guardrails:** never commit real secrets (use gitignored `application-{local,docker}.properties`);
never weaken an ownership check or the timing-guard/rate-limit defenses to make a test pass;
if a phase reveals the design is wrong, return to Phase 2 rather than patching forward.
