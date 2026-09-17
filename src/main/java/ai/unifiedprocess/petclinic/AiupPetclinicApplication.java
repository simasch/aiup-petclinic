package ai.unifiedprocess.petclinic;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@SpringBootApplication
public class AiupPetclinicApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(AiupPetclinicApplication.class, args);
    }

}
