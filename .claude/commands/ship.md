---
description: Ship the current work via the repo's dev→main ritual — verify, commit, push, no-ff merge, watch CI.
argument-hint: [optional commit summary]
---

Ship the current working changes following Passwordy's git workflow. If the user gave a summary
after the command, use it as the basis for the commit message: "$ARGUMENTS".

Work through these steps in order. Stop and report if any step fails — never force past a red
gate.

1. **Branch check.** Confirm you're on `dev` (`git branch --show-current`). If on `main`, stop and
   tell the user — work lands on `dev`, never directly on `main`. If on a feature branch, that's
   fine; you'll still merge through `dev`.

2. **Review what's shipping.** `git status --short` and a quick `git diff --stat`. Make sure
   nothing unintended is staged — especially **no real secrets** (the gitignored
   `application-{local,docker}.properties` must not appear) and no build caches.

3. **Green gate.** If the diff touches `backend/**`, run `cd backend && ./mvnw clean test`
   (Windows: `mvnw.cmd`; set `JAVA_HOME` to the JDK 25 path if the wrapper can't launch a JVM).
   Do not continue until green. (Frontend-only or docs-only diffs can skip this — CI will cover
   `frontend/**`.)

4. **Commit on `dev`.** Stage (`git add -A`, after the secret check above) and commit with a
   descriptive message. End the message with the required trailer on its own line:
   `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`. Push: `git push origin dev`.

5. **Merge into `main`.** `git checkout main && git merge --no-ff dev -m "<merge summary>"`
   (always `--no-ff` — never fast-forward into `main`), then `git push origin main`, then
   `git checkout dev` to return.

6. **Watch CI.** Find the triggered run(s) with `gh run list --limit 3` and
   `gh run watch <id> --exit-status`. Note the **path filters**: `backend-ci.yml` only runs on
   `backend/**`, `frontend-ci.yml` only on `frontend/**` — a docs-only change triggers neither,
   which is expected, not a failure.

7. **Summarize:** branch shas pushed, which CI ran and its result, and anything still open.

**Guardrails:** never commit real secrets; never fast-forward into `main`; if the green gate or CI
is red, stop and surface it rather than papering over it.
