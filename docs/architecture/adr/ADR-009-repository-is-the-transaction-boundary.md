# ADR-009: The repository is the transaction boundary

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Process View, Development View   
**Supersedes:** relying on auto-commit with no transaction declared anywhere

## Context

Every use case in this application writes exactly one row with one statement, so
the JDBC connection's auto-commit mode would do: a single statement is atomic in
PostgreSQL, and nothing can be half-written. Declaring nothing at all is
therefore defensible — the transaction boundary exists, it is simply nowhere in
the code.

That is a bad shape to hand an AI coding agent. If nothing in the source says
where a transaction begins, the first use case that needs two statements gets
them as two calls from a view, each committing on its own — and it looks exactly
like every use case before it. An implicit boundary cannot be reviewed, and it
cannot be enforced.

The question is not *whether* transactions exist but *where they are declared*.
Two candidates: the view, which knows the use case, or the repository, which
knows the data. The view is where a transaction would be held across a user's
thinking time, which is the classic way to exhaust a connection pool.

## Decision

**Declare transactions on the repository.**

- `@Transactional(readOnly = true)` at class level on every `*Repository`.
- Plain `@Transactional` on every writing method (`insert`, `update`).
- **Never `@Transactional` in a `ui` package.** A view calls repository methods
  in sequence; it does not wrap them.
- A use case that needs two statements to succeed together gets **one**
  `@Transactional` method in the feature's `domain` package that does both.

`ArchitectureTest.repositoriesAreTransactional` and `viewsAreNotTransactional`
enforce it; [`../development.md`](../development.md#transaction-boundary) states
it.

## Consequences

- **The boundary is visible and checkable.** An agent adding a repository method
  sees the annotation on every neighbour, and a missing one fails the build.
- **Reads announce themselves as reads.** `readOnly = true` lets the driver and
  the database skip what a write would need.
- **Behaviour is the same as auto-commit today.** One statement per transaction
  either way — this buys a declared place to put the second statement when it
  arrives, not a fix for a bug.
- **A screen's reads are still separate transactions**, and deliberately so:
  wrapping them would mean holding a transaction across the rendering of a view.
  [`../process.md`](../process.md#transaction-boundaries) says what that costs.
- **The check-then-act on duplicate pet names stays two transactions** with a
  click between them. The database's unique index is what closes that race — an
  interaction cannot be made atomic by annotating it.
