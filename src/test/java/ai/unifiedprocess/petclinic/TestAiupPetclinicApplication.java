package ai.unifiedprocess.petclinic;

import org.springframework.boot.SpringApplication;

/**
 * Local development entry point for {@code ./mvnw spring-boot:test-run}:
 * the application with a Testcontainers-backed PostgreSQL. Opening the
 * browser is a convenience of this entry point only, so the property is set
 * here rather than in {@code application.properties}, where it would apply
 * to a production start as well.
 */
public class TestAiupPetclinicApplication {

    public static void main(String[] args) {
        SpringApplication.from(AiupPetclinicApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(withLaunchBrowser(args));
    }

    private static String[] withLaunchBrowser(String[] args) {
        String[] withDefault = new String[args.length + 1];
        withDefault[0] = "--vaadin.launch-browser=true";
        System.arraycopy(args, 0, withDefault, 1, args.length);
        return withDefault;
    }

}
