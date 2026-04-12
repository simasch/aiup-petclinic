package ai.unifiedprocess.petclinic.visit.domain;

import java.time.LocalDate;

/**
 * A veterinary visit booked for a pet. See {@code docs/entity_model.md} → VISIT.
 */
public record Visit(Integer id, LocalDate visitDate, String description, Integer petId) {

    public static Visit empty(Integer petId) {
        return new Visit(null, LocalDate.now(), "", petId);
    }
}
