package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Lazy-loaded list of vets with their specialties. The grid uses a
 * callback data provider (BR-001: infinite scrolling, no fixed page size).
 */
@Route(value = "vets", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VetsView extends VerticalLayout {

    private final H2 heading;
    private final Grid<Vet> vetsGrid;

    public VetsView(VetRepository vetRepository) {
        setSizeFull();

        heading = new H2("Veterinarians");
        heading.addClassNames(LumoUtility.Margin.NONE);

        vetsGrid = new Grid<>(Vet.class, false);
        vetsGrid.addColumn(Vet::firstName).setHeader("First Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::lastName).setHeader("Last Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::specialtiesLabel).setHeader("Specialties").setAutoWidth(true);
        vetsGrid.setSizeFull();
        vetsGrid.setItems(query -> vetRepository.findPage(query.getOffset(), query.getLimit()));

        add(heading, vetsGrid);
        setFlexGrow(1, vetsGrid);
    }
}
