package ai.unifiedprocess.petclinic.vet.domain;

import java.util.List;

/**
 * A veterinarian together with the specialties they hold.
 *
 * <p>{@code specialties} is sorted alphabetically — see UC-002 BR-002.
 */
public record Vet(Integer id, String firstName, String lastName, List<String> specialties) {

    /**
     * Human-readable label for the specialties column. See UC-002 main success
     * scenario step 3: a comma-separated list, or {@code "none"} if the vet
     * holds no specialties.
     */
    public String specialtiesLabel() {
        return specialties.isEmpty() ? "none" : String.join(", ", specialties);
    }
}
