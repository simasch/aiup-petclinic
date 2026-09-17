package ai.unifiedprocess.petclinic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the journey test that automates a {@code docs/test_cases/TC-NNN-*.md}
 * document, and names the use cases the journey walks through.
 *
 * <p>A test case has one coverage unit, the journey, so this sits on the class,
 * where {@link UseCase} sits on a method. {@code useCases} is what the class
 * name cannot express: {@code TestCaseTraceabilityTest} compares it with the
 * {@code [UC-NNN](…)} links in the document's Flow table, in both directions,
 * so the annotation cannot drift away from the specification it automates.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestCase {

	String id();

	String[] useCases();

}
