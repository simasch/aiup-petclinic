package ai.unifiedprocess.petclinic.visit.domain;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ai.unifiedprocess.demo.petclinic.database.Tables.VISITS;
import static org.jooq.Records.mapping;

/**
 * jOOQ-backed persistence for {@link Visit}. Feeds UC-005 (read) and UC-009 (write).
 */
@Repository
public class VisitRepository {

    private final DSLContext dsl;

    public VisitRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Visits for a pet, ascending by date (UC-005 BR-001). */
    public List<Visit> findByPetId(Integer petId) {
        return dsl.select(VISITS.ID, VISITS.VISIT_DATE, VISITS.DESCRIPTION, VISITS.PET_ID)
                .from(VISITS)
                .where(VISITS.PET_ID.eq(petId))
                .orderBy(VISITS.VISIT_DATE.asc(), VISITS.ID.asc())
                .fetch(mapping(Visit::new));
    }

    public Integer insert(Visit visit) {
        return dsl.insertInto(VISITS)
                .set(VISITS.VISIT_DATE, visit.visitDate())
                .set(VISITS.DESCRIPTION, visit.description())
                .set(VISITS.PET_ID, visit.petId())
                .returning(VISITS.ID)
                .fetchOne()
                .getId();
    }
}
