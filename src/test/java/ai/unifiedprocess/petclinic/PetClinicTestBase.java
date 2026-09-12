package ai.unifiedprocess.petclinic;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for server-side (browserless) use-case tests that touch the
 * database. Wires the Testcontainers Postgres, rolls every test back, and
 * exposes the canonical seed-data IDs from
 * {@code src/test/resources/db/migration/V2__seed_reference_data.sql}.
 * <p>
 * Tests that do not need the database may extend
 * {@link SpringBrowserlessTest} directly.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class PetClinicTestBase extends SpringBrowserlessTest {

    // Owners (V2 seed order)
    protected static final int OWNER_FRANKLIN_ID = 1;
    protected static final int OWNER_DAVIS_BETTY_ID = 2;
    protected static final int OWNER_RODRIQUEZ_ID = 3;
    protected static final int OWNER_DAVIS_HAROLD_ID = 4;
    protected static final int OWNER_MCTAVISH_ID = 5;
    protected static final int OWNER_COLEMAN_ID = 6;
    protected static final int OWNER_BLACK_ID = 7;
    protected static final int OWNER_ESCOBITO_ID = 8;
    protected static final int OWNER_SCHROEDER_ID = 9;
    protected static final int OWNER_ESTABAN_ID = 10;

    // Pets
    protected static final int PET_LEO_ID = 1;
    protected static final int PET_SAMANTHA_ID = 7;
    protected static final int PET_MAX_ID = 8;

    // Pet types
    protected static final int TYPE_CAT_ID = 1;
    protected static final int TYPE_DOG_ID = 2;

    // Vets
    protected static final int VET_CARTER_ID = 1;
    protected static final int VET_LEARY_ID = 2;

    /**
     * Navigates by location string without asserting the resulting view
     * type. Use for not-found / error flows where the typed
     * {@code navigate(View.class, params)} would throw because the router
     * rerouted to an error view.
     */
    protected void navigateRaw(String location) {
        UI.getCurrent().navigate(location);
    }
}
