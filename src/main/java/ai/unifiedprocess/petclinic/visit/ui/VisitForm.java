package ai.unifiedprocess.petclinic.visit.ui;

import ai.unifiedprocess.petclinic.visit.domain.Visit;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.binder.ValidationException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Visit form of Book Visit for Pet (UC-009).
 *
 * <p>Validation:
 * <ul>
 *   <li>description required (BR-001) and at most as long as VISIT.description
 *       in {@code docs/entity_model.md}</li>
 *   <li>date pre-populated with today (BR-002) and required — clearing it is a
 *       change, not a way back to the default</li>
 * </ul>
 *
 * <p>Validation and field population are delegated to a {@link Binder}
 * configured for the immutable {@link Visit} record
 * ({@code readRecord} / {@code writeRecord}).
 */
public class VisitForm extends FormLayout {

    /** Column length of VISIT.description in {@code docs/entity_model.md}. */
    static final int DESCRIPTION_LENGTH = 255;

    private static final String REQUIRED = "required";

    private final DatePicker date;
    private final TextField description;
    private final Binder<Visit> binder = new Binder<>(Visit.class);

    public VisitForm() {
        date = new DatePicker("Date");

        description = new TextField("Description");
        // A constraint the component validates on the server too; it only needs a message.
        description.setMaxLength(DESCRIPTION_LENGTH);
        description.setI18n(new TextField.TextFieldI18n()
                .setMaxLengthErrorMessage("At most " + DESCRIPTION_LENGTH + " characters"));

        binder.forField(date)
                .asRequired(REQUIRED)
                .bind("visitDate");
        binder.forField(description)
                .asRequired(REQUIRED)
                .withValidator(s -> !s.trim().isEmpty(), REQUIRED)
                .bind("description");
        // id and petId are record components but not form fields. Binder
        // requires a binding for every record property so writeRecord can
        // call the canonical constructor.
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("id");
        binder.forField(new ReadOnlyHasValue<Integer>(ignored -> {}))
                .bind("petId");

        // BR-002: the date defaults to today.
        binder.readRecord(new Visit(null, LocalDate.now(ZoneId.systemDefault()), "", null));

        setResponsiveSteps(new ResponsiveStep("0", 1));
        add(date, description);
    }

    /**
     * Validate the form. Returns the visit to book for {@code petId} on
     * success, or empty with field-level error messages attached by the
     * Binder.
     */
    public Optional<Visit> validateAndRead(Integer petId) {
        try {
            Visit visit = binder.writeRecord();
            return Optional.of(new Visit(null, visit.visitDate(), visit.description().trim(), petId));
        } catch (ValidationException _) {
            return Optional.empty();
        }
    }
}
