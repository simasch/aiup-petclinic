# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project purpose

Demo for a talk on **Spec-Driven Development with the AI Unified Process
(AIUP)**. Re-implements the classic Spring PetClinic by writing the specs
first (`docs/`) and generating code against them.

**`docs/` is the source of truth, not the code.** When asked to implement
something, read the relevant spec first:

- `docs/entity_model.md` — ER diagram + attribute tables with validation
  rules. The schema in Flyway migrations must match this.
- `docs/use_cases.puml` — PlantUML actor/use-case diagram.
- `docs/use_cases/UC-NNN-*.md` — one file per use case with preconditions,
  main success scenario, alternative flows, postconditions, business rules.
  UI flows, field labels, and navigation come from these.

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Stack

- **Java 25**, **Spring Boot 4.0.5**, **Vaadin 25.1**
- **jOOQ** for type-safe SQL — generated sources live in
  `target/generated-sources/jooq` under package
  `ai.unifiedprocess.demo.petclinic.database`
- **Flyway** migrations in `src/main/resources/db/migration`
  (`V1__initial_schema.sql` covers the full entity model; test-only seed
  data lives in `src/test/resources/db/migration/V2__seed_reference_data.sql`)
- **PostgreSQL** in prod; **Testcontainers** (`postgres:17-alpine`) for
  tests *and* for jOOQ code generation at build time

## Commands

```bash
# Run the app locally (Testcontainers-backed Postgres via TestAiupPetclinicApplication)
./mvnw spring-boot:test-run

# Full build — also runs jOOQ codegen against a throwaway Testcontainers Postgres
./mvnw verify

# Run all tests (Docker must be running)
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=UC004FindOwnersByLastNameTest
./mvnw test -Dtest=UC004FindOwnersByLastNameTest#singleMatchNavigatesDirectlyToDetails

# Regenerate jOOQ sources after changing a Flyway migration
./mvnw generate-sources
```

Docker must be running for `test`, `verify`, and `generate-sources`. **If
you add or change a migration, jOOQ classes won't update until you re-run
`generate-sources` (or any later phase).**

## When to read the detailed guides

- **Before implementing a use case** → read the corresponding
  `docs/use_cases/UC-NNN-*.md` spec first. It defines preconditions, the
  main success scenario, alternative flows, postconditions, business rules,
  field labels, and navigation. The spec is the source of truth.

- **Before implementing a use case, writing a view, adding a repository, or
  touching anything in `src/main/java/`** → read
  [`docs/guidelines/architecture.md`](docs/guidelines/architecture.md)
  first. It covers package layout, jOOQ mapping patterns, Vaadin view
  conventions, the shell exception, form validation, error handling, and
  the `*Repository` stereotype rule.

- **Before writing or modifying any test under `src/test/java/`** → read
  [`docs/guidelines/testing.md`](docs/guidelines/testing.md) first. It
  covers `SpringBrowserlessTest`, the `UC<NNN><Name>Test` naming rule, the
  `@UseCase` annotation, `PetClinicTestBase`, seed-data conventions,
  locator patterns, and what you must **not** do (no public test getters,
  no `assertNotNull` on fields, no nested field reach-in, no Karibu).

Do not skip these. Both files are short and kept current — drift between
them and the code is a bug to fix, not a style preference to ignore.

## Skills available for this project

Prefer these over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`,
  `aiup-core:use-case-diagram`, `aiup-core:requirements` — authoring/updating
  specs in `docs/`.
- `aiup-vaadin-jooq:flyway-migration` — generate `V*.sql` from the entity
  model.
- `aiup-vaadin-jooq:implement` — implement a use case end-to-end (view +
  jOOQ queries). Already honours `docs/guidelines/architecture.md`.
- `aiup-vaadin-jooq:playwright-test` — browser-based end-to-end tests. For
  server-side tests, see `docs/guidelines/testing.md` (the `karibu-test`
  skill is obsolete).
