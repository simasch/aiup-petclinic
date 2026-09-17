# ADR-008: No authentication; a trusted clinic network is assumed

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Use Case View, Physical View

## Context

The use case diagram separates a **Visitor** (welcome page, vet directory, error
page) from a **Clinic User** (everything touching owners, pets, and visits). The
vision states that the application is used by staff on the clinic's own premises
and network, that pet owners are not users, and that per-user accounts and
permissions are out of scope.

Adding authentication would mean login screens, a user store, and access rules —
none of which are specified, all of which would have to be invented, and each of
which would obscure the point of the demo: that every screen comes from a written
use case.

## Decision

Ship **without authentication and without enforced authorization** (C-010). The
Visitor / Clinic User distinction is expressed by which screens exist, not by
access control: whoever reaches the application can perform every Clinic User
action.

The deployment constraint that makes this defensible is part of the decision:
**the application may only run on the clinic's own network** and must not be
exposed beyond it.

## Consequences

- **Owner records — name, address, telephone — are readable and changeable by
  anyone who can reach the port.** The network is the only control.
- **The actor split in `use_cases.puml` is documentation, not enforcement.** An
  agent must not assume a security check exists anywhere, and must not write one
  that no use case asks for.
- **This is the first thing to revisit for a real deployment,** and it is not
  only a code change: enforced roles change the Use Case View (preconditions per
  actor), the Development View (a security layer), and the Physical View.
- **No audit trail either.** Nothing records who changed a record, because there
  is no who.
