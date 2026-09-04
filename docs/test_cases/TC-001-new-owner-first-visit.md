# Test Case: New Owner First Visit

## Overview

**ID:** TC-001  
**Goal:** A clinic user registers a new owner, finds them again by last name, adds a pet and books the pet's first visit — verifying that owner, pet and visit are linked end-to-end and visible on the Owner Details view.  
**Priority:** Critical  
**Status:** Draft

## Roles

- Clinic User (registers owners, searches for them, adds pets and books visits)

## Preconditions

- The pet types "cat", "dog", "lizard", "snake", "bird" and "hamster" exist (Flyway test data `V2__seed_reference_data.sql`) so a type can be selected in step 5.
- No owner with last name "Nowak" exists — the seeded owners use other last names (Flyway test data `V2__seed_reference_data.sql`) — so the search in step 3 matches only the owner created by this journey.

## Flow

| Step | Name                  | Description                                                                                                                            | Test Data                                              | Use Case                                              |
|------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------------------------------|
| 1    | Register owner        | The Clinic User opens Find Owners, chooses "Add Owner", fills in the owner form and submits it                                          | Sofia, Nowak, 42 Elm Street, Madison, 6085550199        | [UC-003](../use_cases/UC-003-register-new-owner.md)   |
| 2    | Verify owner created  | The notification "New Owner Created" is shown and the Owner Details view displays the submitted name, address, city and telephone       | -                                                      | -                                                     |
| 3    | Find owner            | The Clinic User returns to Find Owners and searches for the last name registered in step 1                                              | Nowak                                                  | [UC-004](../use_cases/UC-004-find-owners-by-last-name.md) |
| 4    | Review owner details  | The Clinic User inspects the details of the owner found in step 3, which still lists no pets                                            | -                                                      | [UC-005](../use_cases/UC-005-view-owner-details.md)   |
| 5    | Add pet               | The Clinic User chooses "Add New Pet" on that owner's details and submits name, birth date and type                                     | Luna, 2023-05-14, cat                                  | [UC-007](../use_cases/UC-007-add-pet-to-owner.md)     |
| 6    | Verify pet listed     | The notification "New Pet has been Added" is shown and the Owner Details view lists the pet with its birth date and type                | -                                                      | -                                                     |
| 7    | Book visit            | The Clinic User chooses "Add Visit" next to the pet from step 5, keeps the pre-filled date and enters a description                     | annual vaccination                                     | [UC-009](../use_cases/UC-009-book-visit-for-pet.md)   |
| 8    | Verify visit recorded | The notification "Your visit has been booked" is shown and the visit appears in the pet's visit history on the Owner Details view       | -                                                      | -                                                     |

## Validation

1. **Owner is findable**: After the flow, searching Find Owners for "Nowak" leads to the details of Sofia Nowak, 42 Elm Street, Madison, 6085550199.
2. **Pet belongs to the owner**: The Owner Details view of Sofia Nowak lists exactly one pet, "Luna", of type "cat" with birth date 14.05.2023.
3. **Visit history**: "Luna" has exactly one visit, dated today, with the description "annual vaccination".
4. **Nothing else changed**: The seeded owners, pets and visits are unchanged — the journey added exactly one owner, one pet and one visit.

## Postconditions

- One owner "Sofia Nowak", 42 Elm Street, Madison, telephone 6085550199, exists.
- One pet "Luna" (birth date 2023-05-14, type "cat") belonging to that owner exists.
- One visit for "Luna", dated the day the test ran, with description "annual vaccination", exists.
- Cleanup order: the visit must be deleted before the pet, and the pet before the owner. The seeded pet types and the seeded owners, pets and visits remain untouched.
