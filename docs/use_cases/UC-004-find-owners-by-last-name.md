# Use Case: Find Owners by Last Name

## Overview

**Use Case ID:** UC-004   
**Use Case Name:** Find Owners by Last Name   
**Primary Actor:** Clinic User   
**Goal:** Locate one or more owners by their last name so their details can be reviewed or edited.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.

## Main Success Scenario

1. Clinic User clicks "Find Owners" in the navigation bar.
2. System displays the Find Owners form with a single "Last name" input field.
3. Clinic User enters all or the beginning of an owner's last name and submits the form.
4. System queries the owner repository using a "starts with" match on last name.
5. System finds more than one matching owner and renders the paginated Owners List (5 owners per page) showing, for each owner, their name, address, city, telephone, and pets.
6. Clinic User selects an owner from the list to navigate to the Owner Details page (UC-005).

## Alternative Flows

### A1: Empty Last-Name Search

**Trigger:** Clinic User submits the form with an empty last-name field in step 3.
**Flow:**

1. System treats the empty string as a broadest-possible search and returns all owners, paginated.
2. Use case continues at step 5.

### A2: Exactly One Match

**Trigger:** The "starts with" query returns exactly one owner in step 4.
**Flow:**

1. System redirects directly to the Owner Details page for that owner (UC-005).
2. Use case ends.

### A3: No Match

**Trigger:** The query returns no owners in step 4.
**Flow:**

1. System re-renders the Find Owners form.
2. System attaches the error "not found" to the last-name field.
3. Clinic User adjusts the search term.
4. Use case continues at step 3.

### A4: Navigate Pagination

**Trigger:** Result set spans multiple pages (step 5).
**Flow:**

1. Clinic User clicks a page number.
2. System fetches the requested page and re-renders the list.
3. Use case continues at step 5 or 6.

## Postconditions

### Success Postconditions

- The matching owners are displayed, or the user is redirected to a single owner's details page.
- No data is modified.

### Failure Postconditions

- The Find Owners form is redisplayed with a "not found" message.

## Business Rules

### BR-001: Prefix Match

Searches use a case-sensitive "starts with" match on last name; full-string matches are not required.

### BR-002: Page Size

Owners list pagination uses a fixed page size of 5.

### BR-003: Empty Search Returns All

An empty last-name field returns every owner rather than raising a validation error.
