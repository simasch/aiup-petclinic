# Use Case: Book Visit for Pet

## Overview

**Use Case ID:** UC-009   
**Use Case Name:** Book Visit for Pet   
**Primary Actor:** Clinic User   
**Goal:** Record a veterinary visit for an existing pet, documenting the date and the reason for the appointment.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.
- The owner exists.
- The pet exists and belongs to the specified owner.

## Main Success Scenario

1. Clinic User clicks "Add Visit" next to the pet on the Owner Details view.
2. System loads the owner and pet, then displays the visit form. The date field is pre-populated with today's date; the pet's name and previous visits are shown for context.
3. Clinic User optionally adjusts the date, enters a description of the visit, and submits the form.
4. System validates that the description is not blank.
5. System adds the visit to the pet and persists the owner (cascading the visit insert via `pet_id`).
6. System returns to the Owner Details view and displays the notification "Your visit has been booked".

## Alternative Flows

### A1: Missing Description

**Trigger:** The description field is blank at step 4.
**Flow:**

1. System re-renders the visit form with a validation error on `description`.
2. Clinic User enters a description.
3. Use case continues at step 3.

### A2: Pet Not Owned by Given Owner

**Trigger:** In step 2, the supplied pet id does not correspond to any pet belonging to the owner.
**Flow:**

1. System cannot resolve the pet for the given owner and shows the application error view.
2. Use case ends.

### A3: Owner Not Found

**Trigger:** In step 2, no owner exists for the supplied owner id.
**Flow:**

1. System cannot resolve the owner and shows the application error view.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new `Visit` record exists, linked to the pet via `pet_id`.
- The visit appears in the pet's visit history on the Owner Details view.

### Failure Postconditions

- No visit is persisted.
- The visit form is redisplayed with validation errors, or the application error view is shown when the owner/pet cannot be resolved.

## Business Rules

### BR-001: Description Required

Every visit must have a non-blank description.

### BR-002: Default Date

If the user does not change the date field, the visit is recorded with today's date.

### BR-003: Owner/Pet Consistency

A visit can only be booked through the owner who owns the pet; the controller rejects the request if the pet id does not belong to the owner id in the URL.
