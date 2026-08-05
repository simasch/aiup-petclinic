package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Owner creation form for UC-003: Register New Owner.
 */
public class OwnerForm extends FormLayout {

    private static final Pattern TELEPHONE_PATTERN = Pattern.compile("\\d{10}");

    private final TextField firstName = new TextField("First Name");
    private final TextField lastName = new TextField("Last Name");
    private final TextField address = new TextField("Address");
    private final TextField city = new TextField("City");
    private final TextField telephone = new TextField("Telephone");

    public OwnerForm() {
        add(firstName, lastName, address, city, telephone);
    }

    /**
     * Validates all fields (BR-001, BR-002), surfacing field-level errors on failure (A1) and
     * returning the populated owner on success. {@code existingId} carries the id through for an
     * update; pass {@code null} when registering a new owner.
     */
    public Optional<Owner> validateAndRead(Integer existingId) {
        boolean valid = validateRequired(firstName, "First Name is required.");
        valid &= validateRequired(lastName, "Last Name is required.");
        valid &= validateRequired(address, "Address is required.");
        valid &= validateRequired(city, "City is required.");
        valid &= validateTelephone();

        if (!valid) {
            return Optional.empty();
        }
        return Optional.of(new Owner(existingId, firstName.getValue().trim(), lastName.getValue().trim(),
                address.getValue().trim(), city.getValue().trim(), telephone.getValue().trim()));
    }

    private boolean validateRequired(TextField field, String message) {
        boolean blank = field.getValue().isBlank();
        field.setInvalid(blank);
        field.setErrorMessage(blank ? message : null);
        return !blank;
    }

    private boolean validateTelephone() {
        String value = telephone.getValue();
        if (value.isBlank()) {
            telephone.setInvalid(true);
            telephone.setErrorMessage("Telephone is required.");
            return false;
        }
        if (!TELEPHONE_PATTERN.matcher(value).matches()) {
            telephone.setInvalid(true);
            telephone.setErrorMessage("Telephone must be exactly 10 digits.");
            return false;
        }
        telephone.setInvalid(false);
        telephone.setErrorMessage(null);
        return true;
    }
}
