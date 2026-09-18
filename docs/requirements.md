# Requirements

Requirements catalog for AIUP PetClinic, derived from [`vision.md`](vision.md).

Statuses reflect the state of **this branch**, where only UC-001 is built:

- **Verified** — exercised or enforced by something that runs here: a `UC*Test`,
  an ArchUnit rule, or the build itself.
- **Implemented** — the code exists, but no test covers it on this branch yet.
  The error views are the case in point: they are there, their use case is not
  `Done`, and no `UC010…Test` exists.
- **Open** — specified and agreed, not yet built.

On `main`, where every use case is implemented and tested, these read
**Verified** throughout.

## Functional Requirements

| ID     | Title                        | User Story                                                                                                                                              | Priority | Status      |
|--------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------|
| FR-001 | Welcome Page                 | As a visitor, I want to land on a welcome page that shows the clinic's identity so that I know I am in the right place.                                  | High     | Verified    |
| FR-002 | Main Navigation              | As a visitor, I want a navigation menu that is present on every screen so that I can reach the home page, owner search, and vet directory from anywhere. | High     | Implemented |
| FR-003 | Browse Veterinarians         | As a visitor, I want to see the clinic's veterinarians with the specialties each of them holds so that I know who treats what.                           | High     | Open        |
| FR-004 | Register Owner               | As a clinic user, I want to register a new owner with their contact details so that their pets and visits can be tracked from then on.                   | High     | Open        |
| FR-005 | Complete Contact Details     | As a clinic user, I want the system to insist on name, address, city, and telephone so that I can always reach an owner about their animal.              | High     | Open        |
| FR-006 | Usable Telephone Number      | As a clinic user, I want telephone numbers rejected unless they are exactly 10 digits so that the number I dial back is actually callable.               | High     | Open        |
| FR-007 | Find Owners by Last Name     | As a clinic user, I want to search owners by the beginning of a last name so that I can find a caller's record from the little they tell me.             | High     | Open        |
| FR-008 | Jump to Single Match         | As a clinic user, I want to be taken straight to an owner's record when the search matches only one owner so that I skip a pointless one-row list.       | Medium   | Open        |
| FR-009 | Browse All Owners            | As a clinic user, I want an empty search to list every owner so that I can browse the customer base without inventing a search term.                     | Low      | Open        |
| FR-010 | Report Unsuccessful Search   | As a clinic user, I want to be told when no owner matches my search so that I can correct the spelling instead of assuming the owner is unknown.         | High     | Open        |
| FR-011 | View Owner Record            | As a clinic user, I want one screen showing an owner's contact details, their pets, and each pet's visit history so that I can answer a caller at once.   | High     | Open        |
| FR-012 | Act From the Owner Record    | As a clinic user, I want every editing action reachable from the owner's record so that I never have to navigate back to a menu mid-call.                | High     | Open        |
| FR-013 | Update Owner                 | As a clinic user, I want to correct an owner's contact details so that the record stays accurate when the owner moves or changes number.                 | High     | Open        |
| FR-014 | Add Pet                      | As a clinic user, I want to record a pet's name, date of birth, and species under its owner so that visits can be logged against the right animal.       | High     | Open        |
| FR-015 | Distinguishable Pet Names    | As a clinic user, I want a pet name rejected when the same owner already has a pet with that name so that I never log a visit against the wrong animal.  | High     | Open        |
| FR-016 | Plausible Date of Birth      | As a clinic user, I want a date of birth in the future rejected so that an animal's age is never nonsensical.                                            | Medium   | Open        |
| FR-017 | Update Pet                   | As a clinic user, I want to correct a pet's name, date of birth, or species so that mistakes made at registration can be repaired.                       | High     | Open        |
| FR-018 | Book Visit                   | As a clinic user, I want to record a visit for a pet with a date and the reason for it so that the animal's history is complete.                         | High     | Open        |
| FR-019 | Visit Date Defaults to Today | As a clinic user, I want the visit date pre-filled with today's date so that recording a walk-in takes one field less.                                   | Medium   | Open        |
| FR-020 | Stated Reason per Visit      | As a clinic user, I want a visit rejected without a description so that the history explains why the animal came in.                                     | High     | Open        |
| FR-021 | Visit History in Context     | As a clinic user, I want a pet's previous visits shown while I book a new one so that I can see whether this is a follow-up.                             | Medium   | Open        |
| FR-022 | Confirm Successful Changes   | As a clinic user, I want an explicit confirmation after every saved change so that I know the record was stored and not silently lost.                   | High     | Open        |
| FR-023 | Field-Level Correction       | As a clinic user, I want rejected input reported on the offending field with my other entries preserved so that I fix one field instead of retyping all. | High     | Open        |
| FR-024 | Plain-Language Error Page    | As a visitor, I want an unexpected failure to produce a readable message with a way back to the home page so that I am never stranded.                   | High     | Implemented |
| FR-025 | Error Page Demonstration     | As a visitor, I want a route that deliberately fails so that the error handling can be shown without breaking anything.                                  | Low      | Implemented |

## Non-Functional Requirements

| ID      | Title                          | Requirement                                                                                                                                          | Category        | Priority | Status      |
|---------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|----------|-------------|
| NFR-001 | Lists Load Incrementally       | Owner and veterinarian lists must fetch rows lazily as the user scrolls, with no user-visible page controls and no fixed page size.                   | Performance     | High     | Open        |
| NFR-002 | Constant Query Cost per Scroll | The number of rows fetched per scroll step must not grow with the total number of records, so list performance is independent of database size.        | Scalability     | High     | Open        |
| NFR-003 | Deterministic Ordering         | Every list has a defined sort order: an owner's pets alphabetically by name, a pet's visits by ascending date, a vet's specialties alphabetically.     | Usability       | High     | Open        |
| NFR-004 | No Technical Detail Exposed    | Error pages show the failure message only; stack traces and other technical detail appear in server logs and never in the browser.                    | Security        | High     | Implemented |
| NFR-005 | Navigation Survives Failure    | The error view renders inside the application shell, so the navigation drawer and header stay functional after any failure.                           | Usability       | High     | Implemented |
| NFR-006 | Use Case Test Coverage         | Every use case in `use_cases.puml` has at least one automated test, and each test method carries the `@UseCase` annotation naming the use case it covers. | Maintainability | High     | Open        |
| NFR-007 | Tests Run Against Real Database | Automated tests execute against a real PostgreSQL instance started by Testcontainers — no in-memory database and no mocked persistence layer.         | Maintainability | High     | Verified    |
| NFR-008 | Specification Traceability     | Each of the 10 use case specifications maps to exactly one `UC<NNN><Name>Test` class, so any use case can be verified by running one test class.       | Maintainability | Medium   | Open        |
| NFR-009 | Schema Matches Entity Model    | The Flyway migrations reproduce every entity, attribute, type, length, and constraint in `entity_model.md` with zero deviations.                       | Maintainability | High     | Verified    |

## Constraints

| ID    | Title                     | Constraint                                                                                                                                        | Category    | Priority | Status   |
|-------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|-------------|----------|----------|
| C-001 | Runtime Platform          | The application must run on Java 25.                                                                                                               | Technical   | High     | Verified |
| C-002 | Application Framework     | The backend must use Spring Boot 4.1.1.                                                                                                            | Technical   | High     | Verified |
| C-003 | Server-Side UI            | The user interface must be built with Vaadin Flow 25.2 — server-side Java views, no separate frontend project and no REST API behind the UI.        | Technical   | High     | Verified |
| C-004 | Persistence Technology    | Database access must use jOOQ 3.21 with generated type-safe SQL; no ORM and no JPA entities.                                                       | Technical   | High     | Verified |
| C-005 | Database Platform         | PostgreSQL is the only supported database; tests and jOOQ code generation run against `postgres:17-alpine`.                                        | Technical   | High     | Verified |
| C-006 | Schema Ownership          | The database schema must be created and versioned exclusively by Flyway migrations under `src/main/resources/db/migration`.                        | Technical   | High     | Verified |
| C-007 | Docker Required for Build | Building and testing requires a running Docker daemon, because Testcontainers provides the database for both tests and jOOQ code generation.       | Operational | Medium   | Verified |
| C-008 | Specifications Rule       | `docs/` is the source of truth. Where code and a use case specification disagree, the specification wins and the code is the defect.               | Business    | High     | Verified |
| C-009 | Parity With Spring PetClinic | The domain model, terminology, screens, and business rules must follow the original Spring PetClinic sample; deviations require an explicit reason. | Business    | High     | Verified |
| C-010 | No Authentication         | The application ships without authentication or enforced authorization; it is assumed to run on a trusted clinic network.                          | Operational | High     | Verified |
| C-011 | Reference Data Maintained Externally | Veterinarians, specialties, and pet types are loaded directly into the database; the application provides no screens to maintain them.        | Business    | Medium   | Verified |

## Notes

- **C-010 versus the actor split.** The use case diagram distinguishes a
  *Visitor* (welcome page, vet directory, error page) from a *Clinic User* (all
  owner, pet, and visit management). With no authentication, that separation is
  a matter of which screens exist, not of enforced access control: anyone
  reaching the application can perform Clinic User actions. This is deliberate
  for the demo but would be the first thing to revisit for real deployment.
- **FR-007 is case-sensitive.** UC-004 BR-001 specifies a case-sensitive
  "starts with" match, so searching `smith` does not find *Smith*. Confirm
  whether that is intended before treating it as a requirement rather than a
  faithful reproduction of the original sample's behaviour.
- **NFR gaps.** The vision states no thresholds for page response time,
  availability, concurrent users, or accessibility conformance, and none can be
  recovered from the specifications or the code. If those matter, they need to
  be supplied rather than invented here.
