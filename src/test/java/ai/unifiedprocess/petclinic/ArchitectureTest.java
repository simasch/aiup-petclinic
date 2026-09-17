package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.router.Route;
import org.springframework.stereotype.Repository;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Executable version of docs/guidelines/architecture.md.
 * Every rule references the section it enforces.
 */
@AnalyzeClasses(
        packages = "ai.unifiedprocess.petclinic",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- Module layout ---

    @ArchTest
    static final ArchRule featuresHaveOnlyUiAndDomain =
            classes()
                    .that().resideInAPackage("ai.unifiedprocess.petclinic.(*)..")
                    .and().resideOutsideOfPackage("..core..")
                    .should().resideInAnyPackage("..ui..", "..domain..")
                    .because("architecture.md: each feature has exactly the sub-packages ui and domain");

    @ArchTest
    static final ArchRule noServiceOrDtoLayer =
            noClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("Dto")
                    .orShould().haveSimpleNameEndingWith("DTO")
                    .because("architecture.md: no separate service/DTO layering beyond ui + domain");

    // --- Cross-feature rule ---

    @ArchTest
    static final ArchRule domainDoesNotDependOnUi =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..ui..")
                    .because("architecture.md: domain is the boundary between features");

    @ArchTest
    static final ArchRule domainIsFreeOfVaadin =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.vaadin..")
                    .because("architecture.md: domain holds records and jOOQ queries only");

    // Sliced on domain, not on the whole feature: architecture.md deliberately allows a
    // feature's ui to reach into another feature's domain (OwnerDetailsView -> PetRepository)
    // and to reference another feature's views as .class route tokens, which makes the
    // feature-level graph cyclic by design. The domain packages must stay acyclic.
    @ArchTest
    static final ArchRule noCyclesBetweenFeatures =
            slices()
                    .matching("ai.unifiedprocess.petclinic.(*).domain..")
                    .should().beFreeOfCycles()
                    .because("architecture.md: domain is the boundary between features");

    // --- Data access (jOOQ) ---

    @ArchTest
    static final ArchRule noJpaNoSpringData =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
                    .because("architecture.md: jOOQ only, no JPA, no Spring Data");

    @ArchTest
    static final ArchRule noFetchInto =
            noClasses()
                    .should().callMethodWhere(
                            JavaCall.Predicates.target(HasName.Predicates.name("fetchInto")))
                    .because("architecture.md: use Records.mapping(Type::new) for compile-time column checking");

    @ArchTest
    static final ArchRule domainRecordsAreValidationFree =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
                    .because("architecture.md: the Vaadin form is the validation boundary");

    // --- Persistence stereotype ---

    @ArchTest
    static final ArchRule repositoriesAreNamedAndAnnotated =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().beAnnotatedWith(Repository.class)
                    .andShould().resideInAPackage("..domain..")
                    .because("architecture.md: @Repository enables exception translation");

    @ArchTest
    static final ArchRule repositoryAnnotationOnlyOnRepositories =
            classes()
                    .that().areAnnotatedWith(Repository.class)
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("architecture.md: not *Queries, *Dao, *Store");

    // --- Vaadin view conventions ---

    @ArchTest
    static final ArchRule viewsLiveInUi =
            classes()
                    .that().areAnnotatedWith(Route.class)
                    .should().resideInAPackage("..ui..")
                    .andShould().haveSimpleNameEndingWith("View");

    // getStyle() is declared on HasStyle, but the call site's target owner is the concrete
    // component (Button, VerticalLayout, ...), so callMethod(HasStyle.class, "getStyle")
    // would never match. Match any getStyle() on a HasStyle subtype instead.
    @ArchTest
    static final ArchRule noInlineStyles =
            noClasses()
                    .should().callMethodWhere(describe("getStyle() on a HasStyle component",
                            (JavaCall<?> call) -> call.getTarget().getName().equals("getStyle")
                                    && assignableTo(HasStyle.class).test(call.getTargetOwner())))
                    .because("architecture.md: use LumoUtility class names, never getStyle().set()");
}
