package ai.unifiedprocess.petclinic.e2e;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.TestCase;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.DatePickerElement;
import org.vaadin.addons.dramafinder.element.GridElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.SideNavigationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executes test case TC-001 (docs/test_cases/TC-001-new-owner-first-visit.md):
 * a clinic user registers a new owner (UC-003), finds them again by last name
 * (UC-004), reviews their details (UC-005), adds a pet (UC-007) and books the
 * pet's first visit (UC-009).
 *
 * <p>The whole journey is one {@code @Test} because the steps share state: the
 * owner created in step 1 is the owner searched for in step 3 and the owner the
 * pet and the visit hang off in steps 5 and 7.
 *
 * <p>{@code @TestCase} names all five, and the sensor checks that list against
 * the Flow table of the document. Per-use-case coverage — each alternative flow,
 * each business rule — stays with the browserless {@code UC<NNN>…Test} classes;
 * this test asserts only that the five fit together as one journey.
 */
@TestCase(id = "TC-001", useCases = {"UC-003", "UC-004", "UC-005", "UC-007", "UC-009"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "vaadin.launch-browser=false")
@Import(TestcontainersConfiguration.class)
class TC001NewOwnerFirstVisitIT extends AbstractBasePlaywrightIT {

    // Test Data column of the Flow table.
    private static final String FIRST_NAME = "Sofia";
    private static final String LAST_NAME = "Nowak";
    private static final String ADDRESS = "42 Elm Street";
    private static final String CITY = "Madison";
    private static final String TELEPHONE = "6085550199";
    private static final String PET_NAME = "Luna";
    private static final LocalDate PET_BIRTH_DATE = LocalDate.of(2023, 5, 14);
    private static final String PET_TYPE = "cat";
    private static final String VISIT_DESCRIPTION = "annual vaccination";

    /** The views render dates with {@link DateTimeFormatter#ISO_LOCAL_DATE}. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Accessible name of the drawer's navigation landmark. */
    private static final String NAV_LABEL = "Main menu";

    /**
     * A seeded owner with pets and visits, reached through the UI rather than by
     * id so the journey stays black-box. "Coleman" is a unique prefix, so the
     * search lands straight on the details view (UC-004 A2).
     */
    private static final String SEED_OWNER_LAST_NAME = "Coleman";
    private static final String SEED_OWNER_NAME = "Jean Coleman";

    /** Jean Coleman's visit history as {@code V2__seed_reference_data.sql} seeds it, in render order. */
    private static final List<String> SEED_OWNER_VISITS = List.of(
            "2009-06-04 — neutered",      // Max
            "2011-03-04 — rabies shot",   // Max
            "2008-09-04 — spayed",        // Samantha
            "2010-03-04 — rabies shot");  // Samantha

    @LocalServerPort
    private int port;

    /**
     * Cleanup only. The clinic has no delete screen, so the Postconditions
     * contract cannot be honoured through the UI; every assertion in the
     * journey itself goes through the browser.
     */
    @Autowired
    private DSLContext dsl;

    /**
     * Every owner row the Find Owners grid shows before the journey starts, so
     * the "nothing else changed" check can be a diff rather than a count.
     */
    private List<String> ownerRowsBefore;

    @Override
    public String getUrl() {
        return "http://localhost:%d/".formatted(port);
    }

    @Override
    public String getView() {
        // Route of Flow step 1; later steps navigate onward.
        return "owners/find";
    }

    @Test
    @DisplayName("TC-001: A new owner is registered, found again, given a pet and a first visit")
    void newOwnerFirstVisit() {
        ownerRowsBefore = snapshotOwnerRows();

        // Step 1: Register owner (UC-003)
        registerOwner();

        // Step 2: Verify owner created
        verifyOwnerCreated();

        // Step 3: Find owner (UC-004)
        findOwnerByLastName();

        // Step 4: Review owner details (UC-005)
        reviewOwnerDetailsWithoutPets();

        // Step 5: Add pet (UC-007)
        addPet();

        // Step 6: Verify pet listed
        verifyPetListed();

        // Step 7: Book visit (UC-009)
        bookVisit();

        // Step 8: Verify visit recorded
        verifyVisitRecorded();

        // Validations 2 and 3 are asserted by the steps above, on the view each
        // one becomes observable. Validations 1 and 4 are about the end state,
        // so they run once the whole flow is done.
        verifyOwnerStillFindable();
        verifyNothingElseChanged();
    }

    // --- Flow steps ----------------------------------------------------------

    /** Step 1: choose "Add Owner" on Find Owners, fill the form and submit it. */
    private void registerOwner() {
        ButtonElement.getByText(page, "Add Owner").click();

        TextFieldElement firstName = TextFieldElement.getByLabel(page, "First Name");
        firstName.assertVisible();
        firstName.setValue(FIRST_NAME);
        TextFieldElement.getByLabel(page, "Last Name").setValue(LAST_NAME);
        TextFieldElement.getByLabel(page, "Address").setValue(ADDRESS);
        TextFieldElement.getByLabel(page, "City").setValue(CITY);
        TextFieldElement.getByLabel(page, "Telephone").setValue(TELEPHONE);

        ButtonElement.getByText(page, "Add Owner").click();
    }

    /** Step 2: the success notification and the owner's details are shown. */
    private void verifyOwnerCreated() {
        assertNotification("New Owner Created");
        assertOwnerDetailsShown();
    }

    /** Step 3: return to Find Owners and search for the last name from step 1. */
    private void findOwnerByLastName() {
        returnToFindOwners();

        TextFieldElement lastName = TextFieldElement.getByLabel(page, "Last name");
        lastName.assertVisible();
        lastName.setValue(LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();
    }

    /**
     * Step 4: the single match took us straight to Owner Details (UC-004 A2),
     * and the owner still has no pets.
     */
    private void reviewOwnerDetailsWithoutPets() {
        assertOwnerDetailsShown();
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(0);
    }

    /** Step 5: choose "Add New Pet" and submit name, birth date and type. */
    private void addPet() {
        ButtonElement.getByText(page, "Add New Pet").click();

        TextFieldElement name = TextFieldElement.getByLabel(page, "Name");
        name.assertVisible();
        name.setValue(PET_NAME);
        DatePickerElement.getByLabel(page, "Birth Date").setValue(PET_BIRTH_DATE);
        ComboBoxElement.getByLabel(page, "Type").selectItem(PET_TYPE);

        ButtonElement.getByText(page, "Add Pet").click();
    }

    /** Step 6: the success notification and the pet with its birth date and type. */
    private void verifyPetListed() {
        assertNotification("New Pet has been Added");
        assertOwnerDetailsShown();

        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true))).hasCount(1);
        assertExactText("Birth Date: " + PET_BIRTH_DATE.format(DISPLAY_DATE));
        assertExactText("Type: " + PET_TYPE);
        // Validation 2: exactly one pet — one "Edit Pet" action means one pet box.
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Edit Pet"))).hasCount(1);
        // The new pet starts out without visits, which is what makes the single
        // visit booked in step 7 provably the only one.
        assertExactText("(no visits yet)");
    }

    /** Step 7: choose "Add Visit" next to the pet, keep the pre-filled date, describe the visit. */
    private void bookVisit() {
        ButtonElement.getByText(page, "Add Visit").click();

        TextFieldElement description = TextFieldElement.getByLabel(page, "Description");
        description.assertVisible();
        // BR-002: the date field is pre-filled with today and left untouched.
        DatePickerElement.getByLabel(page, "Date").assertValue(LocalDate.now());
        description.setValue(VISIT_DESCRIPTION);

        ButtonElement.getByText(page, "Add Visit").click();
    }

    /** Step 8: the success notification and the visit in the pet's history. */
    private void verifyVisitRecorded() {
        assertNotification("Your visit has been booked");
        assertOwnerDetailsShown();

        // Validation 3: exactly one visit, dated today, with the description.
        assertExactText(LocalDate.now().format(DISPLAY_DATE) + " — " + VISIT_DESCRIPTION);
        assertThat(page.getByText("(no visits yet)")).hasCount(0);
    }

    /**
     * Validation 1: after the whole flow — not just after step 3 — searching for
     * the last name still resolves to this one owner and their details. Running
     * it here also proves the owner stayed unique once the pet and visit existed.
     */
    private void verifyOwnerStillFindable() {
        findOwnerByLastName();
        assertOwnerDetailsShown();
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(PET_NAME).setExact(true))).hasCount(1);
    }

    /**
     * Validation 4: exactly one owner, one pet and one visit were added, and the
     * seed data is untouched.
     *
     * <p>The Find Owners grid carries every owner's name, address, city,
     * telephone and pet names, so diffing it against the snapshot taken before
     * the journey covers the owners and the pets in one pass: a renamed seed
     * owner, a pet added to the wrong owner or a deleted seed pet all change a
     * row. Visits are not in that grid, so a seeded pet's visit history is
     * checked separately. The new owner's own "exactly one pet, exactly one
     * visit" is Validations 2 and 3, asserted during the flow.
     */
    private void verifyNothingElseChanged() {
        List<String> ownerRowsAfter = snapshotOwnerRows();

        assertEquals(ownerRowsBefore.size() + 1, ownerRowsAfter.size(),
                "The journey must add exactly one owner");
        assertTrue(ownerRowsAfter.containsAll(ownerRowsBefore),
                () -> {
                    List<String> missing = new ArrayList<>(ownerRowsBefore);
                    missing.removeAll(ownerRowsAfter);
                    return "The seeded owners and their pets must be unchanged, but these rows are gone: " + missing;
                });
        assertTrue(ownerRowsAfter.contains(expectedOwnerRow()),
                () -> "The one added row must be the journey's owner with their pet, but the grid holds "
                        + ownerRowsAfter);

        assertSeedVisitHistoryUnchanged();
    }

    /** The seeded visits must survive the journey, which only ever adds one of its own. */
    private void assertSeedVisitHistoryUnchanged() {
        returnToFindOwners();
        TextFieldElement.getByLabel(page, "Last name").setValue(SEED_OWNER_LAST_NAME);
        ButtonElement.getByText(page, "Find Owner").click();

        assertExactText(SEED_OWNER_NAME);
        SEED_OWNER_VISITS.forEach(this::assertExactText);
        assertThat(page.getByText(VISIT_DESCRIPTION)).hasCount(0);
    }

    // --- Shared assertions ---------------------------------------------------

    /** Validation 1: the Owner Details view shows the values submitted in step 1. */
    private void assertOwnerDetailsShown() {
        assertExactText(FIRST_NAME + " " + LAST_NAME);
        assertExactText(ADDRESS);
        assertExactText(CITY);
        assertExactText(TELEPHONE);
    }

    /**
     * Located by text rather than as "the open notification": a later step can
     * start while the previous step's card is still fading out, so more than one
     * card is open at a time.
     */
    private void assertNotification(String message) {
        NotificationElement notification = NotificationElement.getByText(page, message);
        notification.assertOpen();
        notification.assertContent(message);
    }

    /** Owner details, pets and visits are plain text nodes, so they are located by their text. */
    private void assertExactText(String text) {
        assertThat(page.getByText(text, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    /** Navigate back to Find Owners the way the user does — through the drawer menu. */
    private void returnToFindOwners() {
        SideNavigationElement mainMenu = SideNavigationElement.getByLabel(page, NAV_LABEL);
        mainMenu.assertVisible();
        mainMenu.clickItem("Find Owners");

        TextFieldElement.getByLabel(page, "Last name").assertVisible();
    }

    /**
     * Every owner as the Find Owners grid shows them: an empty last-name search
     * returns all of them (UC-004 BR-003), one string per row holding the name,
     * address, city, telephone and pet names.
     */
    private List<String> snapshotOwnerRows() {
        returnToFindOwners();
        ButtonElement.getByText(page, "Find Owner").click();

        GridElement results = GridElement.get(page);
        results.assertVisible();
        results.waitForGridToStopLoading();

        List<String> rows = new ArrayList<>();
        for (int row = 0; row < results.getTotalRowCount(); row++) {
            StringBuilder cells = new StringBuilder();
            for (String column : List.of("Name", "Address", "City", "Telephone", "Pets")) {
                int index = row;
                String text = results.findCell(row, column)
                        .orElseThrow(() -> new AssertionError(
                                "Find Owners grid has no '" + column + "' cell in row " + index))
                        .getCellContentLocator().textContent();
                cells.append(text == null ? "" : text.trim()).append(" | ");
            }
            rows.add(cells.toString());
        }
        return rows;
    }

    /** The single row the journey is allowed to have added. */
    private String expectedOwnerRow() {
        return "%s %s | %s | %s | %s | %s | ".formatted(
                FIRST_NAME, LAST_NAME, ADDRESS, CITY, TELEPHONE, PET_NAME);
    }

    // --- Postconditions ------------------------------------------------------

    /**
     * Removes exactly the records the Postconditions section lists, in the
     * stated order: the visit before the pet, the pet before the owner. Every
     * statement is idempotent so a journey that failed halfway still cleans up
     * whatever it did manage to create. The seeded owners, pets, visits and pet
     * types are never touched — no seed owner is called "Nowak".
     */
    @AfterEach
    void removeJourneyData() {
        deleteJourneyData();
    }

    /**
     * Precondition 2 says no owner is called "Nowak". A run killed between the
     * insert and the {@code @AfterEach} would leave one behind and the next run
     * would then find two matches in step 3 and fail somewhere confusing, so the
     * same deletes run up front.
     */
    @BeforeEach
    void removeLeftoversFromAnEarlierRun() {
        deleteJourneyData();
    }

    private void deleteJourneyData() {
        dsl.execute("""
                DELETE FROM visits WHERE pet_id IN (
                    SELECT p.id FROM pets p JOIN owners o ON o.id = p.owner_id WHERE o.last_name = ?)
                """, LAST_NAME);
        dsl.execute("""
                DELETE FROM pets WHERE owner_id IN (SELECT id FROM owners WHERE last_name = ?)
                """, LAST_NAME);
        dsl.execute("DELETE FROM owners WHERE last_name = ?", LAST_NAME);
    }
}
