# Passwordy API Reference

Base URL (default dev): `http://localhost:8080`
All routes are prefixed with `/api`. Request and response bodies are JSON.

> **Android emulator note:** the app reaches the host via `http://10.0.2.2:8080/`
> (see `RetrofitInstance.BASE_URL`). On a physical device, use the host's LAN IP.

---

## Authentication

Protected endpoints require a JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens are returned by register/login and are valid for **24 hours**.

### Which endpoints are public?

| Access | Routes |
|--------|--------|
| **Public** (no token) | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/password/generate`, `POST /api/password/generate-pin` |
| **Authenticated** | everything else, including `GET /api/password/categories` and all `/api/passwords` routes |

> Note: only the two explicit generation routes are public; `GET /api/password/categories`
> requires a token. The Android client sends its bearer token on this call (and keeps a
> built-in category list as a fallback if the request fails).

---

## Status codes

Errors are returned as `{ "error": "<message>" }` and mapped by `GlobalExceptionHandler`:

| Status | When |
|--------|------|
| `200 OK` | successful read/update/login, or email verification |
| `201 Created` | successful password save |
| `202 Accepted` | registration accepted — generic acknowledgement; a verification email is sent |
| `204 No Content` | successful delete |
| `400 Bad Request` | invalid input — failed validation (`@Valid`), weak master password, duplicate username/email, or out-of-range generation length |
| `401 Unauthorized` | missing/invalid/expired JWT (`JwtAuthenticationEntryPoint`), or a failed login — wrong username **or** password |
| `403 Forbidden` | reserved for ownership/authorization failures (`UnauthorizedException`); **not** used by the password routes, which return `404` for non-owned records (see below) |
| `404 Not Found` | user or password does not exist — **or** a password exists but is owned by another user (`ResourceNotFoundException`). A password you don't own is reported identically to one that doesn't exist, so the `/api/passwords/{id}` routes can't be used to enumerate which ids exist across accounts |
| `500 Internal Server Error` | unexpected server errors (e.g. encryption/decryption failure) — returns a generic message; details are logged server-side |

> Request bodies are validated with Bean Validation: `register` requires a non-blank username,
> a valid `email`, and a non-blank `masterPassword`; `passwords` requires non-blank `label`,
> `password`, and `category`; generation length must be ≥8 (password) or 4–12 (PIN).
> A failed `login` (unknown username or wrong password) returns **401** with one generic
> message, so it can't be used to tell whether a username exists.

---

## Auth endpoints

### `POST /api/auth/register`
Begin registration. Master password must be ≥8 chars and contain upper, lower, digit, and
special characters. **No token is returned** — the account is created disabled and a
verification link is emailed; the user must verify before logging in.

**Request**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "masterPassword": "Str0ng!Pass"
}
```

**Response `202 Accepted`** — always the same generic acknowledgement, whether or not the
username/email was already taken (so the endpoint can't be used to discover existing accounts):
```json
{ "message": "If that username and email are available, a verification link has been sent. Please check your inbox." }
```
`400` if the master password fails the strength rules, **or** has appeared in a known data breach
(checked against Have I Been Pwned via k-anonymity; the check fails open if HIBP is unreachable).

---

### `GET /api/auth/verify?token=<token>`
Verify an account using the token from the email link. Enables the account and consumes the token.

**Response `200 OK`**
```json
{ "message": "Email verified. You can now log in." }
```
`400` if the token is unknown or expired.

---

### `POST /api/auth/login`
Authenticate with username + master password.

**Request**
```json
{ "username": "alice", "masterPassword": "Str0ng!Pass" }
```

**Response `200 OK`** — an `AuthResponse`: `{ token, refreshToken, username, email, message }`.
`token` is a **short-lived access JWT** (~15 min); `refreshToken` is a long-lived opaque token.
`401` for an unknown username or wrong password (one generic message). `403` if the credentials
are correct but the email isn't verified yet (*"Please verify your email before logging in"*) —
returned only after a correct password, so it doesn't reveal whether an account exists.

---

### `POST /api/auth/refresh`
Exchange a valid refresh token for a new access token. The refresh token is **rotated** — the
presented one is consumed and a new one returned, so a replayed token is rejected.
**Request:** `{ "refreshToken": "<token>" }`. **Response `200 OK`** — a new `AuthResponse`
(new `token` + new `refreshToken`). `401` if the refresh token is unknown/expired/already used.

### `POST /api/auth/logout`
Revoke a refresh token (ends that session). **Request:** `{ "refreshToken": "<token>" }`.
**Response `200 OK`** — `{ "message": "Logged out." }`. A password reset revokes *all* of a
user's refresh tokens.

---

## Two-factor authentication (TOTP)

Opt-in. When a user has 2FA enabled, **login returns a challenge instead of tokens**:
`{ "twoFactorRequired": true, "twoFactorToken": "<5-min token>", ... }` (no `token`/`refreshToken`).

### `POST /api/auth/2fa/verify`  *(public — login step 2)*
Complete login. **Request:** `{ "twoFactorToken": "<from login>", "code": "<6-digit TOTP or recovery code>" }`.
**Response `200 OK`** — a normal `AuthResponse` with access + refresh tokens. `401` on a bad/expired code.

### `POST /api/account/2fa/setup`  *(auth required)*
Begin enrollment. **Response `200 OK`** — `{ "secret": "<base32>", "otpauthUri": "otpauth://..." }`
(render the URI as a QR or enter the secret manually). Not active until enabled.

### `POST /api/account/2fa/enable`  *(auth required)*
Confirm enrollment. **Request:** `{ "code": "<6-digit>" }`. **Response `200 OK`** —
`{ "recoveryCodes": ["...", ...], "message": "..." }` (the codes are shown **once**; only hashes
are stored). `400` on a wrong code.

### `POST /api/account/2fa/disable`  *(auth required)*
**Request:** `{ "code": "<6-digit>" }`. Disables 2FA and clears recovery codes. `400` on a wrong code.

---

### `POST /api/auth/forgot-password`
Start a password reset. **Request:** `{ "email": "alice@example.com" }`.
**Response `202 Accepted`** — always a generic ack (it never reveals whether the email is
registered); if the account exists, a reset token is emailed.

### `POST /api/auth/reset-password`
Complete a reset with the emailed token and a new master password (must meet the strength rules).
**Request:** `{ "token": "<reset-token>", "newPassword": "NewStr0ng!Pass" }`.
**Response `200 OK`** — `{ "message": "Your master password has been reset. You can now log in." }`.
`400` if the token is unknown/expired/not a reset token, or the new password is too weak.

### `POST /api/auth/resend-verification`
Re-send the verification email for an unverified account. **Request:** `{ "email": "..." }`.
**Response `202 Accepted`** — generic ack (same enumeration-safe shape as forgot-password).

---

## Generation endpoints (public)

### `POST /api/password/generate`
Generate a random password. Guarantees at least one upper, one lower, one digit
(and one symbol when `includeSymbols` is true).

**Request** (fields optional; defaults shown)
```json
{ "length": 16, "includeSymbols": true }
```
`length` must be ≥ 8.

**Response `200 OK`**
```json
{ "password": "aZ3!kP9xQ2mL7wTr" }
```

---

### `POST /api/password/generate-pin`
Generate a numeric PIN.

**Request** (default shown)
```json
{ "length": 6 }
```
`length` must be between 4 and 12 inclusive.

**Response `200 OK`**
```json
{ "pin": "048213" }
```

---

### `GET /api/password/categories` *(auth required)*
Returns the built-in category labels.

**Response `200 OK`**
```json
["Social Media", "Banking", "Email", "Work", "Shopping", "Entertainment", "Other"]
```

---

## Password endpoints *(auth required)*

The `PasswordResponse` object returned by these endpoints:

```json
{
  "id": 1,
  "label": "GitHub",
  "value": "<Base64 AES-GCM ciphertext>",
  "username": "alice",
  "url": "https://github.com",
  "notes": "work account",
  "category": "Work",
  "favorite": false,
  "createdAt": "2026-06-01T10:15:30",
  "updatedAt": null
}
```

> `value` is the **encrypted** password. Plaintext is only returned by the `/decrypt`
> endpoint. `favorite` indicates whether the user has starred the entry. `updatedAt` is
> `null` until the entry is first edited.

### `GET /api/passwords`
List passwords owned by the authenticated user. Returns `200 OK` with an array of
`PasswordResponse` (encrypted values).

**Query parameters**

| Param | Default | Effect |
|-------|---------|--------|
| `favoritesOnly` | `false` | When `true`, returns only the caller's favorite entries. |

### `POST /api/passwords`
Create a password. The `password` field is encrypted server-side before storage.

**Request (`PasswordSaveRequest`)**
```json
{
  "label": "GitHub",
  "password": "myPlaintextSecret",
  "username": "alice",
  "url": "https://github.com",
  "notes": "work account",
  "category": "Work"
}
```
**Response `201 Created`** — the created `PasswordResponse`.

### `GET /api/passwords/{id}`
Fetch a single password (encrypted `value`).
`404` if it doesn't exist **or** belongs to another user (the two are indistinguishable, by design).

### `PUT /api/passwords/{id}`
Update a password. Same body as `POST /api/passwords`; the new `password` is re-encrypted.
**Response `200 OK`** — the updated `PasswordResponse`.

### `PUT /api/passwords/{id}/favorite`
Mark or unmark a password as a favorite, after verifying ownership.

**Request (`FavoriteRequest`)**
```json
{ "favorite": true }
```
`favorite` is required (omitting it returns `400`).

**Response `200 OK`** — the updated `PasswordResponse` (with the new `favorite` value).
`404` if not found or not owned by the caller.

### `DELETE /api/passwords/{id}`
Delete a password. **Response `204 No Content`.**
`404` if not found or not owned by the caller.

### `POST /api/passwords/{id}/decrypt`
Decrypt and return the plaintext password, after verifying ownership.

**Response `200 OK`**
```json
{ "password": "myPlaintextSecret" }
```
`404` if not found or not owned by the caller, `500` if decryption fails.

---

## Example session (curl)

```bash
# 1. Register (returns a token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","masterPassword":"Str0ng!Pass"}' \
  | jq -r .token)

# 2. Save a password
curl -X POST http://localhost:8080/api/passwords \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"label":"GitHub","password":"hunter2!","username":"alice","category":"Work"}'

# 3. List (encrypted values)
curl http://localhost:8080/api/passwords -H "Authorization: Bearer $TOKEN"

# 4. Reveal one
curl -X POST http://localhost:8080/api/passwords/1/decrypt -H "Authorization: Bearer $TOKEN"
```
