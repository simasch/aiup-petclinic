package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.ErrorParameter;

/**
 * The one error screen of UC-010. Vaadin picks the error view whose
 * {@code HasErrorParameter} type parameter is closest to the thrown exception,
 * so {@link ApplicationErrorView} and {@link NotFoundErrorView} exist to
 * carry that parameter and the HTTP status that goes with it. Everything the
 * user sees — the shell, the heading, the message, the way home — is here and
 * identical for both (UC-010 BR-003: the message, never a stack trace).
 */
abstract class ErrorShellView extends VerticalLayout {

    private final ErrorPanel panel;

    ErrorShellView() {
        setSizeFull();
        setPadding(false);
        panel = new ErrorPanel();
        add(panel);
    }

    /** Renders the failure and returns the status the browser is told. */
    protected int show(ErrorParameter<? extends Exception> parameter, int httpStatus) {
        panel.setMessage(parameter.hasCustomMessage()
                ? parameter.getCustomMessage()
                : parameter.getException().getMessage());
        return httpStatus;
    }
}
