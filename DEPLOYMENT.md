# Deployment guide

How to take the Passwordy backend to production. The app is a stateless Spring Boot service; all
secrets and connection details are supplied via environment variables, so the same image runs in
any environment.

> **Status:** the image, migrations, health checks, rate-limit store, and a production compose file
> are in the repo. The remaining steps below require resources only you can provision — a host,
> a managed database, real secrets, and an SMTP account.

## 1. Container image (done — task #6)
On every push to `main`, CI publishes the image to GitHub Container Registry:
`ghcr.io/adhamamr01/passwordy/backend:latest` (and `:<sha>`). To build locally instead:

```bash
cd backend
docker build -t passwordy-backend:local .
```

## 2. Quick start with docker-compose (app + Postgres + Redis)
```bash
cd backend
cp .env.prod.example .env.prod          # then fill in real values (see §4–§6)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```
This brings up the backend, PostgreSQL, and Redis. For anything beyond a single small host, prefer
a **managed** Postgres and Redis (see below) over the self-hosted containers.

## 3. Hosting + TLS (task #1)
Run the image on any container host (a VPS with Docker, Fly.io, Render, Railway, AWS ECS, GKE/EKS,
etc.). Two hard requirements:
- **TLS.** Terminate HTTPS at a reverse proxy / load balancer (Caddy, nginx, or the platform's
  built-in TLS) in front of the app. The Android release build talks only to HTTPS.
- **Health probe.** Point the platform's health check at `GET /actuator/health` (public, returns
  `{"status":"UP"}`). Use `/actuator/health/liveness` and `/actuator/health/readiness` for
  orchestrators.

Set the app's public URL via `APP_BASE_URL` (used to build email links) and update the Android
release `BASE_URL` (`frontend/app/build.gradle.kts`) to match.

## 4. Secrets (task #4)
Generate fresh secrets and put them in `.env.prod` (never commit them):
```bash
openssl rand -base64 64   # JWT_SECRET
openssl rand -base64 32   # ENCRYPTION_SECRET_KEY  (AES-256 vault key)
```
- **`ENCRYPTION_SECRET_KEY` is permanent.** Rotating it makes all existing encrypted vault entries
  undecryptable — set it once, before real users exist, and keep it backed up securely.
- Prefer your platform's secret manager (Docker/Compose secrets, AWS Secrets Manager, etc.) over a
  plaintext `.env.prod` where available.

## 5. Database + backups (task #2)
- Use a **managed PostgreSQL 16** instance (or the compose `postgres` service for small setups).
- Point the app at it with `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.
- **Schema** is applied automatically by Flyway on startup (`db/migration/postgresql`); Hibernate
  runs in `validate` mode and will refuse to start if the schema doesn't match the entities.
- **Backups:** enable automated daily snapshots on the managed instance, or schedule `pg_dump`:
  ```bash
  pg_dump "$DATABASE_URL" | gzip > passwordy-$(date +%F).sql.gz
  ```
  Test a restore periodically. The vault is encrypted at rest, so backups contain ciphertext — but
  still protect them.

## 6. Email / SMTP (task #5)
Verification and password-reset emails require a working SMTP provider (SendGrid, Mailgun, Amazon
SES, Postmark, …). Set `SMTP_HOST/PORT/USERNAME/PASSWORD`, `APP_MAIL_FROM`, and `APP_BASE_URL`.
For deliverability, configure **SPF**, **DKIM**, and a **DMARC** record for your sending domain so
verification mail isn't dropped or spam-filed.

## 7. Rate limiting at scale (task #7)
The rate-limit store is pluggable. For a **single instance** the default in-memory store is fine.
Running **more than one instance** (behind a load balancer) requires a shared store so buckets are
consistent — set `RATELIMIT_STORE=redis` and `RATELIMIT_REDIS_URL` (the prod compose wires a Redis
service and sets these for you). Also set `ratelimit.trusted-proxies` to your load balancer IPs so
`X-Forwarded-For` client IPs are honored for IP-keyed limits.

## 8. Pre-launch checklist
- [ ] HTTPS terminating in front of the app; `APP_BASE_URL` set to the public URL
- [ ] Android release `BASE_URL` points at the production host
- [ ] Fresh `JWT_SECRET` + `ENCRYPTION_SECRET_KEY` set and backed up
- [ ] Managed Postgres with automated backups; app starts (Flyway migrate + validate OK)
- [ ] SMTP configured with SPF/DKIM/DMARC; a test verification email arrives
- [ ] `RATELIMIT_STORE=redis` if running multiple instances
- [ ] Health probe wired to `/actuator/health`; error tracking/alerting in place
