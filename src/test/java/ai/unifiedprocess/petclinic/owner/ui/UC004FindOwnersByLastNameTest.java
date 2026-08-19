package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-004: Find Owners by Last Name.
 *
 * <p>Exercises the seed owners loaded by {@code V2__seed_reference_data.sql}:
 * <ul>
 *   <li>"Davis" (Betty and Harold) gives the prefix-match case,</li>
 *   <li>"Franklin" is the unique last-name for the exactly-one-match shortcut (prefix "Fra"),</li>
 *   <li>"Nonexistent" drives the no-match branch.</li>
 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC004FindOwnersByLastNameTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void prefixSearchReturnsMatchingOwners() {
        navigate(FindOwnersView.class);
        test($(TextField.class).withCaption("Last name").single()).setValue("Dav");
        test($(Button.class).withText("Find Owner").single()).click();

        assertTrue($(Grid.class).single().isVisible());
        // Betty Davis + Harold Davis
        assertEquals(2, test($(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void emptyLastNameReturnsAllOwners() {
        navigate(FindOwnersView.class);
        test($(TextField.class).withCaption("Last name").single()).setValue("");
        test($(Button.class).withText("Find Owner").single()).click();

        assertTrue($(Grid.class).single().isVisible());
        // all 10 seed owners
        assertEquals(10, test($(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A2: Exactly One Match")
    void exactlyOneMatchNavigatesDirectlyToOwnerDetails() {
        navigate(FindOwnersView.class);
        test($(TextField.class).withCaption("Last name").single()).setValue("Fra");
        test($(Button.class).withText("Find Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertEquals("owners/" + OWNER_FRANKLIN_ID, path);
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A3: No Match")
    void noMatchAttachesNotFoundToLastNameField() {
        navigate(FindOwnersView.class);
        test($(TextField.class).withCaption("Last name").single()).setValue("Nonexistent");
        test($(Button.class).withText("Find Owner").single()).click();

        TextField lastNameField = $(TextField.class).withCaption("Last name").single();
        assertTrue(lastNameField.isInvalid());
        assertEquals("not found", lastNameField.getErrorMessage());
        assertTrue($(Grid.class).all().isEmpty(), "Expected results grid to be hidden");
    }
}
