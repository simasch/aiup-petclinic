package ai.unifiedprocess.petclinic.owner.domain;

import java.util.List;

/**
 * Projection used by the Find Owners grid (UC-004). Bundles an owner with
 * their pet names so the grid can render the "Pets" column without an N+1
 * fan-out through {@code PetRepository}. Pet names come from the same jOOQ
 * query as the owner row via a correlated {@code multiset} subquery.
 */
public record OwnerListing(Owner owner, List<String> petNames) {

    /** Space-separated pet-name label, matching the Spring PetClinic original. */
    public String petsLabel() {
        return String.join(" ", petNames);
    }
}
