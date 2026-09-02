package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * UC-002: View Veterinarians. Rows are seeded by V2__seed_reference_data.sql and ordered by last
 * name then first name: Carter (none), Douglas (dentistry, surgery), Jenkins (none), Leary
 * (radiology), Ortega (surgery), Stevens (radiology).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002ViewVeterinariansTest extends SpringBrowserlessTest {

    @Test
    @UseCase(id = "UC-002")
    void veterinariansLinkResolvesToTheVeterinariansView() {
        assertDoesNotThrow(() -> navigate("vets", VetsView.class),
                "Expected the 'Veterinarians' navigation target 'vets' to resolve to VetsView");
    }

    @Test
    @UseCase(id = "UC-002")
    void gridShowsFirstNameLastNameAndSpecialtyColumns() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        List<String> headers = grid.getColumns().stream().map(Grid.Column::getHeaderText).toList();
        assertThat(headers).containsExactly("First Name", "Last Name", "Specialties");
    }

    @Test
    @UseCase(id = "UC-002")
    void gridRendersVetNamesAndSpecialties() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        assertThat(test(grid).getCellText(3, 0)).isEqualTo("Helen");
        assertThat(test(grid).getCellText(3, 1)).isEqualTo("Leary");
        assertThat(test(grid).getCellText(3, 2)).isEqualTo("radiology");
    }

    @Test
    @UseCase(id = "UC-002")
    void vetWithoutSpecialtiesShowsNone() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        assertThat(test(grid).getCellText(0, 0)).isEqualTo("James");
        assertThat(test(grid).getCellText(0, 1)).isEqualTo("Carter");
        assertThat(test(grid).getCellText(0, 2)).isEqualTo("none");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-002")
    void specialtiesAreListedAlphabeticallyForEachVet() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        // Linda Douglas holds surgery and dentistry; they must render alphabetically.
        assertThat(test(grid).getCellText(1, 1)).isEqualTo("Douglas");
        assertThat(test(grid).getCellText(1, 2)).isEqualTo("dentistry, surgery");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void gridFetchesRowsLazilyRatherThanHoldingThemInMemory() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        assertThat(grid.getDataProvider().isInMemory())
                .as("Expected a lazily fetching data provider, not an in-memory list")
                .isFalse();
        assertThat(grid.getPageSize())
                .as("Expected chunked fetching, not one request for every row")
                .isPositive();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void allSeededVeterinariansAreLoaded() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();

        assertThat(test(grid).size()).isEqualTo(6);
        assertThat(test(grid).getCellText(5, 1)).isEqualTo("Stevens");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-003")
    void veterinariansViewIsAccessibleWithoutAuthentication() {
        // No authentication is performed before navigating, covering BR-003 (Anonymous Access).
        assertDoesNotThrow(() -> navigate(VetsView.class),
                "Expected /vets to resolve without authentication");
    }
}
