# ADR-010: Hooks guard the session; tests guard the repository

**Status:** Accepted, amended by [ADR-011](ADR-011-guards-read-state-and-demand-evidence.md)   
**Date:** 2026-09-18   
**Affects:** Development View

## Context

[ADR-007](ADR-007-traceability-sensors.md) made the link between `docs/` and the
code executable, and `ArchitectureTest` does the same for the code conventions.
Both share a limitation that is easy to miss: **they are post-hoc.** A sensor
can only catch drift once somebody runs it. Nothing in the source tree can
express *"the agent ran the sensors before it claimed to be done"*, because that
is a property of the session, not of the repository.

So the failure mode survives every check in the previous ADRs: an agent edits a
view, skips `./mvnw test`, and reports success. The drift is real from that
moment until CI catches it — minutes or hours later, and only if someone pushes.

A second gap has the same shape. `CLAUDE.md` states in prose that a `Status:`
line is an assertion and must never be set by hand without a coverage check.
Prose shapes behaviour probabilistically; it does not stop an edit. The sensor
does catch a *wrong* status, but only on the next full run — and a narrowed run
(`-Dtest=…`) skips the sensors entirely while still reporting green.

## Decision

Add a fourth enforcement layer, and be precise about what it is for.

**Hooks enforce properties of the session.** `.claude/settings.json` configures
four, with the scripts in `.claude/hooks/`:

- **`session-start.sh`** (`SessionStart`) — records the commit the session
  started on, so a `git commit` during the session does not make a change
  disappear from the Stop hook's view.
- **`require-sensors.sh`** (`Stop`) — refuses to end a turn when `src/` or
  `docs/` changed and the sensors have not run *since the newest change*: a file
  edited after the last run, or one added, deleted or committed that the run
  did not see.
- **`record-sensor-run.sh`** (`PostToolUse`, Bash) — writes the marker the Stop
  hook reads: the change set a **green, full** `test` or `verify` saw. A
  narrowed run (`-Dtest=`), a skipped one (`-DskipTests`) or a failed one does
  not count.
- **`guard-spec-status.sh`** (`PostToolUse`, Edit/Write) — notices a `**Status:**`
  line in a UC or TC specification actually changing value, and demands a
  coverage check.

`smoke.sh` beside them exercises all four against a throwaway repository and
is the thing to run after changing one.

**Everything else stays a test.** The rule that a browserless test must not be
named `*IT` was prose until now; it became `TestLayerConventionsTest`, not a
fourth hook.

The dividing line: *if a rule can be checked by reading the repository, it is a
test.* Only a rule about what happened during a session is a hook.

## Consequences

- **Hooks buy latency, tests buy correctness.** A hook reports a broken rule in
  seconds instead of minutes. Only CI decides whether code is allowed to exist.
  The `Stop` hook is the exception that justifies the layer: it enforces
  something no test can reach.
- **Hooks are the least portable layer here.** They live in `.claude/`, fire only
  in a Claude Code session, and run for nobody else — not CI, not another
  editor, not a human contributor. **Prefer an ArchUnit rule whenever the rule
  can be expressed as one**; it binds everyone and travels with the clone.
- **A hook must fail open.** Unparseable input, a missing `.git`, no `jq` — the
  hook exits quietly. A guardrail that breaks the session is worse than the drift
  it was meant to catch.
- **The `Stop` hook is one enforced reminder per turn, not a wall.** After a
  block Claude continues, and when it tries to stop again the hook sees
  `stop_hook_active` and lets go — that is what keeps a session without Docker
  from looping forever. Its message asks for exactly that case to be reported
  instead of glossed over, and a turn that ends without the sensors having run
  is visible in the transcript. `touch .git/aiup-sensors-ran` is the deliberate
  escape; using it means saying so.
- **Known limits.** A `Status:` line changed through Bash (`sed`, a heredoc)
  never reaches `guard-spec-status.sh`; the Stop hook backs it up, because the
  change is under `docs/` and the sensors then catch a status without the tests
  behind it. Nothing prevents a build from being run outside the session before
  the marker is checked, and nothing here runs for a contributor without Claude
  Code — which is the reason the layer above prefers a test.
- **The hooks are committed, the local overrides are not.**
  `.claude/settings.local.json` is gitignored, so a clone gets the shared
  guardrails and none of the personal permission grants.
