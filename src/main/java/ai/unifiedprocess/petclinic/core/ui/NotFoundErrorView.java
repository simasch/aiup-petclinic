package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UC-010 A1: "resource not found" variant of the application error view.
 * Overrides Vaadin's default {@code HasErrorParameter<NotFoundException>}
 * so that bad owner / pet / visit ids (as produced by UC-005, UC-006,
 * UC-007, UC-008, UC-009) and unknown routes all land on the same friendly
 * error shell as the generic {@link ApplicationErrorView}.
 */
@ParentLayout(MainLayout.class)
public class NotFoundErrorView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private final ErrorPanel panel;

    public NotFoundErrorView() {
        setSizeFull();
        setPadding(false);
        panel = new ErrorPanel();
        add(panel);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        String message = parameter.hasCustomMessage()
                ? parameter.getCustomMessage()
                : parameter.getException().getMessage();
        panel.setMessage(message);
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
