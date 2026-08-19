package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerListing;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-004: Find Owners by Last Name.
 *
 * <p>Route {@code /owners/find}. Also the entry point for UC-003 via the
 * "Add Owner" button.
 *
 * <p>Behaviour (BR-001, BR-002, BR-003 and alt flows A1/A2/A3):
 * <ul>
 *   <li>empty last-name field returns every owner,</li>
 *   <li>exactly one match navigates directly to {@link OwnerDetailsView},</li>
 *   <li>no match attaches "not found" to the field,</li>
 *   <li>otherwise the results grid is lazy-loaded with infinite scroll.</li>
 * </ul>
 */
@Route(value = "owners/find", layout = MainLayout.class)
@PageTitle("Find Owners")
public class FindOwnersView extends VerticalLayout {

    private final H2 heading;
    private final TextField lastNameField;
    private final Button findButton;
    private final Button addOwnerButton;
    private final Grid<OwnerListing> resultsGrid;

    private final OwnerRepository ownerRepository;

    public FindOwnersView(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;

        setSizeFull();

        heading = new H2("Find Owners");
        heading.addClassNames(LumoUtility.Margin.NONE);

        lastNameField = new TextField("Last name");
        lastNameField.setPlaceholder("Starts with...");
        lastNameField.setClearButtonVisible(true);

        findButton = new Button("Find Owner", click -> search());
        findButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        findButton.addClickShortcut(Key.ENTER);

        addOwnerButton = new Button("Add Owner", click ->
                getUI().ifPresent(ui -> ui.navigate(AddOwnerView.class)));

        // FormLayout keeps the buttons anchored to the field label row, so the
        // error message below the text field grows into its own cell without
        // pushing the adjacent buttons downward.
        HorizontalLayout buttonBar = new HorizontalLayout(findButton, addOwnerButton);
        FormLayout formBar = new FormLayout(lastNameField, buttonBar);
        formBar.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        resultsGrid = new Grid<>(OwnerListing.class, false);
        resultsGrid.addColumn(l -> l.owner().firstName() + " " + l.owner().lastName()).setHeader("Name").setAutoWidth(true);
        resultsGrid.addColumn(l -> l.owner().address()).setHeader("Address").setAutoWidth(true);
        resultsGrid.addColumn(l -> l.owner().city()).setHeader("City").setAutoWidth(true);
        resultsGrid.addColumn(l -> l.owner().telephone()).setHeader("Telephone").setAutoWidth(true);
        resultsGrid.addColumn(OwnerListing::petsLabel).setHeader("Pets").setAutoWidth(true);
        resultsGrid.setSizeFull();
        resultsGrid.addItemClickListener(event ->
                getUI().ifPresent(ui -> ui.navigate(
                        OwnerDetailsView.class,
                        OwnerRouteParameters.forOwner(event.getItem().owner().id()))));
        // grid is hidden until a search is performed
        resultsGrid.setVisible(false);

        add(heading, formBar, resultsGrid);
        setFlexGrow(1, resultsGrid);
    }

    private void search() {
        lastNameField.setInvalid(false);
        String prefix = lastNameField.getValue() == null ? "" : lastNameField.getValue().trim();

        int total = ownerRepository.countByLastNamePrefix(prefix);
        if (total == 0) {
            // A3: no match
            lastNameField.setInvalid(true);
            lastNameField.setErrorMessage("not found");
            resultsGrid.setVisible(false);
            return;
        }
        if (total == 1) {
            // A2: exactly one → direct navigate
            Owner only = ownerRepository.findByLastNamePrefix(prefix, 0, 1).findFirst().orElseThrow();
            getUI().ifPresent(ui -> ui.navigate(
                    OwnerDetailsView.class, OwnerRouteParameters.forOwner(only.id())));
            return;
        }
        // main flow / A1: show lazy-loaded results grid. The listing query
        // joins each owner's pet names in a single round-trip (no N+1).
        resultsGrid.setVisible(true);
        resultsGrid.setItems(query ->
                ownerRepository.findListingByLastNamePrefix(prefix, query.getOffset(), query.getLimit()));
    }
}
