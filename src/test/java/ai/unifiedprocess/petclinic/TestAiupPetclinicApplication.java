package ai.unifiedprocess.petclinic;

import org.springframework.boot.SpringApplication;

public class TestAiupPetclinicApplication {

    public static void main(String[] args) {
        SpringApplication.from(AiupPetclinicApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
