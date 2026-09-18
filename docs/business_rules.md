# Business Rules

Rules that more than one use case must honour.

A rule that belongs to a single use case stays **in** that use case as
`BR-NNN` — it is part of that use case's story and has no life of its own. A
rule that several use cases share lives **here** as `GR-NNN`, is stated once,
and is referenced by each use case that applies it. The use case keeps its local
`BR-NNN` heading, so tests and the traceability sensors still point at a rule of
that use case; the heading says which shared rule it realizes instead of
restating it.

`BusinessRuleTraceabilityTest` (`./mvnw test`) keeps the two sides honest: it
fails when a use case references a rule that does not exist, when a link lands
on a renamed heading, when the `**Realized by:**` line and the use cases
disagree in either direction, when the summary table drifts from the rules below
it, or when a rule here is realized by fewer than two use cases.

**Why `GR` and not `BR`.** `BR-NNN` ids are scoped to a use case: every
`UC-NNN-*.md` numbers its rules from `BR-001`, and `@UseCase(businessRules =
"BR-001")` is resolved inside that one file. A global `BR-001` would therefore
stand beside ten local ones and mean nothing on its own. `GR` — global rule —
is a second namespace precisely so that an id never needs a use case to
disambiguate it.

| Id     | Rule                             | Realized by                                  | From    |
|--------|----------------------------------|----------------------------------------------|---------|
| GR-001 | Anonymous Access                 | UC-001 BR-001, UC-002 BR-003, UC-010 BR-001  | C-010   |
| GR-002 | Lazy Loading of Lists            | UC-002 BR-001, UC-004 BR-002                 | NFR-001, NFR-002 |
| GR-003 | Deterministic Ordering           | UC-002 BR-002, UC-005 BR-001, UC-005 BR-002  | NFR-003 |
| GR-004 | Complete Owner Contact Details   | UC-003 BR-001, UC-006 BR-001                 | FR-005  |
| GR-005 | Telephone Format                 | UC-003 BR-002, UC-006 BR-002                 | FR-006  |
| GR-006 | Unique Pet Name per Owner        | UC-007 BR-001, UC-008 BR-001                 | FR-015  |
| GR-007 | Birth Date Not in Future         | UC-007 BR-002, UC-008 BR-002                 | FR-016  |
| GR-008 | Pet Type Required on Creation    | UC-007 BR-003, UC-008 BR-003                 | FR-014, FR-017 |

## GR-001: Anonymous Access

Every screen the application offers is reachable without authentication. There
are no accounts and no login; the application is assumed to run on the clinic's
own trusted network.

**Realized by:** UC-001 BR-001, UC-002 BR-003, UC-010 BR-001

## GR-002: Lazy Loading of Lists

A list whose length grows with the size of the clinic's data is rendered with
infinite scrolling: rows are fetched from the backend as the user scrolls. There
are no user-visible page controls and no fixed page size, and the number of rows
fetched per scroll step does not grow with the total number of records.

**Realized by:** UC-002 BR-001 (veterinarians), UC-004 BR-002 (owners)

## GR-003: Deterministic Ordering

Every list has a defined order, so the same data is always presented the same
way:

- an owner's pets alphabetically by name,
- a pet's visits chronologically, by ascending visit date,
- a vet's specialties alphabetically by name.

**Realized by:** UC-002 BR-002, UC-005 BR-001, UC-005 BR-002

## GR-004: Complete Owner Contact Details

An owner record always carries first name, last name, address, city, and
telephone. None of them may be empty — neither when the owner is registered nor
after an update — because the clinic must be able to reach an owner about their
animal.

**Realized by:** UC-003 BR-001, UC-006 BR-001

## GR-005: Telephone Format

An owner's telephone number is exactly 10 digits.

**Realized by:** UC-003 BR-002, UC-006 BR-002

## GR-006: Unique Pet Name per Owner

One owner cannot have two pets with the same name. Names are compared
**case-insensitively**, so *Leo* and *leo* are the same name for this rule —
a visit must always be attributable to exactly one animal.

Different owners may of course have pets with the same name.

**Realized by:** UC-007 BR-001, UC-008 BR-001

## GR-007: Birth Date Not in Future

A pet's date of birth is always recorded, and it lies on or before today.

**Realized by:** UC-007 BR-002, UC-008 BR-002

## GR-008: Pet Type Required on Creation

A pet type must be chosen when a pet is first recorded. On update the type may
be left unchanged — the requirement is enforced at creation, not on every edit.

**Realized by:** UC-007 BR-003, UC-008 BR-003
