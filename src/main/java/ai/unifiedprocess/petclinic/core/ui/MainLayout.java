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

    /** Accessible name of the drawer's navigation landmark. */
    public static final String NAV_LABEL = "Main menu";

    public MainLayout() {
        Image logo = new Image("images/petclinic-logo.svg", "PetClinic logo");
        logo.setHeight("40px");

        H1 title = new H1("PetClinic");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(), logo, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);

        SideNavItem homeLink = new SideNavItem("Home", WelcomeView.class);
        SideNavItem findOwnersLink = new SideNavItem("Find Owners", FindOwnersView.class);
        SideNavItem vetsLink = new SideNavItem("Veterinarians", VetsView.class);
        SideNavItem errorLink = new SideNavItem("Error", CrashView.class);

        SideNav sideNav = new SideNav();
        // The menu shows no visible heading, so the <nav> landmark would reach
        // assistive technology — and accessibility-first test locators —
        // unnamed. SideNav has no setAriaLabel, hence the element-level attribute.
        sideNav.getElement().setAttribute("aria-label", NAV_LABEL);
        sideNav.addItem(homeLink, findOwnersLink, vetsLink, errorLink);

        addToDrawer(sideNav);
    }
}
