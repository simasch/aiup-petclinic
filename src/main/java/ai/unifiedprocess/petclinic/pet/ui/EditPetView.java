package ai.unifiedprocess.petclinic.pet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
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
 * UC-008: Update Pet.
 *
 * <p>Route {@code /owners/:ownerId/pets/:petId/edit}. Pre-fills the form
 * with the pet, enforces name uniqueness excluding the pet itself (BR-001),
 * and returns to the Owner Details view on success with the notification
 * "Pet details has been edited".
 */
@Route(value = "owners/:ownerId/pets/:petId/edit", layout = MainLayout.class)
@PageTitle("Edit Pet")
public class EditPetView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUCCESS_MESSAGE = "Pet details has been edited";

    private final H2 heading;
    private final PetForm petForm;
    private final Button saveButton;
    private final Button cancelButton;

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private Integer ownerId;
    private Integer petId;

    public EditPetView(OwnerRepository ownerRepository, PetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;

        setSizeFull();

        heading = new H2("Edit Pet");
        heading.addClassNames(LumoUtility.Margin.NONE);

        petForm = new PetForm(petRepository.findAllTypes());

        saveButton = new Button("Update Pet", click -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("Cancel", click -> getUI().ifPresent(ui ->
                ui.navigate(OwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId))));

        add(heading, petForm, new HorizontalLayout(saveButton, cancelButton));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ownerId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(() -> new NotFoundException("Missing owner id"));
        petId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.PET_ID)
                .orElseThrow(() -> new NotFoundException("Missing pet id"));

        // UC-009 A2/A3 analogue: validate consistency of the pet/owner pair
        ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner " + ownerId + " not found"));
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("Pet " + petId + " not found"));
        if (!pet.ownerId().equals(ownerId)) {
            throw new NotFoundException("Pet " + petId + " does not belong to owner " + ownerId);
        }
        petForm.read(pet);
    }

    private void save() {
        // UC-008 BR-003: type may be left unchanged, so do not require it
        petForm.validateAndRead(petId, ownerId, false).ifPresent(pet -> {
            if (petRepository.existsByOwnerAndName(ownerId, pet.name(), petId)) {
                petForm.rejectName("already exists");
                return;
            }
            petRepository.update(pet);
            Notification.show(SUCCESS_MESSAGE);
            getUI().ifPresent(ui -> ui.navigate(
                    OwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId)));
        });
    }
}
