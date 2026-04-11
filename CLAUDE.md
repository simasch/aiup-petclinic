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

## Architecture

Single Maven module, **package-by-feature** under `ai.unifiedprocess.petclinic` — each feature (e.g. `owner`, `pet`,
`visit`, `vet`) is its own package containing two sub-packages:

- **`ui`** — Vaadin views, forms, and other UI components for that feature. One view per use case / screen.
- **`domain`** — domain types and jOOQ query logic for that feature. Queries are written against the generated
  `database.*` tables/records. No JPA, no Spring Data repositories.

So a feature looks like `ai.unifiedprocess.petclinic.<feature>.ui.*` and `ai.unifiedprocess.petclinic.<feature>.domain.*`.
Cross-feature reach-in should go through the other feature's `domain` package, not its `ui`.

**Flyway migrations** define the schema declaratively; jOOQ codegen consumes them, so the migrations effectively *are*
the schema DSL.

The project is intentionally thin on layers — there is no separate service/repository/DTO layering beyond `ui` +
`domain` unless a use case demands it. Prefer putting jOOQ query logic close to where it's used until duplication
justifies extraction.

## Testing conventions

- **Vaadin Browserless Testing** (`browserless-test-junit6`) is the default for Vaadin view tests — server-side, no
  browser, no servlet container. See https://vaadin.com/docs/latest/flow/testing/browserless/getting-started.
- View tests extend `SpringBrowserlessTest` and are annotated `@SpringBootTest`. They run inside a Spring context and
  use the same Testcontainers Postgres as the rest of the integration tests.
- Core API (all inherited from `SpringBrowserlessTest`):
    - `navigate(MyView.class)` — routes to the view *and returns the instantiated view instance*. This is how you get
      hold of the view under test.
    - `test(component).setValue(...)` / `test(component).click()` — wrap a component to simulate user interaction.
      Prefer this over calling setters/listeners directly.
    - `$(Type.class).single()` / `$(Type.class).all()` — query the current UI tree for components by type. Use this for
      `Notification`, `Dialog`, and anything not directly reachable from the view.
    - `fireShortcut(Key.ENTER)` / `fireShortcut(Key.KEY_S, KeyModifier.CONTROL)` — simulate keyboard shortcuts.
- **Component field access:** place tests in the **same package** as the view and access component fields that are *
  *package-private** directly (`view.lastNameField`, `view.resultsGrid`). This is the idiomatic browserless pattern —
  fields are not a "test backdoor", they're the view's structure. **Do not add public getters just for tests.**
- For custom form components (e.g. `OwnerForm`, `PetForm`), the form's package-private fields are part of its contract —
  `test(view.ownerForm.firstName).setValue(...)` is fine.
- For navigation assertions, check `UI.getCurrent().getInternals().getActiveViewLocation().getPath()` rather than
  asserting on domain state pulled back out of the view.
- For rendered-state assertions (owner name shown, pet listed, etc.), find the actual `Paragraph`/`H3`/etc. via
  `$(Paragraph.class)` and assert on `.getText()` so the render path is exercised end-to-end.
- Integration tests compose with `TestcontainersConfiguration` (see
  `src/test/java/.../TestcontainersConfiguration.java`) for a real Postgres.

## Skills available for this project

This repo has AIUP-specific skills registered — prefer them over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`, `aiup-core:use-case-diagram`, `aiup-core:requirements` — for
  authoring/updating specs in `docs/`.
- `aiup-vaadin-jooq:flyway-migration` — generate `V*.sql` from the entity model.
- `aiup-vaadin-jooq:implement` — implement a use case end-to-end (Vaadin view + jOOQ queries).
- `aiup-vaadin-jooq:playwright-test` — browser-based end-to-end tests (the `karibu-test` skill is obsolete; server-side
  tests use `SpringBrowserlessTest` directly, see *Testing conventions* above).
