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
- `docs/test_cases/TC-NNN-*.md` — end-to-end journeys spanning several use
  cases; each is verified by a Playwright `TC<NNN><Name>IT`.

Two sensors (`./mvnw test`) enforce the link between specs and tests:
`UseCaseTraceabilityTest` and `TestCaseTraceabilityTest`. Because of them a
`Status:` line (`Done`/`Tested` on a use case, `Automated` on a test case) is
an assertion that switches the sensor on, not a label — never set one by hand
without running `aiup-vaadin-jooq:coverage-check` first. What each sensor
checks is in
[`docs/guidelines/testing.md`](docs/guidelines/testing.md#the-traceability-sensors).

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Stack

- **Java 25**, **Spring Boot 4.1.1**, **Vaadin 25.2**
- Application code lives under package `ai.unifiedprocess.petclinic`
  (package-by-feature, see `docs/guidelines/architecture.md`).
- **jOOQ** for type-safe SQL — generated sources live in
  `target/generated-sources/jooq` under package
  `ai.unifiedprocess.demo.petclinic.database` (note the extra `demo`
  segment — it differs from the application package on purpose).
- **Flyway** migrations in `src/main/resources/db/migration`
  (`V1__initial_schema.sql` covers the full entity model; test-only seed
  data lives in `src/test/resources/db/migration/V2__seed_reference_data.sql`)
- **PostgreSQL** in prod; **Testcontainers** (`postgres:17-alpine`) for
  tests *and* for jOOQ code generation at build time
- **Vaadin Browserless Testing** (`browserless-test-junit6` + `browserless-test-spring`) for
  server-side view tests; **Playwright + Drama Finder** for browser-based
  integration tests (dependency added by the `playwright-test` skill when
  the first `*IT` is written)

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

Coverage is what the SonarQube quality gate measures, and it spans *both*
test layers. The JaCoCo agent runs in the Surefire JVM and the Failsafe JVM
separately, writing `target/jacoco-ut.exec` and `target/jacoco-it.exec`;
`post-integration-test` merges the two and writes the report Sonar reads at
`target/site/jacoco-merged/jacoco.xml`. A line reached only by a Playwright
`*IT` therefore still counts as covered — **don't add a browserless test just
to cover something a `TC<NNN><Name>IT` already exercises.**

Only `./mvnw verify` produces that report. `./mvnw test` runs the agent but
stops before the merge, so it leaves no merged report behind — check coverage
with `verify`, not `test`.

Generated jOOQ sources are excluded from analysis (`sonar.exclusions`), so
they neither help nor hurt the number. The gate wants 80% on new code; the
hand-written code sits around 97%.

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
  the `*Repository` stereotype rule. Much of it is enforced by ArchUnit in
  `ArchitectureTest` (`./mvnw test`) — if you change a convention there,
  update the matching rule in the same commit.

- **Before writing or modifying any test under `src/test/java/`** → read
  [`docs/guidelines/testing.md`](docs/guidelines/testing.md) first. It
  covers `SpringBrowserlessTest`, the `UC<NNN><Name>Test` / `*IT` naming
  rule, the `@UseCase` annotation, `PetClinicTestBase`, seed-data
  conventions, locator patterns, the Playwright `*IT` layer, and what you
  must **not** do (no public test getters, no `assertNotNull` on fields, no
  field reach-in, no Karibu).

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
- `aiup-vaadin-jooq:browserless-test` — server-side Vaadin view tests
  (`SpringBrowserlessTest`, `find()` locators), named `UC<NNN><Name>Test`, run
  by `./mvnw test`. Default for UC tests; see `docs/guidelines/testing.md`.
- `aiup-vaadin-jooq:playwright-test` — browser-based tests with Drama
  Finder, named `UC<NNN><Name>IT` / `TC<NNN><Name>IT`, run by
  `./mvnw verify`. Use for test cases (TC-*) and journeys that need a real
  browser. The `karibu-test` skill is obsolete.
- `aiup-core:test-case` — author `docs/test_cases/TC-NNN-*.md`.
- `aiup-vaadin-jooq:coverage-check` — audit a UC/TC against code and tests
  before changing its `Status:` line.
