package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-003: Register New Owner.
 *
 * <p>"Whitfield" is deliberately absent from {@code V2__seed_reference_data.sql}
 * so that after the save the post-navigation details page is unambiguous:
 * any rendered "Whitfield" must have come from this test's insert.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC003RegisterNewOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-003")
    void addingValidOwnerPersistsAndNavigatesToDetails() {
        navigate(AddOwnerView.class);

        test($(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test($(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test($(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test($(TextField.class).withCaption("City").single()).setValue("Madison");
        test($(TextField.class).withCaption("Telephone").single()).setValue("5551234567");
        test($(Button.class).withText("Add Owner").single()).click();

        // Post-condition: routed to owners/<newId> (unknown id, but a number).
        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertTrue(path.matches("owners/\\d+"),
                "Expected owners/<id>, got: " + path);

        // The details view is now the active view — rendered paragraphs verify
        // the values actually round-tripped through the database, without
        // calling any repository directly.
        $(OwnerDetailsView.class).single();
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Jane Whitfield").single(),
                "Expected owner name to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("123 Oak St").single(),
                "Expected address to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Madison").single(),
                "Expected city to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("5551234567").single(),
                "Expected telephone to be rendered");

        assertFalse($(Notification.class).all().isEmpty(),
                "Expected a success notification after create");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-001", scenario = "A1: Validation Errors")
    void missingRequiredFieldsBlockCreation() {
        navigate(AddOwnerView.class);
        // leave everything blank and submit
        test($(Button.class).withText("Add Owner").single()).click();

        assertTrue($(TextField.class).withCaption("First Name").single().isInvalid());
        assertTrue($(TextField.class).withCaption("Last Name").single().isInvalid());
        assertTrue($(TextField.class).withCaption("Address").single().isInvalid());
        assertTrue($(TextField.class).withCaption("City").single().isInvalid());
        assertTrue($(TextField.class).withCaption("Telephone").single().isInvalid());

        // Still on the add-owner view: no navigation = nothing persisted.
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-003", businessRules = "BR-002", scenario = "A1: Validation Errors")
    void telephoneMustBeTenDigits() {
        navigate(AddOwnerView.class);

        test($(TextField.class).withCaption("First Name").single()).setValue("Jane");
        test($(TextField.class).withCaption("Last Name").single()).setValue("Whitfield");
        test($(TextField.class).withCaption("Address").single()).setValue("123 Oak St");
        test($(TextField.class).withCaption("City").single()).setValue("Madison");
        // TextField.allowedCharPattern restricts input to digits, but it does
        // not enforce the length, so short numbers still reach validation.
        test($(TextField.class).withCaption("Telephone").single()).setValue("555");
        test($(Button.class).withText("Add Owner").single()).click();

        assertTrue($(TextField.class).withCaption("Telephone").single().isInvalid(),
                "Expected telephone field to be flagged invalid for non-10-digit value");
        // Still on the add-owner view: no navigation = nothing persisted.
        assertEquals("owners/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }
}
