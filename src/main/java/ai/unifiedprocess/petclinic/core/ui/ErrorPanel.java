package ai.unifiedprocess.petclinic.core.ui;

import ai.unifiedprocess.petclinic.welcome.ui.WelcomeView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Shared content for UC-010 error views. Both {@link ApplicationErrorView}
 * and {@link NotFoundErrorView} mount one of these so the two error flows
 * render identically — only the HTTP status differs.
 *
 * <p>Test assertions on the rendered content use {@code $()} locators
 * scoped to the containing error view, so there's no need to expose
 * fields on this helper.
 */
class ErrorPanel extends VerticalLayout {

    private final Paragraph messagePara;

    ErrorPanel() {
        setPadding(true);
        setSpacing(true);
        addClassNames(LumoUtility.Padding.LARGE);

        H2 heading = new H2("Something happened...");
        heading.addClassNames(LumoUtility.Margin.NONE);

        messagePara = new Paragraph();
        messagePara.addClassNames(LumoUtility.TextColor.SECONDARY);

        RouterLink backLink = new RouterLink("Back to Home", WelcomeView.class);

        add(heading, messagePara, backLink);
    }

    void setMessage(String message) {
        messagePara.setText(message == null ? "" : message);
    }
}
