# Terms of Service

_Last updated: 2026-06-13_

These Terms of Service ("Terms") govern your use of the **Passwordy** application and backend
service (together, the "Service"), provided by **Adham Mohamed Amr Farouk Aboelela** ("we", "us",
"our"). By creating an
account or using the Service you agree to these Terms. If you do not agree, do not use the Service.

> **Note.** This document was drafted for the project and is not legal advice. Having it reviewed
> by a qualified lawyer before production launch is recommended.

## 1. The Service
Passwordy is a personal password manager. It lets you store, generate, and retrieve credentials.
Stored passwords are encrypted on the server (AES-256-GCM) and your master password is never stored
in plaintext (it is hashed with Argon2id). See the [Privacy Policy](PRIVACY.md) for how data is
handled.

## 2. Your account
- You must provide a valid email address and verify it before logging in.
- You are responsible for keeping your master password secret. **We cannot recover it for you** —
  if you lose it, you can reset it via your verified email, which begins a new vault.
- The Service is a general-audience utility and is not directed to children under 13. You must be
  old enough to form a binding contract in your jurisdiction (or have your parent/guardian's
  consent).
- One person or entity per account; do not share credentials to the Service itself.

## 3. Acceptable use
You agree **not** to:
- use the Service to store, generate, or transmit unlawful content;
- attempt to access another user's account or data;
- probe, scan, or test the vulnerability of the Service except under an authorized security
  assessment (responsible-disclosure contact: adhamamr01@gmail.com);
- disrupt the Service (e.g. denial-of-service, circumventing rate limits), or use it to build a
  competing product by scraping.

## 4. Security & your responsibilities
We apply industry-standard protections (TLS in transit, encryption at rest, rate limiting, optional
two-factor authentication, breached-password screening). No system is perfectly secure. You are
responsible for using a strong, unique master password and enabling two-factor authentication.

## 5. Your data
- **Ownership.** Your data is yours. We claim no ownership of the credentials you store.
- **Export.** You may export all of your data at any time (Settings → Export, or
  `GET /api/account/export`). See the [Privacy Policy](PRIVACY.md) for details.
- **Deletion.** You may permanently delete your account and all associated data at any time
  (Settings → Delete account, or `DELETE /api/account`). Deletion is irreversible.

## 6. Availability & changes
The Service is provided on an "as available" basis. We may modify, suspend, or discontinue features,
and we may update these Terms. Material changes will be communicated by email or an in-app notice.
Continued use after changes take effect constitutes acceptance.

## 7. Disclaimer of warranties
To the maximum extent permitted by law, the Service is provided **"as is"** and **"as available"**
without warranties of any kind, express or implied, including merchantability, fitness for a
particular purpose, and non-infringement. We do not warrant that the Service will be uninterrupted,
error-free, or that data loss will never occur.

## 8. Limitation of liability
To the maximum extent permitted by law, Adham Mohamed Amr Farouk Aboelela ("we") will not be liable
for any indirect,
incidental, special, consequential, or punitive damages, or any loss of data, arising from your use
of (or inability to use) the Service. Where liability cannot be excluded, it is limited to the
greater of the amount you paid us for the Service in the past 12 months (the Service is currently
provided free of charge) or USD 50.

## 9. Termination
You may stop using the Service and delete your account at any time. We may suspend or terminate
access if you materially breach these Terms or use the Service unlawfully.

## 10. Governing law
These Terms are governed by the laws of the Arab Republic of Egypt, without regard to
conflict-of-laws rules.

## 11. Contact
Questions about these Terms: adhamamr01@gmail.com.
