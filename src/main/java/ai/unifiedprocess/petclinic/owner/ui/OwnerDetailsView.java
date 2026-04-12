package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.ui.AddPetView;
import ai.unifiedprocess.petclinic.pet.ui.EditPetView;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.visit.domain.VisitRepository;
import ai.unifiedprocess.petclinic.visit.ui.AddVisitView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
 * UC-005: View Owner Details.
 *
 * <p>Route {@code /owners/:ownerId}. Renders the owner's contact info plus
 * pets (alphabetical, BR-002) and each pet's visits (chronological, BR-001).
 * Offers action buttons for UC-006, UC-007, UC-008 and UC-009. If the owner
 * id cannot be resolved (A1) the view throws {@link NotFoundException} so
 * Vaadin falls back to the error view.
 */
@Route(value = "owners/:ownerId", layout = MainLayout.class)
@PageTitle("Owner Details")
public class OwnerDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final H2 heading;
    private final Paragraph namePara;
    private final Paragraph addressPara;
    private final Paragraph cityPara;
    private final Paragraph telephonePara;
    private final Button editOwnerButton;
    private final Button addPetButton;
    private final VerticalLayout petsSection;

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;

    private Integer currentOwnerId;

    public OwnerDetailsView(OwnerRepository ownerRepository, PetRepository petRepository, VisitRepository visitRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;

        setSizeFull();

        heading = new H2("Owner Information");
        heading.addClassNames(LumoUtility.Margin.NONE);

        namePara = new Paragraph();
        addressPara = new Paragraph();
        cityPara = new Paragraph();
        telephonePara = new Paragraph();

        editOwnerButton = new Button("Edit Owner", click -> getUI().ifPresent(ui ->
                ui.navigate(EditOwnerView.class, OwnerRouteParameters.forOwner(currentOwnerId))));
        addPetButton = new Button("Add New Pet", click -> getUI().ifPresent(ui ->
                ui.navigate(AddPetView.class, OwnerRouteParameters.forOwner(currentOwnerId))));
        editOwnerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addPetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout ownerActions = new HorizontalLayout(editOwnerButton, addPetButton);

        petsSection = new VerticalLayout();
        petsSection.setPadding(false);
        petsSection.setSpacing(true);

        add(heading, namePara, addressPara, cityPara, telephonePara, ownerActions,
                new H3("Pets and Visits"), petsSection);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Integer ownerId = parseOwnerId(event);
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner " + ownerId + " not found"));
        render(owner);
    }

    private static Integer parseOwnerId(BeforeEnterEvent event) {
        return event.getRouteParameters()
                .getInteger(OwnerRouteParameters.OWNER_ID)
                .orElseThrow(() -> new NotFoundException("Missing owner id"));
    }

    private void render(Owner owner) {
        currentOwnerId = owner.id();
        namePara.setText(owner.firstName() + " " + owner.lastName());
        addressPara.setText(owner.address());
        cityPara.setText(owner.city());
        telephonePara.setText(owner.telephone());

        petsSection.removeAll();
        List<Pet> pets = petRepository.findByOwnerId(owner.id());
        for (Pet pet : pets) {
            petsSection.add(renderPet(owner, pet));
        }
    }

    private VerticalLayout renderPet(Owner owner, Pet pet) {
        VerticalLayout petBox = new VerticalLayout();
        petBox.setPadding(false);
        petBox.setSpacing(false);
        petBox.addClassNames(LumoUtility.Border.ALL, LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.SMALL, LumoUtility.Margin.Bottom.SMALL);

        H3 petName = new H3(pet.name());
        petName.addClassNames(LumoUtility.Margin.NONE);

        Paragraph birthDate = new Paragraph("Birth Date: " +
                (pet.birthDate() == null ? "-" : pet.birthDate().format(DATE_FORMAT)));
        Paragraph type = new Paragraph("Type: " + pet.type().name());

        Button editPet = new Button("Edit Pet", click -> getUI().ifPresent(ui ->
                ui.navigate(EditPetView.class, OwnerRouteParameters.forPet(owner.id(), pet.id()))));
        Button addVisit = new Button("Add Visit", click -> getUI().ifPresent(ui ->
                ui.navigate(AddVisitView.class, OwnerRouteParameters.forPet(owner.id(), pet.id()))));
        editPet.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addVisit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout petActions = new HorizontalLayout(editPet, addVisit);
        petActions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        VerticalLayout visits = new VerticalLayout();
        visits.setPadding(false);
        visits.setSpacing(false);
        visits.add(new Paragraph("Visits:"));
        List<Visit> visitList = visitRepository.findByPetId(pet.id());
        if (visitList.isEmpty()) {
            Paragraph none = new Paragraph("(no visits yet)");
            none.addClassNames(LumoUtility.TextColor.SECONDARY);
            visits.add(none);
        } else {
            for (Visit visit : visitList) {
                visits.add(new Paragraph(visit.visitDate().format(DATE_FORMAT) + " — " + visit.description()));
            }
        }

        petBox.add(petName, birthDate, type, petActions, visits);
        return petBox;
    }
}
