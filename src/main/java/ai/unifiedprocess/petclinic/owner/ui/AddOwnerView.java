package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-003: Register New Owner.
 *
 * <p>Route {@code /owners/new}. On successful submit, routes to the new
 * owner's details view (UC-005) and shows the "New Owner Created"
 * notification. On validation failure, the form is redisplayed with
 * field-level errors and a notification.
 */
@Route(value = "owners/new", layout = MainLayout.class)
@PageTitle("Add Owner")
public class AddOwnerView extends VerticalLayout {

    static final String SUCCESS_MESSAGE = "New Owner Created";
    static final String ERROR_MESSAGE = "There was an error in creating the owner.";

    private final H2 heading;
    private final OwnerForm ownerForm;
    private final Button saveButton;
    private final Button cancelButton;

    public AddOwnerView(OwnerRepository ownerRepository) {
        setSizeFull();

        heading = new H2("Add Owner");
        heading.addClassNames(LumoUtility.Margin.NONE);

        ownerForm = new OwnerForm();

        saveButton = new Button("Add Owner", click -> {
            ownerForm.validateAndRead(null).ifPresentOrElse(
                    owner -> {
                        Integer newId = ownerRepository.insert(owner);
                        Notification.show(SUCCESS_MESSAGE);
                        getUI().ifPresent(ui -> ui.navigate(
                                OwnerDetailsView.class, OwnerRouteParameters.forOwner(newId)));
                    },
                    () -> Notification.show(ERROR_MESSAGE));
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton = new Button("Cancel", click ->
                getUI().ifPresent(ui -> ui.navigate(FindOwnersView.class)));

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

        add(heading, ownerForm, buttons);
    }
}
