# Architecture

Read this **before implementing** anything in `src/main/java/`.

## Module layout

Single Maven module, **package-by-feature** under `ai.unifiedprocess.petclinic`. Each feature is its own package —
`welcome` exists today; `owner`, `pet`, `visit`, and `vet` follow as their use cases are implemented — with these
sub-packages:

- **`ui`** — Vaadin views, forms, and other UI components. One view per use case / screen.
- **`domain`** — domain types (records) and jOOQ query logic. Queries are written against the generated
  `ai.unifiedprocess.demo.petclinic.database.*` tables/records. **No JPA, no Spring Data repositories.**
- **`web`** — only when a use case demands a machine-readable representation (e.g. UC-002 A1: `/vets` as JSON/XML).
  Holds the `@RestController` and its response container record (e.g. `Vets`). Never add a `web` package for UI-only
  use cases; jOOQ queries still live in `domain` and are shared with the views.

`ui/` at the root (`ai.unifiedprocess.petclinic.ui`) holds the app shell (`MainLayout`) and the shared error views
(`NotFoundErrorView`, `ApplicationErrorView`, `ErrorPanel`) once they exist.

The project is intentionally thin on layers — there is no separate service/repository/DTO layering beyond `ui` +
`domain` unless a use case demands it. Put jOOQ query logic close to where it's used until duplication justifies
extraction.

## Cross-feature rule — and its one exception

Cross-feature reach-in goes through the other feature's `domain` package, not its `ui`.

**Exception — the app shell and detail aggregators.** `ui/MainLayout` and views that aggregate cross-feature
actions (e.g. `OwnerDetailsView` linking to `AddPetView`, `EditPetView`, `AddVisitView`) may import other features'
`ui/` classes **as `.class` route tokens** for `SideNavItem` or `ui.navigate(...)`. No instantiation, no method calls,
no state reads — the token is only a routing key. Any richer interaction must still go through the other feature's
`domain` package.

## Data access (jOOQ)

- **All repositories use `org.jooq.Records.mapping(Type::new)` as the terminal mapper.** Never `fetchInto(Type.class)`,
  never a manual `map(r -> new Type(...))`. Constructor references give compile-time checking of column order against
  record components.
- **Nested records load via `row(...).mapping(Nested::new)`** inside the select list:
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
- **Parent→children collections use `multiset(...).convertFrom(...)`** in one query to avoid N+1 — e.g.
  `VetRepository.findPage` (vet → specialties) and `OwnerRepository.findListingByLastNamePrefix` (owner → pet names)
  when those are implemented. Any repository feeding a grid with a to-many must do the same.
- **Lazy-loaded grids return `Stream<T>`** from a `findPage(int offset, int limit)` plus a paired `int count()`. The
  Vaadin view wires them as a callback data provider.
- **Domain records stay validation-free.** No Bean Validation annotations, no constructor checks — the Vaadin form is
  the validation boundary (see below).

## Persistence stereotype

Persistence classes are named `<Entity>Repository` and annotated `@Repository` — not `*Queries`, `*Dao`, `*Store`. The
ban on *Spring Data* does not extend to the stereotype: `@Repository` is what enables
`PersistenceExceptionTranslationPostProcessor` to translate JDBC `SQLException`s into Spring's `DataAccessException`
hierarchy. Plain `@Component` silently opts out.

## Vaadin view conventions

- **Route parameters centralized per feature.** E.g. `owner/ui/OwnerRouteParameters` defines `OWNER_ID` / `PET_ID`
  constants and builder methods (`forOwner(id)`, `forPet(ownerId, petId)`) so route parameter names can't drift between
  views. **Check for an existing `*RouteParameters` class in the feature package before adding a parameterized route —
  create one if it doesn't exist yet.**
- **Styling — never `component.getStyle().set(...)`.** Always use `addClassNames(LumoUtility.…)` constants (e.g.
  `LumoUtility.Padding.Horizontal.MEDIUM`, `LumoUtility.Margin.NONE`, `LumoUtility.FontSize.LARGE`). If a utility class
  doesn't cover what you need, ask rather than falling back to inline styles.
- **Main menu — `SideNav` + `SideNavItem` in the drawer.** Not `Tabs`, not hand-rolled `RouterLink`s in a
  `HorizontalLayout`. Use `new SideNavItem(label, ViewClass.class)` so active-route highlighting and a11y semantics come
  for free. The navbar holds only `DrawerToggle`, logo, and title. `MainLayout` currently points the not-yet-implemented
  entries (`owners/find`, `vets`, `oups`) at string routes — **switch each to the `.class` form when its view lands**
  so a renamed route fails at compile time.
- **One view per use case, per route.** Put the UC id in the class Javadoc so grep from a failing test lands on the
  right file.
- **Validation lives in the form, not the domain.** Shared forms (`OwnerForm`, `PetForm`) expose `validateAndRead(...)`
  that runs field-level validation (required, regex, date range, type required) and returns a populated domain record on
  success or surfaces field errors on failure. Forms also expose helpers like `rejectName(...)` so views can push
  late-discovered errors (e.g. duplicate check after a DB lookup) back onto the right field. Views call
  `ownerForm.validateAndRead(existingId)` and only touch the repository
  once validation passes.
- **`NotFoundException` in `beforeEnter` for missing entities.** Throwing `com.vaadin.flow.router.NotFoundException`
  from `beforeEnter` — e.g. when `OwnerRepository.findById` returns empty — is the canonical way to
  surface a 404. The router automatically routes to `NotFoundErrorView`, which wins over `ApplicationErrorView` because
  its generic parameter is narrower.
- **Error views render inside `MainLayout`.** The shell stays functional so the user is never stranded.
  `NotFoundErrorView` handles `HasErrorParameter<NotFoundException>` (HTTP 404); `ApplicationErrorView` handles
  `HasErrorParameter<Exception>` (HTTP 500). Both delegate rendering to `ErrorPanel` and live in
  `ai.unifiedprocess.petclinic.ui`. Neither shows a stack trace — message only. Create them with the first use case
  that needs a 404 or the `oups` route; they do not exist yet.

## Flyway

- Production migrations: `src/main/resources/db/migration/V1__initial_schema.sql`.
- Test seed data: `src/test/resources/db/migration/V2__seed_reference_data.sql` (test classpath only — Flyway picks both
  up automatically when tests run).
- **After changing a migration**, run `./mvnw generate-sources` (or any later phase) to refresh jOOQ classes. The
  migrations effectively *are* the schema DSL.
