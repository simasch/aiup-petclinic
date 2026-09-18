# ADR-006: Two test layers — browserless `*Test` and Playwright `*IT`

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Development View

## Context

Two things need verifying, and they are not the same thing. A **use case** is a
screen's behaviour: validation, notifications, navigation, ordering. A **test
case** is a journey across several use cases, and it is only convincing if a
real browser walks it.

Verifying everything in a browser is slow and flaky. Verifying everything
server-side never proves the application actually renders and responds in a
browser. Running both, but measuring coverage on only one, would push an agent
into writing duplicate server-side tests for code an end-to-end test already
covers.

## Decision

Two layers, distinguished by the class name suffix, which also decides the Maven
phase:

| Layer | Name | Runner | Scope |
|-------|------|--------|-------|
| Vaadin Browserless Testing | `UC<NNN><Name>Test` | Surefire, `test` | one use case, server-side, no browser — **the default** |
| Playwright + Drama Finder  | `TC<NNN><Name>IT` (or `UC<NNN><Name>IT`) | Failsafe, `verify` | a test case journey in a real browser |

Both run against a real PostgreSQL from Testcontainers (NFR-007). JaCoCo runs in
both JVMs, and `post-integration-test` **merges the two exec files** into the one
report Sonar reads.

## Consequences

- **A line covered only by a Playwright `*IT` counts as covered,** so nobody
  writes a browserless test just to satisfy the gate. That is the whole point of
  the merge.
- **Only `./mvnw verify` produces the merged report.** `./mvnw test` runs the
  agent and stops before the merge — checking coverage after `test` shows a
  number that is wrong by construction.
- **Naming is load-bearing.** A browserless test called `*IT` never runs in
  `test`; a Playwright test called `*Test` runs in the wrong phase and fails.
- **The browser layer stays small** — one class per specified test case, and no
  more — because it is the expensive one.
