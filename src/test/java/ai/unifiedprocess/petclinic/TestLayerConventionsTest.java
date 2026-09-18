package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.vaadin.browserless.SpringBrowserlessTest;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable version of the naming rule in docs/architecture/testing.md.
 *
 * <p>{@link ArchitectureTest} deliberately excludes the test tree, so the one
 * convention that governs the test tree itself has to live in its own class.
 * A misnamed test is the quietest failure this project has: nothing errors,
 * the wrong Maven plugin simply never picks the class up and it reports as
 * passing by never running at all. That is worth a rule rather than a
 * paragraph.
 *
 * <p>Only the two dangerous directions are asserted. Plenty of {@code *Test}
 * classes are neither view tests nor journeys — the sensors, this class — so
 * "every {@code *Test} extends something" would be false.
 */
@ArchTag("sensor")
@AnalyzeClasses(packages = "ai.unifiedprocess.petclinic")
class TestLayerConventionsTest {

    @ArchTest
    static final ArchRule browserlessTestsAreNotNamedIT =
            noClasses()
                    .that().areAssignableTo(SpringBrowserlessTest.class)
                    .should().haveSimpleNameEndingWith("IT")
                    .because("testing.md: Surefire runs *Test — a browserless test named *IT "
                            + "is skipped by ./mvnw test and runs in the wrong phase");

    @ArchTest
    static final ArchRule playwrightTestsAreNotNamedTest =
            noClasses()
                    .that().areAssignableTo(AbstractBasePlaywrightIT.class)
                    .should().haveSimpleNameEndingWith("Test")
                    .because("testing.md: Failsafe runs *IT — a Playwright test named *Test "
                            + "starts a browser inside the unit-test phase");
}
