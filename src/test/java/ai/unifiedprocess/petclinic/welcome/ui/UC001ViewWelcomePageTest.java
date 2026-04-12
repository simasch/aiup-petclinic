package ai.unifiedprocess.petclinic.welcome.ui;

import ai.unifiedprocess.petclinic.TestcontainersConfiguration;
import ai.unifiedprocess.petclinic.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * UC-001: View Welcome Page.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC001ViewWelcomePageTest extends SpringBrowserlessTest {

    @Test
    @UseCase(id = "UC-001", businessRules = "BR-001")
    void welcomePageRendersAtRootRoute() {
        // No authentication is performed, covering BR-001 (Anonymous Access).
        assertDoesNotThrow(() -> navigate(WelcomeView.class),
                "Expected root route '/' to resolve to WelcomeView without error");
    }

    @Test
    @UseCase(id = "UC-001")
    void welcomePageShowsDecorativeImage() {
        navigate(WelcomeView.class);

        assertDoesNotThrow(() -> $(Image.class)
                .withPropertyValue(Image::getSrc, "images/pets.png")
                .single(),
                "Expected exactly one decorative image with src 'images/pets.png'");
    }

    @Test
    @UseCase(id = "UC-001")
    void clinicLogoIsRendered() {
        navigate(WelcomeView.class);

        assertDoesNotThrow(() -> $(Image.class)
                .withPropertyValue(Image::getSrc, "images/petclinic-logo.svg")
                .single(),
                "Expected exactly one clinic logo image with src 'images/petclinic-logo.svg'");
    }

    @Test
    @UseCase(id = "UC-001")
    void mainNavigationMenuHasRequiredItems() {
        navigate(WelcomeView.class);

        assertDoesNotThrow(() -> $(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Home").single(),
                "Missing Home navigation item");
        assertDoesNotThrow(() -> $(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Find Owners").single(),
                "Missing Find Owners navigation item");
        assertDoesNotThrow(() -> $(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Veterinarians").single(),
                "Missing Veterinarians navigation item");
        assertDoesNotThrow(() -> $(SideNavItem.class)
                .withPropertyValue(SideNavItem::getLabel, "Error").single(),
                "Missing Error navigation item");
    }
}
