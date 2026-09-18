package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

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
class UC004FindOwnersByLastNameTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-001")
    void prefixSearchReturnsMatchingOwners() {
        navigate(FindOwnersView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Dav");
        test(find(Button.class).withText("Find Owner").single()).click();

        assertTrue(find(Grid.class).single().isVisible());
        // Betty Davis + Harold Davis
        assertEquals(2, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-003", scenario = "A1: Empty Last-Name Search")
    void emptyLastNameReturnsAllOwners() {
        navigate(FindOwnersView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("");
        test(find(Button.class).withText("Find Owner").single()).click();

        assertTrue(find(Grid.class).single().isVisible());
        // all 10 seed owners
        assertEquals(10, test(find(Grid.class).single()).size());
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A2: Exactly One Match")
    void exactlyOneMatchNavigatesDirectlyToOwnerDetails() {
        navigate(FindOwnersView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Fra");
        test(find(Button.class).withText("Find Owner").single()).click();

        String path = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
        assertEquals("owners/" + OWNER_FRANKLIN_ID, path);
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A3: No Match")
    void noMatchAttachesNotFoundToLastNameField() {
        navigate(FindOwnersView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("Nonexistent");
        test(find(Button.class).withText("Find Owner").single()).click();

        TextField lastNameField = find(TextField.class).withCaption("Last name").single();
        assertTrue(lastNameField.isInvalid());
        assertEquals("not found", lastNameField.getErrorMessage());
        assertTrue(find(Grid.class).all().isEmpty(), "Expected results grid to be hidden");
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A4: Scroll Through Results")
    void scrollingMaterialisesOwnersBeyondTheFirstRows() {
        searchAllOwners();
        Grid<?> grid = find(Grid.class).single();

        assertEquals(10, test(grid).size(), "Expected all 10 seed owners in the result set");

        // Materialising a row near the end of the result set is the
        // browserless equivalent of scrolling there: the callback data
        // provider is queried for that range and appends the rows.
        Object firstRow = test(grid).getRow(0);
        Object lastRow = test(grid).getRow(9);

        assertNotEquals(firstRow, lastRow,
                "Expected the last row to be a different owner than the first");
        // Owners are ordered by last name, so Schroeder is the final row —
        // a row that is only reachable once the later range has been fetched.
        assertEquals("Jeff Black", test(grid).getCellText(0, 0));
        assertEquals("David Schroeder", test(grid).getCellText(9, 0),
                "Expected the alphabetically last seed owner in the final row");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = "BR-002")
    void ownersGridFetchesRowsLazily() {
        searchAllOwners();
        Grid<?> grid = find(Grid.class).single();

        // BR-002: rows are fetched from the backend per requested range. An
        // in-memory provider would mean the whole result set was loaded up
        // front, which is exactly what the rule forbids.
        assertFalse(grid.getDataProvider().isInMemory(),
                "BR-002: expected a lazy backend data provider, not an in-memory one");
    }

    private void searchAllOwners() {
        navigate(FindOwnersView.class);
        test(find(TextField.class).withCaption("Last name").single()).setValue("");
        test(find(Button.class).withText("Find Owner").single()).click();
    }
}
