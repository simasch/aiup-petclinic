package ai.unifiedprocess.petclinic.visit.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.visit.domain.VisitRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UC-009: Book Visit for Pet.
 *
 * <p>Route {@code /owners/:ownerId/pets/:petId/visits/new}. Shows the
 * shared {@link VisitForm}, which pre-populates the date with today (BR-002)
 * and validates the description (BR-001), plus the pet's previous visits for
 * context. Checks in {@code beforeEnter} that the pet belongs to the owner
 * (BR-003, A2).
 */
@Route(value = "owners/:ownerId/pets/:petId/visits/new", layout = MainLayout.class)
@PageTitle("Book Visit")
public class AddVisitView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUCCESS_MESSAGE = "Your visit has been booked";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Paragraph petContext;
    private final VisitForm visitForm;
    private final VerticalLayout previousVisits;

    private final transient OwnerRepository ownerRepository;
    private final transient PetRepository petRepository;
    private final transient VisitRepository visitRepository;
    private Integer ownerId;
    private Pet pet;

    public AddVisitView(OwnerRepository ownerRepository, PetRepository petRepository, VisitRepository visitRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;

        setSizeFull();

        H2 heading = new H2("New Visit");
        heading.addClassNames(LumoUtility.Margin.NONE);

        petContext = new Paragraph();
        petContext.addClassNames(LumoUtility.TextColor.SECONDARY);

        visitForm = new VisitForm();

        previousVisits = new VerticalLayout();
        previousVisits.setPadding(false);
        previousVisits.setSpacing(false);

        Button saveButton = new Button("Add Visit", click -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", click -> getUI().ifPresent(ui ->
                ui.navigate(OwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId))));

        add(heading, petContext, visitForm,
                new H3("Previous Visits"), previousVisits,
                new HorizontalLayout(saveButton, cancelButton));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ownerId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(() -> new NotFoundException("Missing owner id"));
        Integer petId = event.getRouteParameters()
                .getInteger(OwnerRouteParameters.PET_ID)
                .orElseThrow(() -> new NotFoundException("Missing pet id"));

        // A3: owner not found
        ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner " + ownerId + " not found"));
        // A2: pet not owned by the given owner
        pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException("Pet " + petId + " not found"));
        if (!pet.ownerId().equals(ownerId)) {
            throw new NotFoundException("Pet " + petId + " does not belong to owner " + ownerId);
        }

        petContext.setText("Pet: " + pet.name());

        previousVisits.removeAll();
        List<Visit> visits = visitRepository.findByPetId(pet.id());
        if (visits.isEmpty()) {
            previousVisits.add(new Paragraph("(none)"));
        } else {
            for (Visit v : visits) {
                previousVisits.add(new Paragraph(v.visitDate().format(DATE_FORMAT) + " — " + v.description()));
            }
        }
    }

    private void save() {
        visitForm.validateAndRead(pet.id()).ifPresent(visit -> {
            visitRepository.insert(visit);
            Notification.show(SUCCESS_MESSAGE);
            getUI().ifPresent(ui -> ui.navigate(
                    OwnerDetailsView.class, OwnerRouteParameters.forOwner(ownerId)));
        });
    }
}
