package ai.unifiedprocess.petclinic.vet.ui;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

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
    void veterinariansGridRendersNameAndSpecialtyColumns() {
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
        assertThat(test(grid).getCellText(1, 0)).isEqualTo("Linda");
        assertThat(test(grid).getCellText(1, 1)).isEqualTo("Douglas");
        assertThat(test(grid).getCellText(1, 2)).isEqualTo("dentistry, surgery");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-001")
    void veterinariansGridLoadsAllEntriesViaLazyDataProvider() {
        navigate(VetsView.class);

        Grid<?> grid = $(Grid.class).single();
        assertThat(test(grid).size()).isEqualTo(6);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = "BR-003")
    void veterinariansViewIsAccessibleWithoutAuthentication() {
        assertDoesNotThrow(() -> navigate(VetsView.class),
                "Expected /vets.html to resolve without authentication");
    }
}
