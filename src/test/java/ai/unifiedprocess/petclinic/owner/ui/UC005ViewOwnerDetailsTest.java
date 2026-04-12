package ai.unifiedprocess.petclinic.owner.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-005: View Owner Details.
 *
 * <p>Uses the seed owner "Jean Coleman" ({@link #OWNER_COLEMAN_ID}) who
 * has 2 pets (Max, Samantha) and 2 visits on each inserted out of
 * chronological order in {@code V2__seed_reference_data.sql} so the
 * UC-005 BR-001 sort is actually exercised.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC005ViewOwnerDetailsTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-005")
    void ownerDetailsShowNameAddressCityAndTelephone() {
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Jean Coleman").single(),
                "Expected owner name to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("105 N. Lake St.").single(),
                "Expected address to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("Monona").single(),
                "Expected city to be rendered");
        assertDoesNotThrow(
                () -> $(Paragraph.class).withText("6085552654").single(),
                "Expected telephone to be rendered");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = "BR-002")
    void petsAreListedAlphabeticallyByName() {
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        List<String> names = $(H3.class).all().stream()
                .map(H3::getText)
                .filter(t -> !t.equals("Pets and Visits"))
                .toList();
        assertEquals(List.of("Max", "Samantha"), names);
    }

    @Test
    @UseCase(id = "UC-005", businessRules = "BR-001")
    void visitsForEachPetAreListedChronologically() {
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        // Max's two seed visits were inserted newest-first (2011-03-04 before
        // 2009-06-04), so this test actually exercises the ORDER BY in
        // VisitRepository. Max is alphabetically first, so its visits lead.
        List<String> visitTexts = $(Paragraph.class).all().stream()
                .map(Paragraph::getText)
                .filter(t -> t.contains("\u2014"))
                .toList();
        assertTrue(visitTexts.get(0).startsWith("2009-06-04"),
                "Expected chronological order, got: " + visitTexts);
        assertTrue(visitTexts.get(1).startsWith("2011-03-04"));
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A1: Owner Not Found")
    void unknownOwnerRoutesToErrorView() {
        // UC-005 A1 now flows into UC-010: a missing owner id throws a
        // NotFoundException inside OwnerDetailsView.beforeEnter(), which
        // is rerouted to NotFoundErrorView. The test base's navigate()
        // enforces target-type equality, so we bypass it via UI.navigate.
        com.vaadin.flow.component.UI.getCurrent().navigate(
                OwnerDetailsView.class, OwnerRouteParameters.forOwner(99999));

        assertDoesNotThrow(
                () -> $(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView to be rendered for unknown owner id");
        NotFoundErrorView errorView = $(NotFoundErrorView.class).single();
        H2 heading = $(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
        Paragraph message = $(Paragraph.class).from(errorView)
                .withCondition(p -> p.getText().contains("99999")).single();
        assertTrue(message.getText().contains("99999"),
                "Expected error message to include the missing owner id, got: "
                        + message.getText());
    }

    @Test
    @UseCase(id = "UC-005")
    void editAndAddPetActionsAreOffered() {
        navigate(OwnerDetailsView.class,
                Map.of(OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID)));

        assertDoesNotThrow(
                () -> test($(Button.class).withText("Edit Owner").single()).click());
        assertEquals("owners/" + OWNER_COLEMAN_ID + "/edit",
                com.vaadin.flow.component.UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }
}
