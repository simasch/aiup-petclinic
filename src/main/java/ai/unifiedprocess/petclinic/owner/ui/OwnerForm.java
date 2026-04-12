package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.Optional;

/**
 * Shared owner form used by Add Owner (UC-003) and Update Owner (UC-006).
 *
 * <p>Validation rules (both use cases, BR-001 and BR-002):
 * <ul>
 *   <li>first name, last name, address, city, telephone are all required</li>
 *   <li>telephone must match {@code \d{10}}</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Owner} record. Because records are
 * immutable, the form uses {@code readRecord} / {@code writeRecord} rather
 * than {@code setBean}.
 */
public class OwnerForm extends FormLayout {

    private final TextField firstName;
    private final TextField lastName;
    private final TextField address;
    private final TextField city;
    private final TextField telephone;
    private final Binder<Owner> binder = new Binder<>(Owner.class);

    public OwnerForm() {
        firstName = new TextField("First Name");
        lastName = new TextField("Last Name");
        address = new TextField("Address");
        city = new TextField("City");
        telephone = new TextField("Telephone");
        telephone.setMaxLength(10);
        telephone.setAllowedCharPattern("[0-9]");
        telephone.setHelperText("10 digits");

        binder.forField(firstName)
                .asRequired("First name is required")
                .withValidator(s -> !s.trim().isEmpty(), "First name is required")
                .bind("firstName");
        binder.forField(lastName)
                .asRequired("Last name is required")
                .withValidator(s -> !s.trim().isEmpty(), "Last name is required")
                .bind("lastName");
        binder.forField(address)
                .asRequired("Address is required")
                .withValidator(s -> !s.trim().isEmpty(), "Address is required")
                .bind("address");
        binder.forField(city)
                .asRequired("City is required")
                .withValidator(s -> !s.trim().isEmpty(), "City is required")
                .bind("city");
        binder.forField(telephone)
                .asRequired("Telephone is required")
                .withValidator(s -> s.matches("\\d{10}"), "Telephone must be exactly 10 digits")
                .bind("telephone");
        // id is a record component but not a form field. Binder requires a
        // binding for every record property so writeRecord can call the
        // canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");

        binder.readRecord(Owner.empty());

        add(firstName, lastName, address, city, telephone);
    }

    /** Fill the fields from an existing owner (used by UC-006). */
    public void read(Owner owner) {
        binder.readRecord(owner);
    }

    /**
     * Validate field contents. On success returns a new {@link Owner} preserving
     * {@code existingId} (null for create). On failure the Binder attaches
     * field-level error messages and this returns empty.
     */
    public Optional<Owner> validateAndRead(Integer existingId) {
        try {
            Owner owner = binder.writeRecord();
            return Optional.of(new Owner(
                    existingId,
                    owner.firstName().trim(),
                    owner.lastName().trim(),
                    owner.address().trim(),
                    owner.city().trim(),
                    owner.telephone().trim()));
        } catch (ValidationException e) {
            return Optional.empty();
        }
    }
}
