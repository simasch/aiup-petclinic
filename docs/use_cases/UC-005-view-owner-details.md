# Use Case: View Owner Details

## Overview

**Use Case ID:** UC-005   
**Use Case Name:** View Owner Details   
**Primary Actor:** Clinic User   
**Goal:** Inspect an owner's contact information together with the list of their pets and each pet's visit history.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.
- An owner with the requested identifier exists in the database.

## Main Success Scenario

1. Clinic User opens the Owner Details view for a given owner id (e.g., via a search result, a newly created owner, or a bookmark).
2. System loads the owner by identifier together with their pets (eagerly) and each pet's visits (ordered by ascending date).
3. System renders the Owner Details view showing:
    - Owner: name, address, city, telephone.
    - Pets: name, birth date, type, and visits (date and description) for each pet.
4. System offers action links: "Edit Owner", "Add New Pet", and for each pet "Edit Pet" and "Add Visit".
5. Clinic User optionally follows one of the action links to trigger UC-006, UC-007, UC-008, or UC-009.

## Alternative Flows

### A1: Owner Not Found

**Trigger:** No owner exists with the requested identifier in step 2.
**Flow:**

1. System cannot resolve the owner and shows the application error view.
2. Use case ends.

## Postconditions

### Success Postconditions

- Owner details, pets, and visits are rendered.
- No data is modified.

### Failure Postconditions

- Owner Details view is not rendered; the application error view is shown instead.

## Business Rules

### BR-001: Visit Ordering

Visits for each pet are listed in chronological order (ascending `visit_date`).

### BR-002: Pet Ordering

An owner's pets are listed alphabetically by name.
