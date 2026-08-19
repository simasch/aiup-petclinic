package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
 * UC-008: Update Pet.
 *
 * <p>Uses Jean Coleman's seed pets: {@link #PET_MAX_ID} (cat "Max") and
 * {@link #PET_SAMANTHA_ID} (cat "Samantha"). The name-collision case
 * renames Samantha → Max.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC008UpdatePetTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-008")
    void editingPetPersistsChanges() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        assertEquals("Max", $(TextField.class).withCaption("Name").single().getValue());

        test($(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test($(Button.class).withText("Update Pet").single()).click();

        // On success, navigation returns to the owner details view.
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Coleman's pets, re-read through the details view, should now show
        // "Max Jr" instead of "Max" (alphabetical order preserved).
        OwnerDetailsView details = $(OwnerDetailsView.class).single();
        List<String> petNames = $(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max Jr", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name")
    void cannotRenamePetToMatchSiblingPet() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_SAMANTHA_ID)));

        test($(TextField.class).withCaption("Name").single()).setValue("Max");
        test($(Button.class).withText("Update Pet").single()).click();

        TextField nameField = $(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // Still on edit view.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_SAMANTHA_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Seed pet list unchanged when we re-render the details view.
        OwnerDetailsView details = navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        List<String> petNames = $(H3.class).from(details).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), petNames);
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-002", scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejectedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // Bypass the DatePickerTester (which enforces the component's max).
        $(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(3));
        test($(Button.class).withText("Update Pet").single()).click();

        assertTrue($(DatePicker.class).withCaption("Birth Date").single().isInvalid());
        // Still on edit view — update was rejected.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }
}
