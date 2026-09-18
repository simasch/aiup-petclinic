package ai.unifiedprocess.petclinic;

import ai.unifiedprocess.petclinic.core.ui.MainLayout;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.router.Route;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Set;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static java.util.stream.Collectors.toSet;

/**
 * Executable version of docs/architecture/development.md.
 * Every rule references the section it enforces.
 */
@ArchTag("sensor")
@AnalyzeClasses(
        packages = "ai.unifiedprocess.petclinic",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String APP_PACKAGE = "ai.unifiedprocess.petclinic";

    // --- Module layout ---

    @ArchTest
    static final ArchRule featuresHaveOnlyUiAndDomain =
            classes()
                    .that().resideInAPackage("ai.unifiedprocess.petclinic.(*)..")
                    .and().resideOutsideOfPackage("..core..")
                    .should().resideInAnyPackage("..ui..", "..domain..")
                    .because("development.md: each feature has exactly the sub-packages ui and domain");

    @ArchTest
    static final ArchRule noServiceOrDtoLayer =
            noClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("Dto")
                    .orShould().haveSimpleNameEndingWith("DTO")
                    .because("development.md: no separate service/DTO layering beyond ui + domain");

    // --- Cross-feature rule ---

    @ArchTest
    static final ArchRule domainDoesNotDependOnUi =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..ui..")
                    .because("development.md: domain is the boundary between features");

    @ArchTest
    static final ArchRule domainIsFreeOfVaadin =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("com.vaadin..")
                    .because("development.md: domain holds records and jOOQ queries only");

    // Sliced on domain, not on the whole feature: architecture.md deliberately allows a
    // feature's ui to reach into another feature's domain (OwnerDetailsView -> PetRepository)
    // and to reference another feature's views as .class route tokens, which makes the
    // feature-level graph cyclic by design. The domain packages must stay acyclic.
    @ArchTest
    static final ArchRule noCyclesBetweenFeatures =
            slices()
                    .matching("ai.unifiedprocess.petclinic.(*).domain..")
                    .should().beFreeOfCycles()
                    .because("development.md: domain is the boundary between features");

    // Between features, ui reaches ui only as a routing key: a @Route view named as
    // a .class token, or the *RouteParameters holder that builds its parameters.
    // core is the shell and is reachable from everywhere.
    @ArchTest
    static final ArchRule foreignUiIsOnlyRouteTokensAndRouteParameters =
            classes()
                    .that().resideInAPackage(APP_PACKAGE + ".(*).ui..")
                    .should(dependOnAnotherFeaturesUiOnlyThroughRouting())
                    .because("development.md: cross-feature reach-in goes through domain; "
                            + "another feature's ui is only a routing key");

    // --- Data access (jOOQ) ---

    @ArchTest
    static final ArchRule noJpaNoSpringData =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
                    .because("development.md: jOOQ only, no JPA, no Spring Data");

    @ArchTest
    static final ArchRule noFetchInto =
            noClasses()
                    .should().callMethodWhere(
                            JavaCall.Predicates.target(HasName.Predicates.name("fetchInto")))
                    .because("development.md: use Records.mapping(Type::new) for compile-time column checking");

    @ArchTest
    static final ArchRule domainRecordsAreSerializable =
            classes()
                    .that().resideInAPackage("..domain..")
                    .and(describe("records", JavaClass::isRecord))
                    .should().implement(Serializable.class)
                    .because("development.md: a domain record is the data a view keeps in the session");

    @ArchTest
    static final ArchRule domainRecordsAreValidationFree =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
                    .because("development.md: the Vaadin form is the validation boundary");

    // --- Persistence stereotype ---

    @ArchTest
    static final ArchRule repositoriesAreNamedAndAnnotated =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().beAnnotatedWith(Repository.class)
                    .andShould().resideInAPackage("..domain..")
                    .because("development.md: @Repository enables exception translation");

    @ArchTest
    static final ArchRule repositoriesAreTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().beAnnotatedWith(Transactional.class)
                    .because("development.md: the repository is the transaction boundary");

    // The transaction boundary is the repository, never a view: a view method spans a user
    // interaction, and a transaction that waits for a user is a transaction held far too long.
    @ArchTest
    static final ArchRule viewsAreNotTransactional =
            noClasses()
                    .that().resideInAPackage("..ui..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(Transactional.class.getName())
                    .because("development.md: transactions are declared in domain, not in ui");

    @ArchTest
    static final ArchRule repositoryAnnotationOnlyOnRepositories =
            classes()
                    .that().areAnnotatedWith(Repository.class)
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("development.md: not *Queries, *Dao, *Store");

    // --- Vaadin view conventions ---

    @ArchTest
    static final ArchRule viewsLiveInUi =
            classes()
                    .that().areAnnotatedWith(Route.class)
                    .should().resideInAPackage("..ui..")
                    .andShould().haveSimpleNameEndingWith("View")
                    .because("development.md: one view per use case, in the feature's ui package");

    // The shell is what keeps the drawer and header usable on every screen (NFR-005);
    // a route outside it strands the user on a page without navigation.
    @ArchTest
    static final ArchRule routesRenderInsideTheShell =
            classes()
                    .that().areAnnotatedWith(Route.class)
                    .should(declareRouteLayout(MainLayout.class))
                    .because("development.md: every @Route declares layout = MainLayout.class");

    // A view is session state: Vaadin keeps the component tree in the HTTP session, so
    // every field it holds has to be serializable. A repository wraps a DSLContext and is
    // not — hence transient. Safe only while physical.md rules out session replication.
    @ArchTest
    static final ArchRule repositoryFieldsInViewsAreTransient =
            fields()
                    .that().areDeclaredInClassesThat().resideInAPackage("..ui..")
                    .and().haveRawType(describe("a repository",
                            (JavaClass type) -> type.getSimpleName().endsWith("Repository")))
                    .should().haveModifier(JavaModifier.TRANSIENT)
                    .because("development.md: a view is serializable session state, a repository is not");

    // The field rule above cannot see a repository that a lambda captured straight from
    // a constructor parameter, and neither can ArchUnit: it attributes a lambda body to
    // the constructor and drops the synthetic method. What it can see is a repository
    // that arrives through the constructor and is stored nowhere — the only place left
    // for it to live is a capture.
    @ArchTest
    static final ArchRule injectedRepositoriesAreStoredInAField =
            classes()
                    .that().resideInAPackage("..ui..")
                    .should(storeEveryInjectedRepositoryInAField())
                    .because("development.md: a captured repository ends up in the session "
                            + "exactly like a non-transient field; store it, then read the field");

    // getStyle() is declared on HasStyle, but the call site's target owner is the concrete
    // component (Button, VerticalLayout, ...), so callMethod(HasStyle.class, "getStyle")
    // would never match. Match any getStyle() on a HasStyle subtype instead.
    @ArchTest
    static final ArchRule noInlineStyles =
            noClasses()
                    .should().callMethodWhere(describe("getStyle() on a HasStyle component",
                            (JavaCall<?> call) -> call.getTarget().getName().equals("getStyle")
                                    && assignableTo(HasStyle.class).test(call.getTargetOwner())))
                    .because("development.md: use LumoUtility class names, never getStyle().set()");

    // --- Conditions ---

    private static ArchCondition<JavaClass> dependOnAnotherFeaturesUiOnlyThroughRouting() {
        return new ArchCondition<>("depend on another feature's ui only through @Route views and *RouteParameters") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean foreignUi = resideInAPackage(APP_PACKAGE + "..ui..").test(target)
                            && !feature(target).equals(feature(origin))
                            && !feature(target).equals("core");
                    boolean routing = target.isAnnotatedWith(Route.class)
                            || target.getSimpleName().endsWith("RouteParameters");
                    if (foreignUi && !routing) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** {@code ai.unifiedprocess.petclinic.owner.ui.OwnerForm} → {@code owner}. */
    private static String feature(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(APP_PACKAGE + ".")) {
            return "";
        }
        String rest = packageName.substring(APP_PACKAGE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    private static ArchCondition<JavaClass> storeEveryInjectedRepositoryInAField() {
        return new ArchCondition<>("store every repository taken in a constructor in a field") {
            @Override
            public void check(JavaClass view, ConditionEvents events) {
                Set<JavaClass> stored = view.getFields().stream().map(JavaField::getRawType).collect(toSet());
                for (JavaConstructor constructor : view.getConstructors()) {
                    for (JavaClass parameter : constructor.getRawParameterTypes()) {
                        if (parameter.getSimpleName().endsWith("Repository") && !stored.contains(parameter)) {
                            events.add(SimpleConditionEvent.violated(constructor, view.getSimpleName()
                                    + " takes " + parameter.getSimpleName()
                                    + " in its constructor but has no field for it,"
                                    + " so only a lambda can be holding it"));
                        }
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declareRouteLayout(Class<?> layout) {
        return new ArchCondition<>("declare @Route(layout = " + layout.getSimpleName() + ".class)") {
            @Override
            public void check(JavaClass view, ConditionEvents events) {
                JavaAnnotation<JavaClass> route = view.getAnnotationOfType(Route.class.getName());
                boolean declared = route.get("layout")
                        .map(value -> value instanceof JavaClass declaredLayout
                                && declaredLayout.isEquivalentTo(layout))
                        .orElse(false);
                events.add(new SimpleConditionEvent(view, declared, view.getSimpleName()
                        + (declared ? " declares" : " does not declare")
                        + " layout = " + layout.getSimpleName() + ".class"));
            }
        };
    }
}
