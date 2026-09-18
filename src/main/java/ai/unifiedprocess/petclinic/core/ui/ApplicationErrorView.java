package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UC-010 A2: generic application error view. Catches any uncaught
 * {@link Exception} raised during navigation — including the deliberate
 * throw from {@link CrashView} — and renders the {@link ErrorShellView}
 * inside {@link MainLayout} with HTTP 500.
 *
 * <p>More specific errors (currently only {@code NotFoundException}) are
 * handled by {@link NotFoundErrorView}, which wins over this view by
 * virtue of its narrower generic type.
 */
@ParentLayout(MainLayout.class)
public class ApplicationErrorView extends ErrorShellView implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        return show(parameter, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
