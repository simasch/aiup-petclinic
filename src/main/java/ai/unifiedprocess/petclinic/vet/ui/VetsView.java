package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.stream.Stream;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Lazy-loaded list of vets with their specialties. The grid uses a
 * callback data provider (BR-001: infinite scrolling, no fixed page size).
 */
@Route(value = "vets", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VetsView extends VerticalLayout {

    private final transient VetRepository vetRepository;

    public VetsView(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
        setSizeFull();

        H2 heading = new H2("Veterinarians");
        heading.addClassNames(LumoUtility.Margin.NONE);

        Grid<Vet> vetsGrid = new Grid<>(Vet.class, false);
        vetsGrid.addColumn(Vet::firstName).setHeader("First Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::lastName).setHeader("Last Name").setAutoWidth(true);
        vetsGrid.addColumn(Vet::specialtiesLabel).setHeader("Specialties").setAutoWidth(true);
        vetsGrid.setSizeFull();
        vetsGrid.setItems(this::page);

        add(heading, vetsGrid);
        setFlexGrow(1, vetsGrid);
    }

    /** One scroll step of the grid; reads the field, not the constructor parameter. */
    private Stream<Vet> page(Query<Vet, Void> query) {
        return vetRepository.findPage(query.getOffset(), query.getLimit());
    }
}
