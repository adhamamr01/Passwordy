---
description: Review-only security sweep of the diff (or a target) — /security-review plus a Passwordy-specific checklist. Reports findings; does not fix.
argument-hint: [diff (default) | all | <path/area>]
---

Run a focused **security review** of Passwordy. This command **reports** — it does not change
code. Scope from "$ARGUMENTS": default to the branch diff vs `main`; `all` means the whole
codebase; otherwise treat the argument as a path/area to focus on.

Passwordy is a multi-user password manager (Spring Boot backend holds all security-critical
logic; Android client is a thin consumer). Read `CLAUDE.md` and `DECISIONS.md` for the intended
security model before judging anything.

## Step 1 — Automated pass
Run the `/security-review` skill over the scoped diff and collect its findings.

## Step 2 — Manual checklist (this codebase's real risk areas)
- **Authorization / IDOR:** does every per-user record go through the `findOwnedPassword`
  ownership gate? Any new `/api/passwords/**` route or query that could read/mutate another
  user's data, or filter in-memory instead of owner-scoped at the DB?
- **Enumeration oracles:** does any route distinguish "exists but not yours" from "doesn't
  exist" (should be 404 for both)? Does `register` still leak username/email existence
  (known/tracked)? Any timing signal in login beyond the dummy-hash guard?
- **Secrets & data exposure:** anything sensitive logged, returned unencrypted, or echoed in an
  error? Stored password `value` must stay AES-GCM ciphertext except via the explicit decrypt
  route. On Android: no body logging in release, JWT encrypted at rest (`TokenCrypto`).
- **Auth / session / transport:** routes correctly placed in `SecurityConfig` (public vs
  authenticated)? JWT validation intact? Any rate-limit bypass (tier/keying)? Cleartext blocked
  except dev hosts; `allowBackup=false`.
- **Input handling:** request DTOs are validated records (`@Valid`); watch for missing
  constraints, mass-assignment, oversized fields, or injection into any non-JPA query.

## Step 3 — Report
Produce a findings table: **severity · location (`file:line`) · category · exploit scenario ·
recommended fix**. Separate **newly introduced** issues from **pre-existing / already-tracked**
ones (e.g. registration enumeration, Redis-less single-instance limiter) so the signal is clear.
Only report concrete, exploitable issues — skip theoretical/style nits.

## Step 4 — Hand off (do not auto-fix)
End by recommending how to remediate each confirmed finding: small/contained → suggest `/fix`;
new surface or behavior change → suggest `/add-feature`. Ask the user which to action; make no
code changes under this command.

**Guardrails:** read before asserting — verify a flagged file/route still behaves as claimed.
Never weaken an ownership check, the timing guard, or rate-limit defenses; if a "fix" would, say
so explicitly.
