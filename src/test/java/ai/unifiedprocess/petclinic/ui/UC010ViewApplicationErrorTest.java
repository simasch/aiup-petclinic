package ai.unifiedprocess.petclinic.ui;

import ai.unifiedprocess.petclinic.PetClinicTestBase;
import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import ai.unifiedprocess.petclinic.core.ui.ApplicationErrorView;
import ai.unifiedprocess.petclinic.core.ui.NotFoundErrorView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerDetailsView;
import ai.unifiedprocess.petclinic.owner.ui.OwnerRouteParameters;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-010: View Application Error.
 *
 * <p>Covers the two alternative flows: A1 (NotFoundException → 404 shell)
 * and A2 (generic RuntimeException → 500 shell), plus the main flow
 * triggered by the {@code /oups} demonstration route.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC010ViewApplicationErrorTest extends PetClinicTestBase {

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error")
    void navigatingToOupsShowsApplicationErrorView() {
        UI.getCurrent().navigate("oups");

        assertDoesNotThrow(
                () -> $(ApplicationErrorView.class).single(),
                "Expected ApplicationErrorView to be rendered for /oups");
        ApplicationErrorView errorView = $(ApplicationErrorView.class).single();
        H2 heading = $(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Unexpected Error", businessRules = "BR-003")
    void errorViewShowsExceptionMessage() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = $(ApplicationErrorView.class).single();
        Paragraph message = $(Paragraph.class).from(errorView).single();
        assertTrue(message.getText().startsWith("Expected:"),
                "Expected the CrashView exception message, got: " + message.getText());
    }

    @Test
    @UseCase(id = "UC-010", businessRules = "BR-002")
    void errorViewOffersBackToHomeLink() {
        UI.getCurrent().navigate("oups");

        ApplicationErrorView errorView = $(ApplicationErrorView.class).single();
        RouterLink backLink = $(RouterLink.class).from(errorView).single();
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
                () -> $(NotFoundErrorView.class).single(),
                "Expected NotFoundErrorView for unknown owner id");
        NotFoundErrorView errorView = $(NotFoundErrorView.class).single();
        H2 heading = $(H2.class).from(errorView).single();
        assertEquals("Something happened...", heading.getText());
        Paragraph message = $(Paragraph.class).from(errorView).single();
        assertTrue(message.getText().contains("99999"),
                "Expected the not-found message to include the missing owner id, got: "
                        + message.getText());
    }
}
