package ai.unifiedprocess.petclinic;

import com.vaadin.browserless.SpringBrowserlessTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared test base for view tests.
 *
 * <p>Test data is seeded declaratively by Flyway via
 * {@code src/test/resources/db/migration/V2__seed_reference_data.sql}, the
 * original Spring PetClinic reference dataset. The file lives on the test
 * classpath only, so production runs with an empty database. Individual
 * tests are wrapped in a Spring-managed transaction and rolled back at
 * method end, so mutations performed inside a test never leak into the
 * next one — every test sees the seed state on entry.
 *
 * <p>The constants below point at rows in V2 that tests reference by id.
 * They must stay in sync with {@code V2__seed_reference_data.sql}.
 */
@Transactional
public abstract class PetClinicTestBase extends SpringBrowserlessTest {

    // ---- Owners -------------------------------------------------------------

    /** UC-004 A2 (unique "Fra" prefix match); UC-009 A2 unrelated owner for the mismatch case. */
    protected static final int OWNER_FRANKLIN_ID = 1;
    /** UC-007 A1 duplicate-pet-name owner — already owns "Basil". */
    protected static final int OWNER_DAVIS_BETTY_ID = 2;
    /** UC-006 edit target; UC-007 general add-pet owner (already owns "Iggy"). */
    protected static final int OWNER_DAVIS_HAROLD_ID = 4;
    /** UC-005 / UC-008 / UC-009 main owner — has "Max" and "Samantha" with 2 visits each. */
    protected static final int OWNER_COLEMAN_ID = 6;

    // ---- Pets ---------------------------------------------------------------

    /** UC-008 edit target (renamed to "Max Jr"); UC-009 visit-booking target. Jean Coleman's cat "Max". */
    protected static final int PET_MAX_ID = 8;
    /** UC-008 rename-collision source — Jean Coleman's cat "Samantha", renamed to "Max" to trigger BR-001. */
    protected static final int PET_SAMANTHA_ID = 7;
}
