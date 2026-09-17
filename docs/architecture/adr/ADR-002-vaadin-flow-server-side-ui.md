# ADR-002: Vaadin Flow server-side UI instead of REST plus an SPA

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Process View, Development View, Physical View

## Context

The application is a small internal CRUD front end for a clinic's front desk:
forms, grids, and navigation, used on the clinic's own network. A separate
frontend would mean a second language, a second build, a second test stack, and
an API layer that exists only so the two halves can talk — and for an AI coding
agent, a second place for every use case to be half-implemented.

The project also has to stay recognizable as PetClinic (C-009), which is a
server-rendered application in its original form.

## Decision

Build the UI with **Vaadin Flow** — Java views on the server, one view per use
case, no separate frontend project and no REST API behind the UI (C-003).

The alternative kept open by the stack, **Hilla** (React views calling
`@BrowserCallable` services), is not used here. If a screen ever needs it, it is
a new decision and a new ADR, not a quiet addition.

## Consequences

- **One language, one build, one test run.** A use case is implemented and
  verified in one place, which is what makes the `UC<NNN><Name>Test` layer
  possible at all ([ADR-006](ADR-006-two-test-layers-merged-coverage.md)).
- **UI state lives in the HTTP session.** The deployment is therefore not
  stateless: scaling out needs session affinity, and a lost session loses an open
  form. See [`../physical.md`](../physical.md).
- **No public API.** Nothing outside the application can reach the data, which
  fits C-010's trusted-network assumption — and is also why breaking that
  assumption is a bigger change than it looks.
- **Vaadin conventions become code conventions:** `SideNav` for the menu,
  `LumoUtility` for styling, `NotFoundException` from `beforeEnter` for a 404.
