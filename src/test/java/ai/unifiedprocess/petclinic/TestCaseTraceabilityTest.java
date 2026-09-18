package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ai.unifiedprocess.petclinic.SpecDocuments.assertNoViolations;
import static ai.unifiedprocess.petclinic.SpecDocuments.filesIn;
import static ai.unifiedprocess.petclinic.SpecDocuments.require;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/test_cases/}.
 *
 * <p>{@link UseCaseTraceabilityTest} closes the loop between use cases and the tests
 * that exercise them. Test cases sat outside that loop: a {@code TC-NNN}
 * document could claim {@code Status: Automated} with no journey test behind
 * it, a journey test could outlive the document it automates, and a Flow row
 * could link to a use case that had been renamed away. This sensor closes that
 * second loop.
 *
 * <p><strong>The two annotations.</strong> {@link UseCase} sits on a method,
 * because a use case has many coverage units — the main scenario, each
 * alternative flow, each business rule — spread over many test methods, and only
 * the author knows which method covers which unit. {@link TestCase} sits on the
 * class, because a test case has exactly one coverage unit: the journey. Each
 * belongs to one kind of test, and this sensor and {@link UseCaseTraceabilityTest}
 * hold that line: {@code @UseCase} only on a {@code UC<NNN><Name>Test},
 * {@code @TestCase} only on a {@code TC<NNN><Name>IT}.
 *
 * <p>The annotation does not merely restate the class name, which would be a
 * second place for the id to drift. It carries {@code useCases}, the journey's
 * itinerary, which the class name cannot express — and that list is checked
 * against the {@code [UC-NNN](…)} links in the document's Flow table, in both
 * directions. The id is checked against the class name, so the two cannot
 * disagree either.
 *
 * <p>Failures list every violation at once rather than stopping at the first.
 */
@Tag("sensor")
class TestCaseTraceabilityTest {

    private static final Path TEST_CASE_DIR = Path.of("docs", "test_cases");
    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    /** The vocabulary of the {@code aiup-core:test-case} skill. */
    private static final Set<String> VALID_STATUSES =
            Set.of("Draft", "Reviewed", "Approved", "Automated", "Obsolete");

    /**
     * The status that asserts a journey test exists. Every other value only has
     * to satisfy referential integrity, because this project writes the test
     * case before the test and an unautomated one is a normal intermediate
     * state, not a defect.
     */
    private static final String AUTOMATED = "Automated";

    /** Statuses of {@link UseCaseTraceabilityTest} that assert a use case is finished. */
    private static final Set<String> USE_CASE_CLAIMS_COMPLETE = Set.of("Done", "Tested");

    private static final Pattern TEST_CASE_ID =
            Pattern.compile("^\\*\\*ID:\\*\\*[ \t]*(TC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    private static final Pattern USE_CASE_ID =
            Pattern.compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    /** A Flow row's Use Case column: a markdown link such as {@code [UC-003](../use_cases/UC-003-x.md)}. */
    private static final Pattern USE_CASE_LINK =
            Pattern.compile("\\[(UC-\\d{3})]\\(([^)]+)\\)");
    /** {@code TC001NewOwnerFirstVisitIT} — the journey test naming rule from the testing guidelines. */
    private static final Pattern JOURNEY_CLASS = Pattern.compile("^TC(\\d{3})\\w*IT$");

    private static List<TestCaseSpec> testCases;
    private static Map<String, String> useCaseStatusById;
    private static List<JourneyTest> journeyTests;

    /** One {@code docs/test_cases/TC-NNN-*.md} file. */
    private record TestCaseSpec(String id, String status, Set<UseCaseLink> useCaseLinks, Path file) {

        boolean claimsAutomated() {
            return AUTOMATED.equals(status);
        }
    }

    /** One {@code [UC-NNN](path)} link out of a Flow row. */
    private record UseCaseLink(String useCaseId, String target) {
    }

    /**
     * One journey test on the test classpath: a class named {@code TC<NNN>…IT},
     * a class carrying {@link TestCase}, or — once the checks below pass — both.
     * Either half may be missing, which is precisely what is worth reporting.
     */
    private record JourneyTest(String className, String idFromName, TestCaseAnnotation annotation) {

        /** The annotation wins, so a mismatch is reported once rather than twice. */
        String testCaseId() {
            return annotation != null ? annotation.id() : idFromName;
        }
    }

    /** The {@link TestCase} values of one journey test. */
    private record TestCaseAnnotation(String id, List<String> useCases) {
    }

    @BeforeAll
    static void loadTestCasesAndJourneyTests() throws IOException {
        testCases = readTestCases();
        useCaseStatusById = readUseCaseStatuses();
        journeyTests = readJourneyTests();
    }

    // --- Guard -----------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set — a sensor that reports green because it is blind.
     */
    @Test
    void testCasesAreDiscovered() {
        assertFalse(testCases.isEmpty(),
                "No test case documents found in " + TEST_CASE_DIR.toAbsolutePath()
                        + " — tests must run with the project root as working directory");
        assertFalse(useCaseStatusById.isEmpty(),
                "No use case specifications found in " + USE_CASE_DIR.toAbsolutePath()
                        + ", so the Flow links below could not be resolved");
    }

    // --- Referential integrity: the document must point at things that exist ---

    @Test
    void everyTestCaseDeclaresAKnownStatus() {
        assertNoViolations("Test cases with an unknown status",
                testCases.stream()
                        .filter(testCase -> !VALID_STATUSES.contains(testCase.status()))
                        .map(testCase -> testCase.file() + " declares '**Status:** " + testCase.status()
                                + "', which is not one of " + sorted(VALID_STATUSES)));
    }

    @Test
    void everyUseCaseLinkedFromAFlowResolves() {
        List<String> violations = new ArrayList<>();
        testCases.forEach(testCase -> testCase.useCaseLinks().forEach(link -> {
            Path target = testCase.file().getParent().resolve(link.target()).normalize();
            if (!Files.isRegularFile(target)) {
                violations.add(testCase.id() + " links " + link.useCaseId() + " to " + link.target()
                        + ", which resolves to no file (" + target + ")");
            } else if (!useCaseStatusById.containsKey(link.useCaseId())) {
                violations.add(testCase.id() + " links " + link.useCaseId()
                        + ", which has no specification in " + USE_CASE_DIR);
            } else if (!target.getFileName().toString().startsWith(link.useCaseId() + "-")) {
                violations.add(testCase.id() + " labels the link to " + target.getFileName()
                        + " as " + link.useCaseId() + ", which is a different use case");
            }
        }));
        assertNoViolations("Flow rows linking a use case that does not exist", violations.stream());
    }

    // --- The two links, checked in both directions -----------------------------

    @Test
    void automatedTestCasesHaveAJourneyTest() {
        Set<String> automated = journeyTests.stream().map(JourneyTest::testCaseId).collect(toSet());
        assertNoViolations("Test cases claiming automation without a journey test",
                testCases.stream()
                        .filter(TestCaseSpec::claimsAutomated)
                        .filter(testCase -> !automated.contains(testCase.id()))
                        .map(testCase -> testCase.id() + " is '" + testCase.status()
                                + "' but no test class is named TC" + digits(testCase.id())
                                + "<Name>IT (" + testCase.file() + ")"));
    }

    @Test
    void everyJourneyTestHasATestCase() {
        Set<String> known = testCases.stream().map(TestCaseSpec::id).collect(toSet());
        assertNoViolations("Journey tests without a test case document",
                journeyTests.stream()
                        .filter(journey -> !known.contains(journey.testCaseId()))
                        .map(journey -> journey.className() + " names " + journey.testCaseId()
                                + ", which has no file in " + TEST_CASE_DIR));
    }

    @Test
    void everyJourneyTestCarriesTheAnnotation() {
        assertNoViolations("Journey tests without a @TestCase annotation",
                journeyTests.stream()
                        .filter(journey -> journey.idFromName() != null && journey.annotation() == null)
                        .map(journey -> journey.className() + " has no @TestCase annotation."
                                + " Add @TestCase(id = \"" + journey.idFromName()
                                + "\", useCases = {…}) naming the use cases its Flow walks through"));
    }

    /**
     * The mirror of {@link UseCaseTraceabilityTest}'s placement rule. A class
     * that claims to automate a journey but is not named like one would never be
     * found by the class-name half of this sensor.
     */
    @Test
    void onlyJourneyTestsCarryTheAnnotation() {
        assertNoViolations("@TestCase on a class that is no journey test",
                journeyTests.stream()
                        .filter(journey -> journey.idFromName() == null)
                        .map(journey -> journey.className() + " carries @TestCase(id = \""
                                + journey.annotation().id() + "\") but is no TC<NNN><Name>IT class."
                                + " Rename it TC" + digits(journey.annotation().id()) + "<Name>IT,"
                                + " or annotate its methods @UseCase if it verifies a single use case"));
    }

    @Test
    void theAnnotatedIdAgreesWithTheClassName() {
        assertNoViolations("Journey tests whose @TestCase id contradicts their class name",
                journeyTests.stream()
                        .filter(journey -> journey.idFromName() != null && journey.annotation() != null)
                        .filter(journey -> !journey.idFromName().equals(journey.annotation().id()))
                        .map(journey -> journey.className() + " declares @TestCase(id = \""
                                + journey.annotation().id() + "\"), but its class name says "
                                + journey.idFromName()));
    }

    @Test
    void everyAnnotatedUseCaseExists() {
        List<String> violations = new ArrayList<>();
        annotatedJourneyTests().forEach(journey -> journey.annotation().useCases().stream()
                .filter(useCaseId -> !useCaseStatusById.containsKey(useCaseId))
                .forEach(useCaseId -> violations.add(journey.className() + " declares use case "
                        + useCaseId + ", which has no specification in " + USE_CASE_DIR)));
        assertNoViolations("@TestCase annotations naming an unknown use case", violations.stream());
    }

    /**
     * The itinerary is the one thing the class name cannot carry, so it is the
     * one thing worth checking: the annotation and the Flow table must name the
     * same use cases, or the test and the document have drifted apart.
     */
    @Test
    void theAnnotatedUseCasesMatchTheFlow() {
        List<String> violations = new ArrayList<>();
        annotatedJourneyTests().forEach(journey -> {
            TestCaseSpec spec = specFor(journey.testCaseId());
            if (spec == null) {
                return; // everyJourneyTestHasATestCase already reports this
            }
            Set<String> annotated = new LinkedHashSet<>(journey.annotation().useCases());
            Set<String> inFlow = new LinkedHashSet<>();
            spec.useCaseLinks().forEach(link -> inFlow.add(link.useCaseId()));
            annotated.stream()
                    .filter(useCaseId -> !inFlow.contains(useCaseId))
                    .forEach(useCaseId -> violations.add(journey.className() + " declares use case "
                            + useCaseId + ", which no Flow row of " + spec.file() + " links"));
            inFlow.stream()
                    .filter(useCaseId -> !annotated.contains(useCaseId))
                    .forEach(useCaseId -> violations.add(spec.file() + " walks " + useCaseId
                            + " in its Flow, but " + journey.className()
                            + " does not name it in @TestCase(useCases = …)"));
        });
        assertNoViolations("@TestCase itineraries disagreeing with the Flow table", violations.stream());
    }

    // --- Cross-layer consistency ------------------------------------------------

    /**
     * An automated journey walks through its Flow's use cases and passes, so
     * those use cases are demonstrably finished. A use case still sitting at
     * Draft underneath a green end-to-end test means one of the two status lines
     * is lying.
     */
    @Test
    void automatedTestCasesOnlyWalkCompletedUseCases() {
        List<String> violations = new ArrayList<>();
        testCases.stream().filter(TestCaseSpec::claimsAutomated).forEach(testCase ->
                testCase.useCaseLinks().stream()
                        .map(UseCaseLink::useCaseId)
                        .distinct()
                        .filter(useCaseStatusById::containsKey)
                        .filter(useCaseId -> !USE_CASE_CLAIMS_COMPLETE.contains(useCaseStatusById.get(useCaseId)))
                        .forEach(useCaseId -> violations.add(testCase.id() + " is '" + AUTOMATED
                                + "' but the use case it walks through, " + useCaseId + ", is '"
                                + useCaseStatusById.get(useCaseId) + "' — one of the two statuses is wrong")));
        assertNoViolations("Automated test cases resting on unfinished use cases", violations.stream());
    }

    // --- Reading the two sides -------------------------------------------------

    private static List<TestCaseSpec> readTestCases() throws IOException {
        List<TestCaseSpec> parsed = new ArrayList<>();
        for (Path file : filesIn(TEST_CASE_DIR, "TC-")) {
            String markdown = Files.readString(file);
            parsed.add(new TestCaseSpec(
                    require(TEST_CASE_ID, markdown, file, "**ID:** TC-NNN"),
                    require(SpecDocuments.STATUS, markdown, file, "**Status:** <status>"),
                    useCaseLinks(markdown),
                    file));
        }
        return List.copyOf(parsed);
    }

    private static Set<UseCaseLink> useCaseLinks(String markdown) {
        Set<UseCaseLink> links = new LinkedHashSet<>();
        Matcher matcher = USE_CASE_LINK.matcher(markdown);
        while (matcher.find()) {
            links.add(new UseCaseLink(matcher.group(1), matcher.group(2).trim()));
        }
        return links;
    }

    /** Only the id and the status — the rest of a use case is {@link UseCaseTraceabilityTest}'s business. */
    private static Map<String, String> readUseCaseStatuses() throws IOException {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (Path file : filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            statuses.put(
                    require(USE_CASE_ID, markdown, file, "**Use Case ID:** UC-NNN"),
                    require(SpecDocuments.STATUS, markdown, file, "**Status:** <status>"));
        }
        return Map.copyOf(statuses);
    }

    /**
     * Picks up a class on either signal — the name or the annotation — so a
     * class that carries only one of the two still reaches the checks above
     * instead of silently falling out of the sensor's view.
     */
    private static List<JourneyTest> readJourneyTests() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("ai.unifiedprocess.petclinic");
        return StreamSupport.stream(classes.spliterator(), false)
                .sorted(Comparator.comparing(JavaClass::getSimpleName))
                .flatMap(javaClass -> {
                    Matcher matcher = JOURNEY_CLASS.matcher(javaClass.getSimpleName());
                    String idFromName = matcher.matches() ? "TC-" + matcher.group(1) : null;
                    TestCaseAnnotation annotation = annotationOf(javaClass);
                    return idFromName == null && annotation == null
                            ? Stream.empty()
                            : Stream.of(new JourneyTest(javaClass.getSimpleName(), idFromName, annotation));
                })
                .toList();
    }

    private static TestCaseAnnotation annotationOf(JavaClass javaClass) {
        if (!javaClass.isAnnotatedWith(TestCase.class)) {
            return null;
        }
        TestCase annotation = javaClass.getAnnotationOfType(TestCase.class);
        return new TestCaseAnnotation(annotation.id(), List.of(annotation.useCases()));
    }

    // --- Helpers ---------------------------------------------------------------

    private Stream<JourneyTest> annotatedJourneyTests() {
        return journeyTests.stream().filter(journey -> journey.annotation() != null);
    }

    private static TestCaseSpec specFor(String testCaseId) {
        return testCases.stream()
                .filter(testCase -> testCase.id().equals(testCaseId))
                .findFirst()
                .orElse(null);
    }

    /** {@code TC-001} → {@code 001}, for the message that names the expected class. */
    private static String digits(String testCaseId) {
        return testCaseId.substring("TC-".length());
    }

    private static String sorted(Set<String> values) {
        return values.stream().sorted().toList().toString();
    }
}
