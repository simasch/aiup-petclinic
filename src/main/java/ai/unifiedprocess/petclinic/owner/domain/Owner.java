package ai.unifiedprocess.petclinic.owner.domain;

/**
 * UC-003: Register New Owner.
 */
public record Owner(Integer id, String firstName, String lastName, String address, String city, String telephone) {
}
