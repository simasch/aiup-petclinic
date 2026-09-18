package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-003: Register New Owner.
 *
 * <p>"Whitfield" is deliberately absent from {@code V2__seed_reference_data.sql}
 * so that after the save the post-navigation details page is unambiguous:
 * any rendered "Whitfield" must have come from this test's insert.
 */
class UC003RegisterNewOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-003")
    void addingValidOwnerPersistsAndNavigatesToDetails() {
        navigate(AddOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        // Post-condition: routed to owners/<newId> (unknown id, but a number).
        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"),
                "Expected owners/<id>, got: " + path);

        // The details view is now the active view — rendered paragraphs verify
        // the values actually round-tripped through the database, without
        // calling any repository directly.
        assertDoesNotThrow(
                () -> find(OwnerDetailsView.class).single(),
                "Expected the details view of the new owner to be active");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Jane Whitfield").single(),
                "Expected owner name to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("123 Oak St").single(),
                "Expected address to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("Madison").single(),
                "Expected city to be rendered");
        assertDoesNotThrow(
                () -> find(Paragraph.class).withText("5551234567").single(),
                "Expected telephone to be rendered");

        assertFalse(find(Notification.class).all().isEmpty(),
                "Expected a success notification after create");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-001", scenario = "A1: Validation Errors")
    void missingRequiredFieldsBlockCreation() {
        navigate(AddOwnerView.class);
        // leave everything blank and submit
        test(find(Button.class).withText("Add Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("First Name").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Last Name").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Address").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("City").single().isInvalid());
        assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid());

        // Still on the add-owner view: no navigation = nothing persisted.
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-002", scenario = "A1: Validation Errors")
    void telephoneMustBeTenDigits() {
        navigate(AddOwnerView.class);

        test(find(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        // TextField.allowedCharPattern restricts input to digits, but it does
        // not enforce the length, so short numbers still reach validation.
        test(find(TextField.class).withCaption("Telephone").single()).setValue("555");
        test(find(Button.class).withText("Add Owner").single()).click();

        assertTrue(find(TextField.class).withCaption("Telephone").single().isInvalid(),
                "Expected telephone field to be flagged invalid for non-10-digit value");
        // Still on the add-owner view: no navigation = nothing persisted.
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Validation Errors")
    void overLongValuesAreFieldErrorsNotConstraintViolations() {
        navigate(AddOwnerView.class);

        // One character past each column length of docs/entity_model.md. The
        // values are set on the components directly, past the tester: the
        // browser would stop at maxLength, and the server must not rely on it,
        // or the value ends as a constraint violation on the error page.
        find(TextField.class).withCaption("First Name").single().setValue("F".repeat(OwnerForm.NAME_LENGTH + 1));
        find(TextField.class).withCaption("Last Name").single().setValue("L".repeat(OwnerForm.NAME_LENGTH + 1));
        find(TextField.class).withCaption("Address").single().setValue("A".repeat(OwnerForm.ADDRESS_LENGTH + 1));
        find(TextField.class).withCaption("City").single().setValue("C".repeat(OwnerForm.CITY_LENGTH + 1));
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        for (String caption : new String[] {"First Name", "Last Name", "Address", "City"}) {
            TextField field = find(TextField.class).withCaption(caption).single();
            assertTrue(field.isInvalid(), "Expected " + caption + " to be rejected as too long");
            assertTrue(field.getErrorMessage().startsWith("At most "),
                    "Expected a length message on " + caption + ", got: " + field.getErrorMessage());
        }
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-003")
    void ownerIdIsAssignedByTheDatabase() {
        navigate(AddOwnerView.class);

        // The user is given no way to supply an id: the form has no such field.
        assertTrue(find(TextField.class).withCaption("Id").all().isEmpty(),
                "Expected the add-owner form to expose no id field");

        // Two owners registered with identical data still end up on distinct
        // routes, so the id can only have come from the database sequence.
        int firstId = registerWhitfield("Jane");
        int secondId = registerWhitfield("John");

        assertNotEquals(firstId, secondId,
                "Expected the database to assign a distinct id to each new owner");
    }

    /** Registers an owner and returns the id the server routed to. */
    private int registerWhitfield(String firstName) {
        navigate(AddOwnerView.class);
        test(find(TextField.class).withCaption("First Name").single()).setValue(firstName);
        test(find(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test(find(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test(find(TextField.class).withCaption("City").single()).setValue("Madison");
        test(find(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test(find(Button.class).withText("Add Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"), "Expected owners/<id>, got: " + path);
        return Integer.parseInt(path.substring("owners/".length()));
    }
}
