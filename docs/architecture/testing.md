# Testing

Read this **before writing or modifying any test** under `src/test/java/`.

## Two test layers — the class suffix picks the Maven phase

| Layer                 | Suffix  | Runs in         | Framework                                              | Skill                               |
|-----------------------|---------|-----------------|--------------------------------------------------------|-------------------------------------|
| Server-side view test | `*Test` | `./mvnw test`   | Vaadin Browserless (`SpringBrowserlessTest`)           | `aiup-vaadin-jooq:browserless-test` |
| Browser test          | `*IT`   | `./mvnw verify` | Playwright + Drama Finder (`AbstractBasePlaywrightIT`) | `aiup-vaadin-jooq:playwright-test`  |

Surefire picks up `*Test`, Failsafe picks up `*IT` (both configured via `spring-boot-starter-parent`; the failsafe
plugin is declared in `pom.xml`). A browserless test named `*IT` or a Playwright test named `*Test` runs in the wrong
phase — treat that as a bug. Browserless is the default for use cases; Playwright is for test cases (`TC-*`) and
anything that genuinely needs a browser (client-side rendering, keyboard focus, scrolling).

## Framework

- **Vaadin Browserless Testing** (`browserless-test-junit6` + `browserless-test-spring`) is the default for Vaadin view
  tests — server-side, no
  browser, no servlet container.
  Reference: https://vaadin.com/docs/latest/flow/testing/browserless/getting-started
- View tests extend `PetClinicTestBase` (or `SpringBrowserlessTest` directly for tests that don't touch the DB, in
  which case add `@SpringBootTest` and `@Import(TestcontainersConfiguration.class)` yourself).
  They run inside a Spring context and use the same Testcontainers Postgres as the rest of the tests.
- **Use the `aiup-vaadin-jooq:browserless-test` skill** to scaffold a browserless test for a use case. Prefer it over
  hand-rolling — it knows the conventions in this file (locators, `@UseCase`, naming, etc.).
- **Karibu is gone.** Any `com.github.mvysny.kaributesting.*` import is a mistake — the project is all
  `SpringBrowserlessTest` / `ComponentQuery` now. The `karibu-test` skill is obsolete.

## Test class naming — `UC<NNN><UseCaseName>Test`

Tests that verify a use case are named after the use case, **not** the view. Format:
`UC<NNN><UseCaseNameInPascalCase>Test`, e.g `UC001ViewWelcomePageTest`, `UC004FindOwnersByLastNameTest`,
`UC007AddPetToOwnerTest`. The `NNN` matches the `UC-NNN-*.md` file in `docs/use_cases/`.

If a view is touched by multiple use cases, write one `UC<NNN>…Test` class per use case rather than one `XxxViewTest`
covering all of them. Keep the test file in the **same package as the view under test** and name the class after the UC.

**Exception — cross-cutting tests.** `ArchitectureTest` (the ArchUnit rules behind
[`development.md`](development.md)) and `AiupPetclinicApplicationTests` verify no single use case, so they sit in the
root package under their own names. Everything that *does* verify a use case follows the format above.

## `@UseCase` on every test method

Every `@Test` method that verifies UC behaviour must carry `ai.unifiedprocess.petclinic.UseCase` **on the method**
(never on the class — the annotation is `@Target(METHOD)` and cannot go on a class):

```java

@Test
@UseCase(id = "UC-004", scenario = "A2: Exact match", businessRules = "BR-001")
void singleMatchNavigatesDirectlyToDetails() { ...}
```

- `id` — required, matches a `docs/use_cases/UC-NNN-*.md` file.
- `scenario` — optional, defaults to `"Main Success Scenario"`. Set it for alternative flows (`"A1: Validation Errors"`,
  `"A2: Owner not found"`).
- `businessRules` — optional array of BR IDs. One rule as `businessRules = "BR-001"`, several as
  `businessRules = {"BR-001", "BR-002"}` — **never** as one comma-separated string `"BR-001, BR-002"`.
  `UseCaseTraceabilityTest` rejects anything that is not a bare `BR-NNN` identifier.

- **Placement** — `@UseCase` belongs on a `UC<NNN><Name>Test` method and nowhere else, and the `NNN` in the class name
  must match the `id` it annotates. A journey test does not borrow it to claim one of the use cases it walks through;
  that is what `@TestCase` is for. `UseCaseTraceabilityTest` enforces both halves.

This is the machine-readable spec → test link. `docs/` is the source of truth, so every test points at the exact spec
element it covers.

## `@TestCase` on every journey test

Every `TC<NNN><Name>IT` class must carry `ai.unifiedprocess.petclinic.TestCase` **on the class** (never on a method —
the annotation is `@Target(TYPE)`), because a test case has exactly one coverage unit: the journey.

```java

@TestCase(id = "TC-001", useCases = {"UC-003", "UC-004", "UC-005", "UC-007", "UC-009"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "vaadin.launch-browser=false")
@Import(TestcontainersConfiguration.class)
class TC001NewOwnerFirstVisitIT extends AbstractBasePlaywrightIT { ...
}
```

- `id` — required, matches a `docs/test_cases/TC-NNN-*.md` file, and must agree with the `NNN` in the class name.
- `useCases` — required, the journey's itinerary: exactly the use cases the document's Flow table links, no more and no
  fewer. This is the part the class name cannot express, and it is what the annotation is for — add a step to the Flow
  without adding it here and `TestCaseTraceabilityTest` says so.

The two annotations split cleanly: `@UseCase` on a method of a `UC<NNN>…Test`, `@TestCase` on a `TC<NNN>…IT` class.
Each sensor rejects its own annotation anywhere else.

## The traceability sensors

`docs/` is the source of truth, and three sensors make that claim checkable. All run in `./mvnw test`, all read the
documents from disk rather than trusting a summary, and all list every violation at once so the failure reads as a
work list:

| Sensor                         | Reads                                        | Links the two sides by         |
|--------------------------------|----------------------------------------------|--------------------------------|
| `UseCaseTraceabilityTest`      | `docs/use_cases/` ↔ `@UseCase` methods       | the `@UseCase` annotation      |
| `TestCaseTraceabilityTest`     | `docs/test_cases/` ↔ `TC<NNN>…IT` classes    | the class name and `@TestCase` |
| `BusinessRuleTraceabilityTest` | `docs/business_rules.md` ↔ `docs/use_cases/` | the `GR-NNN` id and its link   |

The shared markdown handling (the `**Status:**` pattern, the file scan, the violation report) lives in
`SpecDocuments`, so the document format is described in one place.

### Use cases — `UseCaseTraceabilityTest`

It reads
every `docs/use_cases/*.md` and every `@UseCase` annotation on the test classpath and compares the two, in both
directions.

**Referential integrity** — applies to every use case, whatever its status. An annotation may not point at something
that does not exist:

- every `id` resolves to a `docs/use_cases/UC-NNN-*.md` file,
- every `scenario` is either the main success scenario or a literal `### A1: …` heading in that file,
- every `businessRules` entry is a `BR-NNN` identifier and a literal `### BR-NNN: …` heading in that file.

So renaming an alternative flow in a spec breaks the build until the annotations follow. That is the point.

**Coverage** — applies only to a use case whose `Status:` is `Done` or `Tested`. Such a use case must have a test for
its main success scenario, for *every* alternative flow, and for *every* business rule. Any other status (`Draft`,
`Specified`, …) is exempt, because this project writes the specification before the code and an unimplemented use case
is a normal intermediate state, not a defect.

The consequence is that **the `Status:` line is an assertion, not a label**. Setting it to `Done` switches the sensor
on for that use case; if the tests are not there, `./mvnw test` says so and names each gap. Use
`aiup-vaadin-jooq:coverage-check` before you change a status line.

When the sensor fails, the honest fixes are to write the missing test or to correct the status — never to weaken the
check.

### Business rules — `BusinessRuleTraceabilityTest`

This one links two documents rather than a document and a test. A rule several use cases share is stated once in
[`../business_rules.md`](../business_rules.md) as `GR-NNN`; each use case keeps its own `BR-NNN` heading and says which
shared rule it realizes. That saves the rule from being written down twice — but only while both sides still point at
each other, and markdown enforces nothing. What the sensor checks:

- every `GR-NNN` a use case references is a `## GR-NNN: …` heading in the catalogue,
- every link into the catalogue from anywhere under `docs/` lands on a heading that really exists — so a rule cannot be
  renamed without the documents pointing at it following along,
- the `**Realized by:**` line of a rule names exactly the use case rules that reference it, in both directions: a claim
  nobody honours fails, and a reference nobody claims fails too,
- the summary table at the top agrees with the rules below it — that table is the one duplication the catalogue keeps,
  which is why it is the one that has to be checked,
- every rule in the catalogue is realized by **at least two** use cases. A rule one use case needs belongs in that use
  case, where it is read together with the flow it constrains.

There is no status line here and nothing is exempt: unlike a use case, a shared rule has no "not written yet" state —
it exists or it does not.

### Test cases — `TestCaseTraceabilityTest`

A test case has one coverage unit — the journey — realized by one class named `TC<NNN><Name>IT` and carrying
`@TestCase`. The name and the annotation are both links, and the sensor makes them agree rather than letting either
drift: the id is checked against the class name, and `useCases` against the Flow table. That itinerary is the
annotation's
reason to exist — without it the annotation would only restate the class name, which is a second place for the same fact
to go wrong. What the sensor checks:

- every `**Status:**` is one of `Draft`, `Reviewed`, `Approved`, `Automated`, `Obsolete`,
- every `[UC-NNN](../use_cases/…)` link in a Flow row resolves to a file that really exists and really is that use case,
- a test case whose status is `Automated` has a `TC<NNN>…IT` class on the test classpath,
- every `TC<NNN>…IT` class has a test case document behind it — no orphan journeys,
- every `TC<NNN>…IT` class carries `@TestCase`, and nothing else does,
- the annotated `id` agrees with the class name,
- the annotated `useCases` are real use cases and are exactly the ones the Flow table links,
- an `Automated` test case only walks through use cases that are themselves `Done` or `Tested`; a green end-to-end test
  standing on a `Draft` use case means one of the two status lines is lying.

As with use cases, `Automated` is the assertion that switches the sensor on. Everything below it is exempt, because
this project writes the test case before the test.

## Test base class

Extend `ai.unifiedprocess.petclinic.PetClinicTestBase`:

- Extends `SpringBrowserlessTest`, carries `@SpringBootTest`, `@Import(TestcontainersConfiguration.class)` and
  `@Transactional` for per-test rollback — subclasses need none of these (repeating them is harmless).
- Exposes canonical seed-data IDs as constants (`OWNER_FRANKLIN_ID`, `OWNER_COLEMAN_ID`, `PET_SAMANTHA_ID`,
  `PET_MAX_ID`, …) that match
  `src/test/resources/db/migration/V2__seed_reference_data.sql`. Add a constant there when a new test needs another
  seed row — never hard-code a numeric ID in a test.
- For not-found / error flows navigate by location string, `UI.getCurrent().navigate("owners/999999")`, because the
  typed `navigate(View.class, params)` asserts the resulting view type and throws when the router rerouted.
- Tests that don't need the DB (`UC001ViewWelcomePageTest`) can still extend `SpringBrowserlessTest` directly.

Test seed data lives **only** on the test classpath (`src/test/resources/db/migration/V2__seed_reference_data.sql`). Do
**not** put seed data under `src/main/resources/db/migration/`.

## Core browserless API

- `navigate(MyView.class)` — routes to the view and returns the view instance (useful as a `from(view)` scope).
  `navigate(MyView.class, Map.of(OwnerRouteParameters.OWNER_ID, "6"))` for parameterized routes.
- `test(component).setValue(...)` / `test(component).click()` / `test(combo).selectItem(...)` — wrap a component to
  simulate user interaction. Prefer this over calling setters/listeners directly.
- `find(Type.class)` — query the current UI tree by type. Chainable matchers:
    - `withPropertyValue(Type::getter, value)` — type-safe, preferred.
    - `withAttribute("name", "value")` — only when no getter exists.
    - `withId(...)`, `withText(...)`, `withValue(...)`, `withClassName(...)`,
      `withCondition(...)`, `withCaption(...)`.
      Terminators: `single()` (exactly one) or `atIndex(int)`. **`first()` is deprecated.**
- `find(Type.class).from(parent)` — scope the query to a sub-tree rooted at `parent`.
- `fireShortcut(Key.ENTER)` / `fireShortcut(Key.KEY_S, KeyModifier.CONTROL)`
  — simulate keyboard shortcuts.

## No direct field access — use locators

All component fields on views and forms are **private**. Tests must **never** access view fields directly. Use `find()`
locators to find components in the live UI tree. This decouples the test from the view's internal layout and exercises
the render path end-to-end.

- Find input components by caption (label): `find(TextField.class).withCaption("Last name").single()`
- Find buttons by text: `find(Button.class).withText("Find Owner").single()`
- Find a grid (usually one per view): `find(Grid.class).single()`
- Scope to a parent: `find(H3.class).from(details).all()`
- Verify rendered text: `assertDoesNotThrow(() -> find(Paragraph.class).withText("Jane Doe").single(), "message")`

Interact through the tester wrapper:

```java
test(find(TextField.class).

withCaption("First Name").

single()).

setValue("Jane");

test(find(Button.class).

withText("Add Owner").

single()).

click();

test(find(ComboBox.class).

withCaption("Type").

single()).

selectItem("dog");
```

Check validation state via the locator:

```java
assertTrue(find(TextField.class).

withCaption("Telephone").

single().

isInvalid());

assertEquals("not found",find(TextField.class).

withCaption("Last name").

single().

getErrorMessage());
```

## Scoping with `from()` vs. global `find()`

Use `find(Type.class).from(parent)` when the view contains multiple instances of the same component type (e.g. two `H3`
elements, or fields with the same label in different forms). Global `find()` is fine when the component type or
caption is unique in the view.

## Positive vs. negative locator assertions

**Component should exist** — wrap the locator in `assertDoesNotThrow`:

```java
assertDoesNotThrow(
        () ->

find(Image .class).

withPropertyValue(Image::getSrc, "images/pets.png").

single(),
        "Expected exactly one decorative pets image");
```

**Component should NOT exist** — invisible components are excluded from `find()` queries, so verify the query returns
nothing:

```java
assertTrue(find(Grid.class).

all().

isEmpty(), "Expected results grid to be hidden");
```

## No public test-only getters

Do **not** add public getters to view classes so tests can reach private fields (`getForm()`, `getResultsGrid()`,
`getLastNameField()`, etc.). All component fields are private. Tests use locators.

## Assertions

- **Never `assertNotNull` on a component reference.** A component created in the constructor cannot be null — the
  assertion is vacuous and proves nothing about rendering. Use a locator to verify the component is in the tree.
- **Always wrap locator calls in `assertDoesNotThrow`** with a message:
  ```java
  assertDoesNotThrow(
          () -> find(Image.class).withPropertyValue(Image::getSrc, "images/pets.png").single(),
          "Expected exactly one decorative pets image");
  assertDoesNotThrow(
          () -> navigate(WelcomeView.class),
          "Expected root route to resolve to WelcomeView");
  ```
  `find(…).single()` throws `NoSuchElementException` on its own, but a bare call reads as a dead statement and the CI
  failure is uninformative.
- **Never `.all().stream().anyMatch(...)`.** Chain the locator matchers instead — it's type-safe and the failure message
  is informative.
- **Rendered-state assertions** go through `find(Paragraph.class)`, `find(H3.class)`, etc. and assert on `.getText()` so
  the
  render path is exercised end-to-end.
- **Navigation assertions** check
  `UI.getCurrent().getInternals().getActiveViewLocation().getPath()`, not domain state pulled back out of the view.

## Integration test configuration

`TestcontainersConfiguration` (under `src/test/java/ai/unifiedprocess/petclinic/`) provides the Postgres 17-alpine
container via `@ServiceConnection`. `PetClinicTestBase` composes this automatically — you don't need to import it
yourself. `TestAiupPetclinicApplication` reuses it for `./mvnw spring-boot:test-run`.

## Playwright `*IT` tests

Browser tests are the second layer, not a replacement for browserless tests. Rules that differ from the above:

- Class name `UC<NNN><UseCaseName>IT` for a use case, `TC<NNN><TestCaseName>IT` for a test case document under
  `docs/test_cases/` (e.g. `TC001NewOwnerFirstVisitIT`). Same package as the view under test; journey tests go in
  `ai.unifiedprocess.petclinic.e2e`.
- Extend `org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT`, annotate
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Import(TestcontainersConfiguration.class)`, inject
  `@LocalServerPort`. The Drama Finder dependency (`org.vaadin.addons:dramafinder`, test scope) is added to `pom.xml`
  by the `playwright-test` skill when the first `*IT` is written.
- Locate elements through Drama Finder element wrappers (`ButtonElement.getByText`, `TextFieldElement.getByLabel`, …)
  — never raw `page.locator(...)`, never `Thread.sleep`.
- No `@Transactional` rollback across a real HTTP round-trip: data a test creates is cleaned up in `@AfterEach`
  (through the UI or targeted deletes, idempotent). A test case's **Postconditions** section is the cleanup contract.
- `@UseCase` on every test method applies here too.
- Run with `./mvnw verify` (all) or `./mvnw verify -Dit.test=TC001NewOwnerFirstVisitIT`;
  add `-Dheadless=false` to watch the browser.
