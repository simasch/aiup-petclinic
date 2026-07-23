# Use Case: View Veterinarians

## Overview

**Use Case ID:** UC-002   
**Use Case Name:** View Veterinarians   
**Primary Actor:** Visitor   
**Goal:** Browse the list of veterinarians employed at the clinic along with their specialties.   
**Status:** Tested

## Preconditions

- At least one veterinarian is recorded in the system (see A1 otherwise).

## Main Success Scenario

1. Visitor clicks the "Veterinarians" link in the navigation bar.
2. System retrieves the first set of veterinarians.
3. System renders the veterinarians grid showing, for each vet, the first name, last name, and a comma-separated list of
   specialties (or "none" if no specialties are held).
4. As the Visitor scrolls toward the end of the grid, the system fetches and appends the next chunk of veterinarians
   until all entries have been loaded.

## Alternative Flows

### A1: No Veterinarians Registered

**Trigger:** The clinic has no veterinarians recorded (step 2).
**Flow:**

1. System renders the veterinarians grid with no rows.
2. Use case ends.

## Postconditions

### Success Postconditions

- The veterinarians grid is rendered with all loaded entries.
- No data is modified.

### Failure Postconditions

- On data-access errors, the application error view is shown and no vet list is displayed.

## Business Rules

### BR-001: Lazy Loading

The veterinarians grid is rendered with infinite scrolling: rows are fetched lazily from the backend as the user
scrolls. There are no user-visible page controls and no fixed page size.

### BR-002: Specialty Ordering

Within each vet, specialties are listed alphabetically by name.

### BR-003: Anonymous Access

Browsing veterinarians does not require authentication.
