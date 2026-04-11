# Use Case: Update Owner

## Overview

**Use Case ID:** UC-006   
**Use Case Name:** Update Owner   
**Primary Actor:** Clinic User   
**Goal:** Modify an existing owner's contact information.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.
- The owner to be updated exists.
- The Clinic User has navigated to the Owner Details view for that owner (UC-005).

## Main Success Scenario

1. Clinic User clicks "Edit Owner" on the Owner Details view.
2. System loads the existing owner and displays the owner edit form, pre-filled with the current first name, last name, address, city, and telephone.
3. Clinic User amends one or more fields and submits the form.
4. System validates that all required fields are present and that telephone matches the 10-digit pattern.
5. System persists the updated owner to the database.
6. System returns to the Owner Details view and displays the notification "Owner Values Updated".

## Alternative Flows

### A1: Validation Errors

**Trigger:** One or more fields fail validation in step 4.
**Flow:**

1. System re-renders the owner edit form with field error messages.
2. System displays the notification "There was an error in updating the owner."
3. Clinic User corrects the input.
4. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The owner record reflects the submitted values.
- The user sees the updated Owner Details view.

### Failure Postconditions

- The owner record is unchanged.
- The edit form is redisplayed with validation feedback.

## Business Rules

### BR-001: Mandatory Fields

First name, last name, address, city, and telephone remain required on update.

### BR-002: Telephone Format

Telephone must be exactly 10 digits (regex `\d{10}`).
