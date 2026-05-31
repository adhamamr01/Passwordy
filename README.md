# Passwordy — Secure Password Manager

A multi-user password manager with a Spring Boot REST API and an Android (Jetpack Compose)
client. Master passwords are hashed with BCrypt; stored passwords are encrypted server-side
with AES-256-GCM and only decrypted on explicit request.

## ✨ Features

- Multi-user accounts with JWT-based authentication (24-hour tokens)
- Stored passwords encrypted at rest (AES-256-GCM, unique IV per entry)
- Decrypt-on-demand: list views never expose plaintext
- Strong random password generator (guaranteed character mix) and numeric PIN generator
- Per-user isolation — every read/write is checked for ownership
- Organize entries by category, with label, username, URL, and notes
- Android app: login/register, list, detail with reveal/copy, add/edit, generate

## 🧱 Tech Stack

**Backend**
- Java 17+, Spring Boot 3.x
- Spring Security + JWT (jjwt)
- Spring Data JPA / Hibernate
- H2 (default, in-memory) or PostgreSQL (local/docker profiles)
- Maven

**Frontend**
- Kotlin, Jetpack Compose (Material 3)
- MVVM + Repository, Coroutines + StateFlow
- Retrofit + OkHttp, Gson
- DataStore (token persistence)

## 🏗️ Architecture (at a glance)

```
Android app (Compose)                 Spring Boot API
┌──────────────────────────┐          ┌───────────────────────────────────────┐
│ Screen → ViewModel        │  HTTPS   │ Controller → Service → Repository → DB │
│   → Repository → ApiService├─────────▶│            (JWT filter, BCrypt, AES)   │
│   (StateFlow UI states)   │  Bearer  │                                         │
└──────────────────────────┘   JWT    └───────────────────────────────────────┘
```

- **Backend** is strictly layered: controllers handle HTTP only, services own business
  logic and authorization, repositories handle persistence.
- **Frontend** is MVVM: Compose screens observe `StateFlow`s from ViewModels, which call
  repositories wrapping a Retrofit `ApiService`.

See **[DECISIONS.md](DECISIONS.md)** for the reasoning behind these choices.

## 📁 Project Structure

```
Passwordy/
├── backend/                        Spring Boot REST API
│   └── src/main/java/com/adhamamr/passwordy/
│       ├── controller/             HTTP endpoints
│       ├── service/                business logic, encryption, auth
│       ├── repository/             Spring Data JPA repositories
│       ├── model/                  JPA entities (User, Password)
│       ├── dto/                    request/response objects
│       ├── security/               JWT util, filter, user details
│       ├── config/                 Spring Security configuration
│       ├── exception/              typed exceptions + global handler
│       └── util/                   master-password validator
├── frontend/                       Android app (Kotlin + Compose)
│   └── app/src/main/java/com/adhamamr/passwordy/
│       ├── ui/                     screens, viewmodels, navigation, theme
│       └── data/                   network (Retrofit), repositories, local (DataStore)
├── README.md                       this file
├── DECISIONS.md                    architecture & design rationale
├── API.md                          full HTTP API reference
└── SETUP.md                        environment setup (DB, secrets, build)
```

## 🚀 Quick Start

### Backend (default H2 profile — zero setup)
```bash
cd backend
./mvnw spring-boot:run
```
The API starts on `http://localhost:8080` with an in-memory H2 database
(console at `/h2-console`). Data resets on restart.

For PostgreSQL (local/docker profiles) and the required secret config, see
**[SETUP.md](SETUP.md)**.

### Frontend (Android)
Open `frontend/` in Android Studio and run the app on an emulator. The client targets
`http://10.0.2.2:8080/` (the emulator's alias for the host's `localhost`); change
`RetrofitInstance.BASE_URL` to your machine's LAN IP for a physical device.

## 🔒 Security Model

- **Master passwords:** BCrypt-hashed, never stored or returned in recoverable form.
- **Stored passwords:** AES-256-GCM with a fresh random IV per entry; ciphertext is
  Base64-encoded with the IV prepended.
- **Authorization:** every password operation verifies the entry belongs to the
  authenticated user (404 if missing, 403 if not yours).
- **Transport:** the JWT is sent as a `Bearer` token; sessions are stateless.

> ⚠️ **Development limitation:** the AES and JWT keys are currently hardcoded constants in
> `AESEncryptionService` / `JwtUtil`. Externalizing them to config/secrets is the
> documented next step — see [DECISIONS.md](DECISIONS.md) §6.

## 📚 API

Full reference with request/response shapes and status codes: **[API.md](API.md)**.

Quick map:

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | `/api/auth/register` | — | Create account, get token |
| POST | `/api/auth/login` | — | Log in, get token |
| POST | `/api/password/generate` | — | Generate a password |
| POST | `/api/password/generate-pin` | — | Generate a PIN |
| GET | `/api/password/categories` | ✓ | List category labels |
| GET | `/api/passwords` | ✓ | List my passwords (encrypted) |
| POST | `/api/passwords` | ✓ | Save a password |
| GET | `/api/passwords/{id}` | ✓ | Get one (encrypted) |
| PUT | `/api/passwords/{id}` | ✓ | Update one |
| DELETE | `/api/passwords/{id}` | ✓ | Delete one |
| POST | `/api/passwords/{id}/decrypt` | ✓ | Reveal plaintext |

## 📖 Documentation

- **[DECISIONS.md](DECISIONS.md)** — architecture and design rationale
- **[API.md](API.md)** — HTTP API reference
- **[SETUP.md](SETUP.md)** — environment / database / secrets setup
