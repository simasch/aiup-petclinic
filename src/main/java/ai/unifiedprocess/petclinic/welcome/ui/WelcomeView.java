package ai.unifiedprocess.petclinic.welcome.ui;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * UC-001: View Welcome Page.
 *
 * Root view shown at {@code /}. Displays a decorative image and a link to
 * https://unifiedprocess.ai. The surrounding logo and navigation menu are
 * rendered by {@link MainLayout}.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Welcome")
public class WelcomeView extends VerticalLayout {

    public WelcomeView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        Image decorativeImage = new Image("images/pets.png", "Pets at the clinic");
        decorativeImage.setMaxWidth("600px");
        decorativeImage.setWidthFull();

        Anchor unifiedProcessLink = new Anchor("https://unifiedprocess.ai", "https://unifiedprocess.ai");
        unifiedProcessLink.setTarget(AnchorTarget.BLANK);
        unifiedProcessLink.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        add(decorativeImage, unifiedProcessLink);
    }
}
