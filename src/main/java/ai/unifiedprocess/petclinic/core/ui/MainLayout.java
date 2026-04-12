package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.petclinic.owner.ui.FindOwnersView;
import ai.unifiedprocess.petclinic.vet.ui.VetsView;
import ai.unifiedprocess.petclinic.welcome.ui.WelcomeView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Shared application shell. Renders the clinic logo in the header and the
 * main navigation menu (Home, Find Owners, Veterinarians, Error) as a
 * {@link SideNav} in the drawer, per UC-001.
 */
public class MainLayout extends AppLayout {

    private final Image logo;
    private final H1 title;
    private final SideNav sideNav;
    private final SideNavItem homeLink;
    private final SideNavItem findOwnersLink;
    private final SideNavItem vetsLink;
    private final SideNavItem errorLink;

    public MainLayout() {
        logo = new Image("images/petclinic-logo.svg", "PetClinic logo");
        logo.setHeight("40px");

        title = new H1("PetClinic");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(), logo, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);

        homeLink = new SideNavItem("Home", WelcomeView.class);
        findOwnersLink = new SideNavItem("Find Owners", FindOwnersView.class);
        vetsLink = new SideNavItem("Veterinarians", VetsView.class);
        errorLink = new SideNavItem("Error", CrashView.class);

        sideNav = new SideNav();
        sideNav.addItem(homeLink, findOwnersLink, vetsLink, errorLink);

        addToDrawer(sideNav);
    }
}
