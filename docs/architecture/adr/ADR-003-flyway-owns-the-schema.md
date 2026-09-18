# ADR-003: Flyway owns the schema, and code generation runs against it

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View, Physical View

## Context

[ADR-001](ADR-001-jooq-instead-of-jpa.md) generates Java from a database schema,
so something has to define that schema. Three candidates: an ORM generating it
from entities (excluded with JPA), a hand-maintained SQL file, or versioned
migrations. The entity model in [`../../entity_model.md`](../../entity_model.md)
is the specification; NFR-009 requires the schema to reproduce it exactly, which
means the schema needs to be reviewable as text next to that document.

## Decision

**Flyway migrations are the only way the schema is created or changed**
(C-006), in `src/main/resources/db/migration`. Code generation runs the
migrations against a throwaway `postgres:17-alpine` container
(`testcontainers-jooq-codegen-maven-plugin`) and generates jOOQ classes from the
result.

Test-only seed data is a separate migration on the test classpath
(`src/test/resources/db/migration/V2__seed_reference_data.sql`) so it can never
reach a production database.

## Consequences

- **The migrations are the schema DSL.** Changing one and not regenerating
  leaves stale jOOQ classes — the most common build surprise in this project,
  and why `generate-sources` is called out in `CLAUDE.md`.
- **Docker becomes a build dependency** (C-007): no Docker, no code generation,
  no compilation.
- **Generated code is not committed.** It lands in `target/generated-sources`
  and is excluded from Sonar analysis under a package name (`…demo.petclinic.database`)
  that cannot accidentally match hand-written code.
- **A schema change is reviewable as a diff** against the entity model, which is
  what makes NFR-009 checkable by a human in seconds.
