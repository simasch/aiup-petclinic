package ai.unifiedprocess.petclinic.visit.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-009: Book Visit for Pet.
 *
 * <p>Books a visit on Jean Coleman's {@link #PET_MAX_ID}. The seed data
 * already gives Max two visits (out of chronological order), so the
 * newly booked visit must be asserted as "contained in" the visit list
 * rather than by exact contents. The mismatched-owner case uses
 * {@link #OWNER_FRANKLIN_ID} — Franklin doesn't own Max — to trigger A2.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC009BookVisitForPetTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-002")
    void bookingVisitPersistsAndReturnsToOwnerDetails() {
        navigate(AddVisitView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        // BR-002: date pre-populated with today
        assertEquals(LocalDate.now(), $(DatePicker.class).withCaption("Date").single().getValue());

        test($(TextField.class).withCaption("Description").single()).setValue("Annual check-up");
        test($(Button.class).withText("Add Visit").single()).click();

        // Returned to Coleman's details page
        assertEquals("owners/" + OWNER_COLEMAN_ID,
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());

        // The newly booked visit should be rendered on the details page.
        // Visits are rendered as Paragraph("yyyy-MM-dd — description"); a
        // fresh paragraph matching today + "Annual check-up" proves the
        // insert landed in the database without touching VisitRepository.
        OwnerDetailsView details = $(OwnerDetailsView.class).single();
        String expectedVisit = LocalDate.now() + " \u2014 Annual check-up";
        List<String> visitLines = $(Paragraph.class).from(details).all().stream()
                .map(Paragraph::getText)
                .filter(t -> t.contains("\u2014"))
                .toList();
        assertTrue(visitLines.contains(expectedVisit),
                "Expected to see '" + expectedVisit + "' in visits, got: " + visitLines);
    }

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-001", scenario = "A1: Missing Description")
    void blankDescriptionIsRejected() {
        navigate(AddVisitView.class, Map.of(
                OwnerRouteParameters.OWNER_ID, Integer.toString(OWNER_COLEMAN_ID),
                OwnerRouteParameters.PET_ID, Integer.toString(PET_MAX_ID)));

        test($(Button.class).withText("Add Visit").single()).click();

        assertTrue($(TextField.class).withCaption("Description").single().isInvalid());
        // Still on the add-visit view — no navigation = nothing persisted.
        assertEquals(
                "owners/" + OWNER_COLEMAN_ID + "/pets/" + PET_MAX_ID + "/visits/new",
                UI.getCurrent().getInternals().getActiveViewLocation().getPath());
    }

    @Test
    @UseCase(id = "UC-009", businessRules = "BR-003", scenario = "A2: Pet Not Owned by Given Owner")
    void mismatchedOwnerAndPetRoutesToErrorView() {
        // Trying to book a visit for Coleman's Max under Franklin's ownership
        // fails the BeforeEnter consistency check and is rerouted to the
        // UC-010 error view. Use UI.navigate to bypass the test base's
        // target-type enforcement, since routing ends up on NotFoundErrorView.
        UI.getCurrent().navigate(AddVisitView.class,
                OwnerRouteParameters.forPet(OWNER_FRANKLIN_ID, PET_MAX_ID));

        assertDoesNotThrow(
                () -> $(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for mismatched owner/pet");
    }
}
