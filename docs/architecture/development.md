# Development View

**Question:** How is the code organized, built, and kept honest?  
**Artifacts:** this document, [`testing.md`](testing.md) (the testing half of this
view), `CLAUDE.md` (the short form an agent always has loaded)  
**Notation:** Markdown  
**For the agent:** the most important view, and the one to read **before writing
anything** in `src/main/java/`. Every rule here saves a correction in every use
case.

Most of what follows is **enforced executably** by
`src/test/java/ai/unifiedprocess/petclinic/ArchitectureTest.java` (ArchUnit, runs
with `./mvnw test`). Each rule names the section it comes from, so a failure
points straight back here. If you change a convention below, change the matching
rule in the same commit — a rule that contradicts this document is the rule
that's wrong.

These rules are the harness for use cases that have **not been written yet**, so
a rule may legitimately match no classes — on a branch where only UC-001 exists
there is no `domain` package to check. `src/test/resources/archunit.properties`
sets `archRule.failOnEmptyShould=false` for exactly that reason: a rule about
code that does not exist yet is vacuously satisfied, the same stance the
traceability sensors take towards a use case that is not `Done` yet.

## Technology stack

| Concern               | Choice                                               | Version      | Decided in                                                |
|-----------------------|------------------------------------------------------|--------------|-----------------------------------------------------------|
| Language / runtime    | Java                                                 | 25           | C-001                                                     |
| Application framework | Spring Boot                                          | 4.1.1        | C-002                                                     |
| UI                    | Vaadin Flow (server-side Java views)                 | 25.2         | [ADR-002](adr/ADR-002-vaadin-flow-server-side-ui.md)      |
| Database access       | jOOQ, generated from the schema                      | 3.21         | [ADR-001](adr/ADR-001-jooq-instead-of-jpa.md)             |
| Schema migration      | Flyway                                               | Boot-managed | [ADR-003](adr/ADR-003-flyway-owns-the-schema.md)          |
| Database              | PostgreSQL (`postgres:17-alpine` for build and test) | 17           | C-005                                                     |
| Build                 | Maven (wrapper), single module                       | —            | —                                                         |
| View tests            | Vaadin Browserless Testing                           | Vaadin BOM   | [ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md) |
| Browser tests         | Playwright + Drama Finder                            | 1.1          | [ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md) |
| Architecture tests    | ArchUnit                                             | 1.5          | —                                                         |
| Coverage              | JaCoCo, merged across both test layers               | 0.8          | —                                                         |
| Quality gate          | SonarQube Cloud                                      | —            | —                                                         |

Exact versions are in `pom.xml`, which wins over this table.

## Module layout

Single Maven module, **package by feature** under
`ai.unifiedprocess.petclinic`. The features follow the use cases — `owner`,
`pet`, `visit`, `vet`, `welcome` — and each is one package with exactly two
sub-packages:

- **`ui`** — Vaadin views, forms, and other UI components. One view per use
  case / screen.
- **`domain`** — domain types (records) and jOOQ query logic, written against
  the generated `database.*` tables. **No JPA, no Spring Data repositories.**

`core/` holds the app shell (`MainLayout`) and the shared error views.

```
src/main/java/ai/unifiedprocess/petclinic/
  <feature>/ui/       Vaadin views, forms, route parameter holders
  <feature>/domain/   records + <Entity>Repository (jOOQ)
  core/ui/            MainLayout, ErrorPanel, error views
src/main/resources/db/migration/     production schema
src/test/resources/db/migration/     test seed data (test classpath only)
target/generated-sources/jooq/       ai.unifiedprocess.demo.petclinic.database (generated, never edited)
```

The project is intentionally thin on layers — **no service, DTO, or mapper
layer** beyond `ui` + `domain` unless a use case demands one. Put jOOQ query
logic close to where it is used until duplication justifies extraction
([ADR-004](adr/ADR-004-package-by-feature-ui-domain.md)).

The generated jOOQ package carries an extra `demo` segment on purpose, so no
wildcard over the application package can match generated code — that is what
makes `sonar.exclusions` safe.

## Cross-feature rule — and its one exception

Cross-feature reach-in goes through the other feature's `domain` package, not
its `ui`.

**Exception — the app shell and detail aggregators.** `core/ui/MainLayout` and
views that aggregate actions of other features (the owner details screen of
UC-005 links to the add-pet, edit-pet, and add-visit screens) may import other
features' `ui/` classes **as `.class` route tokens** for `SideNavItem` or
`ui.navigate(...)`. No instantiation, no method calls, no state reads — the
token is only a routing key. Any richer interaction still goes through the other
feature's `domain` package.

Because of that exception the *feature*-level dependency graph is cyclic by
design (`owner` → `visit` → `pet` → `owner`), so
`ArchitectureTest.noCyclesBetweenFeatures` slices on `(*).domain..` instead: the
`domain` packages have no cross-feature dependencies at all and must stay that
way.

## Data access (jOOQ)

- **Every repository uses `org.jooq.Records.mapping(Type::new)` as the terminal
  mapper.** Never `fetchInto(Type.class)`, never a manual
  `map(r -> new Type(...))`. Constructor references give compile-time checking
  of column order against record components.
- **Nested records load via `row(...).mapping(Nested::new)`** inside the select
  list:
  ```java
  dsl.select(
          PETS.ID, PETS.NAME, PETS.BIRTH_DATE,
          row(TYPES.ID, TYPES.NAME).mapping(PetType::new),
          PETS.OWNER_ID)
     .from(PETS).join(TYPES).on(TYPES.ID.eq(PETS.TYPE_ID))
     .where(PETS.OWNER_ID.eq(ownerId))
     .orderBy(PETS.NAME.asc())
     .fetch(mapping(Pet::new));
  ```
- **Parent→children collections use `multiset(...).convertFrom(...)`** in one
  query to avoid N+1. Any repository feeding a grid with a to-many must do this
  — the vet directory with its specialties (UC-002) and the owner search results
  with their pet names (UC-004) both need it.
- **Lazy-loaded grids return `Stream<T>`** from a `findPage(int offset, int
  limit)` plus a paired `int count()`. The Vaadin view wires them as a callback
  data provider (NFR-001/002).
- **Domain records stay validation-free.** No Bean Validation annotations, no
  constructor checks — the form is the validation boundary (see below and
  [ADR-005](adr/ADR-005-validation-in-the-form.md)).

## Persistence stereotype

Persistence classes are named `<Entity>Repository` and annotated `@Repository` —
not `*Queries`, `*Dao`, `*Store`. The ban on *Spring Data* does not extend to
the stereotype: `@Repository` is what enables
`PersistenceExceptionTranslationPostProcessor` to translate JDBC
`SQLException`s into Spring's `DataAccessException` hierarchy. Plain
`@Component` silently opts out.

## Transaction boundary

**The repository is the transaction boundary**
([ADR-009](adr/ADR-009-repository-is-the-transaction-boundary.md)). Every
repository carries `@Transactional(readOnly = true)` at class level, and every
writing method overrides it with a plain `@Transactional`:

```java

@Repository
@Transactional(readOnly = true)
public class PetRepository {

    @Transactional
    public Integer insert(Pet pet) { …}
}
```

- **Never in a view.** A view method spans a user interaction, and a transaction
  that waits for a user is held far too long. A view calls repository methods one
  after the other; it does not wrap them.
- **`readOnly = true` on reads** is not decoration: it tells the driver and the
  database that no write will follow.
- **A use case needing two statements to succeed together** gets one
  `@Transactional` method in the feature's `domain` package that performs both —
  not two calls from the view. What that means at runtime is in
  [`process.md`](process.md#transaction-boundaries).

## Vaadin view conventions

- **One view per use case, per route.** Put the UC id in the class Javadoc so
  grep from a failing test lands on the right file.
- **Route parameters centralized per feature.** A `<Feature>RouteParameters`
  class defines the parameter name constants and builder methods, so parameter
  names cannot drift between views. **Check for an existing one in the feature
  package before adding a parameterized route — create it if it does not exist
  yet.**
- **Styling — never `component.getStyle().set(...)`.** Always
  `addClassNames(LumoUtility.…)` (e.g. `LumoUtility.Padding.Horizontal.MEDIUM`,
  `LumoUtility.Margin.NONE`). If no utility class covers what you need, ask
  rather than falling back to inline styles.
- **Main menu — `SideNav` + `SideNavItem` in the drawer.** Not `Tabs`, not
  hand-rolled `RouterLink`s in a `HorizontalLayout`. `new SideNavItem(label,
  ViewClass.class)` gives active-route highlighting and a11y semantics for free.
  The navbar holds only `DrawerToggle`, logo, and title.
- **Validation lives in the form, not the domain.** A shared `<Entity>Form`
  exposes `validateAndRead(...)`, which runs the use case's field-level rules and
  returns a populated domain record on success or surfaces field errors on
  failure. It also exposes helpers so a view
  can push a late error — one discovered only by a database lookup, such as a
  duplicate name — back onto the right field. A view calls the repository only
  after validation passes.
- **`NotFoundException` in `beforeEnter` for missing entities.** Throwing
  `com.vaadin.flow.router.NotFoundException` from `beforeEnter` — e.g. when a
  lookup by route parameter returns empty — is the canonical way to surface a
    404. The router routes to `NotFoundErrorView`, which wins over
         `ApplicationErrorView` because its generic parameter is narrower.
- **Error views render inside `MainLayout`.** The shell stays functional so the
  user is never stranded (NFR-005). `NotFoundErrorView` handles
  `HasErrorParameter<NotFoundException>` (HTTP 404), `ApplicationErrorView`
  handles `HasErrorParameter<Exception>` (HTTP 500). Both delegate rendering to
  `ErrorPanel`, and neither shows a stack trace — message only (NFR-004).

## Flyway

- Production migrations: `src/main/resources/db/migration/V*.sql`. They
  reproduce [`../entity_model.md`](../entity_model.md) exactly (NFR-009) — the
  migrations effectively *are* the schema DSL.
- Test seed data: `src/test/resources/db/migration/` (test classpath only —
  Flyway picks both up when tests run).
- **After changing a migration**, run `./mvnw generate-sources` (or any later
  phase) to refresh the jOOQ classes.

## Build

```bash
./mvnw spring-boot:test-run     # run locally, Testcontainers supplies PostgreSQL
./mvnw generate-sources         # regenerate jOOQ after a migration change
./mvnw test                     # *Test: browserless views, ArchUnit, traceability sensors
./mvnw verify                   # additionally *IT: Playwright, and the merged coverage report
```

What `verify` does, in order:

1. **`generate-sources`** — the `testcontainers-jooq-codegen` plugin starts
   `postgres:17-alpine`, runs the Flyway migrations against it, and generates
   jOOQ classes from the resulting schema. A migration change that is not
   followed by a code generation leaves stale classes behind.
2. **`build-frontend`** — the Vaadin production bundle.
3. **`test`** (Surefire) — every `*Test`, JaCoCo writing `jacoco-ut.exec`.
4. **`verify`** (Failsafe) — every `*IT`, JaCoCo writing `jacoco-it.exec`.
5. **`post-integration-test`** — the two exec files are merged into
   `target/site/jacoco-merged/jacoco.xml`, the report Sonar reads.

**Docker must be running** for `generate-sources`, `test` and `verify` (C-007).

CI is `.github/workflows/build.yml`: `mvn -B verify` plus the Sonar scanner on
every push to `main` and every pull request. `.github/workflows/aiup-generate.yml`
is the dispatch hook for AI Unified Process Studio and is not edited by hand.

## Coverage

Coverage is what the quality gate measures, and it spans **both** test layers
([ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md)). Because the two
JaCoCo reports are merged, a line reached only by a Playwright `*IT` counts as
covered — **do not add a browserless test just to cover something a journey test
already exercises.** Only `./mvnw verify` produces the merged report; `./mvnw
test` runs the agent but stops before the merge. Generated jOOQ sources are
excluded from analysis, so they neither help nor hurt the number. The gate wants
80 % on new code.

## Testing

The test conventions are the other half of this view and live in
[`testing.md`](testing.md): the two layers and what the class suffix decides,
`UC<NNN><Name>Test` naming, the `@UseCase` and `@TestCase` annotations, the
traceability sensors, the browserless API and locator patterns, and what you
must **not** do. Read it before writing or changing anything under
`src/test/java/`.

## Definition of done for a use case

1. The `UC-NNN-*.md` specification is the input; where code and specification
   disagree, the specification wins (C-008).
2. The code follows the conventions above; `./mvnw test` passes, ArchUnit
   included.
3. At least one test per use case, each `@Test` carrying `@UseCase` (NFR-006).
4. `./mvnw verify` is green and coverage has not dropped.
5. The `Status:` line is updated only after a coverage check.
6. If the change contradicts a view in this folder, the view changes with it —
   and a decision that could have gone the other way gets an [ADR](adr).
