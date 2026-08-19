package ai.unifiedprocess.petclinic.owner.domain;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ai.unifiedprocess.demo.petclinic.database.Tables.OWNERS;
import static ai.unifiedprocess.demo.petclinic.database.Tables.PETS;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;

/**
 * jOOQ-backed persistence for {@link Owner}. Feeds UC-003…UC-006.
 */
@Repository
public class OwnerRepository {

    private final DSLContext dsl;

    public OwnerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Prefix match on last name (BR-001 of UC-004). An empty prefix returns
     * every owner (BR-003 of UC-004). Results are ordered for stable lazy
     * loading.
     */
    public Stream<Owner> findByLastNamePrefix(String lastNamePrefix, int offset, int limit) {
        return dsl.select(
                        OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                        OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
                .from(OWNERS)
                .where(OWNERS.LAST_NAME.startsWith(lastNamePrefix == null ? "" : lastNamePrefix))
                .orderBy(OWNERS.LAST_NAME.asc(), OWNERS.FIRST_NAME.asc(), OWNERS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(Owner::new))
                .stream();
    }

    /**
     * Prefix match on last name with each owner's pet names pre-joined in a
     * single query (UC-004 main flow). Avoids the N+1 that a per-row
     * {@link ai.unifiedprocess.petclinic.pet.domain.PetRepository} lookup
     * would cause in the lazy-loaded results grid. Pet names are alphabetical
     * to match {@code PetRepository.findByOwnerId}.
     */
    public Stream<OwnerListing> findListingByLastNamePrefix(String lastNamePrefix, int offset, int limit) {
        Field<List<String>> petNames = multiset(
                select(PETS.NAME)
                        .from(PETS)
                        .where(PETS.OWNER_ID.eq(OWNERS.ID))
                        .orderBy(PETS.NAME.asc()))
                .convertFrom(r -> r.getValues(PETS.NAME));

        return dsl.select(
                        row(OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                                OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE).mapping(Owner::new),
                        petNames)
                .from(OWNERS)
                .where(OWNERS.LAST_NAME.startsWith(lastNamePrefix == null ? "" : lastNamePrefix))
                .orderBy(OWNERS.LAST_NAME.asc(), OWNERS.FIRST_NAME.asc(), OWNERS.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch(mapping(OwnerListing::new))
                .stream();
    }

    public int countByLastNamePrefix(String lastNamePrefix) {
        return dsl.fetchCount(
                dsl.selectFrom(OWNERS)
                        .where(OWNERS.LAST_NAME.startsWith(lastNamePrefix == null ? "" : lastNamePrefix)));
    }

    public Optional<Owner> findById(Integer id) {
        return dsl.select(
                        OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME,
                        OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
                .from(OWNERS)
                .where(OWNERS.ID.eq(id))
                .fetchOptional(mapping(Owner::new));
    }

    /** Insert the owner and return the generated id. */
    public Integer insert(Owner owner) {
        return dsl.insertInto(OWNERS)
                .set(OWNERS.FIRST_NAME, owner.firstName())
                .set(OWNERS.LAST_NAME, owner.lastName())
                .set(OWNERS.ADDRESS, owner.address())
                .set(OWNERS.CITY, owner.city())
                .set(OWNERS.TELEPHONE, owner.telephone())
                .returning(OWNERS.ID)
                .fetchOne()
                .getId();
    }

    public void update(Owner owner) {
        dsl.update(OWNERS)
                .set(OWNERS.FIRST_NAME, owner.firstName())
                .set(OWNERS.LAST_NAME, owner.lastName())
                .set(OWNERS.ADDRESS, owner.address())
                .set(OWNERS.CITY, owner.city())
                .set(OWNERS.TELEPHONE, owner.telephone())
                .where(OWNERS.ID.eq(owner.id()))
                .execute();
    }
}
