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
- `docs/business_rules.md` — the rules several use cases share, as `GR-NNN`.
  A use case's `BR-NNN` heading references one instead of restating it, so
  follow the link before implementing that rule. A rule only one use case
  needs stays in that use case; **no business rule belongs in
  `docs/architecture/`**.
- `docs/test_cases/TC-NNN-*.md` — end-to-end journeys spanning several use
  cases; each is verified by a Playwright `TC<NNN><Name>IT`.
- `docs/architecture/` — the 4+1 views (logical, process, development,
  physical), the code and test conventions, and the ADRs behind them. The use
  cases say *what*, these say *how it is built*;
  [`docs/architecture/README.md`](docs/architecture/README.md) says which
  document answers which question.

Three sensors (`./mvnw test`) enforce the links between the documents and the
code: `UseCaseTraceabilityTest`, `TestCaseTraceabilityTest`, and
`BusinessRuleTraceabilityTest` — the last one between `docs/business_rules.md`
and the use cases that reference a `GR-NNN`. Because of the first two a
`Status:` line (`Done`/`Tested` on a use case, `Automated` on a test case) is
an assertion that switches the sensor on, not a label — never set one by hand
without running `aiup-vaadin-jooq:coverage-check` first. What each sensor
checks is in
[`docs/architecture/testing.md`](docs/architecture/testing.md#the-traceability-sensors).

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Stack

**Java 25**, **Spring Boot 4.1.1**, **Vaadin Flow 25.2**, **jOOQ** on
**PostgreSQL**, schema owned by **Flyway**, **Testcontainers** for tests and for
jOOQ code generation. Application code lives under
`ai.unifiedprocess.petclinic`, package by feature; generated jOOQ sources under
`ai.unifiedprocess.demo.petclinic.database` (the extra `demo` segment is
deliberate).

The full table, with the decisions behind each choice, is in
[`docs/architecture/development.md`](docs/architecture/development.md#technology-stack).

## Commands

```bash
# Run the app locally (Testcontainers-backed Postgres via TestAiupPetclinicApplication)
./mvnw spring-boot:test-run

# Regenerate jOOQ sources after changing a Flyway migration
./mvnw generate-sources

# Unit / server-side tests: Surefire runs every *Test class
# (browserless Vaadin view tests, UC<NNN><Name>Test, the ArchUnit rules in
# ArchitectureTest, and the spec/test traceability sensor UseCaseTraceabilityTest)
./mvnw test

# Run a single *Test class / method
./mvnw test -Dtest=UC004FindOwnersByLastNameTest
./mvnw test -Dtest=UC004FindOwnersByLastNameTest#singleMatchNavigatesDirectlyToDetails

# Integration tests: Failsafe runs every *IT class in the verify phase
# (Playwright browser tests, UC<NNN><Name>IT / TC<NNN><Name>IT).
# verify also runs jOOQ codegen, the frontend build, and all *Test first,
# and writes the merged coverage report SonarQube reads.
./mvnw verify

# Run a single *IT class
./mvnw verify -Dit.test=TC001NewOwnerFirstVisitIT
./mvnw verify -Dit.test=TC001NewOwnerFirstVisitIT -Dheadless=false   # watch the browser
```

Test class suffix decides the phase: `*Test` → `test` (Surefire),
`*IT` → `verify` (Failsafe). Never name a browserless test `*IT` or a
Playwright test `*Test`.

Docker must be running for `test`, `verify`, and `generate-sources`. **If
you add or change a migration, jOOQ classes won't update until you re-run
`generate-sources` (or any later phase).**

## Coverage

Coverage spans **both** test layers and the two JaCoCo reports are merged, so a
line reached only by a Playwright `*IT` counts as covered — **don't add a
browserless test just to cover something a `TC<NNN><Name>IT` already
exercises.** Only `./mvnw verify` produces the merged report. Details:
[`docs/architecture/development.md`](docs/architecture/development.md#coverage).

## When to read the detailed guides

- **Before implementing a use case** → read the corresponding
  `docs/use_cases/UC-NNN-*.md` spec first, and follow every `GR-NNN` link in
  its business rules into
  [`docs/business_rules.md`](docs/business_rules.md). Together they define
  preconditions, the main success scenario, alternative flows, postconditions,
  business rules, field labels, and navigation. The spec is the source of
  truth.

- **Before implementing a use case, writing a view, adding a repository, or
  touching anything in `src/main/java/`** → read
  [`docs/architecture/development.md`](docs/architecture/development.md)
  first. It covers the stack, package layout, jOOQ mapping patterns, the
  transaction boundary, Vaadin view conventions, the shell exception, form
  validation, error handling, the `*Repository` stereotype rule, and the
  build. Much of it is enforced by ArchUnit in `ArchitectureTest`
  (`./mvnw test`) — if you change a convention there, update the matching
  rule in the same commit.

- **Before changing a transaction, a save path, or anything about how the
  application behaves at runtime** → read
  [`docs/architecture/process.md`](docs/architecture/process.md). It holds the
  transaction rule (the repository is the boundary, never a view), the
  check-then-act on duplicate pet names that the database backs up, and the
  fact that nothing here is asynchronous — don't add a job, an event, or server
  push that no use case asks for.

- **Before writing or modifying any test under `src/test/java/`** → read
  [`docs/architecture/testing.md`](docs/architecture/testing.md) first. It
  covers `SpringBrowserlessTest`, the `UC<NNN><Name>Test` / `*IT` naming
  rule, the `@UseCase` annotation, `PetClinicTestBase`, seed-data
  conventions, locator patterns, the Playwright `*IT` layer, and what you
  must **not** do (no public test getters, no `assertNotNull` on fields, no
  field reach-in, no Karibu).

Do not skip these. The files are short and kept current — drift between
them and the code is a bug to fix, not a style preference to ignore.

## Skills available for this project

Prefer these over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`,
  `aiup-core:use-case-diagram`, `aiup-core:requirements` — authoring/updating
  specs in `docs/`.
- `aiup-vaadin-jooq:flyway-migration` — generate `V*.sql` from the entity
  model.
- `aiup-vaadin-jooq:implement` — implement a use case end-to-end (view +
  jOOQ queries). Already honours `docs/architecture/development.md`.
- `aiup-vaadin-jooq:browserless-test` — server-side Vaadin view tests
  (`SpringBrowserlessTest`, `find()` locators), named `UC<NNN><Name>Test`, run
  by `./mvnw test`. Default for UC tests; see `docs/architecture/testing.md`.
- `aiup-vaadin-jooq:playwright-test` — browser-based tests with Drama
  Finder, named `UC<NNN><Name>IT` / `TC<NNN><Name>IT`, run by
  `./mvnw verify`. Use for test cases (TC-*) and journeys that need a real
  browser. The `karibu-test` skill is obsolete.
- `aiup-core:test-case` — author `docs/test_cases/TC-NNN-*.md`.
- `aiup-vaadin-jooq:coverage-check` — audit a UC/TC against code and tests
  before changing its `Status:` line.
