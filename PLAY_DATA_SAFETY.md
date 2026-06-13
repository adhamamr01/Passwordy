# Google Play — Data Safety form answers

This is a ready-to-transcribe mapping for the **Data safety** section in Play Console, derived from
how Passwordy actually handles data (see [PRIVACY.md](PRIVACY.md)). Re-confirm each answer against
the current build before submitting.

## Overview answers
- **Does your app collect or share any of the required user data types?** → **Yes** (collects).
- **Is all of the user data collected by your app encrypted in transit?** → **Yes** (TLS for all
  API traffic; cleartext is blocked except local-dev hosts).
- **Do you provide a way for users to request that their data be deleted?** → **Yes** — in-app
  (Settings → Delete account) and via `DELETE /api/account`. Provide the deletion URL/instructions.

## Data types collected

| Data type | Collected | Shared | Processing | Purpose | Optional? |
|-----------|-----------|--------|------------|---------|-----------|
| **Email address** (Personal info) | Yes | No | Stored | Account management, security/transactional email | Required |
| **User IDs / username** (Personal info) | Yes | No | Stored | Account management | Required |
| **Passwords / vault entries** (App "credentials") | Yes | No | Encrypted at rest (AES-256-GCM) | App functionality | Required |
| **App interactions / crash logs** | Only if a crash-reporting SDK is added (task #16) | No | — | Diagnostics | Update this row if/when added |

> Note: the **master password** is never stored (Argon2id hash only) — there is no plaintext
> credential to declare beyond the encrypted vault entries above.

## Data NOT collected (declare "No")
- Location (approximate or precise)
- Financial info / payment info
- Contacts, calendar, SMS, call logs
- Photos, videos, audio, files
- Advertising ID / advertising or marketing identifiers
- Browsing history
- Analytics / behavioural tracking SDKs

> **IP address:** Google's form does not have a dedicated "IP address" toggle; IP is processed
> transiently for rate limiting/abuse prevention and is not stored as profile data, so it is not
> declared as a collected data type. Mention this transient security use in the Privacy Policy
> (done — see PRIVACY.md §2).

## Security practices section
- **Data encrypted in transit:** Yes.
- **Users can request data deletion:** Yes (in-app + API).
- **Committed to Play Families Policy:** No (not a children's app; general audience, not directed to
  under-13).
- **Independent security review:** Declare per task #23 outcome (leave unchecked until done).

## Required links
- **Privacy policy URL:** host [PRIVACY.md](PRIVACY.md) at a public URL and enter it here.
- **Account deletion URL:** a public page describing in-app deletion + a web request path
  (e.g. emailing adhamamr01@gmail.com), per Play's account-deletion requirement.
