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
| `200 OK` | successful read/update/login |
| `201 Created` | successful register or password save |
| `204 No Content` | successful delete |
| `401 Unauthorized` | missing/invalid/expired JWT on a protected route (`JwtAuthenticationEntryPoint`) |
| `403 Forbidden` | authenticated user is not the owner of the resource (`UnauthorizedException`) |
| `404 Not Found` | user or password does not exist (`ResourceNotFoundException`) |
| `500 Internal Server Error` | other server errors — encryption/decryption failure, invalid generation length, duplicate username/email, wrong password on login |

> The 500 cases for validation/duplicate/wrong-password are a known rough edge: they are
> thrown as plain `RuntimeException` and so are not mapped to 4xx. See `DECISIONS.md` §7.

---

## Auth endpoints

### `POST /api/auth/register`
Register a new user and receive a JWT. Master password must be ≥8 chars and contain
upper, lower, digit, and special characters.

**Request**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "masterPassword": "Str0ng!Pass"
}
```

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice",
  "email": "alice@example.com",
  "message": "User registered successfully"
}
```

---

### `POST /api/auth/login`
Authenticate with username + master password.

**Request**
```json
{ "username": "alice", "masterPassword": "Str0ng!Pass" }
```

**Response `200 OK`** — same `AuthResponse` shape as register, with
`"message": "Login successful"`.

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
  "createdAt": "2026-06-01T10:15:30",
  "updatedAt": null
}
```

> `value` is the **encrypted** password. Plaintext is only returned by the `/decrypt`
> endpoint. `updatedAt` is `null` until the entry is first edited.

### `GET /api/passwords`
List all passwords owned by the authenticated user. Returns `200 OK` with an array of
`PasswordResponse` (encrypted values).

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
`404` if it doesn't exist, `403` if it belongs to another user.

### `PUT /api/passwords/{id}`
Update a password. Same body as `POST /api/passwords`; the new `password` is re-encrypted.
**Response `200 OK`** — the updated `PasswordResponse`.

### `DELETE /api/passwords/{id}`
Delete a password. **Response `204 No Content`.**
`404` if not found, `403` if not the owner.

### `POST /api/passwords/{id}/decrypt`
Decrypt and return the plaintext password, after verifying ownership.

**Response `200 OK`**
```json
{ "password": "myPlaintextSecret" }
```
`404` if not found, `403` if not the owner, `500` if decryption fails.

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
