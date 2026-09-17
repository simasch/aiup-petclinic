# ADR-004: Package by feature with `ui` + `domain`, and no service layer

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View

## Context

An AI coding agent implements one use case at a time. Without an explicit rule
for where code goes, each use case arrives with a slightly different structure —
a service here, a mapper there, a DTO because the last prompt mentioned one. The
result is not wrong in any single place and unmaintainable as a whole.

A layered `controller/service/repository/dto` structure would give an agent four
plausible homes for every piece of logic and a reason to create a class in each.

## Decision

**Package by feature.** One package per feature (`owner`, `pet`, `visit`, `vet`,
`welcome`, plus `core` for the shell), and inside each exactly two sub-packages:

- **`ui`** — Vaadin views, forms, route parameter holders,
- **`domain`** — records and the `<Entity>Repository` holding the jOOQ queries.

**No service layer, no DTOs, no mappers** until a use case demonstrably needs
one. Query logic stays next to its use until duplication justifies extraction.
Cross-feature access goes through the other feature's `domain` package; the only
exception is naming a view class as a route token.

`ArchitectureTest.featuresHaveOnlyUiAndDomain`, `noServiceOrDtoLayer`,
`domainDoesNotDependOnUi`, `domainIsFreeOfVaadin` and `noCyclesBetweenFeatures`
enforce this.

## Consequences

- **"Where does this go?" has one answer,** and a wrong answer fails the build
  rather than the review.
- **A feature is readable end to end** in one folder — useful for a human, and
  for an agent whose context window is finite.
- **Some logic sits in views** that a layered design would extract — the
  duplicate-name check UC-007 needs before its insert, for example. Accepted
  while each use case writes one row; the moment two views need the same rule,
  it moves to `domain`.
- **The feature graph is cyclic by design** because of the routing exception —
  which is why the cycle rule slices on `domain` packages instead.
