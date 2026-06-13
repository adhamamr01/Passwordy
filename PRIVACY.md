# Privacy Policy

_Last updated: 2026-06-13_

This Privacy Policy explains what data the **Passwordy** application and backend service (the
"Service"), provided by **Adham Amr** ("we", "us", "our"), collects, why, and your rights over it.
It is written to reflect how the Service actually works.

> **Note.** This document was drafted for the project and is not legal advice. Having it reviewed
> by a qualified lawyer before production launch is recommended.

## 1. Who is responsible
The data controller for the Service is Adham Amr. Contact for any privacy question, request, or
complaint: **adhamamr01@gmail.com**.

## 2. What we collect

**Account data**
- **Email address** — for account verification, password-reset, and security notifications.
- **Username** — your login identifier.
- **Master password** — **never stored in plaintext.** Only a one-way Argon2id hash is kept; we
  cannot read or recover your master password.

**Vault data**
- **Saved credentials** (label, optional username, URL, notes, category, and the password value).
  Password values are **encrypted at rest with AES-256-GCM** (a fresh IV per entry) and are only
  decrypted when you explicitly request them or export your data.

**Security data**
- **Refresh tokens** — only a SHA-256 hash is stored, so a database leak can't be replayed.
- **Two-factor (TOTP) secret** — stored AES-encrypted; one-time recovery codes are stored only as
  hashes.
- **IP address** — processed transiently for rate limiting / abuse prevention; not used to profile
  you.

We do **not** collect location, contacts, advertising identifiers, or analytics/behavioural
tracking, and we do **not** use third-party advertising or analytics SDKs.

## 3. How your master password is checked against breaches
When you set or reset your master password, we check it against the
[Have I Been Pwned](https://haveibeenpwned.com) breach corpus using **k-anonymity**: only the first
5 characters of the password's SHA-1 hash are sent to the HIBP range API — never your password, and
never the full hash. This prevents you from choosing a known-compromised master password.

## 4. How we use data
- To provide the core service (authentication, storing and retrieving your encrypted vault).
- To send transactional email (verification, password reset). We do **not** send marketing email.
- To secure the Service (rate limiting, breached-password screening, two-factor authentication).

We do not sell your data or share it with third parties for their own purposes.

## 5. Service providers (processors)
We rely on a small number of infrastructure providers strictly to operate the Service:
- **Hosting / database provider** — runs the backend and stores the (encrypted) database.
- **Email provider (SMTP)** — delivers transactional email; receives your email address and the
  message contents (verification / reset links).
- **Have I Been Pwned** — receives only a 5-character hash prefix (see §3).

These providers process data on our behalf under their respective terms; they are not permitted to
use it for their own purposes.

## 6. Data security
Data is encrypted in transit (TLS) and vault password values are encrypted at rest (AES-256-GCM).
Master passwords are hashed with Argon2id. Additional safeguards include rate limiting, optional
two-factor authentication, and screen-capture blocking in the mobile app. No method of transmission
or storage is perfectly secure, but we apply industry-standard protections.

## 7. Data retention
We keep your account and vault data for as long as your account exists. When you delete your account
(in-app, or `DELETE /api/account`), we **permanently delete** your account and all associated data
(vault entries, refresh tokens, recovery codes, and outstanding verification tokens). Transient logs
may persist for a short period for security and operational purposes.

## 8. Your rights
Depending on your jurisdiction, you may have the right to access, correct, export, or delete your
data, and to object to or restrict certain processing. The Service supports these directly:
- **Access / portability** — export all of your data as JSON at any time (Settings → Export, or
  `GET /api/account/export`).
- **Erasure** — permanently delete your account and all data (Settings → Delete account, or
  `DELETE /api/account`).
- **Correction** — edit your vault entries in-app.

To exercise any other right, contact **adhamamr01@gmail.com**.

## 9. Children
The Service is a general-audience utility and is not directed to children under 13. We do not
knowingly collect data from children under 13.

## 10. International transfers
Your data may be processed in the country where our hosting and email providers operate. Where
required, we rely on appropriate safeguards for any cross-border transfer.

## 11. Changes to this policy
We may update this policy; material changes will be communicated by email or an in-app notice, and
the "Last updated" date above will change.

## 12. Contact
Privacy questions or requests: **adhamamr01@gmail.com**.
