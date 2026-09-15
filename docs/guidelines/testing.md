# Testing

Read this **before writing or modifying any test** under `src/test/java/`.

## Two test layers — the class suffix picks the Maven phase

| Layer                 | Suffix   | Runs in         | Framework                                  | Skill                              |
|-----------------------|----------|-----------------|--------------------------------------------|------------------------------------|
| Server-side view test | `*Test`  | `./mvnw test`   | Vaadin Browserless (`SpringBrowserlessTest`) | `aiup-vaadin-jooq:browserless-test` |
| Browser test          | `*IT`    | `./mvnw verify` | Playwright + Drama Finder (`AbstractBasePlaywrightIT`) | `aiup-vaadin-jooq:playwright-test`  |

Surefire picks up `*Test`, Failsafe picks up `*IT` (both configured via `spring-boot-starter-parent`; the failsafe
plugin is declared in `pom.xml`). A browserless test named `*IT` or a Playwright test named `*Test` runs in the wrong
phase — treat that as a bug. Browserless is the default for use cases; Playwright is for test cases (`TC-*`) and
anything that genuinely needs a browser (client-side rendering, keyboard focus, scrolling).

## Framework

- **Vaadin Browserless Testing** (`browserless-test-junit6` + `browserless-test-spring`) is the default for Vaadin view tests — server-side, no
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
[`architecture.md`](architecture.md)) and `AiupPetclinicApplicationTests` verify no single use case, so they sit in the
root package under their own names. Everything that *does* verify a use case follows the format above.

## `@UseCase` on every test method

Every `@Test` method that verifies UC behaviour must carry `ai.unifiedprocess.petclinic.UseCase` **on the method** (
never on the class — the annotation is `@Target(METHOD)` and cannot go on a class):

```java

@Test
@UseCase(id = "UC-004", scenario = "A2: Exact match", businessRules = "BR-001")
void singleMatchNavigatesDirectlyToDetails() { ...}
```

- `id` — required, matches a `docs/use_cases/UC-NNN-*.md` file.
- `scenario` — optional, defaults to `"Main Success Scenario"`. Set it for alternative flows (`"A1: Validation Errors"`,
  `"A2: Owner not found"`).
- `businessRules` — optional string array of BR IDs (`"BR-001"`, `"BR-002"`) when the test specifically exercises them.

This is the machine-readable spec → test link. `docs/` is the source of truth, so every test points at the exact spec
element it covers.

## Test base class

Extend `ai.unifiedprocess.petclinic.PetClinicTestBase`:

- Extends `SpringBrowserlessTest`, carries `@SpringBootTest`, `@Import(TestcontainersConfiguration.class)` and
  `@Transactional` for per-test rollback — subclasses need none of these (repeating them is harmless).
- Exposes canonical seed-data IDs as constants
  (`OWNER_FRANKLIN_ID`, `OWNER_COLEMAN_ID`, `PET_SAMANTHA_ID`, `PET_MAX_ID`, …) that match
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
test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
test(find(Button.class).withText("Add Owner").single()).click();
test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
```

Check validation state via the locator:

```java
assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid());
assertEquals("not found", find(TextField.class).withCaption("Last name").single().getErrorMessage());
```

## Scoping with `from()` vs. global `find()`

Use `find(Type.class).from(parent)` when the view contains multiple instances of the same component type (e.g. two `H3`
elements, or fields with the same label in different forms). Global `find()` is fine when the component type or
caption is unique in the view.

## Positive vs. negative locator assertions

**Component should exist** — wrap the locator in `assertDoesNotThrow`:

```java
assertDoesNotThrow(
        () -> find(Image.class).withPropertyValue(Image::getSrc, "images/pets.png").single(),
        "Expected exactly one decorative pets image");
```

**Component should NOT exist** — invisible components are excluded from `find()` queries, so verify the query returns
nothing:

```java
assertTrue(find(Grid.class).all().isEmpty(), "Expected results grid to be hidden");
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
- **Rendered-state assertions** go through `find(Paragraph.class)`, `find(H3.class)`, etc. and assert on `.getText()` so the
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
