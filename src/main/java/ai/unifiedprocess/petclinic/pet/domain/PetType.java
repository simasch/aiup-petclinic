package ai.unifiedprocess.petclinic.pet.domain;

import java.io.Serializable;

/**
 * A species/category for pets (cat, dog, etc.). See {@code docs/entity_model.md} → PET_TYPE.
 */
public record PetType(Integer id, String name) implements Serializable {
}
