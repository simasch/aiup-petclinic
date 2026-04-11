# Use Case: Update Pet

## Overview

**Use Case ID:** UC-008   
**Use Case Name:** Update Pet   
**Primary Actor:** Clinic User   
**Goal:** Modify the details of an existing pet (name, birth date, or type).   
**Status:** Approved

## Preconditions

- The PetClinic application is running.
- The owner and the pet exist.
- The Clinic User has navigated to the Owner Details page for the pet's owner.

## Main Success Scenario

1. Clinic User clicks "Edit Pet" next to the pet on the Owner Details page.
2. System displays the pet edit form pre-filled with the pet's current name, birth date, and type.
3. Clinic User amends one or more fields and submits the form.
4. System validates that:
    - name is not blank,
    - birth date is provided and is not in the future,
    - no other pet belonging to the same owner (with a different id) already has the same name.
5. System updates the pet's properties (name, birth date, type) on the owner's in-memory pet collection and persists the owner.
6. System redirects to the Owner Details page and displays the flash message "Pet details has been edited".

## Alternative Flows

### A1: Duplicate Pet Name

**Trigger:** The submitted name matches a different pet already owned by this owner (step 4).
**Flow:**

1. System rejects the `name` field with the error "already exists".
2. System re-renders the pet edit form with the error.
3. Clinic User adjusts the name.
4. Use case continues at step 3.

### A2: Birth Date in the Future

**Trigger:** The submitted birth date is later than today (step 4).
**Flow:**

1. System rejects the `birthDate` field with a type-mismatch error.
2. System re-renders the pet edit form with the error.
3. Clinic User corrects the date.
4. Use case continues at step 3.

### A3: Missing Required Field

**Trigger:** Name or birth date is missing (step 4).
**Flow:**

1. System rejects the affected field with a "required" error.
2. System re-renders the pet edit form with the error messages.
3. Clinic User supplies the missing value(s).
4. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The pet record reflects the updated name, birth date, and type.
- The user is viewing the Owner Details page with the updated pet.

### Failure Postconditions

- The pet record is unchanged.
- The edit form is redisplayed with validation errors.

## Business Rules

### BR-001: Unique Pet Name per Owner

Two different pets belonging to the same owner cannot share a name (case-insensitive).

### BR-002: Birth Date Not in Future

A pet's birth date must not be after today.

### BR-003: Pet Type on Update

Type may be left unchanged on update; the pet validator only enforces a type when the pet is new.
