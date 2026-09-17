package ai.unifiedprocess.petclinic.owner.ui;

import com.vaadin.flow.router.RouteParameters;

/**
 * Small builder for the nested route parameters used across owner/pet/visit views.
 * Centralises the parameter names so routing and view code cannot drift.
 */
public final class OwnerRouteParameters {

    public static final String OWNER_ID = "ownerId";
    public static final String PET_ID = "petId";

    private OwnerRouteParameters() {}

    public static RouteParameters forOwner(Integer ownerId) {
        return new RouteParameters(OWNER_ID, ownerId.toString());
    }

    public static RouteParameters forPet(Integer ownerId, Integer petId) {
        return new RouteParameters(
                java.util.Map.of(
                        OWNER_ID, ownerId.toString(),
                        PET_ID, petId.toString()));
    }
}
