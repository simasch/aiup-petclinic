# ADR-001: jOOQ with generated SQL instead of JPA/Hibernate

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View

## Context

The original Spring PetClinic uses Spring Data JPA. This re-implementation needs
a persistence approach that an AI coding agent can be held to: the specifications
state exact queries, orderings, and "load this in one query" rules (UC-002
BR-002, UC-004 BR-002, UC-005 BR-001/002, NFR-001/002), and an agent must not be
free to satisfy them with a lazy-loading side effect nobody can see in the code.

With an ORM, what actually reaches the database is the product of mappings,
fetch types, and a session lifecycle. Reviewing generated code then means
reviewing what the framework will do with it — which is precisely the kind of
implicit behaviour an agent gets wrong and a reviewer misses.

## Decision

Use **jOOQ** with code generated from the real schema. No JPA, no Spring Data,
no entities with an identity lifecycle.

- Domain types are plain Java records, mapped with
  `org.jooq.Records.mapping(Type::new)` — the constructor reference makes column
  order a compile error when it drifts.
- To-many data loads with `multiset(...)` in the same query.
- Persistence classes are still called `<Entity>Repository` and annotated
  `@Repository`, for exception translation and for a name developers recognize.

`ArchitectureTest.noJpaNoSpringData` and `noFetchInto` enforce this.

## Consequences

- **The query in the code is the query at the database.** Review and
  specification compare directly; the N+1 discussions in
  [`../process.md`](../process.md) are visible facts, not guesses.
- **Code generation is now part of the build.** jOOQ classes must be regenerated
  after every migration change, which is why `generate-sources` needs Docker
  ([ADR-003](ADR-003-flyway-owns-the-schema.md)).
- **No dirty checking, no cascade, no lazy loading.** Every write is an explicit
  statement. For the five single-statement writes this application has, that is
  a simplification, not a cost.
- **The SQL is PostgreSQL-specific.** Accepted: C-005 names one database.
