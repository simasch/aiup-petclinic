package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Uses the V2 seed data (vets + specialties from the main migrations;
 * no V1000 fixtures are needed here). Grid rows are pulled via
 * {@code test(grid).getRow(i)} so the test never touches the repository
 * directly — the grid's own lazy loader executes the real query.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002ViewVeterinariansTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-003")
    void vetsViewIsReachableWithoutAuthentication() {
        assertDoesNotThrow(() -> navigate(VetsView.class),
                "Expected /vets to render without authentication");
    }

    @Test
    @UseCase(id = "UC-002")
    void vetsGridContainsSeededVets() {
        navigate(VetsView.class);
        Grid<Vet> grid = grid();
        int total = test(grid).size();
        assertTrue(total > 0, "Expected the seeded vets to appear in the grid");

        Vet first = test(grid).getRow(0);
        assertEquals("James", first.firstName(), "Expected first vet (by last name) to be James Carter");
        assertEquals("Carter", first.lastName());
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-002")
    void specialtiesAreListedAlphabeticallyWithinEachVet() {
        navigate(VetsView.class);

        // Dr. Douglas holds both 'surgery' and 'dentistry' per V2 seed data —
        // BR-002 requires the view to display them alphabetically.
        Vet douglas = findVetByLastName("Douglas");
        assertEquals(List.of("dentistry", "surgery"), douglas.specialties());
    }

    @Test
    @UseCase(id = "UC-002")
    void vetWithoutSpecialtiesIsRenderedAsNone() {
        navigate(VetsView.class);

        // Dr. Carter has no specialties in the V2 seed data.
        Vet carter = findVetByLastName("Carter");
        assertTrue(carter.specialties().isEmpty());
        assertEquals("none", carter.specialtiesLabel());
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void gridColumnsMatchSpecification() {
        navigate(VetsView.class);
        List<String> headers = grid().getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();
        assertEquals(List.of("First Name", "Last Name", "Specialties"), headers);
    }

    @SuppressWarnings("unchecked")
    private Grid<Vet> grid() {
        return (Grid<Vet>) $(Grid.class).single();
    }

    private Vet findVetByLastName(String lastName) {
        Grid<Vet> grid = grid();
        int size = test(grid).size();
        for (int i = 0; i < size; i++) {
            Vet vet = test(grid).getRow(i);
            if (lastName.equals(vet.lastName())) {
                return vet;
            }
        }
        throw new AssertionError("Vet with last name '" + lastName + "' not found in grid");
    }
}
