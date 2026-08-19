package ai.unifiedprocess.petclinic.owner.domain;

/**
 * A pet owner registered with the clinic.
 *
 * <p>See {@code docs/entity_model.md} → OWNER. All textual attributes are
 * mandatory; {@code telephone} is ten digits (validated in the UI layer).
 * {@code id} is {@code null} for unsaved owners.
 */
public record Owner(
        Integer id,
        String firstName,
        String lastName,
        String address,
        String city,
        String telephone) {

    public static Owner empty() {
        return new Owner(null, "", "", "", "", "");
    }

    public Owner withId(Integer newId) {
        return new Owner(newId, firstName, lastName, address, city, telephone);
    }
}
