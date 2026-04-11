# Use Case: View Welcome Page

## Overview

**Use Case ID:** UC-001   
**Use Case Name:** View Welcome Page   
**Primary Actor:** Visitor   
**Goal:** Display the application's home page so the user can orient themselves and navigate to the main functional areas.   
**Status:** Approved

## Preconditions

- The PetClinic web application is running and reachable over HTTP.

## Main Success Scenario

1. Visitor navigates to the root URL (`/`) of the PetClinic application.
2. System renders the welcome page with the clinic logo, a decorative image, and the main navigation bar.
3. Visitor sees navigation links for Home, Find Owners, Veterinarians, and Error.

## Alternative Flows

_None — the welcome page is static and takes no user input._

## Postconditions

### Success Postconditions

- Welcome page is rendered in the visitor's browser.
- No application state is changed.

### Failure Postconditions

- If the server is unavailable, the visitor's browser shows a transport error (handled outside the application).

## Business Rules

### BR-001: Anonymous Access

The welcome page is accessible without authentication.
