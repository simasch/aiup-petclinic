package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-006: Update Owner.
 *
 * <p>Route {@code /owners/:ownerId/edit}. Pre-fills the {@link OwnerForm}
 * with the existing owner and, on successful submit, returns to the
 * Owner Details view with the "Owner Values Updated" notification.
 */
@Route(value = "owners/:ownerId/edit", layout = MainLayout.class)
@PageTitle("Edit Owner")
public class EditOwnerView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUCCESS_MESSAGE = "Owner Values Updated";
    static final String ERROR_MESSAGE = "There was an error in updating the owner.";

    private final H2 heading;
    private final OwnerForm ownerForm;
    private final Button saveButton;
    private final Button cancelButton;

    private final OwnerRepository ownerRepository;
    private Integer currentOwnerId;

    public EditOwnerView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
        setSizeFull();

        heading = new H2("Edit Owner");
        heading.addClassNames(LumoUtility.Margin.NONE);

        ownerForm = new OwnerForm();

        saveButton = new Button("Update Owner", click -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("Cancel", click -> getUI().ifPresent(ui ->
                ui.navigate(OwnerDetailsView.class, OwnerRouteParameters.forOwner(currentOwnerId))));

        add(heading, ownerForm, new HorizontalLayout(saveButton, cancelButton));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Integer ownerId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(() -> new NotFoundException("Missing owner id"));
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner " + ownerId + " not found"));
        currentOwnerId = owner.id();
        ownerForm.read(owner);
    }

    private void save() {
        ownerForm.validateAndRead(currentOwnerId).ifPresentOrElse(
                owner -> {
                    ownerRepository.update(owner);
                    Notification.show(SUCCESS_MESSAGE);
                    getUI().ifPresent(ui -> ui.navigate(
                            OwnerDetailsView.class, OwnerRouteParameters.forOwner(owner.id())));
                },
                () -> Notification.show(ERROR_MESSAGE));
    }
}
