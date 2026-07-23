# Use Case: View Welcome Page

## Overview

**Use Case ID:** UC-001   
**Use Case Name:** View Welcome Page   
**Primary Actor:** Visitor   
**Goal:** Display the application's home page so the user can orient themselves and navigate to the main functional
areas.   
**Status:** Done

## Preconditions

## Main Success Scenario

1. Visitor navigates to the root URL (`/`) of the PetClinic application.
2. System renders the welcome page with the clinic logo, a decorative image, and the main navigation bar.
3. Visitor sees navigation links for Home, Find Owners, Veterinarians, and Error.

## Alternative Flows

### A1: Page Fails to Render

**Trigger:** An unexpected failure occurs while the system renders the welcome page (step 2).
**Flow:**

1. System shows the application error view instead (UC-010).
2. Use case ends.

## Postconditions

### Success Postconditions

- Welcome page is rendered in the visitor's browser.
- No application state is changed.

### Failure Postconditions

- The welcome page is not shown; the application error view is displayed instead (UC-010).
- No application state is changed.

## Business Rules

### BR-001: Anonymous Access

The welcome page is accessible without authentication.
