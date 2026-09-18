package ai.unifiedprocess.petclinic.owner.domain;

import java.io.Serializable;

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
        String telephone) implements Serializable {

    public static Owner empty() {
        return new Owner(null, "", "", "", "", "");
    }
}
