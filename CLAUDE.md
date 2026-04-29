# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

This is a demo for a talk on **Spec-Driven Development with the AI Unified Process (AIUP)**. It re-implements the
classic Spring PetClinic by writing the specs first (`docs/`) and generating code against them.

**`docs/` is the source of truth, not the code.** When asked to implement something, read the relevant spec first:

- `docs/entity_model.md` — ER diagram and attribute tables with validation rules. The schema in Flyway migrations must
  match this.
- `docs/use_cases.puml` — PlantUML actor/use-case diagram.
- `docs/use_cases/UC-NNN-*.md` — one file per use case with preconditions, main success scenario, alternative flows,
  postconditions, business rules. UI flows, field labels, and navigation come from these.

If a use case and the code disagree, the use case wins unless the user says otherwise.

## Stack

- **Java 25**, **Spring Boot 4.0.5**, **Vaadin 25.1**
- **jOOQ** for type-safe SQL — generated sources live in `target/generated-sources/jooq` under package
  `ai.unifiedprocess.demo.petclinic.database`
- **Flyway** migrations in `src/main/resources/db/migration` (currently empty — migrations are added as features are
  implemented)
- **PostgreSQL** in prod; **Testcontainers** (`postgres:17-alpine`) for tests *and* for jOOQ code generation at build
  time

## Commands

```bash
# Run the app locally (Testcontainers-backed Postgres via TestAiupPetclinicApplication)
./mvnw spring-boot:test-run

# Full build — this also runs jOOQ codegen against a throwaway Testcontainers Postgres
./mvnw verify

# Run all tests (Docker must be running)
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=AiupPetclinicApplicationTests
./mvnw test -Dtest=OwnerViewTest#findsOwnersByLastName

# Regenerate jOOQ sources after changing a Flyway migration
./mvnw generate-sources
```

Docker must be running for `test`, `verify`, and `generate-sources` — the `testcontainers-jooq-codegen-maven-plugin`
spins up Postgres, applies the Flyway scripts from `src/main/resources/db/migration`, and generates jOOQ classes from
the resulting schema. **If you add or change a migration, jOOQ classes won't update until you
re-run `generate-sources` (or any phase after it).**

## Guidelines

Two topic guides hold the detailed rules — read them before touching code or tests:

- **[docs/guidelines/architecture.md](docs/guidelines/architecture.md)** — read **before implementing** anything under
  `src/main/java/`. Covers package-by-feature layout, the cross-feature rule, jOOQ conventions
  (`Records.mapping`, `multiset`, lazy paging), the `*Repository` + `@Repository` stereotype, Vaadin view conventions
  (route parameters, `LumoUtility` styling, `SideNav`, form-level validation, `NotFoundException` in `beforeEnter`,
  error views), and Flyway.
- **[docs/guidelines/testing.md](docs/guidelines/testing.md)** — read **before writing or modifying any test** under
  `src/test/java/`. Covers Vaadin Browserless Testing, `UC<NNN><UseCaseName>Test` naming, the `@UseCase` annotation,
  `PetClinicTestBase`, the `$()` locator API (no direct field access, no public test-only getters), `from()` scoping,
  positive vs. negative assertions, and the Testcontainers wiring.

## Skills available for this project

This repo has AIUP-specific skills registered — prefer them over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`, `aiup-core:use-case-diagram`, `aiup-core:requirements` — for
  authoring/updating specs in `docs/`.
- `aiup-vaadin-jooq:flyway-migration` — generate `V*.sql` from the entity model.
- `aiup-vaadin-jooq:implement` — implement a use case end-to-end (Vaadin view + jOOQ queries).
- `aiup-vaadin-jooq:browserless-test` — server-side Vaadin view tests (`SpringBrowserlessTest`, `$()` locators). Default
  for UC tests; see [docs/guidelines/testing.md](docs/guidelines/testing.md).
- `aiup-vaadin-jooq:playwright-test` — browser-based end-to-end tests. The `karibu-test` skill is obsolete.
