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
2. System retrieves the first page of veterinarians (5 per page) from the repository.
3. System renders the veterinarians table showing, for each vet, the first name, last name, and a comma-separated list of specialties (or "none" if no specialties are held).
4. System displays pagination controls showing the current page and total number of pages.
5. Visitor may click a page number to browse additional pages; the system repeats steps 2–4 for the selected page.

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

- On data-access errors, an application error page is shown and no vet list is displayed.

## Business Rules

### BR-001: Page Size

The HTML view paginates the vet list at a fixed page size of 5 entries.

### BR-002: Specialty Ordering

Within each vet, specialties are listed alphabetically by name.

### BR-003: Anonymous Access

Browsing veterinarians does not require authentication.
