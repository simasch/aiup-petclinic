package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.vet.domain.Vet;
import com.vaadin.flow.component.grid.Grid;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VET_SPECIALTIES;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-002: View Veterinarians.
 *
 * <p>Uses the V2 seed data (vets + specialties from the main migrations;
 * no V1000 fixtures are needed here). Grid rows are pulled via
 * {@code test(grid).getRow(i)} so the test never touches the repository
 * directly — the grid's own lazy loader executes the real query.
 */
class UC002ViewVeterinariansTest extends PetClinicTestBase {

    @Autowired
    private DSLContext dsl;

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
    @UseCase(id = "UC-002")
    void gridColumnsMatchSpecification() {
        navigate(VetsView.class);
        List<String> headers = grid().getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();
        assertEquals(List.of("First Name", "Last Name", "Specialties"), headers);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void vetsGridFetchesRowsLazily() {
        navigate(VetsView.class);
        Grid<Vet> grid = grid();

        // BR-001: rows come from the backend per requested range. An in-memory
        // provider would mean the whole list was loaded up front — precisely
        // what "no fixed page size, no page controls" rules out.
        assertFalse(grid.getDataProvider().isInMemory(),
                "BR-001: expected a lazy backend data provider, not an in-memory one");

        // Main scenario step 4: materialising the final row is the browserless
        // equivalent of scrolling to the end — the next chunk is fetched and
        // appended on demand. Vets are ordered by last name.
        assertEquals(6, test(grid).size());
        assertEquals("Carter", test(grid).getRow(0).lastName());
        assertEquals("Stevens", test(grid).getRow(5).lastName(),
                "Expected the last vet to be reachable through a later fetch");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: No Veterinarians Registered")
    void gridRendersWithoutRowsWhenNoVetsAreRegistered() {
        // A1 is the *absence* of data, which V2 seed data cannot express —
        // seeding can only add rows. Emptying the tables here is safe because
        // PetClinicTestBase wraps every test in a transaction that is rolled
        // back, so no other test observes it. The view still runs the real
        // VetRepository query, now against an empty table.
        dsl.deleteFrom(VET_SPECIALTIES).execute();
        dsl.deleteFrom(VETS).execute();

        navigate(VetsView.class);

        assertEquals(0, test(grid()).size(),
                "Expected an empty grid when no veterinarians are registered");
        // A1 must render the grid with no rows — not degrade into an error page.
        assertDoesNotThrow(() -> find(VetsView.class).single(),
                "Expected VetsView to still render when there are no vets");
        assertEquals(List.of("First Name", "Last Name", "Specialties"),
                grid().getColumns().stream().map(Grid.Column::getHeaderText).toList(),
                "Expected the column headers to survive the empty result set");
    }

    @SuppressWarnings("unchecked")
    private Grid<Vet> grid() {
        return (Grid<Vet>) find(Grid.class).single();
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
