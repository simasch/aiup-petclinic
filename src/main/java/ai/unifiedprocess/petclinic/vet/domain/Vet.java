package ai.unifiedprocess.petclinic.vet.domain;

import java.util.List;

/**
 * UC-002: View Veterinarians. Specialties are ordered alphabetically by name (BR-002).
 */
public record Vet(Integer id, String firstName, String lastName, List<String> specialties) {
}
