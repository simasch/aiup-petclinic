package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Shared pet form used by Add Pet (UC-007) and Update Pet (UC-008).
 *
 * <p>Validation (BR-001 uniqueness is enforced in the enclosing view
 * because it needs query access):
 * <ul>
 *   <li>name required</li>
 *   <li>birthDate required and not in the future (UC-007/008 BR-002)</li>
 *   <li>type required at creation time only (UC-008 BR-003 allows update without change)</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Pet} record
 * ({@code readRecord} / {@code writeRecord}).
 */
public class PetForm extends FormLayout {

    private final TextField name;
    private final DatePicker birthDate;
    private final ComboBox<PetType> type;
    private final Binder<Pet> binder = new Binder<>(Pet.class);

    /**
     * Controls whether the type ComboBox is required at validation time.
     * Mutated per-call via {@link #validateAndRead(Integer, Integer, boolean)}
     * so a single form instance can serve both UC-007 (create, required) and
     * UC-008 (update, optional, BR-003).
     */
    private boolean typeRequired = true;

    public PetForm(List<PetType> availableTypes) {
        name = new TextField("Name");

        birthDate = new DatePicker("Birth Date");
        birthDate.setMax(LocalDate.now());

        type = new ComboBox<>("Type");
        type.setItems(availableTypes);
        type.setItemLabelGenerator(PetType::name);
        type.setRequiredIndicatorVisible(true);

        binder.forField(name)
                .asRequired("required")
                .withValidator(s -> !s.trim().isEmpty(), "required")
                .bind("name");
        binder.forField(birthDate)
                .asRequired("required")
                .withValidator(d -> !d.isAfter(LocalDate.now()), "must not be in the future")
                .bind("birthDate");
        binder.forField(type)
                .withValidator(t -> t != null || !typeRequired, "required")
                .bind("type");
        // id and ownerId are record components but not form fields. Binder
        // requires a binding for every record property so writeRecord can
        // call the canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("ownerId");

        binder.readRecord(Pet.empty(null));

        add(name, birthDate, type);
    }

    public void read(Pet pet) {
        binder.readRecord(pet);
    }

    /**
     * Validate the form. {@code requireType} is true for creation and false
     * for update (UC-008 BR-003). Returns the populated Pet on success or an
     * empty optional on failure, with field-level error messages attached by
     * the Binder.
     */
    public Optional<Pet> validateAndRead(Integer existingId, Integer ownerId, boolean requireType) {
        this.typeRequired = requireType;
        try {
            Pet pet = binder.writeRecord();
            return Optional.of(new Pet(existingId, pet.name().trim(), pet.birthDate(), pet.type(), ownerId));
        } catch (ValidationException e) {
            return Optional.empty();
        }
    }

    public void rejectName(String message) {
        name.setInvalid(true);
        name.setErrorMessage(message);
    }
}
