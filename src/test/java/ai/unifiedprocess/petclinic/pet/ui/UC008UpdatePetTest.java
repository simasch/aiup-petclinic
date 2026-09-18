package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

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
class UC008UpdatePetTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-008")
    void editingPetPersistsChanges() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        assertEquals("Max", find(TextField.class).withCaption("Name").single().getValue());

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        // On success, navigation returns to the owner details view.
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Coleman's pets, re-read through the details view, should now show
        // "Max Jr" instead of "Max" (alphabetical order preserved).
        OwnerDetailsView details = find(OwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
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

        test(find(TextField.class).withCaption("Name").single()).setValue("Max");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // Still on edit view.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_SAMANTHA_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Seed pet list unchanged when we re-render the details view.
        OwnerDetailsView details = navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));
        List<String> petNames = find(H3.class).from(details).all().stream()
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
        find(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(3));
        test(find(Button.class).withText("Update Pet").single()).click();

        assertTrue(find(DatePicker.class).withCaption("Birth Date").single().isInvalid());
        // Still on edit view — update was rejected.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A3: Missing Required Field")
    void blankNameIsRejectedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("");
        test(find(Button.class).withText("Update Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid(), "Expected a blank name to be rejected");
        assertEquals("required", nameField.getErrorMessage());

        // Still on the edit view — the form is re-rendered, nothing persisted.
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/edit",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void typeMayBeLeftUnchangedOnUpdate() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // BR-003: a type is only enforced at creation. On update the field is
        // pre-populated and the user may leave it alone — unlike UC-007 A3,
        // where an untouched type blocks the save.
        assertEquals("cat", petTypeField().getValue().name());

        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        // Leaving the edit view at all proves the save was accepted: a rejected
        // form re-renders in place (see blankNameIsRejectedOnUpdate).
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // Re-reading the pet through the edit view shows the untouched type
        // survived the round-trip to the database.
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max Jr", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name());
    }

    @Test
    @UseCase(id = "UC-008", businessRules = "BR-003")
    void clearingTypeOnUpdateKeepsTheStoredType() {
        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // The form deliberately does not require a type on update (BR-003), so
        // an emptied field must be accepted rather than blow up on the way to
        // the database — pets.type_id is NOT NULL, so "unchanged" is the only
        // sensible reading.
        petTypeField().clear();
        test(find(TextField.class).withCaption("Name").single()).setValue("Max Jr");
        test(find(Button.class).withText("Update Pet").single()).click();

        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath(),
                "Expected the update to be accepted despite the cleared type");

        navigate(EditPetView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));
        assertEquals("Max Jr", find(TextField.class).withCaption("Name").single().getValue());
        assertEquals("cat", petTypeField().getValue().name(),
                "Expected the stored type to survive an update that cleared the field");
    }

    @SuppressWarnings("unchecked")
    private ComboBox<PetType> petTypeField() {
        return (ComboBox<PetType>) find(ComboBox.class).withCaption("Type").single();
    }
}
