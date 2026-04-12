package ai.unifiedprocess.petclinic.core.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-010 BR-004: the {@code /oups} demonstration route. Always throws a
 * {@link RuntimeException} so the navigation drawer's "Error" link has a
 * target that exercises {@link ApplicationErrorView}. Mirrors Spring
 * PetClinic's {@code CrashController}, including the exception message.
 */
@Route(value = "oups", layout = MainLayout.class)
@PageTitle("Error")
public class CrashView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        throw new RuntimeException(
                "Expected: controller used to showcase what happens when an exception is thrown");
    }
}
