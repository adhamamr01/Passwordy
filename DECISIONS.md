# Design Decisions

This document records the significant architectural and implementation choices behind
Passwordy, and the reasoning (and trade-offs) behind each. It is meant to answer
"why is it built this way?" for anyone reading or extending the code.

---

## 1. Overall shape: Spring Boot REST API + Android (Compose) client

Passwordy is split into two independently buildable projects:

- **`backend/`** — a stateless Spring Boot 3 REST API that owns all persistence,
  authentication, and cryptography.
- **`frontend/`** — an Android app (Kotlin + Jetpack Compose) that is a pure client of
  that API and holds no long-lived secrets beyond the session JWT.

**Why:** keeping the client "thin" means the security-critical logic (hashing,
encryption, authorization) lives in exactly one place that we control and can audit.
The phone never decrypts anything itself; it asks the server.

---

## 2. Layered backend architecture

The backend follows a strict `Controller → Service → Repository` layering:

| Layer | Responsibility | Example |
|-------|----------------|---------|
| Controller | HTTP concerns only — routing, status codes, auth principal extraction | `PasswordController` |
| Service | Business logic, authorization checks, encryption orchestration | `PasswordServiceImpl` |
| Repository | Data access (Spring Data JPA) | `PasswordRepository` |

**Why:** controllers stay trivially thin and the service layer becomes the single
source of truth for rules like "a user may only touch their own passwords."

**Decision (refactor):** the `POST /api/passwords/{id}/decrypt` endpoint originally
reached into the repository and the encryption service *directly from the controller*,
duplicating the ownership check. It was moved into `PasswordService.decryptPassword`,
so every data path goes through one ownership gate (`findOwnedPassword`). The controller
no longer depends on `PasswordRepository` or `EncryptionService` at all.

---

## 3. Authentication: stateless JWT

- On register/login the server issues a signed JWT (HS256) with the username as subject
  and a **24-hour** expiry (`JwtUtil.JWT_TOKEN_VALIDITY`).
- Every protected request carries `Authorization: Bearer <token>`.
- `JwtAuthenticationFilter` (a `OncePerRequestFilter`) validates the token and populates
  the `SecurityContext`; sessions are disabled
  (`SessionCreationPolicy.STATELESS` in `SecurityConfig`).

**Why stateless:** no server-side session store to scale or invalidate, and the API can
run behind any number of instances without sticky sessions. The token *is* the session.

**Trade-off:** statelessness means we cannot force-expire a single token before its
natural expiry without adding a denylist. Given the 24h lifetime this is acceptable for
now; a refresh-token + denylist scheme is the documented upgrade path.

**Public routes:** `/api/auth/**` plus the two explicit generation routes
(`/api/password/generate` and `/api/password/generate-pin`) are permitted without a token
(generation is a pure utility and needs no identity); everything else — including
`/api/password/categories` — requires authentication. The generation routes are listed
explicitly rather than via a `generate*` wildcard so no unintended path is left open.

---

## 4. Master password storage: BCrypt

Master passwords are never stored or transmitted in a recoverable form. They are hashed
with **BCrypt** (`BCryptPasswordEncoder`) and only the hash is persisted
(`User.masterPasswordHash`).

**Why BCrypt:** adaptive work factor and per-hash salt out of the box, so the cost can be
raised over time as hardware improves. Login compares via `passwordEncoder.matches`,
never by decrypting.

Master-password strength is enforced at registration by `MasterPasswordValidator`
(min length 8, plus upper/lower/digit/special class checks).

---

## 5. Stored-password encryption: AES-256-GCM with a per-entry IV

Saved password values are encrypted at rest by `AESEncryptionService`:

- **Algorithm:** `AES/GCM/NoPadding`, 256-bit key, 128-bit auth tag.
- **Per-entry IV:** a fresh 12-byte random IV is generated for *every* encryption from a
  shared `SecureRandom`. The IV is prepended to the ciphertext and the whole blob is
  Base64-encoded for storage.

**Why GCM:** it is authenticated encryption — decryption fails loudly if the ciphertext
was tampered with, which a plain mode like CBC would not catch.

**Why a per-entry random IV:** reusing an IV under the same key in GCM is catastrophic
(it leaks plaintext relationships and breaks authentication). A fresh IV per record makes
identical plaintexts encrypt to different ciphertexts.

**Why prepend the IV rather than store it separately:** the IV is not secret; it only
needs to be *unique*. Prepending keeps each record self-contained — one Base64 string is
everything `decrypt` needs.

### Decrypt-on-demand model

The list endpoint (`GET /api/passwords`) returns the **still-encrypted** value. The
plaintext is only ever returned by the explicit `POST /api/passwords/{id}/decrypt` call,
after an ownership check.

**Why:** it keeps plaintext out of bulk responses and logs, and makes "reveal password"
an auditable, deliberate action rather than a side effect of loading a list.

---

## 6. Known limitation: crypto keys are currently hardcoded

`AESEncryptionService` and `JwtUtil` currently embed their keys as constants in source.
This is **a deliberate, documented shortcut for development**, not the intended
production posture.

- The gitignored `application-local.properties` / `application-docker.properties` already
  carry `jwt.secret` and `encryption.secret.key` entries, and `SETUP.md` describes
  generating them — the migration path is to read these via `@Value`/`@ConfigurationProperties`
  and remove the constants.
- Until then the constants carry an in-code comment pointing to env/vault as the target.

This is listed here so it is impossible to mistake the hardcoded keys for an oversight.

---

## 7. Error handling: typed exceptions mapped to HTTP status

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to status codes:

| Exception | HTTP status | Meaning |
|-----------|-------------|---------|
| `MethodArgumentNotValidException`, `BadRequestException`, `IllegalArgumentException` | 400 | invalid input (failed `@Valid`, weak master password, duplicate account, bad length) |
| `JwtAuthenticationEntryPoint` (filter), `InvalidCredentialsException` (login) | 401 | missing/invalid/expired JWT, or a failed login |
| `UnauthorizedException` | 403 | authenticated, but not the owner |
| `ResourceNotFoundException` | 404 | user/password not found |
| other `RuntimeException` | 500 | unexpected server error (e.g. crypto failure) |

**Decision (refactor):** previously *every* `RuntimeException` was mapped to 404; introducing
typed exceptions lets clients distinguish the cases above. **Input validation** uses Bean
Validation (`@Valid` on request DTOs with `jakarta.validation` constraints), so malformed
requests return a clean **400** with field messages instead of a 500. Weak-master-password and
duplicate-account failures throw `BadRequestException` (400) rather than a bare `RuntimeException`.

**Login** returns **401** for both an unknown username and a wrong password, via
`InvalidCredentialsException` carrying one generic message — the API never reveals which
field was wrong, so it can't be used to enumerate accounts.

---

## 8. Data model

Two JPA entities: `User` and `Password`, with a `@ManyToOne` from `Password` to `User`
(lazy-loaded). Ownership queries go through `PasswordRepository.findByUserUsername`.

**Decision (refactor):** `getAllPasswords` originally did `findAll()` then filtered by
user in Java — an O(all rows) full-table scan that leaks across users if the filter is
ever dropped. It now uses a derived `findByUserUsername` query so the database does the
filtering.

**Timestamps:** `createdAt` is set in `@PrePersist`; `updatedAt` is set **only** in
`@PreUpdate`. A freshly created row therefore has a null `updatedAt`, which truthfully
means "never modified since creation" and keeps `updatedAt` meaningful for audit queries.

---

## 9. Frontend: MVVM + Repository

The Android app uses **MVVM**: Compose screens observe `StateFlow`s exposed by
`ViewModel`s, which call `Repository` classes that wrap the Retrofit `ApiService`.

- **UI state as sealed classes:** each operation has an explicit state type
  (`Loading / Success / Error`, e.g. `PasswordUiState`, `AuthUiState`). Screens
  `when`-match on it, so loading spinners and error text are driven by one source of truth
  instead of scattered booleans.
- **Token storage:** the JWT is persisted with Jetpack **DataStore** (`TokenManager`),
  which is async and survives process death, rather than `SharedPreferences`.

### Frontend decisions made during cleanup

- **In-memory token cache:** `PasswordRepository` caches the token after the first
  DataStore read instead of reading from disk before every API call;
  `clearTokenCache()` is the logout hook.
- **`Response.unwrap` helper:** the repeated `isSuccessful && body != null` unwrap pattern
  was collapsed into one extension function so each repository method is a single line.
- **Optimistic in-place list updates:** after save/update/delete the ViewModel mutates the
  already-loaded `PasswordUiState.Success` list directly instead of firing a second
  `GET /api/passwords`. Save uses the entity returned by the server; delete filters by id.
  This removes a redundant network round-trip per mutation.
- **Shared coroutine helpers:** `AuthViewModel.authenticate` and
  `PasswordViewModel.persistPassword` factor out the identical
  `Loading → try → Success/Error` boilerplate that `login`/`register` and
  `save`/`update` otherwise duplicated.

### Frontend networking note

`RetrofitInstance.BASE_URL` defaults to `http://10.0.2.2:8080/`, which is how the Android
**emulator** reaches the host machine's `localhost`. On a physical device this must be
changed to the host's LAN IP.

---

## 10. Persistence profiles

- **Default (`application.properties`):** in-memory **H2** — zero-setup, and what the test
  suite runs against; data is wiped on restart. The H2 console is *not* reachable: the
  security config authenticates every non-public route, so the console route is blocked.
- **`docker` profile (recommended for real runs):** **PostgreSQL** via
  `backend/docker-compose.yml` (`docker compose up -d`), configured through the gitignored
  `application-docker.properties` (copy from the committed `.example`; see `SETUP.md`).
- **`local` profile:** **PostgreSQL** against a natively-installed server, via the gitignored
  `application-local.properties`.

**Tests run on in-memory H2** — `mvn test` / `clean install` need no database or Docker. A
Testcontainers-backed PostgreSQL test was prototyped but reverted: the docker-java client in
current Testcontainers (≤1.21.4) can't negotiate with Docker Engine 29.x. Revisit once
Testcontainers supports Docker 29; until then, PostgreSQL parity is exercised by running the
app under the `docker`/`local` profiles.

> The PostgreSQL JDBC driver (`org.postgresql:postgresql`) was previously missing from
> `pom.xml` — the `docker`/`local` profiles could not actually have connected without it. It
> was added (runtime scope) alongside the Testcontainers work.
