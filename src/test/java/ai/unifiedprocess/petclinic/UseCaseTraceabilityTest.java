package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ai.unifiedprocess.petclinic.SpecDocuments.assertNoViolations;
import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/use_cases/}.
 *
 * <p>{@code docs/} is the source of truth, but until now nothing connected it
 * to the tests: a use case could claim {@code Status: Done} while an
 * alternative flow or a business rule had never been exercised, and a renamed
 * flow in a specification would silently orphan the {@code @UseCase} annotation
 * pointing at it. This test closes that loop in both directions.
 *
 * <p><strong>Referential integrity</strong>, every {@code @UseCase} annotation
 * must point at a use case, a scenario, and business rules that actually exist
 * in {@code docs/use_cases/}. This applies to every specification regardless of
 * status, because a dangling reference is always a defect.
 *
 * <p><strong>Coverage</strong>, a specification whose status is in
 * {@link #CLAIMS_COMPLETE} must additionally have a test behind its main
 * success scenario, behind every alternative flow, and behind every business
 * rule. Specifications with any other status are exempt: this project writes
 * the specification first, so an unimplemented use case is a normal
 * intermediate state, not a violation. The status line is what turns the
 * coverage requirement on, which is precisely why it must not be set by hand
 * without evidence.
 *
 * <p>Failures list every violation at once rather than stopping at the first,
 * so the report reads as a work list.
 */
@Tag("sensor")
class UseCaseTraceabilityTest {

    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    /**
     * Status values that assert the use case is finished, and therefore demand
     * full test coverage. Every other value (Draft, Specified, In Progress, …)
     * only has to satisfy referential integrity.
     */
    private static final Set<String> CLAIMS_COMPLETE = Set.of("Done", "Tested");

    /** Read from the annotation itself so the two cannot drift apart. */
    private static final String MAIN_SCENARIO = mainScenarioDefault();

    private static final Pattern USE_CASE_ID =
            Pattern.compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    private static final Pattern ALTERNATIVE_FLOW =
            Pattern.compile("^### (A\\d+):[ \t]*+(.*)$", Pattern.MULTILINE);
    private static final Pattern BUSINESS_RULE =
            Pattern.compile("^### (BR-\\d{3}):[ \t]*+.+$", Pattern.MULTILINE);
    private static final Pattern BUSINESS_RULE_ID = Pattern.compile("BR-\\d{3}");
    /**
     * {@code UC004FindOwnersByLastNameTest} or, for a use case that needs a
     * browser, {@code UC004FindOwnersByLastNameIT} — the naming rule of
     * testing.md. The suffix picks the Maven phase, not the kind of coverage.
     */
    private static final Pattern USE_CASE_CLASS = Pattern.compile("^UC(\\d{3})\\w*(Test|IT)$");

    private static List<UseCaseSpec> specifications;
    private static Map<String, UseCaseSpec> byId;
    private static List<TestReference> references;

    /** One {@code docs/use_cases/UC-NNN-*.md} file. */
    private record UseCaseSpec(String id, String status, Set<String> alternativeFlows,
                               Set<String> businessRules, Path file) {

        boolean claimsComplete() {
            return CLAIMS_COMPLETE.contains(status);
        }
    }

    /** One {@code @UseCase} annotation on one test method. */
    private record TestReference(String useCaseId, String scenario, List<String> businessRules,
                                 String className, String location) {
    }

    @BeforeAll
    static void loadSpecificationsAndTests() throws IOException {
        specifications = readSpecifications();
        byId = specifications.stream().collect(toMap(UseCaseSpec::id, spec -> spec));
        references = readTestReferences();
    }

    // --- Guard -----------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set, a sensor that reports green because it is blind.
     */
    @Test
    void specificationsAndAnnotationsAreDiscovered() {
        assertFalse(specifications.isEmpty(),
                "No use case specifications found in " + USE_CASE_DIR.toAbsolutePath()
                        + " Tests must run with the project root as working directory");
        assertFalse(references.isEmpty(),
                "No @UseCase annotations found on the test classpath");
    }

    // --- Referential integrity: annotations must point at something real -------

    @Test
    void everyAnnotationPointsAtAKnownUseCase() {
        assertNoViolations("@UseCase annotations referencing an unknown use case",
                references.stream()
                        .filter(reference -> !byId.containsKey(reference.useCaseId()))
                        .map(reference -> reference.location() + " references " + reference.useCaseId()
                                + ", which has no file in " + USE_CASE_DIR));
    }

    @Test
    void everyAnnotatedScenarioExistsInItsUseCase() {
        assertNoViolations("@UseCase annotations referencing an unknown scenario",
                knownReferences()
                        .filter(reference -> !MAIN_SCENARIO.equals(reference.scenario()))
                        .filter(reference -> !byId.get(reference.useCaseId())
                                .alternativeFlows().contains(reference.scenario()))
                        .map(reference -> reference.location() + " declares scenario \"" + reference.scenario()
                                + "\", which is no '### ' heading in "
                                + byId.get(reference.useCaseId()).file()));
    }

    @Test
    void everyAnnotatedBusinessRuleExistsInItsUseCase() {
        List<String> violations = new ArrayList<>();
        knownReferences().forEach(reference -> {
            UseCaseSpec spec = byId.get(reference.useCaseId());
            for (String rule : reference.businessRules()) {
                if (!BUSINESS_RULE_ID.matcher(rule).matches()) {
                    violations.add(reference.location() + " declares business rule \"" + rule
                            + "\", which is not a BR-NNN identifier."
                            + " List several rules as {\"BR-001\", \"BR-002\"}, not as one comma-separated string");
                } else if (!spec.businessRules().contains(rule)) {
                    violations.add(reference.location() + " declares business rule " + rule
                            + ", which is no '### ' heading in " + spec.file());
                }
            }
        });
        assertNoViolations("@UseCase annotations referencing an unknown business rule", violations.stream());
    }

    // --- Placement: the annotation belongs to that use case's own test ---------

    /**
     * {@code @UseCase} is the use-case test's annotation and {@link TestCase} is
     * the journey test's. Without this, a {@code TC<NNN>…IT} could quietly claim
     * one of the five use cases it walks through, which reads as coverage while
     * the alternative flows and business rules of that use case go untested.
     *
     * <p>The class name also has to agree with the id it annotates, so a test
     * copied from a neighbouring use case cannot keep pointing at the old one.
     */
    @Test
    void everyAnnotationSitsOnItsOwnUseCaseTest() {
        List<String> violations = new ArrayList<>();
        references.forEach(reference -> {
            Matcher matcher = USE_CASE_CLASS.matcher(reference.className());
            if (!matcher.matches()) {
                violations.add(reference.location() + " is annotated @UseCase, but "
                        + reference.className() + " is no UC<NNN><Name>Test or UC<NNN><Name>IT class."
                        + " A journey test names the use cases it walks with @TestCase instead");
            } else if (!reference.useCaseId().equals("UC-" + matcher.group(1))) {
                violations.add(reference.location() + " declares " + reference.useCaseId()
                        + ", but its class name says UC-" + matcher.group(1));
            }
        });
        assertNoViolations("@UseCase annotations on the wrong class", violations.stream());
    }

    // --- Coverage: a completed specification must be backed by tests -----------

    @Test
    void completedUseCasesHaveAMainSuccessScenarioTest() {
        assertNoViolations("Use cases claiming completion without a main success scenario test",
                completedSpecifications()
                        .filter(spec -> testedScenarios(spec.id()).noneMatch(MAIN_SCENARIO::equals))
                        .map(spec -> spec.id() + " is '" + spec.status()
                                + "' but no test is annotated @UseCase(id = \"" + spec.id() + "\")"
                                + " for the main success scenario (" + spec.file() + ")"));
    }

    @Test
    void completedUseCasesCoverEveryAlternativeFlow() {
        List<String> violations = new ArrayList<>();
        completedSpecifications().forEach(spec -> {
            Set<String> tested = testedScenarios(spec.id()).collect(toSet());
            spec.alternativeFlows().stream()
                    .filter(flow -> !tested.contains(flow))
                    .forEach(flow -> violations.add(spec.id() + " is '" + spec.status()
                            + "' but alternative flow \"" + flow + "\" has no test."
                            + " Annotate one @UseCase(id = \"" + spec.id() + "\", scenario = \"" + flow + "\")"));
        });
        assertNoViolations("Alternative flows of completed use cases without a test", violations.stream());
    }

    @Test
    void completedUseCasesCoverEveryBusinessRule() {
        List<String> violations = new ArrayList<>();
        completedSpecifications().forEach(spec -> {
            Set<String> tested = references.stream()
                    .filter(reference -> reference.useCaseId().equals(spec.id()))
                    .flatMap(reference -> reference.businessRules().stream())
                    .collect(toSet());
            spec.businessRules().stream()
                    .filter(rule -> !tested.contains(rule))
                    .forEach(rule -> violations.add(spec.id() + " is '" + spec.status()
                            + "' but business rule " + rule + " has no test."
                            + " Annotate one @UseCase(id = \"" + spec.id()
                            + "\", businessRules = \"" + rule + "\")"));
        });
        assertNoViolations("Business rules of completed use cases without a test", violations.stream());
    }

    // --- Reading the two sides -------------------------------------------------

    private static List<UseCaseSpec> readSpecifications() throws IOException {
        List<UseCaseSpec> parsed = new ArrayList<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            parsed.add(parse(file, Files.readString(file)));
        }
        parsed.sort(Comparator.comparing(UseCaseSpec::id));
        return List.copyOf(parsed);
    }

    private static UseCaseSpec parse(Path file, String markdown) {
        return new UseCaseSpec(
                SpecDocuments.require(USE_CASE_ID, markdown, file, "**Use Case ID:** UC-NNN"),
                SpecDocuments.require(SpecDocuments.STATUS, markdown, file, "**Status:** <status>"),
                headings(ALTERNATIVE_FLOW, markdown, matcher -> SpecDocuments.normalize(matcher.group(1) + ": " + matcher.group(2))),
                headings(BUSINESS_RULE, markdown, matcher -> matcher.group(1)),
                file);
    }

    private static List<TestReference> readTestReferences() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("ai.unifiedprocess.petclinic");
        return StreamSupport.stream(classes.spliterator(), false)
                .sorted(Comparator.comparing(JavaClass::getSimpleName))
                .flatMap(javaClass -> javaClass.getMethods().stream()
                        .filter(method -> method.isAnnotatedWith(UseCase.class))
                        .sorted(Comparator.comparing(JavaMethod::getName))
                        .map(method -> {
                            UseCase annotation = method.getAnnotationOfType(UseCase.class);
                            return new TestReference(
                                    annotation.id(),
                                    SpecDocuments.normalize(annotation.scenario()),
                                    List.of(annotation.businessRules()),
                                    javaClass.getSimpleName(),
                                    javaClass.getSimpleName() + "#" + method.getName());
                        }))
                .toList();
    }

    // --- Helpers ---------------------------------------------------------------

    private Stream<TestReference> knownReferences() {
        return references.stream().filter(reference -> byId.containsKey(reference.useCaseId()));
    }

    private Stream<UseCaseSpec> completedSpecifications() {
        return specifications.stream().filter(UseCaseSpec::claimsComplete);
    }

    private Stream<String> testedScenarios(String useCaseId) {
        return references.stream()
                .filter(reference -> reference.useCaseId().equals(useCaseId))
                .map(TestReference::scenario);
    }

    private static Set<String> headings(Pattern pattern, String markdown,
                                        Function<Matcher, String> key) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(markdown);
        while (matcher.find()) {
            found.add(key.apply(matcher));
        }
        return found;
    }

    private static String mainScenarioDefault() {
        try {
            return (String) UseCase.class.getDeclaredMethod("scenario").getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("@UseCase no longer declares scenario()", e);
        }
    }

}
