package ai.unifiedprocess.petclinic.owner.domain;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;

import ai.unifiedprocess.demo.petclinic.database.tables.records.OwnersRecord;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

/**
 * Data access for UC-003: Register New Owner.
 */
@Repository
public class OwnerRepository {

    private final DSLContext dsl;

    public OwnerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Persists a new owner, assigning it a server-generated identifier (BR-003).
     */
    public Owner insert(Owner owner) {
        OwnersRecord record = dsl.newRecord(OWNERS);
        record.setFirstName(owner.firstName());
        record.setLastName(owner.lastName());
        record.setAddress(owner.address());
        record.setCity(owner.city());
        record.setTelephone(owner.telephone());
        record.store();
        return new Owner(record.getId(), record.getFirstName(), record.getLastName(), record.getAddress(),
                record.getCity(), record.getTelephone());
    }

    public Optional<Owner> findById(Integer id) {
        return dsl.select(OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME, OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
                .from(OWNERS)
                .where(OWNERS.ID.eq(id))
                .fetchOptional(Records.mapping(Owner::new));
    }
}
