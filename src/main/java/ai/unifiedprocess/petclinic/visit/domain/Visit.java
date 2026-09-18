package ai.unifiedprocess.petclinic.visit.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A veterinary visit booked for a pet. See {@code docs/entity_model.md} → VISIT.
 */
public record Visit(Integer id, LocalDate visitDate, String description, Integer petId) implements Serializable {
}
