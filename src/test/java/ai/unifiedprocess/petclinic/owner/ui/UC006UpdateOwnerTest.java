package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-006: Update Owner.
 *
 * <p>Uses the seed owner "Harold Davis" ({@link #OWNER_DAVIS_HAROLD_ID}).
 * Mutations roll back at the end of each test via {@link PetClinicTestBase}'s
 * {@code @Transactional}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC006UpdateOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-006")
    void editingOwnerPersistsChangesAndReturnsToDetails() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        // fields pre-filled from the seed data
        assertEquals("Harold", $(TextField.class).withCaption("First Name").single().getValue());
        assertEquals("Davis", $(TextField.class).withCaption("Last Name").single().getValue());

        test($(TextField.class).withCaption("First Name").single()).setValue("Harry");
        test($(TextField.class).withCaption("City").single()).setValue("Springfield");
        test($(Button.class).withText("Update Owner").single()).click();

        // Returned to owner details
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // The details view now reflects the persisted update — rendered
        // paragraphs verify the round-trip without the repo.
        $(OwnerDetailsView.class).single();
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Harry Davis").single(),
                "Expected updated name to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Springfield").single(),
                "Expected updated city to be rendered");
    }

    @Test
    @UseCase(id = "UC-006", businessRules = "BR-001, BR-002", scenario = "A1: Validation Errors")
    void blankRequiredFieldsBlockUpdate() {
        navigate(EditOwnerView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test($(TextField.class).withCaption("Last Name").single()).setValue("");
        test($(Button.class).withText("Update Owner").single()).click();

        assertTrue($(TextField.class).withCaption("Last Name").single().isInvalid());
        // Still on edit view — no save happened.
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Re-render the details page and verify the seed value is unchanged.
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Harold Davis").single(),
                "Expected seed name to remain unchanged");
    }
}
