package ai.unifiedprocess.petclinic.vet.domain;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

import static ai.unifiedprocess.demo.petclinic.database.Tables.*;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

/**
 * jOOQ-backed persistence for {@link Vet}. Feeds the Veterinarians grid (UC-002).
 */
@Repository
public class VetRepository {

    private final DSLContext dsl;

    public VetRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Fetch a page of vets in stable order (last name, first name, id), each
     * with their specialties pre-loaded alphabetically (BR-002).
     */
    public Stream<Vet> findPage(int offset, int limit) {
        Field<List<String>> specialties =
                multiset(
                        select(SPECIALTIES.NAME)
                                .from(VET_SPECIALTIES)
                                .join(SPECIALTIES).on(SPECIALTIES.ID.eq(VET_SPECIALTIES.SPECIALTY_ID))
                                .where(VET_SPECIALTIES.VET_ID.eq(VETS.ID))
                                .orderBy(SPECIALTIES.NAME.asc()))
                        .convertFrom(r -> r.getValues(SPECIALTIES.NAME));

        return dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, specialties)
                .from(VETS)
                .orderBy(VETS.LAST_NAME.asc(), VETS.FIRST_NAME.asc(), VETS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(Vet::new))
                .stream();
    }

    public int count() {
        return dsl.fetchCount(VETS);
    }
}
