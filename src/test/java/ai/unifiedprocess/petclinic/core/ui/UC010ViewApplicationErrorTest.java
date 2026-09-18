package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-010: View Application Error.
 *
 * <p>Covers the two alternative flows: A1 (NotFoundException → 404 shell)
 * and A2 (generic RuntimeException → 500 shell), plus the main flow
 * triggered by the {@code /oups} demonstration route.
 */
class UC010ViewApplicationErrorTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error")
    void navigatingToOupsShowsApplicationErrorView() {
        UI.getCurrent().navigate("oups");

        assertDoesNotThrow(
                () -> find(ApplicationErrorView.class).single(),
                "Expected ApplicationErrorView to be rendered for /oups");
        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        H2 heading = find(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = "BR-003")
    void errorViewShowsExceptionMessage() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        Paragraph message = find(Paragraph.class).from(errorView).single();
        assertTrue(message.getText().startsWith("Expected:"),
                "Expected the CrashView exception message, got: " + message.getText());
    }

    @Test
    @UseCase(id = "UC-010", businessRules = "BR-002")
    void errorViewOffersBackToHomeLink() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        RouterLink backLink = find(RouterLink.class).from(errorView).single();
        assertEquals("Back to Home", backLink.getText());
        // The RouterLink resolves to the WelcomeView route, which is "".
        assertEquals("", backLink.getHref());
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A1: Resource Not Found")
    void unknownOwnerRoutesToNotFoundErrorView() {
        // Bypass the test base's navigate(Class, ...) which validates target
        // type equality — routing lands on NotFoundErrorView here.
        UI.getCurrent().navigate(OwnerDetailsView.class,
                OwnerRouteParameters.forOwner(99999));

        assertDoesNotThrow(
                () -> find(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for unknown owner id");
        NotFoundErrorView errorView = find(NotFoundErrorView.class).single();
        H2 heading = find(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
        Paragraph message = find(Paragraph.class).from(errorView).single();
        assertTrue(message.getText().contains("99999"),
                "Expected the not-found message to include the missing owner id, got: "
                        + message.getText());
    }

    @Test
    @UseCase(id = "UC-010", businessRules = "BR-001")
    void errorViewIsReachableWithoutAuthentication() {
        assertDoesNotThrow(() -> UI.getCurrent().navigate("oups"),
                "Expected /oups to be reachable without authentication");
        assertDoesNotThrow(() -> find(ApplicationErrorView.class).single(),
                "Expected the error view to render anonymously, as for UC-001 and UC-002");
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = "BR-004")
    void oupsRouteAlwaysFailsWithTheShowcaseMessage() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = find(ApplicationErrorView.class).single();
        Paragraph message = find(Paragraph.class).from(errorView).single();
        // BR-004 pins the exact wording, which the original Spring PetClinic
        // CrashController uses — an approximate match would let it drift.
        assertEquals(
                "Expected: controller used to showcase what happens when an exception is thrown",
                message.getText());
    }
}
