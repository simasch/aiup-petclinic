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
- The Clinic User has navigated to the Owner Details page for that owner (UC-005).

## Main Success Scenario

1. Clinic User clicks "Edit Owner" on the Owner Details page.
2. System loads the existing owner and displays the owner edit form, pre-filled with the current first name, last name, address, city, and telephone.
3. Clinic User amends one or more fields and submits the form.
4. System validates that all required fields are present and that telephone matches the 10-digit pattern.
5. System confirms that the owner id in the submitted form matches the id in the URL path.
6. System persists the updated owner to the database.
7. System redirects back to the Owner Details page and displays the flash message "Owner Values Updated".

## Alternative Flows

### A1: Validation Errors

**Trigger:** One or more fields fail validation in step 4.
**Flow:**

1. System re-renders the owner edit form with field error messages.
2. System displays the flash message "There was an error in updating the owner."
3. Clinic User corrects the input.
4. Use case continues at step 3.

### A2: Owner ID Mismatch

**Trigger:** The id submitted in the form does not match the id in the URL path (step 5).
**Flow:**

1. System rejects the submission with the error "The owner ID in the form does not match the URL."
2. System displays the flash message "Owner ID mismatch. Please try again." and redirects back to the edit form.
3. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The owner record reflects the submitted values.
- The user sees the updated Owner Details page.

### Failure Postconditions

- The owner record is unchanged.
- The edit form is redisplayed with validation or mismatch feedback.

## Business Rules

### BR-001: Mandatory Fields

First name, last name, address, city, and telephone remain required on update.

### BR-002: Telephone Format

Telephone must be exactly 10 digits (regex `\d{10}`).

### BR-003: Form/Path ID Consistency

The owner id contained in the form payload must equal the id in the URL; otherwise the update is rejected as a safety check.
