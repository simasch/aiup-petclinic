package ai.unifiedprocess.petclinic.vet.domain;

import static ai.unifiedprocess.demo.petclinic.database.Tables.SPECIALTIES;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VETS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.VET_SPECIALTIES;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import java.util.List;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

/**
 * Data access for UC-002: View Veterinarians.
 */
@Repository
public class VetRepository {

    private final DSLContext dsl;

    public VetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * A chunk of vets for the lazy-loading grid (BR-001), ordered by last name then first name.
     */
    public Stream<Vet> findPage(int offset, int limit) {
        return dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, specialtyNames())
                .from(VETS)
                .orderBy(VETS.LAST_NAME.asc(), VETS.FIRST_NAME.asc())
                .offset(offset)
                .limit(limit)
                .fetch(Records.mapping(Vet::new))
                .stream();
    }

    public int count() {
        return dsl.fetchCount(VETS);
    }

    /**
     * All vets at once, for the JSON/XML representation (A1).
     */
    public List<Vet> findAll() {
        return dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, specialtyNames())
                .from(VETS)
                .orderBy(VETS.LAST_NAME.asc(), VETS.FIRST_NAME.asc())
                .fetch(Records.mapping(Vet::new));
    }

    private static Field<List<String>> specialtyNames() {
        return multiset(
                select(SPECIALTIES.NAME)
                        .from(VET_SPECIALTIES)
                        .join(SPECIALTIES).on(SPECIALTIES.ID.eq(VET_SPECIALTIES.SPECIALTY_ID))
                        .where(VET_SPECIALTIES.VET_ID.eq(VETS.ID))
                        .orderBy(SPECIALTIES.NAME.asc()))
                .convertFrom(r -> r.map(Record1::value1));
    }
}
