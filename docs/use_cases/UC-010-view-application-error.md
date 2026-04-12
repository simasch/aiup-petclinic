# Use Case: View Application Error

## Overview

**Use Case ID:** UC-010   
**Use Case Name:** View Application Error   
**Primary Actor:** Visitor (and Clinic User, via any failure flow)   
**Goal:** Present a friendly error page whenever an uncaught exception reaches the Vaadin router, so users are never shown a raw stack trace or an unstyled servlet error. Also reachable on demand via the "Error" drawer link, which mirrors the Spring PetClinic `/oups` demonstration page.   
**Status:** Approved

## Preconditions

- The PetClinic application is running.

## Main Success Scenario

1. An uncaught exception is raised during navigation (for example from a view's `beforeEnter` hook), **or** the Visitor clicks the "Error" link in the navigation drawer.
2. System resolves the failure to the application error view via the router's error-parameter handler.
3. System renders the application error view inside `MainLayout` showing:
    - A heading "Something happened...".
    - A paragraph with the exception message.
    - A "Back to Home" link that returns the user to the welcome page (UC-001).
4. Visitor optionally follows the "Back to Home" link to exit the error flow.

## Alternative Flows

### A1: Resource Not Found

**Trigger:** Any view throws `com.vaadin.flow.router.NotFoundException` during navigation — for example UC-005 A1 when an unknown owner id is supplied, UC-006 / UC-007 / UC-008 / UC-009 when an owner or pet id cannot be resolved, or the user opens a URL that does not match any registered route.

**Flow:**

1. System resolves the failure to the not-found variant of the error view.
2. System renders the error view with HTTP status 404 and the exception's message.
3. Use case ends.

### A2: Unexpected Error

**Trigger:** Any other uncaught runtime exception, including the exception thrown by the `/oups` demonstration route.

**Flow:**

1. System resolves the failure to the generic variant of the error view.
2. System renders the error view with HTTP status 500 and the exception's message.
3. Use case ends.

## Postconditions

### Success Postconditions

- The application error view is rendered inside `MainLayout`.
- The navigation drawer and header remain available so the user can leave the error view via any nav link.
- No data is modified.

### Failure Postconditions

_None — the error view is itself the terminal state for failed navigations._

## Business Rules

### BR-001: Anonymous Access

The application error view is reachable without authentication. This matches UC-001 and UC-002.

### BR-002: Navigation Shell Preserved

The error view renders inside `MainLayout` so the drawer and header remain functional. Users are never stranded on a blank page.

### BR-003: Message Only, No Stack Trace

The error view displays the exception's message but never its stack trace. Stack traces remain visible in server logs and developer tools only.

### BR-004: `/oups` Demonstration Route

A `/oups` route exists purely to demonstrate the error view. Navigating to it always throws a `RuntimeException` whose message is `"Expected: controller used to showcase what happens when an exception is thrown"`, matching the Spring PetClinic original.
