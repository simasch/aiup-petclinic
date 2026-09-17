# ADR-005: Validation lives in the Vaadin form, not in the domain record

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View

## Context

Every use case that writes data states its validation as user-visible behaviour:
the offending field is marked, the message names the problem, and the user's
other entries survive (FR-023, and the alternative flows of UC-003, UC-006,
UC-007, UC-008, UC-009). Validation is therefore not only a guard against bad
data — it is a specified interaction.

Bean Validation annotations on the domain records would put the rule in a second
place: an exception thrown after the fact, which the view would then have to
translate back into a field-level error message. Two implementations of the same
rule, one of which cannot produce the specified behaviour on its own.

## Decision

**The form is the validation boundary.** A feature's shared `<Entity>Form`
exposes `validateAndRead(...)`, which runs field-level validation and either
returns a populated domain record or surfaces field errors. A form also exposes
helpers for pushing a late error — one discovered only by a database lookup —
back onto a named field.

**Domain records stay validation-free:** no annotations, no constructor checks.
`ArchitectureTest.domainRecordsAreValidationFree` enforces it.

## Consequences

- **The specified behaviour is the implementation,** not a translation of an
  exception into it.
- **A record can be constructed in an invalid state** — in tests, for instance.
  Accepted: the database's `NOT NULL` and foreign keys are the backstop against
  a genuinely broken write, and there is exactly one path into the write.
- **A second entry point would need its own validation.** There is none today
  ([ADR-002](ADR-002-vaadin-flow-server-side-ui.md): no API). If one is ever
  added, this decision has to be revisited before it is.
- **Rules that need the database stay in the view** (duplicate pet name), which
  is why [`../process.md`](../process.md) documents that check-then-act
  explicitly.
