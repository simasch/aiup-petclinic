package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.ui.MainLayout;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-002: View Veterinarians. Served at {@code /vets.html}; the machine-readable representation
 * (A1) is served separately at {@code /vets} by {@code VetsController}.
 */
@Route(value = "vets.html", layout = MainLayout.class)
@PageTitle("Veterinarians")
public class VetsView extends VerticalLayout {

    public VetsView(VetRepository vetRepository) {
        setSizeFull();

        Grid<Vet> grid = new Grid<>();
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
