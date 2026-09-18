package ai.unifiedprocess.petclinic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ai.unifiedprocess.petclinic.SpecDocuments.assertNoViolations;
import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/business_rules.md}.
 *
 * <p>A rule several use cases share is stated once in the catalogue as
 * {@code GR-NNN}; each use case keeps its own {@code BR-NNN} heading and says
 * which shared rule it realizes instead of restating it. That saves the rule
 * from being written down twice and drifting — but only while the two sides
 * still point at each other. Nothing in markdown enforces that: a renamed rule
 * orphans every anchor pointing at it, a deleted use case leaves a rule nobody
 * realizes, and a new reference is invisible in the catalogue's
 * {@code **Realized by:**} line. This sensor closes that loop, the way
 * {@link UseCaseTraceabilityTest} closes the loop between specifications and
 * tests.
 *
 * <p>What it checks:
 *
 * <ul>
 *   <li><strong>Referential integrity</strong> — every {@code GR-NNN} a use
 *       case references exists in the catalogue, and every link into the
 *       catalogue from anywhere under {@code docs/} lands on a heading that is
 *       really there.</li>
 *   <li><strong>Both directions agree</strong> — the {@code **Realized by:**}
 *       line of a rule names exactly the use case rules that reference it,
 *       no more and no fewer.</li>
 *   <li><strong>The summary table matches the rules below it</strong>, so the
 *       one place in the catalogue that repeats itself cannot lie.</li>
 *   <li><strong>A shared rule is actually shared</strong> — a rule realized by
 *       a single use case belongs in that use case, not in the catalogue.</li>
 * </ul>
 *
 * <p>Failures list every violation at once rather than stopping at the first,
 * so the report reads as a work list.
 */
@Tag("sensor")
class BusinessRuleTraceabilityTest {

    private static final Path DOCS_DIR = Path.of("docs");
    private static final Path CATALOGUE = DOCS_DIR.resolve("business_rules.md");
    private static final Path USE_CASE_DIR = DOCS_DIR.resolve("use_cases");

    /** {@code ## GR-001: Anonymous Access} — one shared rule in the catalogue. */
    private static final Pattern CATALOGUE_RULE =
            Pattern.compile("^## (GR-\\d{3}):[ \t]*+(.*)$", Pattern.MULTILINE);
    /** {@code | GR-001 | Anonymous Access | UC-001 BR-001, … | C-010 |} — a row of the summary table. */
    private static final Pattern CATALOGUE_TABLE_ROW =
            Pattern.compile("^\\|[ \t]*(GR-\\d{3})[ \t]*\\|([^|]*)\\|([^|]*)\\|", Pattern.MULTILINE);
    /** {@code **Realized by:** UC-007 BR-001, UC-008 BR-001} */
    private static final Pattern REALIZED_BY =
            Pattern.compile("^\\*\\*Realized by:\\*\\*[ \t]*+(.*)$", Pattern.MULTILINE);
    /** A {@code UC-NNN BR-NNN} pair, wherever it appears in prose or a table cell. */
    private static final Pattern REALIZATION = Pattern.compile("(UC-\\d{3})[ \t]+(BR-\\d{3})");

    /** {@code ### BR-001: Unique Pet Name per Owner} — one rule of one use case. */
    private static final Pattern USE_CASE_RULE =
            Pattern.compile("^### (BR-\\d{3}):[ \t]*+.+$", Pattern.MULTILINE);
    private static final Pattern USE_CASE_ID =
            Pattern.compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    /** A markdown link into the catalogue, capturing the {@code #anchor} it aims at. */
    private static final Pattern CATALOGUE_LINK =
            Pattern.compile("\\]\\((?:\\.{1,2}/)*business_rules\\.md#([^)]*)\\)");
    private static final Pattern SHARED_RULE_ID = Pattern.compile("GR-\\d{3}");

    private static List<SharedRule> catalogue;
    private static Map<String, SharedRule> byId;
    private static List<Reference> references;

    /** One {@code ## GR-NNN} section of {@code docs/business_rules.md}. */
    private record SharedRule(String id, String title, String anchor, Set<String> claimedRealizations) {
    }

    /** One {@code GR-NNN} reference from one business rule of one use case. */
    private record Reference(String useCaseId, String businessRuleId, String sharedRuleId, Path file) {

        String realization() {
            return useCaseId + " " + businessRuleId;
        }

        String location() {
            return useCaseId + " " + businessRuleId + " (" + file + ")";
        }
    }

    @BeforeAll
    static void loadCatalogueAndReferences() throws IOException {
        catalogue = readCatalogue();
        byId = catalogue.stream().collect(LinkedHashMap::new,
                (map, rule) -> map.put(rule.id(), rule), Map::putAll);
        references = readReferences();
    }

    // --- Guard -----------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set, a sensor that reports green because it is blind.
     */
    @Test
    void catalogueAndReferencesAreDiscovered() {
        assertFalse(catalogue.isEmpty(),
                "No GR-NNN rules found in " + CATALOGUE.toAbsolutePath()
                        + " Tests must run with the project root as working directory");
        assertFalse(references.isEmpty(),
                "No use case references a shared rule. Either " + USE_CASE_DIR
                        + " was not read, or the catalogue is not referenced at all");
    }

    // --- Referential integrity -------------------------------------------------

    @Test
    void everyReferencedRuleExistsInTheCatalogue() {
        assertNoViolations("Use case rules referencing an unknown shared rule",
                references.stream()
                        .filter(reference -> !byId.containsKey(reference.sharedRuleId()))
                        .map(reference -> reference.location() + " realizes " + reference.sharedRuleId()
                                + ", which is no '## ' heading in " + CATALOGUE));
    }

    /**
     * The id alone would survive a renamed rule; the anchor would not. Checking
     * the link target as well means a rule cannot be given a better name without
     * the documents that point at it following along.
     */
    @Test
    void everyLinkIntoTheCatalogueResolves() throws IOException {
        Set<String> anchors = catalogue.stream().map(SharedRule::anchor).collect(toCollection(LinkedHashSet::new));
        List<String> violations = new ArrayList<>();
        for (Path file : markdownUnderDocs()) {
            Matcher matcher = CATALOGUE_LINK.matcher(Files.readString(file));
            while (matcher.find()) {
                String anchor = matcher.group(1);
                if (!anchors.contains(anchor)) {
                    violations.add(file + " links to business_rules.md#" + anchor
                            + ", which matches no rule heading. Known anchors: " + String.join(", ", anchors));
                }
            }
        }
        assertNoViolations("Links into the business rule catalogue that do not resolve", violations.stream());
    }

    // --- Both directions agree -------------------------------------------------

    @Test
    void everyRuleIsRealizedByExactlyWhatItClaims() {
        List<String> violations = new ArrayList<>();
        catalogue.forEach(rule -> {
            Set<String> actual = realizationsOf(rule.id());
            rule.claimedRealizations().stream()
                    .filter(claimed -> !actual.contains(claimed))
                    .forEach(claimed -> violations.add(rule.id() + " claims to be realized by " + claimed
                            + ", but that business rule does not reference " + rule.id()));
            actual.stream()
                    .filter(found -> !rule.claimedRealizations().contains(found))
                    .forEach(found -> violations.add(found + " references " + rule.id()
                            + ", but the '**Realized by:**' line of " + rule.id() + " does not list it"));
        });
        assertNoViolations("Shared rules whose '**Realized by:**' line disagrees with the use cases",
                violations.stream());
    }

    /**
     * The summary table at the top of the catalogue repeats what the rules below
     * it say. That is the one duplication the document keeps, because a reader
     * wants the overview — so it is also the one that has to be checked.
     */
    @Test
    void theSummaryTableMatchesTheRulesBelowIt() throws IOException {
        String markdown = Files.readString(CATALOGUE);
        List<String> violations = new ArrayList<>();
        Set<String> listed = new LinkedHashSet<>();

        Matcher row = CATALOGUE_TABLE_ROW.matcher(markdown);
        while (row.find()) {
            String id = row.group(1);
            listed.add(id);
            SharedRule rule = byId.get(id);
            if (rule == null) {
                violations.add("The summary table lists " + id + ", which has no '## " + id + ": …' section");
                continue;
            }
            String title = row.group(2).trim();
            if (!title.equals(rule.title())) {
                violations.add(id + " is \"" + title + "\" in the summary table but \""
                        + rule.title() + "\" in its own heading");
            }
            Set<String> inRow = realizationsIn(row.group(3));
            if (!inRow.equals(rule.claimedRealizations())) {
                violations.add(id + " is realized by " + join(inRow) + " in the summary table but by "
                        + join(rule.claimedRealizations()) + " in its '**Realized by:**' line");
            }
        }

        catalogue.stream()
                .map(SharedRule::id)
                .filter(id -> !listed.contains(id))
                .forEach(id -> violations.add(id + " has a section but no row in the summary table"));

        assertNoViolations("Rows of the summary table disagreeing with the rules below it", violations.stream());
    }

    // --- A shared rule is actually shared --------------------------------------

    /**
     * The catalogue holds rules <em>several</em> use cases must honour. One with
     * a single realization has no reason to be here: it belongs in the use case
     * that needs it, where it is read together with the flow it constrains.
     */
    @Test
    void everySharedRuleIsSharedByAtLeastTwoUseCases() {
        List<String> violations = new ArrayList<>();
        catalogue.forEach(rule -> {
            Set<String> useCases = realizationsOf(rule.id()).stream()
                    .map(realization -> realization.substring(0, "UC-000".length()))
                    .collect(toCollection(LinkedHashSet::new));
            if (useCases.isEmpty()) {
                violations.add(rule.id() + " is realized by no use case at all."
                        + " Either a use case should reference it, or it does not belong in " + CATALOGUE);
            } else if (useCases.size() == 1) {
                violations.add(rule.id() + " is realized only by " + useCases.iterator().next()
                        + ". A rule one use case needs belongs in that use case, not in " + CATALOGUE);
            }
        });
        assertNoViolations("Rules in the catalogue that no second use case shares", violations.stream());
    }

    // --- Reading the two sides -------------------------------------------------

    private static List<SharedRule> readCatalogue() throws IOException {
        if (!Files.isRegularFile(CATALOGUE)) {
            return List.of();
        }
        String markdown = Files.readString(CATALOGUE);
        List<SharedRule> parsed = new ArrayList<>();
        Matcher matcher = CATALOGUE_RULE.matcher(markdown);
        int sectionStart = -1;
        String id = null;
        String title = null;
        while (matcher.find()) {
            if (id != null) {
                parsed.add(rule(id, title, markdown.substring(sectionStart, matcher.start())));
            }
            id = matcher.group(1);
            title = matcher.group(2).trim();
            sectionStart = matcher.end();
        }
        if (id != null) {
            parsed.add(rule(id, title, markdown.substring(sectionStart)));
        }
        return List.copyOf(parsed);
    }

    private static SharedRule rule(String id, String title, String body) {
        Matcher realizedBy = REALIZED_BY.matcher(body);
        Set<String> claimed = realizedBy.find() ? realizationsIn(realizedBy.group(1)) : Set.of();
        return new SharedRule(id, title, anchorOf(id + ": " + title), claimed);
    }

    private static List<Reference> readReferences() throws IOException {
        List<Reference> found = new ArrayList<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            String useCaseId = SpecDocuments.require(USE_CASE_ID, markdown, file, "**Use Case ID:** UC-NNN");
            Matcher heading = USE_CASE_RULE.matcher(markdown);
            int bodyStart = -1;
            String businessRuleId = null;
            while (heading.find()) {
                if (businessRuleId != null) {
                    collect(found, useCaseId, businessRuleId, markdown.substring(bodyStart, heading.start()), file);
                }
                businessRuleId = heading.group(1);
                bodyStart = heading.end();
            }
            if (businessRuleId != null) {
                collect(found, useCaseId, businessRuleId, markdown.substring(bodyStart), file);
            }
        }
        return List.copyOf(found);
    }

    private static void collect(List<Reference> found, String useCaseId, String businessRuleId,
                                String body, Path file) {
        // A business rule section ends at the next '### ' heading, so a body may
        // run into the following section of the document. Only what precedes the
        // next heading of any level belongs to this rule.
        String scoped = body.split("(?m)^#{1,6} ", 2)[0];
        Matcher matcher = SHARED_RULE_ID.matcher(scoped);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            if (seen.add(matcher.group())) {
                found.add(new Reference(useCaseId, businessRuleId, matcher.group(), file));
            }
        }
    }

    // --- Helpers ---------------------------------------------------------------

    private Set<String> realizationsOf(String sharedRuleId) {
        return references.stream()
                .filter(reference -> reference.sharedRuleId().equals(sharedRuleId))
                .map(Reference::realization)
                .collect(toCollection(LinkedHashSet::new));
    }

    private static Set<String> realizationsIn(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = REALIZATION.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1) + " " + matcher.group(2));
        }
        return found;
    }

    private static List<Path> markdownUnderDocs() throws IOException {
        try (Stream<Path> files = Files.walk(DOCS_DIR)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .toList();
        }
    }

    /** The GitHub anchor of a heading: lower case, punctuation dropped, spaces to hyphens. */
    private static String anchorOf(String heading) {
        return heading.toLowerCase()
                .replaceAll("[^a-z0-9 -]", "")
                .replace(' ', '-');
    }

    private static String join(Set<String> realizations) {
        return realizations.isEmpty() ? "nothing" : String.join(", ", realizations);
    }

}
