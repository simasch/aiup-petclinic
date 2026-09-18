package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-007: Add Pet to Owner.
 *
 * <p>Uses the seed owner "Harold Davis" ({@link #OWNER_DAVIS_HAROLD_ID}),
 * who already owns "Iggy", for the general add-pet cases. Uses "Betty Davis"
 * ({@link #OWNER_DAVIS_BETTY_ID}), who already owns "Basil", for the
 * duplicate-name branch.
 */
class UC007AddPetToOwnerTest extends PetClinicTestBase {

    @Autowired
    private PetRepository petRepository;

    @Test
    @UseCase(id = "UC-007")
    void addingValidPetPersistsAndReturnsToOwnerDetails() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        // Returned to Harold's details page
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // The freshly rendered OwnerDetailsView should list Buddy alongside
        // Harold's seed pet Iggy — alphabetical order (Buddy, Iggy) proves
        // the insert landed and the rendering sort is intact, without going
        // through the PetRepository. Pet names are rendered as H3; the
        // static "Pets and Visits" section header is also an H3 and is
        // filtered out.
        OwnerDetailsView details = find(OwnerDetailsView.class).single();
        List<String> petNames = find(H3.class).from(details).all().stream()
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

        test(find(TextField.class).withCaption("Name").single()).setValue("Basil");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2023, 2, 2));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("cat");
        test(find(Button.class).withText("Add Pet").single()).click();

        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertTrue(nameField.isInvalid());
        assertEquals("already exists", nameField.getErrorMessage());
        // Still on the add-pet view (no navigation happened).
        assertEquals("owners/" + OWNER_DAVIS_BETTY_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-001", scenario = "A1: Duplicate Pet Name for Owner")
    void duplicatePetNameIsRejectedByTheDatabaseWhateverTheCase() {
        // BR-001 compares case-insensitively, so the database backstop has to as
        // well: pets_owner_name_unique indexes lower(name). Goes straight to the
        // repository — the form check above never lets a duplicate reach the DB,
        // which is exactly why the constraint needs its own test.
        PetType cat = petRepository.findAllTypes().stream()
                .filter(type -> type.name().equals("cat"))
                .findFirst()
                .orElseThrow();

        Pet duplicate = new Pet(null, "BASIL", LocalDate.of(2023, 2, 2), cat, OWNER_DAVIS_BETTY_ID);

        assertThrows(DataIntegrityViolationException.class, () -> petRepository.insert(duplicate));
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-002", scenario = "A2: Birth Date in the Future")
    void futureBirthDateIsRejected() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Futuro");
        // Bypass the DatePickerTester (which enforces the component's max)
        // so we can simulate submitting a future date directly.
        find(DatePicker.class).withCaption("Birth Date").single().setValue(LocalDate.now().plusDays(7));
        test(find(ComboBox.class).withCaption("Type").single()).selectItem("dog");
        test(find(Button.class).withText("Add Pet").single()).click();

        assertTrue(find(DatePicker.class).withCaption("Birth Date").single().isInvalid());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-007", businessRules = "BR-003", scenario = "A3: Missing Required Field")
    void missingTypeBlocksCreation() {
        navigate(AddPetView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_DAVIS_HAROLD_ID)));

        test(find(TextField.class).withCaption("Name").single()).setValue("Buddy");
        test(find(DatePicker.class).withCaption("Birth Date").single()).setValue(LocalDate.of(2022, 6, 1));
        // intentionally skip type
        test(find(Button.class).withText("Add Pet").single()).click();

        assertTrue(find(ComboBox.class).withCaption("Type").single().isInvalid());
        assertEquals("owners/" + OWNER_DAVIS_HAROLD_ID + "/pets/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }
}
