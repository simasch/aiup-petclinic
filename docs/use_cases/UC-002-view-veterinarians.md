# Use Case: View Veterinarians

## Overview

**Use Case ID:** UC-002   
**Use Case Name:** View Veterinarians   
**Primary Actor:** Visitor   
**Goal:** Browse the list of veterinarians employed at the clinic along with their specialties.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.
- At least one veterinarian exists in the database (otherwise the list is simply empty).

## Main Success Scenario

1. Visitor clicks the "Veterinarians" link in the navigation bar.
2. System retrieves the first chunk of veterinarians from the repository via a lazy data provider.
3. System renders the veterinarians grid showing, for each vet, the first name, last name, and a comma-separated list of specialties (or "none" if no specialties are held).
4. As the Visitor scrolls toward the end of the grid, the system fetches and appends the next chunk of veterinarians until all entries have been loaded.

## Alternative Flows

### A1: Request Vets as JSON/XML

**Trigger:** A client requests `/vets` (without `.html`) expecting a machine-readable representation.
**Flow:**

1. System loads all veterinarians from the repository.
2. System wraps them in a `Vets` container object.
3. System returns the collection serialized as JSON or XML (content-negotiated).
4. Use case ends.

## Postconditions

### Success Postconditions

- The requested page (or full list) of veterinarians is rendered or returned to the caller.
- No data is modified.

### Failure Postconditions

- On data-access errors, the application error view is shown and no vet list is displayed.

## Business Rules

### BR-001: Lazy Loading

The veterinarians grid is rendered with infinite scrolling: rows are fetched lazily from the backend as the user scrolls. There are no user-visible page controls and no fixed page size.

### BR-002: Specialty Ordering

Within each vet, specialties are listed alphabetically by name.

### BR-003: Anonymous Access

Browsing veterinarians does not require authentication.
