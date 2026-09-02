package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-002: View Veterinarians. Lists every vet with their specialties, lazily
 * loaded as the visitor scrolls (BR-001).
 */
@Route(value = "vets", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VetsView extends VerticalLayout {

    final Grid<Vet> grid;

    public VetsView(VetRepository vetRepository) {
        setSizeFull();

        grid = new Grid<>();
        grid.addColumn(Vet::firstName).setHeader("First Name");
        grid.addColumn(Vet::lastName).setHeader("Last Name");
        grid.addColumn(VetsView::specialtiesLabel).setHeader("Specialties");
        grid.setSizeFull();
        grid.setItems(
                query -> vetRepository.findPage(query.getOffset(), query.getLimit()),
                query -> vetRepository.count());

        add(grid);
    }

    private static String specialtiesLabel(Vet vet) {
        return vet.specialties().isEmpty() ? "none" : String.join(", ", vet.specialties());
    }
}
