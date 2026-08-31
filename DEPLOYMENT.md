# Deployment guide

How to take the Passwordy backend to production. The app is a stateless Spring Boot service; all
secrets and connection details are supplied via environment variables, so the same image runs in
any environment.

> **Status:** the image, migrations, health checks, rate-limit store, and a production compose file
> are in the repo. The remaining steps below require resources only you can provision — a host,
> a managed database, real secrets, and an SMTP account.
>
> Two paths through the rest of this guide: **§0** is a concrete, genuinely $0 stack (self-hosted
> on a free-forever VM). **§1 onward** is the generic version for any host, including paid managed
> services, if you'd rather use those instead.

## 0. $0 deployment path

Every piece below is free indefinitely, not a time-limited trial — chosen specifically to avoid
the two usual traps (a host that sleeps/cold-starts, or a "free" DB that gets deleted after 30
days). Nothing here needs a credit card to be charged; where a provider asks for one, it's for
identity verification only.

| Piece | Free option used | Why not the usual alternative |
|---|---|---|
| Compute | Oracle Cloud "Always Free" Ampere A1 VM | Render/Railway/Fly free tiers sleep, cold-start, or expire; Oracle's is free forever with real always-on resources (up to 4 cores / 24 GB RAM) |
| Domain + TLS | DuckDNS (free subdomain) + Caddy (free automatic Let's Encrypt cert) | No domain purchase needed; Caddy gets HTTPS with ~5 lines of config |
| Database | Self-hosted PostgreSQL (already in `docker-compose.prod.yml`) | Managed free-tier Postgres (Supabase, etc.) tends to pause or cap storage aggressively |
| Cache / rate-limit store | Self-hosted Redis (already in the compose file) | Same box, no extra cost |
| Image registry | GHCR (already wired in CI — task #6) | Free for the images this repo publishes |
| SMTP | Brevo or Mailjet free tier (300/day and 200/day respectively, free forever) | Real transactional-email providers with a permanent free tier, not a trial |
| Secrets | `openssl rand` | Free regardless of host |
| Crash reporting | Sentry free developer tier | Already wired (task #16); free at this app's likely volume |

**What this costs you instead of money:** you own uptime, security patching, and backups yourself
(no managed provider doing it for you), and a single VM is a single point of failure. That's the
right trade for a pre-revenue/personal-scale launch; revisit it if the user base grows.

### 0.1 Provision the VM
1. Create an [Oracle Cloud](https://www.oracle.com/cloud/free/) account (needs a card for identity
   verification; the Always Free resources are never billed unless you deliberately provision paid
   ones alongside them).
2. Create a Compute instance: shape **VM.Standard.A1.Flex** (Ampere/Arm, Always Free — 2–4 OCPUs /
   12–24 GB RAM is available free), image **Ubuntu 24.04**. If A1 capacity shows unavailable in
   your home region, retry later or try another Always-Free-eligible region.
3. In the instance's attached security list (or the simpler VCN "default" security list), open
   ingress on **80** and **443** (and 22 for SSH, restricted to your IP if possible).
4. SSH in and install Docker:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker $USER    # log out/in after this
   sudo apt install -y docker-compose-plugin
   ```

### 0.2 Point a free domain at it
1. Create a free subdomain at [duckdns.org](https://www.duckdns.org) (e.g. `passwordy.duckdns.org`)
   pointed at the VM's public IP.
2. DuckDNS offers a small script to keep the IP updated if it ever changes (Oracle Always Free
   instances get a stable public IP by default, so this is usually a one-time set-and-forget).

### 0.3 TLS via Caddy (automatic, free Let's Encrypt certs)
Add a `caddy` service to the front of the stack — simplest as an addition to
`docker-compose.prod.yml` on the VM (not committed to the repo, since it's host-specific):
```yaml
  caddy:
    image: caddy:2-alpine
    container_name: passwordy-caddy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - passwordy-caddy-data:/data
    depends_on:
      - app
    restart: unless-stopped
```
and a `Caddyfile` next to it on the VM:
```
passwordy.duckdns.org {
    reverse_proxy app:8080
}
```
Caddy handles the ACME HTTP challenge and certificate renewal automatically — no certbot, no
manual renewal cron. Add `passwordy-caddy-data:` under the compose file's `volumes:` so the
certificate persists across restarts.

### 0.4 Deploy the stack
Same as §2 below, run on the VM:
```bash
git clone https://github.com/adhamamr01/Passwordy.git   # or just copy backend/
cd Passwordy/backend
cp .env.prod.example .env.prod        # fill in secrets (§4) and SMTP (§0.5)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```
Set `APP_BASE_URL=https://passwordy.duckdns.org` in `.env.prod`.

### 0.5 Free SMTP
Sign up for [Brevo](https://www.brevo.com) (300 emails/day, free forever, no card required) or
[Mailjet](https://www.mailjet.com) (200/day free forever). Both give you SMTP host/port +
an API-key-as-password you drop straight into `SMTP_HOST/PORT/USERNAME/PASSWORD` in `.env.prod`.
Caveat: full SPF/DKIM/DMARC control needs a domain you own — DuckDNS won't give you that, so
deliverability is a notch below a custom-domain setup, but mail still sends.

### 0.6 Backups without a managed DB
Cron a nightly dump on the VM (the vault is already encrypted at rest, so a leaked dump is
ciphertext, not plaintext — still keep it non-public):
```bash
# /etc/cron.d/passwordy-backup
0 3 * * * root docker exec passwordy-postgres-prod pg_dump -U $POSTGRES_USER $POSTGRES_DB | gzip > /root/backups/passwordy-$(date +\%F).sql.gz
```
Add a cleanup step to prune backups older than ~2 weeks so the VM's free block storage doesn't
fill up. True off-site backup (e.g. rsync to Backblaze B2's free 10 GB tier) is a good next step
once this matters more, but isn't required to launch.

### 0.7 The one cost this doesn't remove
Everything above is $0. **Google Play's developer registration is a separate, unavoidable
one-time $25 fee** — that's Google's charge, not an infrastructure cost, and there's no free path
onto the Play Store itself. If $0 has to be absolute end-to-end, the alternative is distributing a
signed APK directly (sideload) or publishing via **F-Droid** (free, but requires the build to be
fully open-source/reproducible, which would mean reconsidering the Sentry dependency).

---

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
For a $0 setup, see **§0** above. Otherwise, run the image on any container host (a VPS with
Docker, Fly.io, Render, Railway, AWS ECS, GKE/EKS, etc.). Two hard requirements:
- **TLS.** Terminate HTTPS at a reverse proxy / load balancer (Caddy, nginx, or the platform's
  built-in TLS) in front of the app. The Android release build talks only to HTTPS.
- **Health probe.** Point the platform's health check at `GET /actuator/health` (public, returns
  `{"status":"UP"}`). Use `/actuator/health/liveness` and `/actuator/health/readiness` for
  orchestrators.

Once the host + cert exist, also **pin the Android release build to it**: copy
`frontend/cert-pins.properties.example` to `frontend/cert-pins.properties`, compute the primary
pin from the live cert (command in the example file) plus a *different* backup pin for the next
cert you'll rotate to, and rebuild the release. Without `cert-pins.properties` the app is
unpinned — TLS still applies, just not certificate pinning.

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
For a $0 setup (self-hosted Postgres + cron backups), see **§0.4/§0.6** above.
- Otherwise, use a **managed PostgreSQL 16** instance (or the compose `postgres` service for small setups).
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
For a $0 setup (Brevo/Mailjet free tier), see **§0.5** above. Otherwise, verification and
password-reset emails require a working SMTP provider (SendGrid, Mailgun, Amazon SES, Postmark,
…). Set `SMTP_HOST/PORT/USERNAME/PASSWORD`, `APP_MAIL_FROM`, and `APP_BASE_URL`.
For deliverability, configure **SPF**, **DKIM**, and a **DMARC** record for your sending domain so
verification mail isn't dropped or spam-filed.

## 7. Rate limiting at scale (task #7)
The rate-limit store is pluggable. For a **single instance** the default in-memory store is fine.
Running **more than one instance** (behind a load balancer) requires a shared store so buckets are
consistent — set `RATELIMIT_STORE=redis` and `RATELIMIT_REDIS_URL` (the prod compose wires a Redis
service and sets these for you). Also set `ratelimit.trusted-proxies` to your load balancer IPs so
`X-Forwarded-For` client IPs are honored for IP-keyed limits.

## 8. Pre-launch checklist
_(Items marked **$0** are satisfied by the §0 free-tier path.)_
- [ ] HTTPS terminating in front of the app; `APP_BASE_URL` set to the public URL **($0: Caddy + DuckDNS, §0.2–0.3)**
- [ ] Android release `BASE_URL` points at the production host
- [ ] `frontend/cert-pins.properties` set from the live production cert (§3) and release rebuilt
- [ ] `frontend/sentry.properties` set from a provisioned Sentry project (crash/ANR reporting;
      see `sentry.properties.example`) and release rebuilt
- [ ] Fresh `JWT_SECRET` + `ENCRYPTION_SECRET_KEY` set and backed up **($0 either way — `openssl rand`)**
- [ ] Postgres with backups running; app starts (Flyway migrate + validate OK) **($0: self-hosted + cron `pg_dump`, §0.4/0.6)**
- [ ] SMTP configured; a test verification email arrives **($0: Brevo/Mailjet free tier, §0.5 — full SPF/DKIM/DMARC needs an owned domain)**
- [ ] `RATELIMIT_STORE=redis` if running multiple instances **($0: self-hosted Redis is already in the compose stack)**
- [ ] Health probe wired to `/actuator/health`; error tracking/alerting in place **($0: Sentry free tier, already wired — task #16)**
- [ ] If targeting the Play Store: the one cost that isn't $0 — the **$25 one-time Google Play registration fee** (§0.7); the sideload/F-Droid route avoids even that
