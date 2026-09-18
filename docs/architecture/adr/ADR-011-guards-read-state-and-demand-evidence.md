# ADR-011: Guards read the repository's state and demand positive evidence

**Status:** Accepted   
**Date:** 2026-09-18   
**Affects:** Development View   
**Amends:** [ADR-010](ADR-010-hooks-guard-the-session.md)

## Context

[ADR-010](ADR-010-hooks-guard-the-session.md) added hooks for the two properties
no test can reach: that the sensors ran before a turn ended, and that a
`Status:` line was audited before it was set. A review of them showed that each
could be satisfied while the property did not hold — and always for the same
reason. The hook keyed on *which tool* ran or on *what the console printed*,
not on the state of the repository.

- The status guard matched Edit and Write. A `sed` or a heredoc through Bash
  never reached it, and in auto mode the harness prefers Bash for edits, so the
  bypass was the default path rather than an edge case.
- The sensor recorder took any full `test` / `verify` whose output lacked
  failure text as green. A run in the background, piped through `tail`,
  redirected to `/dev/null` or followed by `|| true` produced no failure text
  and wrote the marker before — or without — a passing build.
- The status guard fired after the edit. The claim was already in the file, and
  "run the audit first" was a request, not a rule.
- The only run that counted was the full Testcontainers suite, though the five
  sensor classes are plain JUnit and ArchUnit. Every turn that touched a
  document cost a container start, and "Docker is not available" was a
  sanctioned way to end unverified.
- "Changed" meant `src/` and `docs/`. A `pom.xml` edit got no check, a hook
  edit never forced `smoke.sh`, and no CI ran `smoke.sh` either.
- The committed settings authorized none of the commands the hooks demand, so a
  fresh clone was told to run a build it then had to ask permission for.

## Decision

Two rules, applied to every hook.

**A guard reads the state of the repository, never the shape of a tool call.**
`check-spec-status.sh` runs after every tool call, rereads every `Status:`
line, and compares it with what it saw last. Which tool made the change is
irrelevant. The `PreToolUse` guard stays for Edit and Write because there it can
refuse the edit before it lands; it is the fast path, not the guarantee.

**Evidence is positive.** A sensor run counts because the Surefire report of
every sensor class exists, is newer than the session start and than every
changed file, ran tests, and counts no failure or error — the test JVM writes
those files whatever the console shows. An audit counts because the
`uc-coverage` agent finished (`SubagentStop`), which writes a marker per audited
id; the guards accept `Done` / `Tested` / `Automated` only with such a marker
newer than the last change under `src/`. The marker says an audit happened, not
that it found nothing: the sensors judge the claim itself on the next run.

And three consequences of taking the rules seriously:

- The five sensor classes carry the JUnit tag `sensor`, and a `sensors` Maven
  profile activated by `-Dgroups=sensor` skips jOOQ code generation and JaCoCo.
  `./mvnw -q test -Dgroups=sensor` is what the Stop hook asks for: seconds, no
  Docker. The full suite remains the definition of done and the commit gate.
- `pom.xml` joins the watched paths. A change under `.claude/hooks/` or to
  `.claude/settings.json` needs a green `smoke.sh`, which records its own run
  and which CI executes before the Maven build.
- `.claude/settings.json` allows the Maven build, the smoke test and read-only
  git, so the guardrails work on a clone without a prompt.

## Consequences

- **The three ways to end a turn with an unverified claim while every visible
  check said green are closed**: the Bash edit, the background or truncated
  run, and the Docker excuse. A turn that still ends unverified does so through
  the one deliberate escape ADR-010 kept — the second Stop after a block — and
  is visible as such in the transcript. The other escape ADR-010 named,
  `touch .git/aiup-sensors-ran`, no longer works: the Stop hook reads the
  Surefire reports behind the marker, and a touched file has none.
- **The sensor tier is cheap enough to run every turn.** That is what the
  profile buys. It costs one more Maven profile and a `build-helper` execution
  that registers the generated sources the skipped generator no longer does.
- **The status guards depend on the plugin's agent.** The `SubagentStop`
  matcher names `aiup-vaadin-jooq:uc-coverage`; an audit done inline, without
  the agent, leaves no marker and the edit is refused. That is intended: the
  agent is read-only and its checklist is the audit.
- **Known limits.** A hook still runs only in a Claude Code session. The report
  check reads `target/`, so `mvn clean` after a run takes the evidence away and
  the Stop hook says so. A report is per class, so a run interrupted after the
  five sensors passed but before the rest of the suite counts as a sensor run —
  which is what it is. And a status set through Bash is refused only after the
  fact; the Stop hook then holds the turn until the sensors have judged it.
- `smoke.sh` remains the only test of the hooks, now run by CI as well.
