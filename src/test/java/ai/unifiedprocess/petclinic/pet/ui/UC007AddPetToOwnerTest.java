package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-007: Add Pet to Owner.
 *
 * <p>Uses the seed owner "Harold Davis" ({@link #OWNER_DAVIS_HAROLD_ID}),
 * who already owns "Iggy", for the general add-pet cases. Uses "Betty Davis"
 * ({@link #OWNER_DAVIS_BETTY_ID}), who already owns "Basil", for the
 * duplicate-name branch.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC007AddPetToOwnerTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-007")
    void addingValidPetPersistsAndReturnsToOwnerDetails() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test($(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test($(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test($(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test($(Button.class).withText("Add Pet").single()).click();

        // Returned to Harold's details page
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // The freshly rendered OwnerDetailsView should list Buddy alongside
        // Harold's seed pet Iggy — alphabetical order (Buddy, Iggy) proves
        // the insert landed and the rendering sort is intact, without going
        // through the PetRepository. Pet names are rendered as H3; the
        // static "Pets and Visits" section header is also an H3 and is
        // filtered out.
        OwnerDetailsView details = $(OwnerDetailsView.class).single();
        List<String> petNames = $(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Buddy", "Iggy"), petNames);
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name for Owner")
    void duplicatePetNameForOwnerIsRejected() {
        // Betty Davis already owns Basil in the seed data.
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_BETTY_ID)));

        test($(TextField.class).withCaption("Name").single()).setValue("Basil");
        test($(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2023, 2, 2));
        test($(ComboBox.class).withCaption("Type").single()).selectItem("cat");
        test($(Button.class).withText("Add Pet").single()).click();

        TextField nameField = $(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // Still on the add-pet view (no navigation happened).
        assertEquals("owners/" + OWNER_DAVIS_BETTY_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-002", scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejected() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test($(TextField.class).withCaption("Name").single()).setValue("Futuro");
        // Bypass the DatePickerTester (which enforces the component's max)
        // so we can simulate submitting a future date directly.
        $(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(7));
        test($(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test($(Button.class).withText("Add Pet").single()).click();

        assertTrue($(DatePicker.class).withCaption("Birth Date").single().isInvalid());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-003", scenario = "A3: Missing Required Field")
    void missingTypeBlocksCreation() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test($(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test($(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        // intentionally skip type
        test($(Button.class).withText("Add Pet").single()).click();

        assertTrue($(ComboBox.class).withCaption("Type").single().isInvalid());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }
}
