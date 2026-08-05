package ai.unifiedprocess.petclinic.owner.ui;

import com.vaadin.flow.router.RouteParameters;

/**
 * Centralizes owner-feature route parameter names so they can't drift between views.
 */
public final class OwnerRouteParameters {

    public static final String OWNER_ID = "ownerId";

    private OwnerRouteParameters() {
    }

    public static RouteParameters forOwner(Integer ownerId) {
        return new RouteParameters(OWNER_ID, String.valueOf(ownerId));
    }
}
