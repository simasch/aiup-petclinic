package ai.unifiedprocess.petclinic.pet.domain;

import java.time.LocalDate;

/**
 * A pet belonging to an owner. See {@code docs/entity_model.md} → PET.
 *
 * <p>{@code birthDate} is optional; {@code type} is required at creation
 * time (UC-007 BR-003) but the view may leave it unchanged on update
 * (UC-008 BR-003).
 */
public record Pet(
        Integer id,
        String name,
        LocalDate birthDate,
        PetType type,
        Integer ownerId) {

    public static Pet empty(Integer ownerId) {
        return new Pet(null, "", null, null, ownerId);
    }
}
