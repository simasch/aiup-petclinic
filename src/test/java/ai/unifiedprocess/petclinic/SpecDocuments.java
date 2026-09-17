package ai.unifiedprocess.petclinic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The bits of markdown handling that both traceability sensors share.
 *
 * <p>{@link UseCaseTraceabilityTest} reads {@code docs/use_cases/} and
 * {@link TestCaseTraceabilityTest} reads {@code docs/test_cases/}, but the two
 * document families share an Overview format and both report their findings as
 * one work list. Keeping the {@code **Status:**} pattern and the reporting
 * idiom here means a change to either is made once, rather than made twice and
 * eventually only once.
 */
final class SpecDocuments {

    /** Every AI Unified Process document carries this line in its Overview. */
    static final Pattern STATUS =
            Pattern.compile("^\\*\\*Status:\\*\\*[ \t]*(.+?)[ \t]*$", Pattern.MULTILINE);

    private SpecDocuments() {
    }

    /**
     * The specification files in {@code directory}, sorted by file name. Only
     * {@code <prefix>*.md} counts, so a README or an index page living beside
     * them is skipped instead of failing the parser. A missing directory yields
     * an empty list; the callers' guard tests turn that into a failure.
     */
    static List<Path> filesIn(Path directory, String prefix) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> {
                        String name = file.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".md");
                    })
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }
    }

    /** The first capture of {@code pattern}, or a failure naming the line that is missing. */
    static String require(Pattern pattern, String markdown, Path file, String expected) {
        Matcher matcher = pattern.matcher(markdown);
        if (!matcher.find()) {
            throw new IllegalStateException(file + " has no '" + expected + "' line in its Overview section");
        }
        return matcher.group(1).trim();
    }

    /** Collapses the whitespace that markdown hard breaks leave behind. */
    static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Fails with every violation at once rather than stopping at the first, so
     * the report reads as a work list.
     */
    static void assertNoViolations(String headline, Stream<String> violations) {
        List<String> found = violations.toList();
        if (!found.isEmpty()) {
            fail(headline + " (" + found.size() + "):" + System.lineSeparator()
                    + found.stream().collect(joining(System.lineSeparator() + "  ", "  ", "")));
        }
    }
}
