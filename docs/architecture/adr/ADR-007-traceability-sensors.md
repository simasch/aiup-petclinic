# ADR-007: Specification traceability is enforced by tests, not by review

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Use Case View, Development View

## Context

`docs/` is declared the source of truth (C-008). A declaration alone does not
survive contact with a fast-moving codebase: an alternative flow gets renamed, a
business rule is dropped, a use case is marked `Done` while two of its flows have
no test — and nothing notices, because documents do not fail builds.

This matters more with an AI coding agent than without one. An agent will happily
write a test annotated with a business rule that no longer exists, and a
reviewer reading a diff of generated code is unlikely to catch it.

## Decision

Make the link executable. Every use case test method carries
`@UseCase(id, scenario, businessRules)`; every journey class carries
`@TestCase`. Two tests run in `./mvnw test` and read the documents from disk:

- **`UseCaseTraceabilityTest`** — every annotated id, scenario, and business rule
  must exist in the specification, and every use case whose `Status:` is `Done`
  or `Tested` must have a test for its main success scenario, every alternative
  flow, and every business rule.
- **`TestCaseTraceabilityTest`** — the same for test cases, including that an
  `Automated` journey only walks use cases that are themselves `Done` or
  `Tested`.

Any other status (`Draft`, `Specified`, …) is exempt: specification-first work
means an unimplemented use case is a normal intermediate state, not a defect.

## Consequences

- **A `Status:` line is an assertion, not a label.** Setting it to `Done`
  switches the sensor on. Never set one by hand without running the
  `coverage-check` skill first.
- **Renaming a flow in a specification breaks the build** until the annotations
  follow. That is the point.
- **The honest fixes are always two:** write the missing test, or correct the
  status. Weakening the check is not one of them.
- **A new use case costs a little ceremony** — an annotation per test method — in
  exchange for a traceability chain nobody has to maintain by hand.
